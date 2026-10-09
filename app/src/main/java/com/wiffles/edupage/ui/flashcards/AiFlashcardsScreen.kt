package com.wiffles.edupage.ui.flashcards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiffles.edupage.R
import com.wiffles.edupage.data.FlashcardDeck
import com.wiffles.edupage.data.MaterialLoadState
import com.wiffles.edupage.data.StudyMaterial
import com.wiffles.edupage.ui.core.sheets.AppBottomSheet
import com.wiffles.edupage.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFlashcardsScreen(
    onBack: () -> Unit,
    viewModel: AiFlashcardsViewModel = hiltViewModel(),
) {
    val decks by viewModel.decks.collectAsState()
    val genState by viewModel.generateState.collectAsState()
    val examState by viewModel.examMaterials.collectAsState()
    var showNewSheet by remember { mutableStateOf(false) }
    var activeDeck by remember { mutableStateOf<FlashcardDeck?>(null) }
    val haptics = rememberAppHaptics()

    BackHandler(enabled = activeDeck != null) { activeDeck = null }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.flash_title),
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
                        IconButton(onClick = { haptics.virtualKey(); showNewSheet = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.flash_new))
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
            if (decks.isEmpty()) {
                FlashcardsEmptyState(Modifier.fillMaxSize().padding(padding)) {
                    haptics.virtualKey()
                    showNewSheet = true
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(decks, key = { it.id }) { deck ->
                        DeckRow(
                            deck = deck,
                            onStudy = { haptics.virtualKey(); activeDeck = deck },
                            onDelete = { viewModel.delete(deck.id) },
                        )
                    }
                }
            }
        }

        activeDeck?.let { deck ->
            StudyRunner(
                deck = deck,
                modifier = Modifier.fillMaxSize(),
                onRecord = { known -> viewModel.recordProgress(deck.id, known) },
                onExit = { activeDeck = null },
            )
        }
    }

    if (showNewSheet) {
        NewDeckSheet(
            generating = genState is AiFlashcardsViewModel.GenerateState.Generating,
            error = (genState as? AiFlashcardsViewModel.GenerateState.Error)?.message,
            homeworkProvider = { viewModel.homeworkMaterials() },
            examState = examState,
            onLoadExamMaterials = { viewModel.loadExamMaterials() },
            onDismiss = {
                viewModel.consumeError()
                showNewSheet = false
            },
            onGenerate = { topic, count, material ->
                viewModel.generate(topic, count, material) { created ->
                    showNewSheet = false
                    activeDeck = created
                }
            },
        )
    }
}

@Composable
private fun FlashcardsEmptyState(modifier: Modifier, onNew: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Rounded.Style,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.flash_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onNew, shape = RoundedCornerShape(20.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.flash_new))
            }
        }
    }
}

