package com.wiffles.edupage.ui.prepare

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.timetable.Lesson
import com.wiffles.edupage.R
import com.wiffles.edupage.ui.core.containers.RoundedCardContainer
import com.wiffles.edupage.ui.modifiers.swipeMotionBlur
import com.wiffles.edupage.ui.util.rememberAppHaptics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun PrepareWidgetCard(
    totalCount: Int?,
    preparedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    RoundedCardContainer(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = {
                haptics.virtualKey()
                onClick()
            },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                CookieBadgeSmall()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prepare_widget_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (totalCount == null) {
                            stringResource(R.string.prepare_widget_loading)
                        } else {
                            stringResource(R.string.prepare_widget_subtitle, totalCount, preparedCount)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (totalCount != null && totalCount > 0 && preparedCount >= totalCount) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(6.dp).size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CookieBadgeSmall(modifier: Modifier = Modifier) {
    val container = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = modifier
            .size(56.dp)
            .drawWithCache {
                val poly = RoundedPolygon(
                    numVertices = 6,
                    rounding = CornerRounding(0.4f),
                )
                val androidPath = poly.toPath()
                val path = Path()
                val matrix = Matrix()
                onDrawBehind {
                    path.reset()
                    path.addPath(androidPath.asComposePath())
                    matrix.reset()
                    val s = size.minDimension / 2f * 0.98f
                    matrix.scale(s, s)
                    matrix.translate(1f, 1f)
                    path.transform(matrix)
                    val bounds = path.getBounds()
                    path.translate(
                        androidx.compose.ui.geometry.Offset(
                            (size.width - bounds.width) / 2f - bounds.left,
                            (size.height - bounds.height) / 2f - bounds.top,
                        )
                    )
                    drawPath(path, color = container)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(26.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareFullscreenDialog(
    onClose: () -> Unit,
    viewModel: PrepareViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val preparedMap by viewModel.preparedMap.collectAsState()
    val haptics = rememberAppHaptics()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            when (val state = uiState) {
                is PrepareUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PrepareUiState.Error -> {
                    Column(Modifier.fillMaxSize()) {
                        PrepareTopBar(
                            title = stringResource(R.string.prepare_title),
                            onClose = onClose,
                            onReset = {},
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.load() }) {
                                    Text(stringResource(R.string.timetable_retry))
                                }
                            }
                        }
                    }
                }
                is PrepareUiState.Success -> {
                    val prepared = preparedMap[state.date.toString()] ?: emptySet()
                    var queue by remember(state.date, state.lessons) {
                        mutableStateOf(state.lessons.filter { viewModel.lessonKey(it) !in prepared })
                    }
                    val motionBlurEnabled = viewModel.isMotionBlurEnabled()
                    val total = state.lessons.size
                    val doneCount = prepared.size.coerceAtMost(total)

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surface,
                        topBar = {
                            PrepareTopBar(
                                title = stringResource(
                                    R.string.prepare_title_date,
                                    state.date.format(
                                        DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
                                    ),
                                ),
                                onClose = onClose,
                                onReset = {
                                    haptics.virtualKey()
                                    viewModel.resetDay(state.date)
                                    queue = state.lessons
                                },
                            )
                        },
                        bottomBar = {
                            if (queue.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp, vertical = 20.dp),
                                ) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            haptics.virtualKey()
                                            val top = queue.firstOrNull() ?: return@FilledTonalIconButton
                                            queue = queue.drop(1) + top
                                        },
                                        modifier = Modifier.size(64.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.prepare_later),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                    FilledTonalIconButton(
                                        onClick = {
                                            haptics.virtualKey()
                                            val top = queue.firstOrNull() ?: return@FilledTonalIconButton
                                            viewModel.markPrepared(state.date, top)
                                            queue = queue.drop(1)
                                            if (queue.isEmpty()) haptics.confirm()
                                        },
                                        modifier = Modifier.size(64.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = stringResource(R.string.prepare_prepared),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                            }
                        },
                    ) { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LinearProgressIndicator(
                                progress = { if (total == 0) 1f else doneCount.toFloat() / total },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            )
                            Text(
                                text = stringResource(R.string.prepare_progress, doneCount, total),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            if (queue.isEmpty()) {
                                PrepareDonePanel(
                                    date = state.date,
                                    onClose = onClose,
                                )
                            } else {
                                SwipeDeck(
                                    queue = queue,
                                    lessonKey = viewModel::lessonKey,
                                    motionBlurEnabled = motionBlurEnabled,
                                    onSwipeRight = { lesson ->
                                        haptics.virtualKey()
                                        viewModel.markPrepared(state.date, lesson)
                                        queue = queue.drop(1)
                                        if (queue.size == 1) haptics.confirm()
                                    },
                                    onSwipeLeft = { lesson ->
                                        haptics.virtualKey()
                                        queue = queue.drop(1) + lesson
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.prepare_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrepareTopBar(
    title: String,
    onClose: () -> Unit,
    onReset: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.prepare_reset),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun SwipeDeck(
    queue: List<Lesson>,
    lessonKey: (Lesson) -> String,
    motionBlurEnabled: Boolean,
    onSwipeRight: (Lesson) -> Unit,
    onSwipeLeft: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val density = LocalDensity.current
        val thresholdPx = remember(maxWidth, density) {
            with(density) { maxWidth.toPx() * 0.22f }
        }
        val visible = queue.take(3)
        visible.indices.reversed().forEach { depth ->
            val lesson = visible[depth]
            if (depth == 0) {
                key(lessonKey(lesson)) {
                    SwipeablePrepareCard(
                        lesson = lesson,
                        swipeThresholdPx = thresholdPx,
                        motionBlurEnabled = motionBlurEnabled,
                        onSwipeRight = { onSwipeRight(lesson) },
                        onSwipeLeft = { onSwipeLeft(lesson) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                key(lessonKey(lesson)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1f - 0.05f * depth
                                scaleY = 1f - 0.05f * depth
                                translationY = 14.dp.toPx() * depth
                                alpha = 1f - 0.15f * depth
                            },
                    ) {
                        PrepareCardContent(lesson = lesson)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeablePrepareCard(
    lesson: Lesson,
    swipeThresholdPx: Float,
    motionBlurEnabled: Boolean,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val blurVelocity = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val velocityTracker = remember { androidx.compose.ui.input.pointer.util.VelocityTracker() }

    androidx.compose.runtime.LaunchedEffect(dragging) {
        if (!dragging) {
            blurVelocity.animateTo(0f, tween(60))
        }
    }

    fun commit(right: Boolean) {
        scope.launch {
            offsetX.animateTo(if (right) 1400f else -1400f, tween(220))
            if (right) onSwipeRight() else onSwipeLeft()
        }
    }

    Box(
        modifier = modifier
            .swipeMotionBlur(
                velocity = blurVelocity.value,
                enabled = true,
            )
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = (offsetX.value / 60f).coerceIn(-12f, 12f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                            val instant = (dragAmount.x / 16f).coerceIn(-3f, 3f)
                            blurVelocity.snapTo(blurVelocity.value * 0.55f + instant * 0.45f)
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        val x = offsetX.value
                        val vx = runCatching { velocityTracker.calculateVelocity().x }.getOrDefault(0f)
                        when {
                            x > swipeThresholdPx || vx > 1100f -> commit(right = true)
                            x < -swipeThresholdPx || vx < -1100f -> commit(right = false)
                            else -> scope.launch {
                                launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
                                launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
                            }
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring()) }
                            launch { offsetY.animateTo(0f, spring()) }
                        }
                    },
                )
            },
    ) {
        PrepareCardContent(lesson = lesson)

        val stampStart = swipeThresholdPx * 0.35f
        val rightAlpha = ((offsetX.value - stampStart) / (swipeThresholdPx - stampStart).coerceAtLeast(1f))
            .coerceIn(0f, 1f)
        val leftAlpha = ((-offsetX.value - stampStart) / (swipeThresholdPx - stampStart).coerceAtLeast(1f))
            .coerceIn(0f, 1f)
        if (rightAlpha > 0.02f || leftAlpha > 0.02f || dragging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (rightAlpha >= leftAlpha) {
                    Stamp(
                        text = stringResource(R.string.prepare_stamp_prepared),
                        color = MaterialTheme.colorScheme.primary,
                        alpha = rightAlpha,
                        rotation = -18f,
                    )
                } else {
                    Stamp(
                        text = stringResource(R.string.prepare_stamp_later),
                        color = MaterialTheme.colorScheme.error,
                        alpha = leftAlpha,
                        rotation = 18f,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stamp(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
    rotation: Float,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(3.dp, color.copy(alpha = alpha)),
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation
            this.alpha = alpha
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = color.copy(alpha = alpha),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PrepareCardContent(lesson: Lesson) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lesson.period?.let {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "$it.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                lesson.startTime?.let { start ->
                    lesson.endTime?.let { end ->
                        Text(
                            text = "${start}–$end",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = lesson.subject?.name ?: lesson.curriculum ?: "–",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val meta = buildList {
                lesson.teachers?.mapNotNull { it.name }?.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")?.let { add(it) }
                lesson.classrooms?.mapNotNull { it.name }?.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")?.let { add(it) }
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!lesson.curriculum.isNullOrBlank() && lesson.subject?.name != null) {
                Text(
                    text = lesson.curriculum!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (lesson.hasChange()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.timetable_badge_changed),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrepareDonePanel(
    date: LocalDate,
    onClose: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.prepare_all_done),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.prepare_all_done_desc,
                date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.prepare_back),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

