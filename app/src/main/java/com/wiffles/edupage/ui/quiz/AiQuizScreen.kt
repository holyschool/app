package com.wiffles.edupage.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AiQuiz
import com.wiffles.edupage.data.QuizQuestion
import com.wiffles.edupage.data.MaterialLoadState
import com.wiffles.edupage.data.QuizAttempt
import com.wiffles.edupage.data.StudyMaterial
import com.wiffles.edupage.ui.core.containers.RoundedCardContainer
import com.wiffles.edupage.ui.core.sheets.AppBottomSheet
import com.wiffles.edupage.ui.util.rememberAppHaptics
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuizScreen(
    onBack: () -> Unit,
    viewModel: AiQuizViewModel = hiltViewModel(),
) {
    val quizzes by viewModel.quizzes.collectAsState()
    val genState by viewModel.generateState.collectAsState()
    val examState by viewModel.examMaterials.collectAsState()
    val explainState by viewModel.explainState.collectAsState()
    val attemptsByQuiz by viewModel.attempts.collectAsState()
    val regenerating by viewModel.regenerating.collectAsState()
    val regenerateError by viewModel.regenerateError.collectAsState()
    var showNewSheet by remember { mutableStateOf(false) }
    var activeQuiz by remember { mutableStateOf<AiQuiz?>(null) }
    val haptics = rememberAppHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    val hasInsights = attemptsByQuiz.values.any { it.isNotEmpty() }

    LaunchedEffect(regenerateError) {
        regenerateError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeRegenerateError()
        }
    }

    BackHandler(enabled = activeQuiz != null) {
        viewModel.resetExplain()
        activeQuiz = null
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.quiz_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { haptics.virtualKey(); onBack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            haptics.virtualKey()
                            showNewSheet = true
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.quiz_new))
                        }
                        Spacer(Modifier.width(4.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        ) { padding ->
            if (quizzes.isEmpty()) {
                EmptyState(Modifier.fillMaxSize().padding(padding)) {
                    haptics.virtualKey()
                    showNewSheet = true
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (hasInsights) {
                        item(key = "insights") {
                            QuizInsights(
                                quizzes = quizzes,
                                attemptsByQuiz = attemptsByQuiz,
                            )
                        }
                    }
                    items(quizzes, key = { it.id }) { quiz ->
                        QuizRow(
                            quiz = quiz,
                            attemptsForQuiz = attemptsByQuiz[quiz.id].orEmpty(),
                            regenerating = regenerating,
                            onStart = { haptics.virtualKey(); activeQuiz = quiz },
                            onRefresh = {
                                haptics.virtualKey()
                                viewModel.regenerate(quiz, focusWeak = false) { created -> activeQuiz = created }
                            },
                            onPracticeMistakes = {
                                haptics.virtualKey()
                                viewModel.regenerate(quiz, focusWeak = true) { created -> activeQuiz = created }
                            },
                            onDelete = { viewModel.delete(quiz.id) },
                        )
                    }
                }
            }
        }

        activeQuiz?.let { quiz ->
            QuizRunner(
                quiz = quiz,
                attempts = attemptsByQuiz[quiz.id].orEmpty(),
                explainState = explainState,
                modifier = Modifier.fillMaxSize(),
                onRecord = { score, total, wrong -> viewModel.recordResult(quiz.id, score, total, wrong) },
                onExplain = { answers -> viewModel.explain(quiz, answers) },
                onResetExplain = { viewModel.resetExplain() },
                onPracticeMistakes = {
                    viewModel.regenerate(quiz, focusWeak = true) { created -> activeQuiz = created }
                },
                onRefreshQuestions = {
                    viewModel.regenerate(quiz, focusWeak = false) { created -> activeQuiz = created }
                },
                onExit = {
                    viewModel.resetExplain()
                    activeQuiz = null
                },
            )
        }
    }

    if (showNewSheet) {
        NewQuizSheet(
            generating = genState is AiQuizViewModel.GenerateState.Generating,
            error = (genState as? AiQuizViewModel.GenerateState.Error)?.message,
            homeworkProvider = { viewModel.homeworkMaterials() },
            examState = examState,
            onLoadExamMaterials = { viewModel.loadExamMaterials() },
            onDismiss = {
                viewModel.consumeError()
                showNewSheet = false
            },
            onGenerate = { topic, count, difficulty, material ->
                viewModel.generate(topic, count, difficulty, material) { created ->
                    showNewSheet = false
                    activeQuiz = created
                }
            },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onNew: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.quiz_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onNew, shape = RoundedCornerShape(20.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quiz_new))
            }
        }
    }
}

