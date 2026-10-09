package com.enderplusbayzuiship.edupage2.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.Meal
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.timetable.Lesson
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.cards.FeatureCard
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelIcon
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.modifiers.scrollMotionBlur
import com.enderplusbayzuiship.edupage2.ui.prepare.PrepareFullscreenDialog
import com.enderplusbayzuiship.edupage2.ui.prepare.PrepareUiState
import com.enderplusbayzuiship.edupage2.ui.prepare.PrepareViewModel
import com.enderplusbayzuiship.edupage2.ui.prepare.PrepareWidgetCard
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    bottomPadding: PaddingValues,
    onSettings: () -> Unit = {},
    onGoToMessages: (() -> Unit)? = null,
    onGoToTimetable: (() -> Unit)? = null,
    onGoToGrades: (() -> Unit)? = null,
    onGoToMeals: (() -> Unit)? = null,
    onHomework: (() -> Unit)? = null,
    aiStudyEnabled: Boolean = false,
    onAiQuiz: (() -> Unit)? = null,
    onAiFlashcards: (() -> Unit)? = null,
    onAiChat: (() -> Unit)? = null,
    mealsEnabled: Boolean = true,
    viewModel: OverviewViewModel = hiltViewModel(),
    prepareViewModel: PrepareViewModel = hiltViewModel(),
) {
    val timetableState by viewModel.timetableState.collectAsState()
    val gradesState    by viewModel.gradesState.collectAsState()
    val messagesState  by viewModel.messagesState.collectAsState()
    val mealsState     by viewModel.mealsState.collectAsState()
    val homeworkItems  by viewModel.homeworkItems.collectAsState()
    val isRefreshing   by viewModel.isRefreshing.collectAsState()
    val currentTime    by viewModel.currentTime.collectAsState()
    val showSeconds    by viewModel.showSeconds.collectAsState()
    val haptics        = rememberAppHaptics()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val overviewScroll = rememberScrollState()
    var showPrepare    by remember { mutableStateOf(false) }

    val prepareState by prepareViewModel.uiState.collectAsState()
    val preparedMap  by prepareViewModel.preparedMap.collectAsState()

    val isInitialLoading = timetableState is TimetableOverviewState.Loading &&
        gradesState is GradesOverviewState.Loading &&
        messagesState is MessagesOverviewState.Loading &&
        (!mealsEnabled || mealsState is MealsOverviewState.Loading)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.overview_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    FilledTonalIconButton(onClick = { haptics.click(); viewModel.refresh() }) {
                        if (isRefreshing) {
                            RotatingRefreshIcon()
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.overview_refresh),
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(onClick = { haptics.click(); onSettings() }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.overview_settings))
                    }
                    Spacer(Modifier.width(8.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues),
        ) {
            AnimatedVisibility(visible = isRefreshing) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }

            if (isInitialLoading) {
                OverviewSkeleton(
                    bottomPadding = bottomPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollMotionBlur(
                            scrollState = overviewScroll,
                            enabled = com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate.forTabs(),
                        )
                        .verticalScroll(overviewScroll)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp + bottomPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(Modifier.height(4.dp))

                    OverviewHero(
                        timetableState = timetableState,
                        messagesState = messagesState,
                        todoCount = homeworkItems.count { !it.done },
                    )

                    val prepareSuccess = prepareState as? PrepareUiState.Success
                    val prepareTotal = prepareSuccess?.lessons?.size
                    val prepareDone = prepareSuccess?.let { s ->
                        (preparedMap[s.date.toString()]?.size ?: 0).coerceAtMost(s.lessons.size)
                    } ?: 0
                    PrepareWidgetCard(
                        totalCount = prepareTotal,
                        preparedCount = prepareDone,
                        onClick = { showPrepare = true },
                    )

                    TimetableCard(
                        state = timetableState,
                        currentTime = currentTime,
                        onGoToTimetable = onGoToTimetable,
                        haptics = haptics,
                        showSeconds = showSeconds,
                        onRetry = viewModel::refresh,
                    )

                    GradesCard(
                        state = gradesState,
                        onGoToGrades = onGoToGrades,
                        haptics = haptics,
                        onRetry = viewModel::refresh,
                    )

                    HomeworkCard(
                        items = homeworkItems,
                        onHomework = onHomework,
                        haptics = haptics,
                    )

                    MessagesCard(
                        state = messagesState,
                        onGoToMessages = onGoToMessages,
                        haptics = haptics,
                        onRetry = viewModel::refresh,
                    )

                    if (mealsEnabled) {
                        MealsCard(
                            state = mealsState,
                            onGoToMeals = onGoToMeals,
                            haptics = haptics,
                            onRetry = viewModel::refresh,
                        )
                    }

                    QuickActionsCard(
                        onCompose   = onGoToMessages,
                        onTimetable = onGoToTimetable,
                        onGrades    = onGoToGrades,
                        onMeals     = if (mealsEnabled) onGoToMeals else null,
                        onHomework  = onHomework,
                        haptics     = haptics,
                    )

                    if (aiStudyEnabled && (onAiQuiz != null || onAiFlashcards != null || onAiChat != null)) {
                        AiStudyCard(
                            onQuiz       = onAiQuiz,
                            onFlashcards = onAiFlashcards,
                            onChat       = onAiChat,
                        )
                    }
                }
            }
        }
    }

    if (showPrepare) {
        PrepareFullscreenDialog(onClose = { showPrepare = false })
    }
}

