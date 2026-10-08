package com.enderplusbayzuiship.edupage2.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AiQuiz
import com.enderplusbayzuiship.edupage2.data.HomeworkItem
import com.enderplusbayzuiship.edupage2.data.QuizQuestion
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuizScreen(
    onBack: () -> Unit,
    viewModel: AiQuizViewModel = hiltViewModel(),
) {
    val quizzes by viewModel.quizzes.collectAsState()
    val genState by viewModel.generateState.collectAsState()
    var showNewSheet by remember { mutableStateOf(false) }
    var activeQuiz by remember { mutableStateOf<AiQuiz?>(null) }
    val haptics = rememberAppHaptics()

    BackHandler(enabled = activeQuiz != null) { activeQuiz = null }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
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
                    items(quizzes, key = { it.id }) { quiz ->
                        QuizRow(
                            quiz = quiz,
                            onStart = { haptics.virtualKey(); activeQuiz = quiz },
                            onDelete = { viewModel.delete(quiz.id) },
                        )
                    }
                }
            }
        }

        activeQuiz?.let { quiz ->
            QuizRunner(
                quiz = quiz,
                modifier = Modifier.fillMaxSize(),
                onRecord = { score, total -> viewModel.recordResult(quiz.id, score, total) },
                onExit = { activeQuiz = null },
            )
        }
    }

    if (showNewSheet) {
        NewQuizSheet(
            generating = genState is AiQuizViewModel.GenerateState.Generating,
            error = (genState as? AiQuizViewModel.GenerateState.Error)?.message,
            materialsProvider = { viewModel.homeworkMaterials() },
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
private fun QuizRow(quiz: AiQuiz, onStart: () -> Unit, onDelete: () -> Unit) {
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.quiz_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewQuizSheet(
    generating: Boolean,
    error: String?,
    materialsProvider: () -> List<HomeworkItem>,
    onDismiss: () -> Unit,
    onGenerate: (String, Int, String, String?) -> Unit,
) {
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(10) }
    var difficulty by remember { mutableStateOf("medium") }
    var material by remember { mutableStateOf<String?>(null) }
    var materialLabel by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var materials by remember { mutableStateOf(emptyList<HomeworkItem>()) }
    val haptics = rememberAppHaptics()

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
                    text = stringResource(R.string.quiz_materials_pick_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (materials.isEmpty()) {
                    Text(
                        text = stringResource(R.string.quiz_materials_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    materials.forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.virtualKey()
                                        val body = buildString {
                                            append(item.title)
                                            if (item.notes.isNotBlank()) {
                                                append("\n\n")
                                                append(item.notes)
                                            }
                                        }
                                        material = body
                                        materialLabel = item.title
                                        if (topic.isBlank()) topic = item.title
                                        showPicker = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = item.title.ifBlank { item.subject.ifBlank { "Homework" } },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                )
                                if (item.notes.isNotBlank()) {
                                    Text(
                                        text = item.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                    )
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
                OutlinedButton(
                    onClick = {
                        haptics.virtualKey()
                        materials = materialsProvider()
                        showPicker = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.quiz_source_homework))
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

private enum class Difficulty(val key: String, val labelRes: Int) {
    EASY("easy", R.string.quiz_difficulty_easy),
    MEDIUM("medium", R.string.quiz_difficulty_medium),
    HARD("hard", R.string.quiz_difficulty_hard),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizRunner(
    quiz: AiQuiz,
    modifier: Modifier = Modifier,
    onRecord: (Int, Int) -> Unit,
    onExit: () -> Unit,
) {
    var index by remember(quiz.id) { mutableIntStateOf(0) }
    var selected by remember(quiz.id) { mutableStateOf<Int?>(null) }
    var score by remember(quiz.id) { mutableIntStateOf(0) }
    var finished by remember(quiz.id) { mutableStateOf(false) }
    val haptics = rememberAppHaptics()

    if (quiz.questions.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            TextButton(onClick = onExit) { Text(stringResource(R.string.quiz_back)) }
        }
        return
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        if (finished) {
            ResultScreen(score = score, total = quiz.questions.size, onRetry = {
                index = 0; selected = null; score = 0; finished = false
            }, onDone = onExit)
            return@Surface
        }

        val question = quiz.questions[index]
        val answered = selected != null

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
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
                                text = stringResource(R.string.quiz_question, index + 1, quiz.questions.size),
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
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            if (optionIndex == question.correctIndex) score++
                        },
                    )
                }
                if (answered) {
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
                            if (index == quiz.questions.lastIndex) {
                                onRecord(score, quiz.questions.size)
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
                            text = if (index == quiz.questions.lastIndex) {
                                stringResource(R.string.quiz_finish)
                            } else {
                                stringResource(R.string.quiz_next)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
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

@Composable
private fun OptionRow(
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val container = when (state) {
        OptionState.CORRECT -> MaterialTheme.colorScheme.primaryContainer
        OptionState.WRONG -> MaterialTheme.colorScheme.errorContainer
        OptionState.IDLE -> MaterialTheme.colorScheme.surfaceBright
    }
    val content = when (state) {
        OptionState.CORRECT -> MaterialTheme.colorScheme.onPrimaryContainer
        OptionState.WRONG -> MaterialTheme.colorScheme.onErrorContainer
        OptionState.IDLE -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
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
            if (state == OptionState.CORRECT || state == OptionState.WRONG) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(score: Int, total: Int, onRetry: () -> Unit, onDone: () -> Unit) {
    val haptics = rememberAppHaptics()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.quiz_result_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.quiz_result_score, score, total),
                style = MaterialTheme.typography.titleLarge,
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
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { haptics.virtualKey(); onRetry() },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(stringResource(R.string.quiz_retry), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { haptics.virtualKey(); onDone() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quiz_back))
            }
        }
    }
}
