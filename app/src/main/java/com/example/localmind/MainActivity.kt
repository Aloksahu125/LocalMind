package com.example.localmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Message(val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatScreen()
        }
    }
}

@Composable
fun ChatScreen() {
    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            items(messages) { msg ->
                Text(
                    text = if (msg.isUser) "You: ${msg.text}" else "Bot: ${msg.text}",
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // Auto-scroll to latest message
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (userInput.isNotEmpty()) {
                    val prompt = userInput

                    // Add user message
                    messages = messages + Message(prompt, true)

                    // Add placeholder bot message
                    messages = messages + Message("Typing...", false)
                    val botIndex = messages.size - 1

                    userInput = ""

                    // Background processing
                    scope.launch {
                        delay(500) // brief delay for UX

                        val modelPath = context.filesDir.absolutePath + "/models/gemma.gguf"
                        
                        // FIX: Run on IO thread to prevent UI freeze
                        val response = withContext(Dispatchers.IO) {
                            try {
                                LlamaBridge.generate(prompt, modelPath)
                            } catch (e: Exception) {
                                "Error: ${e.message}"
                            }
                        }

                        // Replace placeholder instead of appending
                        messages = messages.toMutableList().also {
                            it[botIndex] = Message(response, false)
                        }
                    }
                }
            }) {
                Text("Send")
            }
        }
    }
}