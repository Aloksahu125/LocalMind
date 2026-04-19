package com.example.localmind

object LlamaBridge {
    init {
        System.loadLibrary("llama_bridge")
    }
    // Ensure the parameters match what you call in MainActivity
    external fun generate(prompt: String, modelPath: String): String
}