@Composable
private fun DeckRow(deck: FlashcardDeck, onStudy: () -> Unit, onDelete: () -> Unit) {
    val known = deck.knownIndices.count { it in deck.cards.indices }
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
                modifier = Modifier.weight(1f).clickable(onClick = onStudy),
            ) {
                Text(
                    text = deck.topic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Text(
                    text = stringResource(R.string.flash_deck_count, deck.cards.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (known > 0) {
                    Text(
                        text = stringResource(R.string.flash_best_known, known, deck.cards.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.flash_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private enum class FlashSource { TOPIC, MATERIAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDeckSheet(
    generating: Boolean,
    error: String?,
    homeworkProvider: () -> List<StudyMaterial>,
    examState: MaterialLoadState,
    onLoadExamMaterials: () -> Unit,
    onDismiss: () -> Unit,
    onGenerate: (String, Int, String?) -> Unit,
) {
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(10) }
    var source by remember { mutableStateOf(FlashSource.TOPIC) }
    var material by remember { mutableStateOf<String?>(null) }
    var materialLabel by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var pickerExam by remember { mutableStateOf(false) }
    var homeworkMaterials by remember { mutableStateOf(emptyList<StudyMaterial>()) }
    val haptics = rememberAppHaptics()

    val pickerItems: List<StudyMaterial> = if (pickerExam) {
        (examState as? MaterialLoadState.Loaded)?.items.orEmpty()
    } else homeworkMaterials

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
                    text = stringResource(R.string.flash_materials_pick_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                when {
                    pickerExam && examState is MaterialLoadState.Loading -> Box(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                    pickerExam && examState is MaterialLoadState.Error -> Text(
                        text = examState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    pickerItems.isEmpty() -> Text(
                        text = stringResource(R.string.quiz_materials_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> pickerItems.forEach { item ->
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
                                        material = item.body
                                        materialLabel = item.title
                                        if (topic.isBlank()) topic = item.title
                                        showPicker = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                )
                                item.subtitle?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
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
                    Text(stringResource(R.string.flash_back))
                }
                return@Column
            }

            Text(
                text = stringResource(R.string.flash_new),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text(stringResource(R.string.flash_topic)) },
                placeholder = { Text(stringResource(R.string.flash_topic_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = source == FlashSource.TOPIC,
                    onClick = {
                        haptics.virtualKey()
                        source = FlashSource.TOPIC
                        material = null
                        materialLabel = null
                    },
                    label = { Text(stringResource(R.string.flash_source_topic)) },
                )
                FilterChip(
                    selected = source == FlashSource.MATERIAL,
                    onClick = { haptics.virtualKey(); source = FlashSource.MATERIAL },
                    label = { Text(stringResource(R.string.flash_source_material)) },
                )
            }
            if (source == FlashSource.MATERIAL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.virtualKey()
                            pickerExam = false
                            homeworkMaterials = homeworkProvider()
                            showPicker = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.quiz_source_homework))
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.virtualKey()
                            pickerExam = true
                            onLoadExamMaterials()
                            showPicker = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.quiz_source_exam))
                    }
                }
                materialLabel?.let { label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        TextButton(onClick = { material = null; materialLabel = null }) {
                            Text(stringResource(R.string.flash_clear_materials))
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.flash_count),
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
                    onGenerate(topic, count, if (source == FlashSource.MATERIAL) material else null)
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
                    Text(stringResource(R.string.flash_generating))
                } else {
                    Text(stringResource(R.string.flash_generate), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyRunner(
    deck: FlashcardDeck,
    modifier: Modifier = Modifier,
    onRecord: (Set<Int>) -> Unit,
    onExit: () -> Unit,
) {
    var index by remember(deck.id) { mutableIntStateOf(0) }
    var revealed by remember(deck.id) { mutableStateOf(false) }
    var known by remember(deck.id) { mutableStateOf(deck.knownIndices.toSet()) }
    var finished by remember(deck.id) { mutableStateOf(false) }
    var rotation by remember(deck.id, index) { mutableFloatStateOf(0f) }
    val haptics = rememberAppHaptics()

    if (deck.cards.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            TextButton(onClick = onExit) { Text(stringResource(R.string.flash_back)) }
        }
        return
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        if (finished) {
            FlashResult(
                known = known.count { it in deck.cards.indices },
                total = deck.cards.size,
                onAgain = {
                    index = 0; revealed = false; known = emptySet(); finished = false; rotation = 0f
                },
                onDone = onExit,
            )
            return@Surface
        }

        val card = deck.cards[index]
        val total = deck.cards.size
        val animatedRotation by animateFloatAsState(
            targetValue = rotation,
            animationSpec = tween(420),
            label = "flip",
        )
        val showBack = animatedRotation > 90f

        fun goNext(markKnown: Boolean) {
            val nextKnown = if (markKnown) known + index else known
            known = nextKnown
            if (index >= total - 1) {
                finished = true
                onRecord(nextKnown)
            } else {
                index += 1
                revealed = false
                rotation = 0f
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = deck.topic,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            Text(
                                text = stringResource(R.string.flash_card_of, index + 1, total),
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LinearProgressIndicator(
                    progress = { (index + if (revealed) 1 else 0).toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f)
                            .graphicsLayer {
                                rotationY = animatedRotation
                                cameraDistance = 16 * density
                            }
                            .clickable(enabled = !revealed) {
                                haptics.virtualKey()
                                revealed = true
                                rotation = 180f
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showBack) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = card.back,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center,
                                    )
                                    card.hint?.takeIf { it.isNotBlank() }?.let { hint ->
                                        Spacer(Modifier.height(16.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.flash_hint) + ": " + hint,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = card.front,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Text(
                                        text = stringResource(R.string.flash_tap_reveal),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = revealed,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "actions",
                ) { isRevealed ->
                    if (isRevealed) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.virtualKey()
                                    goNext(markKnown = false)
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).height(54.dp),
                            ) {
                                Text(stringResource(R.string.flash_still_learning))
                            }
                            Button(
                                onClick = {
                                    haptics.virtualKey()
                                    goNext(markKnown = true)
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).height(54.dp),
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.flash_i_know))
                            }
                        }
                    } else {
                        Button(
                            onClick = { haptics.virtualKey(); revealed = true; rotation = 180f },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(54.dp),
                        ) {
                            Text(stringResource(R.string.flash_tap_reveal), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashResult(
    known: Int,
    total: Int,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.flash_result_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.flash_result_known, known, total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { haptics.virtualKey(); onAgain() },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(stringResource(R.string.flash_again), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { haptics.virtualKey(); onDone() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.flash_back))
            }
        }
    }
}