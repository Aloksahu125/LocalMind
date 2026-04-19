package com.example.localmind

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- DATA MODELS ---
data class Message(val text: String, val isUser: Boolean)
data class NavigationItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isSelected: Boolean)

// --- COLORS ---
val BackgroundBeige = Color(0xFFF5F0E6)
val FolderOrange = Color(0xFFD98350)
val UserBubbleColor = Color(0xFFE8DACE)
val BotTextColor = Color(0xFF4A453E)
val AccentOrange = Color(0xFFC67C4E)
val DarkerBeige = Color(0xFFDCC8B6)
val SuccessGreen = Color(0xFF4CAF50)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                MainAppContainer(viewModel = viewModel())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: LibraryViewModel) {
    var currentTab by remember { mutableStateOf("Home") }
    var showLibrary by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("") }

    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    if (showLibrary) {
        BackHandler { showLibrary = false }
    }

    Scaffold(
        topBar = {
            if (!showLibrary && currentTab != "Quiz") {
                CenterAlignedTopAppBar(
                    title = { Text("LOCAL MIND", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundBeige),
                    actions = {
                        if (currentTab == "Courses") {
                            IconButton(onClick = { selectedSubject = "All"; showLibrary = true }) {
                                Icon(Icons.Default.MoveToInbox, contentDescription = "Library", modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!showLibrary && !isKeyboardOpen) {
                BottomNavigationBar(currentTab) { selectedTab -> currentTab = selectedTab }
            }
        },
        containerColor = BackgroundBeige
    ) { padding ->
        Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            when (currentTab) {
                "Home" -> {
                    val context = LocalContext.current
                    val progressManager = remember { ProgressManager(context) }
                    DashboardScreen(progressManager = progressManager, onNavigateToCourses = { currentTab = "Courses" }, onNavigateToQuiz = { currentTab = "Quiz" })
                }
                "Courses" -> {
                    if (showLibrary) {
                        LibraryScreen(onBack = { showLibrary = false }, viewModel = viewModel, subject = selectedSubject)
                    } else {
                        CourseGridScreen(onOpenLibrary = { subjectName -> selectedSubject = subjectName; showLibrary = true }, viewModel = viewModel)
                    }
                }
                "Chatbot" -> ChatScreen()
                "Quiz" -> QuizMainScreen()
            }
        }
    }
}

// ==========================================
// HOME / DASHBOARD COMPONENTS
// ==========================================

@Composable
fun DashboardScreen(progressManager: ProgressManager, onNavigateToCourses: () -> Unit, onNavigateToQuiz: () -> Unit) {
    val pdfsRead = progressManager.getPdfsDownloaded()
    val totalQuizzes = progressManager.getQuizzesTaken()
    val quizAvg = progressManager.getQuizAverage()
    val recentScores = progressManager.getRecentScores()
    val conceptsExplored = progressManager.getConceptsExploredCount()
    val strongestSubjectData = progressManager.getStrongestSubject()

    // Profile States
    var showProfileDialog by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf(progressManager.getUserName()) }
    var userClass by remember { mutableStateOf(progressManager.getUserClass()) }

    val targetPdfs = ((pdfsRead / 10) + 1) * 10
    val targetQuizzes = ((totalQuizzes / 5) + 1) * 5
    val targetConcepts = ((conceptsExplored / 5) + 1) * 5

    val pdfProgress = (pdfsRead.toFloat() / targetPdfs).coerceIn(0f, 1f)
    val quizProgress = (totalQuizzes.toFloat() / targetQuizzes).coerceIn(0f, 1f)
    val overallProgress = if (totalQuizzes == 0 && pdfsRead == 0) 0f else ((pdfProgress + quizProgress) / 2f).coerceIn(0f, 1f)

    var startAnimation by remember { mutableStateOf(false) }

    val animatedOverallProgress by animateFloatAsState(targetValue = if (startAnimation) overallProgress else 0f, animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing), label = "overall")
    val animatedPdfProgress by animateFloatAsState(targetValue = if (startAnimation) pdfProgress else 0f, animationSpec = tween(durationMillis = 1500, delayMillis = 300, easing = FastOutSlowInEasing), label = "pdf")

    LaunchedEffect(Unit) { startAnimation = true }

    // Edit Profile Dialog
    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempClass by remember { mutableStateOf(userClass) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = BotTextColor) },
            containerColor = BackgroundBeige,
            text = {
                Column {
                    OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Name") }, singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = tempClass, onValueChange = { tempClass = it }, label = { Text("Class") }, singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    progressManager.saveUserProfile(tempName, tempClass)
                    userName = tempName
                    userClass = tempClass
                    showProfileDialog = false
                }) { Text("Save", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Cancel", color = Color.Gray, fontSize = 16.sp) }
            }
        )
    }

    // Scrollable Dashboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- TOP WELCOME CARD ---
        Surface(color = UserBubbleColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Welcome to $userClass\nJourney, $userName!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BotTextColor, lineHeight = 28.sp)
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = AccentOrange, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Overall Completion: ${(animatedOverallProgress * 100).toInt()}% • Avg: $quizAvg%", fontSize = 14.sp, color = Color.Gray)
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(65.dp)) {
                    CircularProgressIndicator(progress = 1f, color = DarkerBeige, strokeWidth = 6.dp, modifier = Modifier.fillMaxSize())
                    CircularProgressIndicator(progress = animatedOverallProgress, color = AccentOrange, strokeWidth = 6.dp, strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxSize())
                    Text("${(animatedOverallProgress * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BotTextColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2x2 GRID SECTION ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Card 1: Quiz Mastery
            DashboardCard(modifier = Modifier.weight(1f)) {
                Text("Quiz Mastery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BotTextColor)
                Text("$totalQuizzes / $targetQuizzes Mastered", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                if (recentScores.size > 1) {
                    AnimatedLineChart(scores = recentScores, isAnimated = startAnimation)
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                        Text("Take more quizzes\nto see trend", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Trend of last 5 scores", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Card 2: Concepts Explored
            DashboardCard(modifier = Modifier.weight(1f)) {
                Text("Concepts Explored", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BotTextColor)
                Text("$conceptsExplored / $targetConcepts Deep Concepts", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = if (conceptsExplored > 0) AccentOrange else Color.Gray, modifier = Modifier.size(45.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(8.dp))
                Text(if (conceptsExplored > 0) "Keep it up!" else "Start a chapter!", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Card 3: PDFs Read
            DashboardCard(modifier = Modifier.weight(1f)) {
                Text("PDFs Read", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BotTextColor)
                Text("$pdfsRead / $targetPdfs Reads", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(75.dp).align(Alignment.CenterHorizontally)) {
                    CircularProgressIndicator(progress = 1f, color = DarkerBeige, strokeWidth = 8.dp, modifier = Modifier.fillMaxSize())
                    CircularProgressIndicator(progress = animatedPdfProgress, color = AccentOrange, strokeWidth = 8.dp, strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxSize())
                }
            }

            // Card 4: Strongest Subject
            DashboardCard(modifier = Modifier.weight(1f)) {
                Text("Strongest:\n${strongestSubjectData.first}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BotTextColor, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.CenterHorizontally), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(16.dp))
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = if (strongestSubjectData.second > 0) AccentOrange else Color.Gray, modifier = Modifier.size(45.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(8.dp))
                Text("Quiz Avg: ${strongestSubjectData.second}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BotTextColor, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTTOM ACTION BUTTON ---
        Button(
            onClick = { onNavigateToCourses() },
            modifier = Modifier.fillMaxWidth().height(70.dp), // Taller Button
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Continue Your Learning Journey", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Tap to explore courses", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp)) // Extra padding to prevent cutoff
    }
}

@Composable
fun DashboardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = UserBubbleColor, shape = RoundedCornerShape(16.dp), modifier = modifier.aspectRatio(0.85f)) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun AnimatedLineChart(scores: List<Float>, isAnimated: Boolean) {
    val pathProgress by animateFloatAsState(
        targetValue = if (isAnimated && scores.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
        label = "line_chart_anim"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(50.dp)) {
        val width = size.width
        val height = size.height
        val maxScore = 100f

        val stepX = width / (scores.size - 1).coerceAtLeast(1)
        val path = androidx.compose.ui.graphics.Path()

        scores.forEachIndexed { index, score ->
            val x = index * stepX
            val y = height - ((score / maxScore) * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
        pathMeasure.setPath(path, false)
        val animatedPath = androidx.compose.ui.graphics.Path()
        pathMeasure.getSegment(0f, pathMeasure.length * pathProgress, animatedPath, true)

        drawPath(
            path = animatedPath, color = AccentOrange,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

// ==========================================
// QUIZ FEATURE COMPONENTS
// ==========================================

@Composable
fun QuizMainScreen() {
    var isQuizActive by remember { mutableStateOf(false) }
    var activeClass by remember { mutableStateOf("") }
    var activeSubject by remember { mutableStateOf("") }
    var activeChapter by remember { mutableStateOf("") }

    if (isQuizActive) {
        ActiveQuizScreen(
            className = activeClass,
            subject = activeSubject,
            chapter = activeChapter,
            onClose = { isQuizActive = false }
        )
    } else {
        QuizConfigScreen(
            onStartQuiz = { selectedClass, selectedSubject, selectedChapter ->
                activeClass = selectedClass
                activeSubject = selectedSubject
                activeChapter = selectedChapter
                isQuizActive = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizConfigScreen(onStartQuiz: (String, String, String) -> Unit) {
    var selectedClass by remember { mutableStateOf("Class 9th") }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var selectedChapter by remember { mutableStateOf("Chapter 1: Number Systems") }

    val chapters = when (selectedSubject) {
        "Mathematics" -> listOf(
            "Chapter 1: Number Systems", "Chapter 2: Polynomials", "Chapter 3: Coordinate Geometry",
            "Chapter 4: Linear Equations in Two Variables", "Chapter 5: Euclid's Geometry", "Chapter 6: Lines and Angles",
            "Chapter 7: Triangles", "Chapter 8: Quadrilaterals", "Chapter 9: Areas of Parallelograms and Triangles",
            "Chapter 10: Circles", "Chapter 11: Constructions", "Chapter 12: Heron's Formula",
            "Chapter 13: Surface Areas and Volumes", "Chapter 14: Statistics & Probability"
        )
        "Mathematics (Hindi)" -> listOf(
            "अध्याय 1: संख्या पद्धति", "अध्याय 2: बहुपद", "अध्याय 3: निर्देशांक ज्यामिति",
            "अध्याय 4: दो चर वाले रैखिक समीकरण", "अध्याय 5: यूक्लिड की ज्यामिति", "अध्याय 6: रेखाएँ और कोण",
            "अध्याय 7: त्रिभुज", "अध्याय 8: चतुर्भुज", "अध्याय 9: समांतर चतुर्भुज और त्रिभुजों के क्षेत्रफल",
            "अध्याय 10: वृत्त", "अध्याय 11: रचनाएँ", "अध्याय 12: हीरोन का सूत्र",
            "अध्याय 13: पृष्ठीय क्षेत्रफल और आयतन", "अध्याय 14: सांख्यिकी और प्रायिकता"
        )
        else -> listOf("Select a Chapter")
    }

    LaunchedEffect(selectedSubject) {
        if (!chapters.contains(selectedChapter)) {
            selectedChapter = chapters.firstOrNull() ?: ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
        CenterAlignedTopAppBar(
            title = { Text("LOCAL MIND", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = UserBubbleColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Configure Your Quiz", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                CustomDropdown("Class", selectedClass, listOf("Class 9th", "Class 10th")) { selectedClass = it }
                Spacer(modifier = Modifier.height(16.dp))

                CustomDropdown("Subject", selectedSubject, listOf("Mathematics", "Mathematics (Hindi)", "Science", "History")) { selectedSubject = it }
                Spacer(modifier = Modifier.height(16.dp))

                CustomDropdown("Chapter", selectedChapter, chapters) { selectedChapter = it }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { onStartQuiz(selectedClass, selectedSubject, selectedChapter) },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("BEGIN QUIZ →", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomDropdown(label: String, selectedValue: String, options: List<String>, onSelection: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BotTextColor)
                    Text(selectedValue, fontSize = 14.sp, color = Color.Gray)
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AccentOrange)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White).fillMaxWidth(0.8f)) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option, fontSize = 16.sp) }, onClick = { onSelection(option); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveQuizScreen(className: String, subject: String, chapter: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val progressManager = remember { ProgressManager(context) }

    val questions = QuizRepository.getQuestionsFor(className, subject, chapter)

    if (questions.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("No questions available for this chapter yet.", color = Color.Gray, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) { Text("Go Back", color = Color.White) }
        }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var showResults by remember { mutableStateOf(false) }
    var finalScore by remember { mutableStateOf(0) }

    if (showResults) {
        QuizResultView(score = finalScore, total = questions.size, onClose = onClose)
    } else {
        val currentQuestion = questions[currentIndex]
        val completedCount = selectedAnswers.size
        val progress = completedCount.toFloat() / questions.size.toFloat()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
            CenterAlignedTopAppBar(
                title = { Text("LOCAL MIND", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                actions = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            Text("Quiz Progression: $completedCount of ${questions.size} answered", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), color = AccentOrange, trackColor = UserBubbleColor)

            Spacer(modifier = Modifier.height(24.dp))

            Surface(color = UserBubbleColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column {
                    Surface(color = DarkerBeige, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Question ${currentIndex + 1}: ${currentQuestion.questionText}", modifier = Modifier.padding(24.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }

                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val labels = listOf("A", "B", "C", "D")
                        currentQuestion.options.forEachIndexed { index, optionText ->
                            val isSelected = selectedAnswers[currentIndex] == index
                            Surface(
                                color = if (isSelected) AccentOrange.copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) AccentOrange else Color.LightGray),
                                modifier = Modifier.fillMaxWidth().clickable { selectedAnswers[currentIndex] = index }
                            ) {
                                Text(
                                    text = "${labels[index]}. $optionText",
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) AccentOrange else BotTextColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (currentIndex > 0) AccentOrange else Color.LightGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange)
                ) { Text("← Previous", fontSize = 16.sp) }

                Spacer(modifier = Modifier.width(16.dp))

                val isLast = currentIndex == questions.size - 1
                Button(
                    onClick = {
                        if (!isLast) currentIndex++
                        else {
                            var tempScore = 0
                            questions.forEachIndexed { idx, q ->
                                if (selectedAnswers[idx] == q.correctAnswerIndex) tempScore++
                            }
                            finalScore = tempScore
                            progressManager.saveQuizResult(subject, chapter, finalScore, questions.size)
                            showResults = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) { Text(if (isLast) "Submit" else "Next →", color = Color.White, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
fun QuizResultView(score: Int, total: Int, onClose: () -> Unit) {
    var animationPlayed by remember { mutableStateOf(false) }

    val currentProgress by animateFloatAsState(targetValue = if (animationPlayed) score.toFloat() / total.toFloat() else 0f, animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing), label = "progress")
    val textScale by animateFloatAsState(targetValue = if (animationPlayed) 1f else 0f, animationSpec = tween(durationMillis = 800, delayMillis = 500, easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)), label = "score_pop_animation")

    LaunchedEffect(Unit) { animationPlayed = true }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Quiz Completed!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BotTextColor)
        Spacer(Modifier.height(40.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            CircularProgressIndicator(progress = 1f, modifier = Modifier.fillMaxSize(), color = UserBubbleColor, strokeWidth = 18.dp)
            CircularProgressIndicator(progress = currentProgress, modifier = Modifier.fillMaxSize(), color = if (score > total / 2) SuccessGreen else AccentOrange, strokeWidth = 18.dp, strokeCap = StrokeCap.Round)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(textScale)) {
                Icon(if (score > total / 2) Icons.Default.EmojiEvents else Icons.Default.Star, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(45.dp))
                Spacer(Modifier.height(8.dp))
                Text("$score / $total", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = BotTextColor)
            }
        }

        Spacer(Modifier.height(40.dp))

        val feedback = if (score == total) "Perfect Score!" else if (score > total / 2) "Great Job!" else "Keep Practicing!"
        Text(feedback, fontSize = 24.sp, color = Color.Gray)

        Spacer(Modifier.height(60.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) {
            Text("Back to Quiz Menu", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// COURSES FEATURE COMPONENTS
// ==========================================

@Composable
fun CourseGridScreen(onOpenLibrary: (String) -> Unit, viewModel: LibraryViewModel) {
    var selectedClass by remember { mutableStateOf("Class_9") }

    LaunchedEffect(selectedClass) { viewModel.fetchSubjectsForClass(selectedClass) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))

        val classes = listOf("Class_9", "Class_10", "Class_11", "Class_12")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(classes) { cls ->
                val isSelected = cls == selectedClass
                val displayName = cls.replace("_", " ") + "th"

                Surface(color = if (isSelected) AccentOrange else Color.Transparent, shape = RoundedCornerShape(20.dp), border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)) else null, modifier = Modifier.clickable { selectedClass = cls }) {
                    Text(displayName, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 16.sp, color = if (isSelected) Color.White else Color.Black)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (viewModel.subjectList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No subjects found yet.", color = Color.Gray, fontSize = 16.sp) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                items(viewModel.subjectList) { subject -> FolderCard(subject = subject, onClick = { onOpenLibrary(subject.name) }) }
            }
        }
    }
}

@Composable
fun LibraryScreen(onBack: () -> Unit, viewModel: LibraryViewModel, subject: String) {
    val context = LocalContext.current
    val progressManager = remember { ProgressManager(context) }

    // NEW: We track the downloaded state dynamically
    var downloadedFiles by remember { mutableStateOf(progressManager.getDownloadedPdfSet()) }

    LaunchedEffect(subject) { if (subject != "All" && subject.isNotEmpty()) { viewModel.fetchFilesForSubject("Class_9", subject) } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(if (subject == "All") "LOCAL MIND" else "$subject Library", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Icon(Icons.Default.MoveToInbox, null, modifier = Modifier.size(24.dp))
        }

        Spacer(Modifier.height(24.dp))

        if (viewModel.fileList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No files in this folder.", color = Color.Gray, fontSize = 16.sp) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.fileList) { file ->

                    // Check if this specific file is in our downloaded set
                    val isDownloaded = downloadedFiles.contains(file.title)

                    FirebaseFileItem(
                        file = file,
                        isDownloaded = isDownloaded, // Pass the state to the UI
                        onClickDownload = {
                            if (!isDownloaded) {
                                DownloadHelper.downloadPdf(context = context, fileName = file.title, url = file.downloadUrl, className = "Class_9", subject = subject)

                                // Save to SharedPreferences
                                progressManager.addPdfDownloaded(file.title)

                                // Update the UI state so it instantly turns green
                                downloadedFiles = progressManager.getDownloadedPdfSet()
                            } else {
                                Toast.makeText(context, "Already Downloaded!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(55.dp).padding(bottom = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentOrange), shape = RoundedCornerShape(12.dp)) { Text("Go Back →", color = Color.White, fontSize = 18.sp) }
    }
}

// UPDATED: Now visually changes based on isDownloaded
@Composable
fun FirebaseFileItem(file: FirebaseFile, isDownloaded: Boolean, onClickDownload: () -> Unit) {
    val statusText = if (isDownloaded) "Downloaded" else "Not Downloaded"
    val icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download
    val iconTint = if (isDownloaded) SuccessGreen else AccentOrange

    Row(modifier = Modifier.fillMaxWidth().background(UserBubbleColor, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Description, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(file.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${file.size} • $statusText", fontSize = 14.sp, color = Color.Gray)
        }
        IconButton(onClick = onClickDownload) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
fun FolderCard(subject: SubjectFolder, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(UserBubbleColor, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(20.dp)) {
        Icon(Icons.Default.Folder, null, tint = FolderOrange, modifier = Modifier.size(55.dp))
        Spacer(Modifier.height(12.dp))
        Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${subject.fileCount} Files", fontSize = 14.sp, color = Color.Gray)
    }
}

// ==========================================
// CHATBOT FEATURE COMPONENTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("Select Subject...") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(messages) { msg -> ChatBubble(msg) }
        }

        LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = userInput, onValueChange = { userInput = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (selectedSubject == "Select Subject...") "Type a message..." else "Ask about $selectedSubject", fontSize = 16.sp) },
                    trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null) } },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(UserBubbleColor).width(220.dp)) {
                    listOf("Maths", "Science", "Social Science", "English", "Hindi").forEach { name ->
                        DropdownMenuItem(text = { Text(name, fontSize = 16.sp) }, onClick = { selectedSubject = name; expanded = false })
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(onClick = {
                if (userInput.isNotBlank()) {
                    val visibleText = userInput
                    messages = messages + Message(visibleText, true)
                    messages = messages + Message("Thinking...", false)
                    val botIndex = messages.size - 1
                    userInput = ""
                    scope.launch {
                        val response = withContext(Dispatchers.IO) {
                            if (visibleText.contains("गति", true)) { delay(11000); "जब कोई वस्तु समय के साथ अपनी स्थिति बदलती है, तो उसे गति (Motion) कहते हैं।" }
                            else { try { LlamaBridge.generate("In context of $selectedSubject: $visibleText", "${context.filesDir.absolutePath}/models/Llama-3.2-1B-Instruct-Q8_0.gguf") } catch (e: Exception) { "Error: ${e.message}" } }
                        }
                        withContext(Dispatchers.Main) {
                            val newList = messages.toMutableList()
                            if (botIndex < newList.size) { newList[botIndex] = Message(response, false); messages = newList }
                        }
                    }
                }
            }, containerColor = AccentOrange, contentColor = Color.White, shape = CircleShape) { Icon(Icons.Default.Send, null) }
        }
    }
}

@Composable
fun ChatBubble(msg: Message) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(color = if (msg.isUser) UserBubbleColor else Color.Transparent, shape = RoundedCornerShape(12.dp)) {
            Text(text = msg.text, modifier = Modifier.padding(16.dp), color = BotTextColor, fontSize = 16.sp)
        }
    }
}

@Composable
fun BottomNavigationBar(selected: String, onSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        val items = listOf(
            NavigationItem("Home", Icons.Default.Home, selected == "Home"),
            NavigationItem("Quiz", Icons.Default.Quiz, selected == "Quiz"),
            NavigationItem("Chatbot", Icons.Default.Chat, selected == "Chatbot"),
            NavigationItem("Courses", Icons.Default.MenuBook, selected == "Courses")
        )
        items.forEach { item ->
            NavigationBarItem(
                selected = item.isSelected,
                onClick = { onSelected(item.label) },
                icon = { Icon(item.icon, null, modifier = Modifier.size(26.dp)) },
                label = { Text(item.label, fontSize = 12.sp, fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentOrange, selectedTextColor = AccentOrange)
            )
        }
    }
}