@Composable
private fun RotatingRefreshIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = stringResource(R.string.overview_refresh),
        modifier = Modifier.rotate(rotation),
    )
}

@Composable
private fun OverviewHero(
    timetableState: TimetableOverviewState,
    messagesState: MessagesOverviewState,
    todoCount: Int,
) {
    val today = LocalDate.now()
    val dateTitle = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val lessonCount = (timetableState as? TimetableOverviewState.Success)?.lessons?.size
    val unreadCount = (messagesState as? MessagesOverviewState.Success)?.unreadCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CookieBadge(number = today.dayOfMonth.toString())
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val stats = buildList {
                lessonCount?.let { add(stringResource(R.string.overview_stat_lessons, it)) }
                unreadCount?.let { add(stringResource(R.string.overview_stat_unread, it)) }
                add(stringResource(R.string.overview_stat_todo, todoCount))
            }.joinToString(" · ")
            Text(
                text = stats,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CookieBadge(
    number: String,
    modifier: Modifier = Modifier,
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = modifier
            .size(64.dp)
            .drawWithCache {
                val poly = RoundedPolygon(
                    numVertices = 6,
                    rounding = CornerRounding(0.35f),
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
                        Offset(
                            (size.width - bounds.width) / 2f - bounds.left,
                            (size.height - bounds.height) / 2f - bounds.top,
                        )
                    )
                    drawPath(path, color = container)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

@Composable
private fun TimetableCard(
    state: TimetableOverviewState,
    currentTime: LocalTime,
    onGoToTimetable: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
    showSeconds: Boolean,
    onRetry: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    OverviewCard {
        when (state) {
            is TimetableOverviewState.Loading -> TimetableCardLoading()
            is TimetableOverviewState.Error   -> TimetableCardError(onRetry = onRetry)
            is TimetableOverviewState.Success -> TimetableCardContent(
                state        = state,
                currentTime  = currentTime,
                expanded     = expanded,
                onToggle     = { haptics.virtualKey(); expanded = !expanded },
                onGoToTimetable = onGoToTimetable,
                haptics      = haptics,
                showSeconds  = showSeconds,
            )
        }
    }
}

@Composable
private fun TimetableCardLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardSectionHeader(
            title = stringResource(R.string.overview_today_schedule),
            icon  = Icons.Default.DateRange,
        )
        Spacer(Modifier.height(2.dp))
        CurrentLessonBannerSkeleton()
        repeat(3) { CompactLessonRowSkeleton() }
    }
}

@Composable
private fun TimetableCardError(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardSectionHeader(
            title = stringResource(R.string.overview_today_schedule),
            icon  = Icons.Default.DateRange,
        )
        ErrorMessageBanner(text = stringResource(R.string.overview_error_timetable))
        RetryButton(onRetry = onRetry)
    }
}

@Composable
private fun RetryButton(onRetry: () -> Unit) {
    OutlinedButton(
        onClick = onRetry,
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(stringResource(R.string.overview_retry))
    }
}

@Composable
private fun ErrorMessageBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun TimetableCardContent(
    state: TimetableOverviewState.Success,
    currentTime: LocalTime,
    expanded: Boolean,
    onToggle: () -> Unit,
    onGoToTimetable: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
    showSeconds: Boolean,
) {
    val lessons = state.lessons
    val title = when {
        !state.isNextDay -> stringResource(R.string.overview_today_schedule)
        state.date == LocalDate.now().plusDays(1) -> stringResource(R.string.overview_tomorrow_schedule)
        else -> stringResource(
            R.string.overview_schedule_for,
            state.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
        )
    }

    Column(modifier = Modifier.animateContentSize(tween(250))) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = lessons.size > TIMETABLE_COLLAPSED_COUNT) { onToggle() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (lessons.size > TIMETABLE_COLLAPSED_COUNT) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (lessons.isEmpty()) {
            Text(
                text  = stringResource(R.string.overview_no_lessons),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val currentLesson = if (!state.isNextDay)
            lessons.firstOrNull { it.startTime != null && it.endTime != null &&
                !it.isCancelled &&
                currentTime >= it.startTime!! && currentTime < it.endTime!! }
        else null

        val nextLesson = if (!state.isNextDay)
            lessons.firstOrNull { it.startTime != null && !it.isCancelled &&
                currentTime < it.startTime!! }
        else null

        if (!state.isNextDay && currentLesson == null) {
            val next = nextLesson
            val start = next?.startTime
            if (next != null && start != null) {
                val secondsTo = Duration.between(currentTime, start).toSeconds()
                if (secondsTo in 0..(180 * 60)) {
                    NextBellBar(
                        label = stringResource(
                            R.string.overview_next_starts_in,
                            getDisplayName(next),
                            formatTimeLeft(secondsTo, showSeconds)
                        )
                    )
                }
            }
        }

        if (currentLesson != null) {
            CurrentLessonBanner(lesson = currentLesson, currentTime = currentTime, showSeconds = showSeconds)
            Spacer(Modifier.height(10.dp))
        }

        val visibleLessons = if (expanded) lessons else lessons.take(TIMETABLE_COLLAPSED_COUNT)
        visibleLessons.forEachIndexed { index, lesson ->
            CompactLessonRow(
                lesson    = lesson,
                isCurrent = lesson == currentLesson,
                isNext    = lesson == nextLesson && currentLesson == null,
            )
            if (index < visibleLessons.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
        }

        val hiddenCount = lessons.size - TIMETABLE_COLLAPSED_COUNT
        if (hiddenCount > 0) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { haptics.virtualKey(); onToggle() },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text(
                        text  = if (expanded) stringResource(R.string.overview_show_less)
                                else stringResource(R.string.overview_lessons_more, hiddenCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (onGoToTimetable != null) {
                    TextButton(
                        onClick = { haptics.virtualKey(); onGoToTimetable() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text  = stringResource(R.string.overview_see_timetable),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else if (onGoToTimetable != null) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { haptics.virtualKey(); onGoToTimetable() },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text(
                        text  = stringResource(R.string.overview_see_timetable),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentLessonBanner(lesson: Lesson, currentTime: LocalTime, showSeconds: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = stringResource(R.string.overview_current_lesson),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text       = getDisplayName(lesson),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                val details = buildList {
                    lesson.teachers?.firstOrNull()?.name?.let { add(it) }
                    lesson.classrooms?.firstOrNull()?.name?.let { add(it) }
                }.joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(
                        text     = details,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            lesson.endTime?.let { end ->
                val minsLeft = java.time.Duration.between(currentTime, end).toSeconds()
                if (minsLeft >= 0) {
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text       = formatTimeLeft(minsLeft, showSeconds),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onPrimary,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactLessonRow(
    lesson: Lesson,
    isCurrent: Boolean,
    isNext: Boolean,
) {
    val alpha = if (lesson.isCancelled) 0.45f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrent) {
                    Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer)
                } else Modifier
            )
            .padding(vertical = 7.dp, horizontal = if (isCurrent) 10.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text       = lesson.period?.let { "$it." } ?: "–",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color      = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                         else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            modifier   = Modifier.width(26.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = getDisplayName(lesson),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent || isNext) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                             else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            val teacher = lesson.teachers?.firstOrNull()?.name
            val room    = lesson.classrooms?.firstOrNull()?.name
            val meta    = listOfNotNull(teacher, room).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text     = meta,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            val startStr = lesson.startTime?.format(timeFmt) ?: "?"
            val endStr   = lesson.endTime?.format(timeFmt)   ?: "?"
            Text(
                text  = "$startStr–$endStr",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
            if (lesson.isCancelled) {
                Text(
                    text       = stringResource(R.string.timetable_badge_cancelled),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            } else if (isNext) {
                Text(
                    text       = stringResource(R.string.overview_next_lesson),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun GradesCard(
    state: GradesOverviewState,
    onGoToGrades: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
    onRetry: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    OverviewCard {
        Column(modifier = Modifier.animateContentSize(tween(250))) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .let { m ->
                        val grades = (state as? GradesOverviewState.Success)?.recentGrades
                        if (grades != null && grades.size > GRADES_COLLAPSED_COUNT)
                            m.clickable { haptics.virtualKey(); expanded = !expanded }
                        else m
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text       = stringResource(R.string.overview_recent_grades),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                val grades = (state as? GradesOverviewState.Success)?.recentGrades
                if (grades != null && grades.size > GRADES_COLLAPSED_COUNT) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when (state) {
                is GradesOverviewState.Loading -> repeat(3) { GradeRowSkeleton() }
                is GradesOverviewState.Unavailable -> {
                    ErrorMessageBanner(text = stringResource(R.string.overview_no_grades))
                    RetryButton(onRetry = onRetry)
                }
                is GradesOverviewState.Success -> {
                    if (state.recentGrades.isEmpty()) {
                        Text(
                            text  = stringResource(R.string.overview_no_grades),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val visible = if (expanded) state.recentGrades else state.recentGrades.take(GRADES_COLLAPSED_COUNT)
                        visible.forEachIndexed { index, grade ->
                            GradeRow(grade = grade)
                            if (index < visible.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                        val hiddenCount = state.recentGrades.size - GRADES_COLLAPSED_COUNT
                        if (hiddenCount > 0 || onGoToGrades != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (hiddenCount > 0) {
                                    TextButton(
                                        onClick = { haptics.virtualKey(); expanded = !expanded },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    ) {
                                        Text(
                                            text  = if (expanded) stringResource(R.string.overview_show_less)
                                                    else stringResource(R.string.overview_grades_more, hiddenCount),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Icon(
                                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(0.dp))
                                }
                                if (onGoToGrades != null) {
                                    TextButton(
                                        onClick = { haptics.virtualKey(); onGoToGrades() },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    ) {
                                        Text(
                                            text  = stringResource(R.string.overview_see_grades),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeRow(grade: EduGrade) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val gradeText = when (val v = grade.gradeN) {
            is Double -> if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            is String -> v
            else      -> "–"
        }
        val numericValue = (grade.gradeN as? Double)
        val bubbleColor  = if (numericValue != null) gradeColor(numericValue)
                           else MaterialTheme.colorScheme.secondary

        Surface(
            shape    = CircleShape,
            color    = bubbleColor.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text       = gradeText,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = bubbleColor,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = grade.subjectName ?: stringResource(R.string.grades_unknown_subject),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (grade.title.isNotBlank()) {
                Text(
                    text     = grade.title,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text  = grade.date.toLocalDate().format(dateFmt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeworkCard(
    items: List<com.enderplusbayzuiship.edupage2.data.HomeworkItem>,
    onHomework: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
) {
    OverviewCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { m -> if (onHomework != null) m.clip(RoundedCornerShape(12.dp)).clickable { haptics.virtualKey(); onHomework() } else m },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.overview_my_homework),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (onHomework != null) {
                    FilledTonalIconButton(
                        onClick = { haptics.virtualKey(); onHomework() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.overview_action_homework),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            val pendingHomework = items
                .filter { !it.done }
                .sortedBy { it.date }

            if (pendingHomework.isEmpty()) {
                Text(
                    text = stringResource(R.string.overview_no_homework),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                pendingHomework.take(3).forEach { item ->
                    HomeworkOverviewRow(item = item)
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { haptics.virtualKey(); onHomework?.invoke() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.overview_see_homework),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeworkOverviewRow(item: com.enderplusbayzuiship.edupage2.data.HomeworkItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PastelIcon(
            icon = Icons.AutoMirrored.Filled.Assignment,
            key = item.subject.ifBlank { item.title },
            containerSize = 40.dp,
            iconSize = 20.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subject.isNotBlank()) {
                Text(
                    text = item.subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = formatOverviewDate(item.date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatOverviewDate(iso: String): String {
    return try {
        java.time.LocalDate.parse(iso).format(dateFmt)
    } catch (e: Exception) {
        iso
    }
}

@Composable
private fun MessagesCard(
    state: MessagesOverviewState,
    onGoToMessages: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
    onRetry: () -> Unit,
) {
    OverviewCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { m -> if (onGoToMessages != null) m.clip(RoundedCornerShape(12.dp)).clickable { haptics.virtualKey(); onGoToMessages() } else m },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val unread = (state as? MessagesOverviewState.Success)?.unreadCount ?: 0
                    BadgedBox(
                        badge = {
                            if (unread > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor   = MaterialTheme.colorScheme.onError,
                                ) {
                                    Text(
                                        text  = if (unread > 99) "99+" else unread.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MailOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text       = stringResource(R.string.overview_recent_messages),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (onGoToMessages != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when (state) {
                is MessagesOverviewState.Loading -> repeat(3) { MessageRowSkeleton() }
                is MessagesOverviewState.Unavailable -> {
                    ErrorMessageBanner(text = stringResource(R.string.overview_no_messages))
                    RetryButton(onRetry = onRetry)
                }
                is MessagesOverviewState.Success -> {
                    if (state.recentMessages.isEmpty()) {
                        Text(
                            text  = stringResource(R.string.overview_no_messages),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.recentMessages.forEachIndexed { index, event ->
                            MessageRow(event = event)
                            if (index < state.recentMessages.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                        if (onGoToMessages != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = { haptics.virtualKey(); onGoToMessages() },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                ) {
                                    Text(
                                        text  = stringResource(R.string.overview_see_messages),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(event: TimelineEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        val initials = event.authorName
            ?.split(" ")
            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
            ?.take(2)
            ?.joinToString("") ?: "?"
        PastelIcon(
            icon = Icons.Rounded.MailOutline,
            key = event.authorName ?: initials,
            containerSize = 40.dp,
            iconSize = 20.dp,
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = event.authorName ?: stringResource(R.string.messages_unknown_sender),
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f),
                )
                event.timestamp?.let {
                    Text(
                        text  = it.toLocalDate().format(dateFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val preview = (event.title ?: event.text)
                ?.replace(Regex("<[^>]+>"), "")
                ?.trim()
                ?.take(80)
            if (!preview.isNullOrBlank()) {
                Text(
                    text     = preview,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MealsCard(
    state: MealsOverviewState,
    onGoToMeals: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
    onRetry: () -> Unit,
) {
    OverviewCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { m -> if (onGoToMeals != null) m.clip(RoundedCornerShape(12.dp)).clickable { haptics.virtualKey(); onGoToMeals() } else m },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text       = stringResource(R.string.overview_today_meals),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (onGoToMeals != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when (state) {
                is MealsOverviewState.Loading -> {
                    repeat(2) { MealCardRowSkeleton() }
                }
                is MealsOverviewState.Unavailable -> {
                    ErrorMessageBanner(text = stringResource(R.string.overview_no_meals))
                    RetryButton(onRetry = onRetry)
                }
                is MealsOverviewState.Success -> {
                    if (state.meals.isEmpty()) {
                        Text(
                            text  = stringResource(R.string.overview_no_meals),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val visibleMeals = state.meals.take(3)
                        visibleMeals.forEachIndexed { index, meal ->
                            MealsCardRow(meal = meal)
                            if (index < visibleMeals.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                        if (onGoToMeals != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = { haptics.virtualKey(); onGoToMeals() },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                ) {
                                    Text(
                                        text  = stringResource(R.string.overview_see_meals),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealsCardRow(meal: Meal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PastelIcon(
            icon = Icons.Default.Restaurant,
            key = meal.name,
            containerSize = 40.dp,
            iconSize = 20.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = meal.name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
            val details = buildList {
                meal.weight?.let { add(it) }
                meal.allergens?.let { add("A: ${it.joinToString(", ")}") }
            }.joinToString(" · ")
            if (details.isNotBlank()) {
                Text(
                    text     = details,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MealCardRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(4.dp),
                    ),
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(10.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(4.dp),
                    ),
            )
        }
    }
}

@Composable
private fun AiStudyCard(
    onQuiz: (() -> Unit)?,
    onFlashcards: (() -> Unit)?,
    onChat: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        CardSectionHeader(
            title = stringResource(R.string.overview_ai_study),
            icon  = Icons.Rounded.AutoAwesome,
        )
        Spacer(Modifier.height(4.dp))

        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            if (onQuiz != null) {
                FeatureCard(
                    title = stringResource(R.string.quiz_title),
                    description = stringResource(R.string.overview_ai_quiz_desc),
                    icon  = Icons.Rounded.Quiz,
                    onClick = onQuiz,
                )
            }
            if (onFlashcards != null) {
                FeatureCard(
                    title = stringResource(R.string.flash_title),
                    description = stringResource(R.string.overview_ai_flashcards_desc),
                    icon  = Icons.Rounded.Style,
                    onClick = onFlashcards,
                )
            }
            if (onChat != null) {
                FeatureCard(
                    title = stringResource(R.string.chat_title),
                    description = stringResource(R.string.overview_ai_chat_desc),
                    icon  = Icons.Rounded.Psychology,
                    onClick = onChat,
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onCompose:   (() -> Unit)?,
    onTimetable: (() -> Unit)?,
    onGrades:    (() -> Unit)?,
    onMeals:     (() -> Unit)?,
    onHomework:  (() -> Unit)?,
    haptics:     com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        CardSectionHeader(
            title = stringResource(R.string.overview_quick_actions),
            icon  = Icons.AutoMirrored.Filled.Assignment,
        )
        Spacer(Modifier.height(4.dp))

        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            if (onCompose != null) {
                FeatureCard(
                    title = stringResource(R.string.overview_action_compose),
                    icon  = Icons.Default.Edit,
                    onClick = onCompose,
                )
            }
            if (onHomework != null) {
                FeatureCard(
                    title = stringResource(R.string.overview_action_homework),
                    icon  = Icons.AutoMirrored.Filled.Assignment,
                    onClick = onHomework,
                )
            }
            if (onTimetable != null) {
                FeatureCard(
                    title = stringResource(R.string.overview_action_timetable),
                    icon  = Icons.Default.DateRange,
                    onClick = onTimetable,
                )
            }
            if (onGrades != null) {
                FeatureCard(
                    title = stringResource(R.string.overview_action_grades),
                    icon  = Icons.Default.Star,
                    onClick = onGrades,
                )
            }
            if (onMeals != null) {
                FeatureCard(
                    title = stringResource(R.string.meals_action_meals),
                    icon  = Icons.Default.Restaurant,
                    onClick = onMeals,
                )
            }
        }
    }
}

private const val TIMETABLE_COLLAPSED_COUNT = 4
private const val GRADES_COLLAPSED_COUNT    = 3

@Composable
private fun OverviewCard(content: @Composable () -> Unit) {
    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CardSectionHeader(title: String, icon: ImageVector) {
    SectionLabel(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        startPadding = 0.dp,
        topPadding = 0.dp,
    )
}

@Composable
private fun gradeColor(grade: Double): Color = when {
    grade <= 1.0 -> MaterialTheme.colorScheme.tertiary
    grade <= 2.0 -> MaterialTheme.colorScheme.primary
    grade <= 3.0 -> MaterialTheme.colorScheme.secondary
    grade <= 4.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    else         -> MaterialTheme.colorScheme.error
}

@Composable
private fun NextBellBar(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun formatTimeLeft(seconds: Long, showSeconds: Boolean): String = when {
    seconds < 0 -> ""
    !showSeconds -> formatWholeMinutes(seconds / 60)
    else -> {
        val m = seconds / 60
        val s = seconds % 60
        when {
            m < 1 -> "${s}s"
            m < 60 -> "${m}m ${s}s"
            s == 0L && m % 60 == 0L -> "${m / 60}h"
            m % 60 == 0L -> "${m / 60}h ${s}s"
            else -> "${m / 60}h ${m % 60}m ${s}s"
        }
    }
}

private fun formatWholeMinutes(minutes: Long): String = when {
    minutes < 1 -> "<1m"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0L -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

@Preview(name = "Overview – Light", showBackground = true)
@Preview(name = "Overview – Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OverviewScreenPreview() {
    Edupage2Theme {
        OverviewScreen(bottomPadding = PaddingValues())
    }
}

@Composable
private fun getDisplayName(lesson: Lesson): String {
    return when {

        lesson.isEvent && !lesson.curriculum.isNullOrBlank() -> lesson.curriculum!!

        else -> {
            val subjectName = lesson.subject?.name
            when {
                subjectName != null && subjectName.isNotBlank() &&
                !subjectName.equals("unknown", ignoreCase = true) &&
                !subjectName.equals("undefined", ignoreCase = true) -> subjectName

                !lesson.curriculum.isNullOrBlank() -> lesson.curriculum!!

                else -> stringResource(R.string.timetable_unknown_subject)
            }
        }
    }
}

