package com.example.localmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Message(val text: String, val isUser: Boolean)

val BackgroundBeige = Color(0xFFF5F0E6)
val UserBubbleColor = Color(0xFFE8DACE)
val BotTextColor = Color(0xFF4A453E)
val AccentOrange = Color(0xFFC67C4E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LOCAL MIND", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundBeige)
            )
        },
        bottomBar = { BottomNavigationBar() },
        containerColor = BackgroundBeige,
        // FIX 1: Tell Scaffold to account for the keyboard (IME) insets
        contentWindowInsets = WindowInsets.ime
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // FIX 2: This modifier ensures the Column content stays above the keyboard
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            LazyColumn(
                // weight(1f) is critical: it makes the list "stretchy" so it
                // shrinks when the keyboard takes up screen space.
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Me....") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                FloatingActionButton(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            val prompt = userInput
                            messages = messages + Message(prompt, true)
                            messages = messages + Message("Thinking...", false)
                            val botIndex = messages.size - 1
                            userInput = ""

                            scope.launch {
                                val modelPath = "${context.filesDir.absolutePath}/models/gemma.gguf"
                                val response = withContext(Dispatchers.IO) {
                                    try { LlamaBridge.generate(prompt, modelPath) }
                                    catch (e: Exception) { "Error: ${e.message}" }
                                }
                                withContext(Dispatchers.Main) {
                                    val newList = messages.toMutableList()
                                    if (botIndex < newList.size) {
                                        newList[botIndex] = Message(response, false)
                                        messages = newList
                                    }
                                }
                            }
                        }
                    },
                    containerColor = AccentOrange,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: Message) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (msg.isUser) UserBubbleColor else Color.Transparent,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 0.dp,
                bottomEnd = if (msg.isUser) 0.dp else 16.dp
            )
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(12.dp).widthIn(max = 280.dp),
                color = BotTextColor,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar() {
    NavigationBar(containerColor = Color.White) {
        val items = listOf(
            NavigationItem("Home", Icons.Default.Home, false),
            NavigationItem("Quiz", Icons.Default.Book, false),
            NavigationItem("Chat", Icons.Default.Chat, true),
            NavigationItem("Courses", Icons.Default.MenuBook, false)
        )
        items.forEach { item ->
            NavigationBarItem(
                selected = item.isSelected,
                onClick = { },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentOrange,
                    selectedTextColor = AccentOrange,
                    unselectedIconColor = Color.Gray
                )
            )
        }
    }
}

data class NavigationItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isSelected: Boolean)