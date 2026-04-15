package com.example.localmind

object LlamaBridge {
    init {
        // This one library now contains ggml-base, ggml-cpu, ggml, and llama
        System.loadLibrary("llama_bridge")
    }

    external fun generate(prompt: String, modelPath: String): String
}