@Composable
private fun QuizRow(
    quiz: AiQuiz,
    attemptsForQuiz: List<QuizAttempt>,
    regenerating: Boolean,
    onStart: () -> Unit,
    onRefresh: () -> Unit,
    onPracticeMistakes: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasMistakes = remember(attemptsForQuiz) {
        attemptsForQuiz.any { it.wrongQuestions.isNotEmpty() }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onStart),
            ) {
                Text(
                    text = quiz.topic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Text(
                    text = stringResource(R.string.quiz_questions_count, quiz.questions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (quiz.bestScore >= 0 && quiz.bestTotal > 0) {
                    Text(
                        text = stringResource(R.string.quiz_best, quiz.bestScore, quiz.bestTotal),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }, enabled = !regenerating) {
                    if (regenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.quiz_more),
                        )
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quiz_regenerate_refresh)) },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                        onClick = { menuOpen = false; onRefresh() },
                    )
                    if (hasMistakes) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quiz_regenerate_weak)) },
                            leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null) },
                            onClick = { menuOpen = false; onPracticeMistakes() },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.quiz_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

/** A compact overview of recent quiz activity and the concepts that need practice. */
@Composable
private fun QuizInsights(
    quizzes: List<AiQuiz>,
    attemptsByQuiz: Map<String, List<QuizAttempt>>,
) {
    val allAttempts = remember(attemptsByQuiz) { attemptsByQuiz.values.flatten() }
    if (allAttempts.isEmpty()) return

    val weakSpots = remember(attemptsByQuiz) {
        attemptsByQuiz.values.flatten()
            .flatMap { it.wrongQuestions }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }
    val recent = remember(attemptsByQuiz, quizzes) {
        val topics = quizzes.associate { it.id to it.topic }
        attemptsByQuiz.entries
            .flatMap { (id, list) -> list.map { topics[id] to it } }
            .sortedByDescending { it.second.timestampMs }
            .take(4)
    }
    val average = remember(allAttempts) { allAttempts.map { it.fraction }.average() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.quiz_insights_title))
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    InsightStat(
                        value = allAttempts.size.toString(),
                        label = stringResource(R.string.quiz_insights_attempts),
                    )
                    InsightStat(
                        value = "${(average * 100).toInt()}%",
                        label = stringResource(R.string.quiz_insights_average),
                    )
                }
                if (weakSpots.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.quiz_insights_weak_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        weakSpots.forEach { (question, count) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = question,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = stringResource(R.string.quiz_insights_wrong_count, count),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
                if (recent.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.quiz_insights_recent_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        recent.forEach { (topic, attempt) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = topic ?: stringResource(R.string.quiz_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${attempt.score}/${attempt.total}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewQuizSheet(
    generating: Boolean,
    error: String?,
    homeworkProvider: () -> List<StudyMaterial>,
    examState: MaterialLoadState,
    onLoadExamMaterials: () -> Unit,
    onDismiss: () -> Unit,
    onGenerate: (String, Int, String, String?) -> Unit,
) {
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(10) }
    var difficulty by remember { mutableStateOf("medium") }
    var material by remember { mutableStateOf<String?>(null) }
    var materialLabel by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var pickerSource by remember { mutableStateOf(QuizSource.HOMEWORK) }
    var homeworkMaterials by remember { mutableStateOf(emptyList<StudyMaterial>()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = rememberAppHaptics()

    val pickerItems: List<StudyMaterial> = when (pickerSource) {
        QuizSource.HOMEWORK -> homeworkMaterials
        QuizSource.EXAM -> (examState as? MaterialLoadState.Loaded)?.items.orEmpty()
    }

    fun openPicker(source: QuizSource) {
        haptics.virtualKey()
        pickerSource = source
        if (source == QuizSource.HOMEWORK) homeworkMaterials = homeworkProvider()
        else onLoadExamMaterials()
        showPicker = true
    }

    fun selectMaterial(item: StudyMaterial) {
        haptics.virtualKey()
        material = item.body
        materialLabel = item.title
        if (topic.isBlank()) topic = item.title
        showPicker = false
    }

    AppBottomSheet(onDismissRequest = { if (!generating) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showPicker) {
                Text(
                    text = stringResource(
                        if (pickerSource == QuizSource.EXAM) R.string.quiz_materials_pick_exam_title
                        else R.string.quiz_materials_pick_title
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                when {
                    pickerSource == QuizSource.EXAM &&
                        examState is MaterialLoadState.Loading -> {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        }
                    }
                    pickerSource == QuizSource.EXAM &&
                        examState is MaterialLoadState.Error -> {
                        Text(
                            text = examState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    pickerItems.isEmpty() -> {
                        Text(
                            text = stringResource(
                                if (pickerSource == QuizSource.EXAM) R.string.quiz_materials_empty_exam
                                else R.string.quiz_materials_empty
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        pickerItems.forEach { item ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectMaterial(item) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                    )
                                    item.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { haptics.virtualKey(); showPicker = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.quiz_back))
                }
                return@Column
            }

            Text(
                text = stringResource(R.string.quiz_new),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text(stringResource(R.string.quiz_topic)) },
                placeholder = { Text(stringResource(R.string.quiz_topic_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (materialLabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = materialLabel.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    TextButton(onClick = { material = null; materialLabel = null }) {
                        Text(stringResource(R.string.quiz_clear_materials))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    OutlinedButton(
                        onClick = { openPicker(QuizSource.HOMEWORK) },
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            bottomStart = 20.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Icon(Icons.Rounded.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.quiz_source_homework), maxLines = 1)
                    }
                    Box {
                        OutlinedButton(
                            onClick = { haptics.virtualKey(); menuExpanded = true },
                            shape = RoundedCornerShape(
                                topEnd = 20.dp,
                                bottomEnd = 20.dp,
                                topStart = 4.dp,
                                bottomStart = 4.dp,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.quiz_source_menu),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.quiz_source_homework)) },
                                leadingIcon = { Icon(Icons.Rounded.Assignment, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    openPicker(QuizSource.HOMEWORK)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.quiz_source_exam)) },
                                leadingIcon = { Icon(Icons.Rounded.Quiz, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    openPicker(QuizSource.EXAM)
                                },
                            )
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.quiz_count),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { option ->
                        FilterChip(
                            selected = count == option,
                            onClick = { haptics.virtualKey(); count = option },
                            label = { Text(option.toString()) },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.quiz_difficulty),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { option ->
                        FilterChip(
                            selected = difficulty == option.key,
                            onClick = { haptics.virtualKey(); difficulty = option.key },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
            }
            if (error != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Button(
                onClick = {
                    haptics.virtualKey()
                    onGenerate(topic, count, difficulty, material)
                },
                enabled = topic.isNotBlank() && !generating,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.quiz_generating))
                } else {
                    Text(stringResource(R.string.quiz_generate), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class QuizSource { HOMEWORK, EXAM }

private enum class Difficulty(val key: String, val labelRes: Int) {
    EASY("easy", R.string.quiz_difficulty_easy),
    MEDIUM("medium", R.string.quiz_difficulty_medium),
    HARD("hard", R.string.quiz_difficulty_hard),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizRunner(
    quiz: AiQuiz,
    attempts: List<QuizAttempt>,
    explainState: AiQuizViewModel.ExplainState,
    modifier: Modifier = Modifier,
    onRecord: (Int, Int, List<String>) -> Unit,
    onExplain: (List<Int?>) -> Unit,
    onResetExplain: () -> Unit,
    onPracticeMistakes: () -> Unit,
    onRefreshQuestions: () -> Unit,
    onExit: () -> Unit,
) {
    var index by remember(quiz.id) { mutableIntStateOf(0) }
    var selected by remember(quiz.id) { mutableStateOf<Int?>(null) }
    var score by remember(quiz.id) { mutableIntStateOf(0) }
    var finished by remember(quiz.id) { mutableStateOf(false) }
    var finishedAtMs by remember(quiz.id) { mutableLongStateOf(0L) }
    // Bumping the nonce reshuffles the options and starts a fresh attempt.
    var shuffleNonce by remember(quiz.id) { mutableIntStateOf(0) }
    // Answer options are shuffled each session so the correct position is never predictable.
    val questions = remember(quiz.id, shuffleNonce) { quiz.questions.shuffledOptions() }
    val answers = remember(quiz.id, shuffleNonce) {
        mutableStateListOf<Int?>().apply { repeat(questions.size) { add(null) } }
    }
    val haptics = rememberAppHaptics()

    if (questions.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            TextButton(onClick = onExit) { Text(stringResource(R.string.quiz_back)) }
        }
        return
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        if (finished) {
            ResultScreen(
                quiz = quiz,
                questions = questions,
                score = score,
                answers = answers.toList(),
                attempts = attempts,
                finishedAtMs = finishedAtMs,
                explainState = explainState,
                onExplain = { onExplain(answers.toList()) },
                onRetry = {
                    onResetExplain()
                    shuffleNonce++
                    index = 0; selected = null; score = 0; finished = false
                },
                onPracticeMistakes = onPracticeMistakes,
                onRefreshQuestions = onRefreshQuestions,
                onDone = onExit,
            )
            return@Surface
        }

        val answered = selected != null
        val progress by animateFloatAsState(
            targetValue = (index + if (answered) 1 else 0).toFloat() / questions.size,
            animationSpec = tween(450),
            label = "quizProgress",
        )

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = quiz.topic,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                Text(
                                    text = stringResource(R.string.quiz_question, index + 1, questions.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { haptics.virtualKey(); onExit() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(200)))
                },
                modifier = Modifier.fillMaxSize().padding(padding),
                label = "quizQuestion",
            ) { targetIndex ->
                val question = questions[targetIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.animateContentSize(),
                    )
                    Spacer(Modifier.height(4.dp))
                    question.options.forEachIndexed { optionIndex, option ->
                        OptionRow(
                            text = option,
                            state = optionState(optionIndex, selected, question.correctIndex),
                            enabled = !answered,
                            onClick = {
                                haptics.virtualKey()
                                selected = optionIndex
                                answers[targetIndex] = optionIndex
                                if (optionIndex == question.correctIndex) score++
                            },
                        )
                    }
                    AnimatedVisibility(
                        visible = answered,
                        enter = expandVertically(tween(300)) + fadeIn(tween(250)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            question.explanation?.takeIf { it.isNotBlank() }?.let { explanation ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            text = stringResource(R.string.quiz_explanation),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = explanation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    haptics.virtualKey()
                                    if (index == questions.lastIndex) {
                                        val wrong = questions.filterIndexed { i, q ->
                                            answers.getOrNull(i) != q.correctIndex
                                        }.map { it.question }
                                        onRecord(score, questions.size, wrong)
                                        finishedAtMs = System.currentTimeMillis()
                                        finished = true
                                    } else {
                                        index++
                                        selected = null
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                            ) {
                                Text(
                                    text = if (index == questions.lastIndex) {
                                        stringResource(R.string.quiz_finish)
                                    } else {
                                        stringResource(R.string.quiz_next)
                                    },
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private enum class OptionState { IDLE, CORRECT, WRONG }

private fun optionState(optionIndex: Int, selected: Int?, correctIndex: Int): OptionState = when {
    selected == null -> OptionState.IDLE
    optionIndex == correctIndex -> OptionState.CORRECT
    optionIndex == selected -> OptionState.WRONG
    else -> OptionState.IDLE
}

/** Returns the questions with their answer options shuffled and correctIndex remapped. */
private fun List<QuizQuestion>.shuffledOptions(): List<QuizQuestion> = map { question ->
    if (question.options.size < 2) return@map question
    val shuffled = question.options.withIndex().shuffled()
    val newCorrect = shuffled.indexOfFirst { it.index == question.correctIndex }
    question.copy(
        options = shuffled.map { it.value },
        correctIndex = newCorrect.coerceAtLeast(0),
    )
}

@Composable
private fun OptionRow(
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = when (state) {
            OptionState.CORRECT -> MaterialTheme.colorScheme.primaryContainer
            OptionState.WRONG -> MaterialTheme.colorScheme.errorContainer
            OptionState.IDLE -> MaterialTheme.colorScheme.surfaceBright
        },
        animationSpec = tween(280),
        label = "optionContainer",
    )
    val content by animateColorAsState(
        targetValue = when (state) {
            OptionState.CORRECT -> MaterialTheme.colorScheme.onPrimaryContainer
            OptionState.WRONG -> MaterialTheme.colorScheme.onErrorContainer
            OptionState.IDLE -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(280),
        label = "optionContent",
    )
    val scale by animateFloatAsState(
        targetValue = if (state == OptionState.WRONG) 0.98f else 1f,
        animationSpec = tween(220),
        label = "optionScale",
    )
    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = content,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = state == OptionState.CORRECT || state == OptionState.WRONG,
                enter = fadeIn(tween(200)) + scaleIn(tween(220)),
                exit = fadeOut(tween(120)),
            ) {
                Icon(
                    imageVector = if (state == OptionState.CORRECT) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultScreen(
    quiz: AiQuiz,
    questions: List<QuizQuestion>,
    score: Int,
    answers: List<Int?>,
    attempts: List<QuizAttempt>,
    finishedAtMs: Long,
    explainState: AiQuizViewModel.ExplainState,
    onExplain: () -> Unit,
    onRetry: () -> Unit,
    onPracticeMistakes: () -> Unit,
    onRefreshQuestions: () -> Unit,
    onDone: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val total = questions.size
    val wrongCount = questions.indices.count { answers.getOrNull(it) != questions[it].correctIndex }
    val fraction = if (total > 0) score.toFloat() / total else 0f
    val progress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(900),
        label = "resultProgress",
    )
    val shownScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(900),
        label = "resultScore",
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.quiz_result_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptics.virtualKey(); onDone() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(140.dp),
                        strokeWidth = 12.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = shownScore.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "/$total",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.quiz_result_score, score, total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (score == total && total > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.quiz_result_perfect),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (finishedAtMs > 0) {
                    Spacer(Modifier.height(4.dp))
                    val dateText = remember(finishedAtMs) {
                        java.time.Instant.ofEpochMilli(finishedAtMs)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                    }
                    Text(
                        text = stringResource(R.string.quiz_result_date, dateText),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ProgressGraphCard(attempts = attempts)

            ReviewCard(questions = questions, answers = answers)

            ExplainCard(
                state = explainState,
                onExplain = { haptics.virtualKey(); onExplain() },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (wrongCount > 0) {
                    OutlinedButton(
                        onClick = { haptics.virtualKey(); onPracticeMistakes() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) {
                        Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.quiz_regenerate_weak),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                OutlinedButton(
                    onClick = { haptics.virtualKey(); onRefreshQuestions() },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.quiz_regenerate_refresh),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { haptics.virtualKey(); onRetry() },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text(stringResource(R.string.quiz_retry), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { haptics.virtualKey(); onDone() },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text(stringResource(R.string.quiz_back), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProgressGraphCard(attempts: List<QuizAttempt>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            text = stringResource(R.string.quiz_progress_title),
            trailing = if (attempts.isNotEmpty()) stringResource(R.string.quiz_progress_attempts, attempts.size) else null,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (attempts.size < 2) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.quiz_progress_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                ProgressLineChart(
                    attempts = attempts,
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(16.dp),
                )
            }
        }
    }
}

/** A compact line chart of score fractions across attempts, drawn with Canvas. */
@Composable
private fun ProgressLineChart(attempts: List<QuizAttempt>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val fill = primary.copy(alpha = 0.14f)
    val latestFraction = attempts.last().fraction
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(700),
        label = "chartReveal",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padL = 8.dp.toPx()
        val padR = 8.dp.toPx()
        val padT = 8.dp.toPx()
        val padB = 8.dp.toPx()
        val chartW = (w - padL - padR).coerceAtLeast(1f)
        val chartH = (h - padT - padB).coerceAtLeast(1f)

        // Horizontal grid lines at 0%, 50%, 100%.
        listOf(0f, 0.5f, 1f).forEach { level ->
            val y = padT + chartH * (1f - level)
            drawLine(
                color = grid.copy(alpha = 0.5f),
                start = Offset(padL, y),
                end = Offset(w - padR, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        if (attempts.isEmpty()) return@Canvas

        fun pointAt(i: Int): Offset {
            val x = if (attempts.size == 1) padL + chartW / 2f
            else padL + chartW * (i.toFloat() / (attempts.size - 1))
            val y = padT + chartH * (1f - attempts[i].fraction.coerceIn(0f, 1f))
            return Offset(x, y)
        }

        val path = Path()
        attempts.indices.forEach { i ->
            val p = pointAt(i)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }

        // Area fill under the line.
        val area = Path().apply {
            addPath(path)
            lineTo(pointAt(attempts.lastIndex).x, padT + chartH)
            lineTo(pointAt(0).x, padT + chartH)
            close()
        }
        drawPath(area, color = fill)
        drawPath(
            path = path,
            color = primary,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Dots; the most recent one is emphasised.
        attempts.indices.forEach { i ->
            val p = pointAt(i)
            val isLast = i == attempts.lastIndex
            drawCircle(
                color = if (isLast) primary else primary.copy(alpha = 0.6f),
                radius = if (isLast) 4.dp.toPx() else 2.5.dp.toPx(),
                center = p,
            )
        }
    }
}

@Composable
private fun ReviewCard(questions: List<QuizQuestion>, answers: List<Int?>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.quiz_review_title))
        questions.forEachIndexed { index, question ->
            val given = answers.getOrNull(index)
            val isCorrect = given == question.correctIndex
            Surface(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "${index + 1}. ${question.question}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    val givenText = given?.let { question.options.getOrNull(it) }
                        ?: stringResource(R.string.quiz_review_no_answer)
                    Text(
                        text = stringResource(R.string.quiz_review_your_answer) + ": " + givenText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    if (!isCorrect) {
                        Text(
                            text = stringResource(R.string.quiz_review_correct_answer) + ": " +
                                (question.options.getOrNull(question.correctIndex) ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    question.explanation?.takeIf { it.isNotBlank() }?.let { explanation ->
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplainCard(state: AiQuizViewModel.ExplainState, onExplain: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.quiz_explain_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state) {
                    is AiQuizViewModel.ExplainState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.quiz_explain_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is AiQuizViewModel.ExplainState.Ready -> {
                        Text(
                            text = state.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OutlinedButton(
                            onClick = onExplain,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.quiz_explain_retry))
                        }
                    }
                    is AiQuizViewModel.ExplainState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(
                            onClick = onExplain,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text(stringResource(R.string.quiz_explain_retry))
                        }
                    }
                    else -> Button(
                        onClick = onExplain,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.quiz_explain_action), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
