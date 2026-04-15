#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include "llama.h"
#include <android/log.h>

#define TAG "LocalMind_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// Wrap in extern "C" to ensure the symbols are visible to JNI
extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_localmind_LlamaBridge_generate(
        JNIEnv *env,
        jobject /* this */,
        jstring prompt_,
        jstring model_path_) {

    const char *prompt = env->GetStringUTFChars(prompt_, 0);
    const char *model_path = env->GetStringUTFChars(model_path_, 0);

    LOGI("Attempting to load model from: %s", model_path);

    // 1. Load Model
    llama_model_params model_params = llama_model_default_params();
    llama_model *model = llama_load_model_from_file(model_path, model_params);

    if (!model) {
        LOGI("❌ Failed to load model");
        env->ReleaseStringUTFChars(prompt_, prompt);
        env->ReleaseStringUTFChars(model_path_, model_path);
        return env->NewStringUTF("❌ Failed to load model - Check path/permissions");
    }

    // 2. Setup Context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 512;
    ctx_params.n_threads = 4; // Optimized for the Snapdragon 7s Gen 2
    llama_context *ctx = llama_new_context_with_model(model, ctx_params);

    if (!ctx) {
        LOGI("❌ Failed to create context");
        llama_free_model(model);
        env->ReleaseStringUTFChars(prompt_, prompt);
        env->ReleaseStringUTFChars(model_path_, model_path);
        return env->NewStringUTF("❌ Failed to create context");
    }

    // 3. Tokenize
    const struct llama_vocab * vocab = llama_model_get_vocab(model);
    std::vector<llama_token> tokens(strlen(prompt) + 2);
    int n_tokens = llama_tokenize(vocab, prompt, (int)strlen(prompt), tokens.data(), (int)tokens.size(), true, false);
    tokens.resize(n_tokens);

    // 4. Batch & Decode Prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
    if (llama_decode(ctx, batch) != 0) {
        llama_free(ctx);
        llama_free_model(model);
        env->ReleaseStringUTFChars(prompt_, prompt);
        env->ReleaseStringUTFChars(model_path_, model_path);
        return env->NewStringUTF("❌ Decode failed");
    }

    // 5. Setup Sampler (The "Brain")
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.8f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(1234)); // Static seed for testing

    std::string result = "";
    llama_token curr_token;

    // 6. Generation Loop
    for (int i = 0; i < 64; i++) {
        curr_token = llama_sampler_sample(smpl, ctx, -1);

        // Check for End of Generation
        if (llama_token_is_eog(vocab, curr_token)) break;

        // Convert token to string piece
        char buf[128];
        int n = llama_token_to_piece(vocab, curr_token, buf, sizeof(buf), 0, true);
        if (n > 0) {
            result.append(buf, n);
        }

        // Prepare for next token
        batch = llama_batch_get_one(&curr_token, 1);
        if (llama_decode(ctx, batch) != 0) break;
    }

    // FINAL CLEANUP
    llama_sampler_free(smpl);
    llama_free(ctx);
    llama_free_model(model);
    env->ReleaseStringUTFChars(prompt_, prompt);
    env->ReleaseStringUTFChars(model_path_, model_path);

    // If result is empty, return a fallback message
    if (result.empty()) {
        return env->NewStringUTF("Model generated an empty response.");
    }

    return env->NewStringUTF(result.c_str());
}

} // End of extern "C"