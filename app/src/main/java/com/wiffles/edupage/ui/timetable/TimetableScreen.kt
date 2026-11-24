package com.wiffles.edupage.ui.timetable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.Classroom
import com.edupage.api.model.EduEvent
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.timetable.Lesson
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.BreakVisibility
import com.wiffles.edupage.data.CancelledLessonStyle
import com.wiffles.edupage.data.LessonGrouping
import com.wiffles.edupage.ui.core.containers.RoundedCardContainer
import com.wiffles.edupage.ui.modifiers.MotionBlurGate
import com.wiffles.edupage.ui.modifiers.scrollMotionBlur
import com.wiffles.edupage.ui.overview.EventHighlightCard
import com.wiffles.edupage.ui.overview.EventTypeChip
import com.wiffles.edupage.ui.overview.EventWeekChip
import com.wiffles.edupage.ui.overview.EventsSectionHeader
import com.wiffles.edupage.ui.overview.covers
import com.wiffles.edupage.ui.overview.coversPeriod
import com.wiffles.edupage.ui.overview.eventAccent
import com.wiffles.edupage.ui.overview.eventDetailLine
import com.wiffles.edupage.ui.overview.eventIcon
import com.wiffles.edupage.ui.theme.Edupage2Theme
import com.wiffles.edupage.ui.util.ShimmerBox
import com.wiffles.edupage.ui.util.rememberAppHaptics
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    bottomPadding: PaddingValues,
    onWeekModeChanged: (Boolean) -> Unit = {},
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val breakVisibility by viewModel.breakVisibility.collectAsState()
    val cancelledLessonStyle by viewModel.cancelledLessonStyle.collectAsState()
    val lessonGrouping by viewModel.lessonGrouping.collectAsState()
    val showWeekends by viewModel.showWeekends.collectAsState()
    val showSeconds by viewModel.showSeconds.collectAsState()
    val compactTimetable by viewModel.compactTimetable.collectAsState()
    val weekMode by viewModel.weekMode.collectAsState()
    val weekLessons by viewModel.weekLessons.collectAsState()
    val weekRefreshing by viewModel.weekRefreshing.collectAsState()
    val events by viewModel.events.collectAsState()
    val haptics = rememberAppHaptics()

    // Events for the day being shown, so a trip or holiday heads the lesson list.
    val dayEvents = remember(events, selectedDate) { events.filter { it.covers(selectedDate) } }
    val weekDates = remember(selectedDate, showWeekends) { weekDatesFor(selectedDate, showWeekends) }
    val weekEvents = remember(events, weekDates) {
        weekDates.flatMap { date -> events.filter { it.covers(date) }.map { date to it } }
    }

    LaunchedEffect(weekMode) { onWeekModeChanged(weekMode) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val refreshState   = rememberPullToRefreshState()

    val isRefreshing = if (weekMode) {
        weekRefreshing
    } else {
        (uiState as? TimetableUiState.Success)?.isRefreshing == true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.timetable_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { haptics.virtualKey(); viewModel.setWeekMode(!weekMode) }) {
                        Icon(
                            imageVector = if (weekMode) Icons.Rounded.ViewAgenda else Icons.Rounded.CalendarViewWeek,
                            contentDescription = stringResource(
                                if (weekMode) R.string.timetable_view_day else R.string.timetable_view_week
                            )
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = { haptics.virtualKey(); viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.timetable_refresh),
                            modifier = Modifier.rotate(if (isRefreshing) rotation else 0f)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = refreshState,
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                androidx.compose.animation.AnimatedVisibility(visible = isRefreshing) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!weekMode) {
                        DateNavigationBar(
                            date = selectedDate,
                            showWeekends = showWeekends,
                            onPreviousDay = { haptics.virtualKey(); viewModel.setDate(selectedDate.minusDays(1)) },
                            onNextDay = { haptics.virtualKey(); viewModel.setDate(selectedDate.plusDays(1)) },
                            onToday = { haptics.virtualKey(); viewModel.setDate(LocalDate.now()) }
                        )
                    }

                    when (val state = uiState) {
                        is TimetableUiState.Loading -> {
                            TimetableSkeleton(
                                bottomPadding = bottomPadding,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is TimetableUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { haptics.virtualKey(); viewModel.refresh() },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.timetable_retry),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }

                        is TimetableUiState.Success -> {
                            if (weekMode) {
                                // The columns read the same resolved rows as the day list, so
                                // "Event" placeholders turn into the event name instead of "—".
                                val weekPlan = remember(weekLessons, weekEvents) {
                                    weekLessons.mapValues { (date, lessons) ->
                                        planDayRows(
                                            lessons = lessons,
                                            dayEvents = weekEvents.filter { it.first == date }.map { it.second },
                                        ).rows
                                    }
                                }
                                WeekTimetableView(
                                    weekDates = weekDates,
                                    lessonsByDate = weekPlan,
                                    selectedDate = selectedDate,
                                    currentTime = currentTime,
                                    cancelledLessonStyle = cancelledLessonStyle,
                                    compact = compactTimetable,
                                    events = weekEvents,
                                    onSelectDay = { viewModel.selectDay(it) },
                                    onOpenDay = { date ->
                                        haptics.virtualKey()
                                        viewModel.setDate(date)
                                        viewModel.setWeekMode(false)
                                    },
                                    onPreviousWeek = { haptics.virtualKey(); viewModel.shiftWeek(-1) },
                                    onNextWeek = { haptics.virtualKey(); viewModel.shiftWeek(1) },
                                    bottomPadding = bottomPadding,
                                )
                            } else {
                                // Resolve EduPage's placeholders first: an event claims the periods
                                // it takes, and a placeholder that says nothing simply disappears.
                                val plan = remember(state.lessons, dayEvents) {
                                    planDayRows(state.lessons, dayEvents)
                                }
                                if (plan.rows.isEmpty() && plan.bannerEvents.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = stringResource(R.string.timetable_no_lessons),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    val startGroups = plan.rows.groupConsecutiveBy { it.startTime }
                                    val displayGroups = mergeLessonGroups(
                                        startGroups = startGroups,
                                        mode = lessonGrouping,
                                        breakVisibility = breakVisibility,
                                        selectedDate = selectedDate,
                                        currentTime = currentTime,
                                    )
                                    val listState = rememberLazyListState()
                                    // The events block (and the "no lessons" note) sit before the lessons,
                                    // so the auto-scroll index has to skip them.
                                    val leadingItems = (if (plan.bannerEvents.isNotEmpty()) 1 else 0) +
                                        (if (plan.rows.isEmpty()) 1 else 0)

                                    LaunchedEffect(plan.rows, selectedDate, displayGroups) {
                                        if (selectedDate != LocalDate.now()) return@LaunchedEffect
                                        val lessons = plan.rows
                                        val activeLessonIndex = lessons.indexOfFirst { lesson ->
                                            lesson.startTime != null && lesson.endTime != null &&
                                                !currentTime.isBefore(lesson.startTime) &&
                                                currentTime.isBefore(lesson.endTime)
                                        }
                                        val flatIndex = if (activeLessonIndex >= 0) {
                                            activeLessonIndex
                                        } else {
                                            lessons.indexOfFirst { lesson ->
                                                val lessonIdx = lessons.indexOf(lesson)
                                                if (lessonIdx == 0) return@indexOfFirst false
                                                val prevEnd = lessons[lessonIdx - 1].endTime
                                                val thisStart = lesson.startTime
                                                prevEnd != null && thisStart != null &&
                                                    thisStart > prevEnd &&
                                                    !currentTime.isBefore(prevEnd) &&
                                                    currentTime.isBefore(thisStart)
                                            }
                                        }
                                        if (flatIndex >= 0) {
                                            var acc = 0
                                            var targetItem = 0
                                            for ((groupIdx, group) in displayGroups.withIndex()) {
                                                if (flatIndex < acc + group.size) {
                                                    targetItem = groupIdx
                                                    break
                                                }
                                                acc += group.size
                                            }
                                            listState.animateScrollToItem(targetItem + leadingItems)
                                        }
                                    }
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .scrollMotionBlur(
                                                lazyListState = listState,
                                                enabled = MotionBlurGate.forTabs(),
                                            ),
                                        contentPadding = PaddingValues(
                                            start = 16.dp, end = 16.dp,
                                            top = if (compactTimetable) 8.dp else 16.dp,
                                            bottom = (if (compactTimetable) 8.dp else 16.dp) +
                                                bottomPadding.calculateBottomPadding()
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(if (compactTimetable) 8.dp else 14.dp)
                                    ) {
                                        if (plan.bannerEvents.isNotEmpty()) {
                                            item(key = "day-events") {
                                                DayEventsSection(events = plan.bannerEvents)
                                            }
                                        }
                                        if (plan.rows.isEmpty()) {
                                            item(key = "no-lessons") {
                                                Text(
                                                    text = stringResource(R.string.timetable_no_lessons),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 24.dp),
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                        itemsIndexed(
                                            displayGroups,
                                            key = { _, group -> timetableGroupKey(group) },
                                        ) { groupIndex, group ->
                                            Box(Modifier.animateItem()) {
                                                Column {
                                            if (groupIndex > 0) {
                                                val prevGroup = displayGroups[groupIndex - 1]
                                                separatorBetween(
                                                    prev = prevGroup.last(),
                                                    next = group.first(),
                                                    breakVisibility = breakVisibility,
                                                    selectedDate = selectedDate,
                                                    currentTime = currentTime,
                                                )?.let { info ->
                                                    BreakSeparator(
                                                        breakMinutes = info.minutes,
                                                        isActive = info.isActive,
                                                        breakEndsAt = if (info.isActive) group.first().startTime else null,
                                                        currentTime = currentTime
                                                    )
                                                }
                                            }
                                            if (group.size == 1) {
                                                val lesson = group[0]
                                                val event = plan.eventForRow[lesson]
                                                val isCurrentLesson = selectedDate == LocalDate.now() &&
                                                    lesson.startTime != null && lesson.endTime != null &&
                                                    !currentTime.isBefore(lesson.startTime) &&
                                                    currentTime.isBefore(lesson.endTime)
                                                if (event != null) {
                                                    EventLessonCard(
                                                        event = event,
                                                        segments = plan.eventSegments[lesson] ?: listOf(lesson),
                                                        compact = compactTimetable,
                                                    )
                                                } else {
                                                LessonCard(
                                                    lesson = lesson,
                                                    isCurrentLesson = isCurrentLesson,
                                                    currentTime = if (isCurrentLesson) currentTime else null,
                                                    cancelledLessonStyle = cancelledLessonStyle,
                                                    showSeconds = showSeconds,
                                                    compact = compactTimetable
                                                )
                                                }
                                            } else {
                                                GroupedLessonCard(
                                                    lessons = group,
                                                    currentTime = currentTime,
                                                    selectedDate = selectedDate,
                                                    cancelledLessonStyle = cancelledLessonStyle,
                                                    showSeconds = showSeconds,
                                                    compact = compactTimetable
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
            }
        }
    }
}

@Composable
private fun DayEventsSection(events: List<EduEvent>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EventsSectionHeader(stringResource(R.string.timetable_events_today))
        events.forEach { event -> EventHighlightCard(event) }
    }
}

/** Short weekday + day label for the week-mode event chips. */
@Composable
private fun weekChipDayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    if (date == today) return stringResource(R.string.event_today)
    if (date == today.plusDays(1)) return stringResource(R.string.event_tomorrow)
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$weekday ${date.dayOfMonth}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateNavigationBar(
    date: LocalDate,
    showWeekends: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val today = LocalDate.now()
    val todayIsWeekend = today.dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                         today.dayOfWeek == java.time.DayOfWeek.SUNDAY
    val showTodayChip = date != today && !(todayIsWeekend && !showWeekends)
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val formatted = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(onClick = onPreviousDay) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.timetable_previous_day))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showTodayChip) {
                        AssistChip(
                            onClick = onToday,
                            label = { Text(stringResource(R.string.timetable_today), style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                    FilledTonalIconButton(onClick = onNextDay) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.timetable_next_day))
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    isCurrentLesson: Boolean = false,
    currentTime: LocalTime? = null,
    cancelledLessonStyle: CancelledLessonStyle = CancelledLessonStyle.RED,
    showSeconds: Boolean = false,
    compact: Boolean = false,
) {
    val colors = lessonCardColors(lesson, isCurrentLesson, cancelledLessonStyle)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.container,
        contentColor = colors.content
    ) {
        LessonCardContent(lesson, isCurrentLesson, currentTime, cancelledLessonStyle, showSeconds, compact, colors)
    }
}

/**
 * Card colours for one lesson row, resolved in a single place so a card and the text inside it
 * can never disagree. That mattered: a substituted lesson inside a grouped card used to take
 * its text colour from the change tone while the container stayed a plain surface, which on a
 * near-monochrome palette (black-on-mid-grey) left the title invisible.
 */
private data class LessonCardColors(
    val container: Color,
    val content: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val period: Color,
    val contentAlpha: Float,
)

@Composable
private fun lessonCardColors(
    lesson: Lesson,
    isCurrent: Boolean,
    cancelledLessonStyle: CancelledLessonStyle,
): LessonCardColors {
    val isError = lesson.isCancelled && cancelledLessonStyle == CancelledLessonStyle.RED
    val isTertiary = lesson.hasChange()
    val isSecondary = lesson.isOnlineLesson()

    val container = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        lesson.isCancelled -> MaterialTheme.colorScheme.surfaceBright
        isTertiary -> MaterialTheme.colorScheme.tertiaryContainer
        isSecondary -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceBright
    }

    val content = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        lesson.isCancelled -> MaterialTheme.colorScheme.onSurface
        isTertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        isSecondary -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val period = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isTertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        isSecondary -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }

    val greyedOut = lesson.isCancelled && cancelledLessonStyle == CancelledLessonStyle.GREYED_OUT

    return LessonCardColors(
        container = container,
        content = content,
        primaryText = content,
        secondaryText = if (isCurrent || isError || isTertiary || isSecondary) {
            content.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        period = period,
        contentAlpha = if (greyedOut) 0.45f else 1f,
    )
}

@Composable
private fun LessonCardContent(
    lesson: Lesson,
    isCurrentLesson: Boolean = false,
    currentTime: LocalTime? = null,
    cancelledLessonStyle: CancelledLessonStyle = CancelledLessonStyle.RED,
    showSeconds: Boolean = false,
    compact: Boolean = false,
    colors: LessonCardColors = lessonCardColors(lesson, isCurrentLesson, cancelledLessonStyle),
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val startStr = lesson.startTime?.format(timeFormatter) ?: "?"
    val endStr = lesson.endTime?.format(timeFormatter) ?: "?"

    val primaryTextColor = colors.primaryText
    val secondaryTextColor = colors.secondaryText
    val periodColor = colors.period
    val contentAlpha = colors.contentAlpha

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                lesson.period?.let {
                    Text(
                        text = "$it.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = periodColor.copy(alpha = contentAlpha)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = startStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor.copy(alpha = contentAlpha)
                )
                Text(
                    text = endStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor.copy(alpha = contentAlpha)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val subjectName = getDisplayName(lesson)
                val origSubjectName = lesson.origSubject?.name
                if (origSubjectName != null) {
                    Text(
                        text = buildChangedText(old = origSubjectName, new = subjectName, cancelled = lesson.isCancelled),
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor.copy(alpha = contentAlpha),
                        textDecoration = if (lesson.isCancelled) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                val unknownPlaceholder = stringResource(R.string.timetable_unknown_placeholder)
                val teacherNames    = lesson.teachers?.joinToString(", ") { it.name ?: unknownPlaceholder } ?: ""
                val origTeacherNames = lesson.origTeachers?.joinToString(", ") { it.name ?: unknownPlaceholder }
                if (!origTeacherNames.isNullOrEmpty() && origTeacherNames != teacherNames) {
                    Text(
                        text = buildChangedText(old = origTeacherNames, new = teacherNames),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (teacherNames.isNotEmpty()) {
                    Text(
                        text = teacherNames,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor.copy(alpha = contentAlpha),
                        textDecoration = if (lesson.isCancelled) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                val classroomNames    = lesson.classrooms?.joinToString(", ") { it.name ?: unknownPlaceholder } ?: ""
                val origClassroomNames = lesson.origClassrooms?.joinToString(", ") { it.name ?: unknownPlaceholder }
                if (!origClassroomNames.isNullOrEmpty() && origClassroomNames != classroomNames) {
                    Text(
                        text = buildChangedText(old = origClassroomNames, new = classroomNames),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (classroomNames.isNotEmpty()) {
                    Text(
                        text = classroomNames,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor.copy(alpha = contentAlpha),
                        textDecoration = if (lesson.isCancelled) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                if (!lesson.curriculum.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lesson.curriculum!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor.copy(alpha = contentAlpha)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCurrentLesson) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(
                            stringResource(R.string.timetable_badge_now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    val secsLeft = if (currentTime != null && lesson.endTime != null)
                        Duration.between(currentTime, lesson.endTime).toSeconds()
                    else null
                    if (secsLeft != null && secsLeft >= 0) {
                        TimeLeftPill(
                            label = stringResource(R.string.timetable_time_left, formatTimeLeft(secsLeft, showSeconds)),
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (lesson.isCancelled) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text(
                            stringResource(R.string.timetable_badge_cancelled),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
                if (lesson.hasChange() && !lesson.isCancelled) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                        Text(
                            stringResource(R.string.timetable_badge_changed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
                if (lesson.isOnlineLesson()) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                        Text(
                            stringResource(R.string.timetable_badge_online),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }

@Composable
private fun BreakSeparator(
    breakMinutes: Long,
    isActive: Boolean = false,
    breakEndsAt: LocalTime? = null,
    currentTime: LocalTime? = null
) {
    val label = when {
        breakMinutes < 60 -> stringResource(R.string.timetable_break_minutes, breakMinutes)
        breakMinutes % 60 == 0L -> stringResource(R.string.timetable_break_hours, breakMinutes / 60)
        else -> stringResource(R.string.timetable_break_hours_minutes, breakMinutes / 60, breakMinutes % 60)
    }
    val minsLeft = if (isActive && currentTime != null && breakEndsAt != null)
        Duration.between(currentTime, breakEndsAt).toSeconds()
    else null

    val lineColor = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
    val weight    = if (isActive) FontWeight.Bold else FontWeight.Normal
    val strokePx  = if (isActive) 3f else 2f
    val dash      = if (isActive) floatArrayOf(8f, 6f) else floatArrayOf(6f, 6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isActive) 6.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.weight(1f).height(2.dp)) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = strokePx,
                    pathEffect = PathEffect.dashPathEffect(dash, 0f)
                )
            }
            Text(
                text = label,
                style = if (isActive) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.labelSmall,
                fontWeight = weight,
                color = textColor
            )
            Canvas(modifier = Modifier.weight(1f).height(2.dp)) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = strokePx,
                    pathEffect = PathEffect.dashPathEffect(dash, 0f)
                )
            }
        }
        if (isActive && minsLeft != null && minsLeft >= 0) {
            val showSeconds = AppPreferences(LocalContext.current).showSeconds
            TimeLeftPill(
                label = stringResource(R.string.timetable_time_left, formatTimeLeft(minsLeft, showSeconds)),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
private fun TimeLeftPill(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun buildChangedText(old: String, new: String, cancelled: Boolean = false) = buildAnnotatedString {
    withStyle(
        SpanStyle(
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    ) { append(old) }
    append("  →  ")
    withStyle(
        SpanStyle(
            fontWeight = FontWeight.Bold,
            textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None
        )
    ) { append(new) }
}

private fun previewTeacher(name: String) = EduTeacher(
    personId = 1, name = name, gender = null, inSchoolSince = null,
    classroomName = null, teacherTo = null
)

private fun previewLesson(
    period: Int = 1,
    subjectName: String = "Mathematics",
    teacherName: String = "Mgr. Novák",
    classroomName: String = "A12",
    startTime: LocalTime = LocalTime.of(8, 0),
    endTime: LocalTime = LocalTime.of(8, 45),
    isCancelled: Boolean = false,
    isOnline: Boolean = false,
    origSubjectName: String? = null,
    origTeacherName: String? = null,
    origClassroomName: String? = null,
) = Lesson(
    period = period,
    startTime = startTime,
    endTime = endTime,
    duration = 1,
    subject = Subject(subjectId = 1, name = subjectName, shortName = subjectName.take(3)),
    classes = null,
    groups = null,
    teachers = listOf(previewTeacher(teacherName)),
    classrooms = listOf(Classroom(classroomId = 1, name = classroomName, shortName = classroomName)),
    curriculum = null,
    type = null,
    onlineLessonLink = if (isOnline) "https://meet.example.com/abc" else null,
    isCancelled = isCancelled,
    isEvent = false,
    origSubject = origSubjectName?.let { Subject(subjectId = 2, name = it, shortName = it.take(3)) },
    origTeachers = origTeacherName?.let { listOf(previewTeacher(it)) },
    origClassrooms = origClassroomName?.let { listOf(Classroom(classroomId = 2, name = it, shortName = it)) },
)

@Composable
private fun TimetableSkeleton(
    bottomPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(
                start = 16.dp, end = 16.dp,
                top = 12.dp,
                bottom = 12.dp + bottomPadding.calculateBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(5) {
            LessonCardSkeleton()
        }
    }
}

@Composable
private fun LessonCardSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.width(52.dp)
            ) {
                ShimmerBox(modifier = Modifier.width(28.dp), height = 22.dp, cornerRadius = 6.dp)
                ShimmerBox(modifier = Modifier.width(36.dp), height = 11.dp)
                ShimmerBox(modifier = Modifier.width(36.dp), height = 11.dp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f), height = 16.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f), height = 12.dp)
            }
        }
    }
}

@Preview(name = "LessonCard – Normal", showBackground = true, widthDp = 360)
@Composable
private fun LessonCardNormalPreview() {
    Edupage2Theme { LessonCard(previewLesson()) }
}

@Preview(name = "LessonCard – Current (Now)", showBackground = true, widthDp = 360)
@Composable
private fun LessonCardCurrentPreview() {
    Edupage2Theme {
        LessonCard(
            previewLesson(startTime = LocalTime.of(8, 0), endTime = LocalTime.of(8, 45)),
            isCurrentLesson = true,
            currentTime = LocalTime.of(8, 20)
        )
    }
}

@Preview(name = "LessonCard – Cancelled", showBackground = true, widthDp = 360)
@Composable
private fun LessonCardCancelledPreview() {
    Edupage2Theme { LessonCard(previewLesson(isCancelled = true)) }
}

@Preview(name = "LessonCard – Changed (teacher + room)", showBackground = true, widthDp = 360)
@Composable
private fun LessonCardChangedPreview() {
    Edupage2Theme {
        LessonCard(
            previewLesson(
                teacherName = "Mgr. Svobodová",
                classroomName = "B03",
                origTeacherName = "Mgr. Novák",
                origClassroomName = "A12"
            )
        )
    }
}

@Preview(name = "LessonCard – Changed (subject)", showBackground = true, widthDp = 360)
@Composable
private fun LessonCardSubjectChangedPreview() {
    Edupage2Theme {
        LessonCard(
            previewLesson(
                subjectName = "Physics",
                origSubjectName = "Mathematics"
            )
        )
    }
}

@Preview(name = "LessonCard – Online", showBackground = true, widthDp = 360)
@Composable
private fun LessonCardOnlinePreview() {
    Edupage2Theme { LessonCard(previewLesson(isOnline = true)) }
}

@Preview(name = "LessonCard – Dark", showBackground = true, widthDp = 360, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LessonCardDarkPreview() {
    Edupage2Theme {
        LessonCard(
            previewLesson(
                teacherName = "Mgr. Svobodová",
                classroomName = "B03",
                origTeacherName = "Mgr. Novák",
                origClassroomName = "A12"
            )
        )
    }
}

@Preview(name = "DateNavigationBar – Today", showBackground = true, widthDp = 360)
@Composable
private fun DateNavBarTodayPreview() {
    Edupage2Theme {
        DateNavigationBar(
            date = LocalDate.now(),
            showWeekends = true,
            onPreviousDay = {}, onNextDay = {}, onToday = {}
        )
    }
}

@Preview(name = "DateNavigationBar – Other day", showBackground = true, widthDp = 360)
@Composable
private fun DateNavBarOtherPreview() {
    Edupage2Theme {
        DateNavigationBar(
            date = LocalDate.now().plusDays(2),
            showWeekends = true,
            onPreviousDay = {}, onNextDay = {}, onToday = {}
        )
    }
}

@Preview(name = "BreakSeparator – Active with time left", showBackground = true, widthDp = 360)
@Composable
private fun BreakSeparatorActivePreview() {
    Edupage2Theme {
        BreakSeparator(
            breakMinutes = 10,
            isActive = true,
            breakEndsAt = LocalTime.of(9, 55),
            currentTime = LocalTime.of(9, 50)
        )
    }
}

private fun timetableGroupKey(group: List<Lesson>): String {
    val first = group.firstOrNull() ?: return "empty"
    return "${group.size}|${first.period}|${first.subject?.name}|${first.startTime}|${first.endTime}"
}

/** `Lesson.type` marker for the synthetic row that stands in for a whole school event. */
private const val EVENT_ROW_TYPE = "edupage-event"

private val RAW_ID = Regex("^-?\\d+$")

/**
 * A row EduPage only uses as a stand-in — a cancelled-lesson marker, a free period, or the
 * slot an event takes. It has no subject of its own, so it should never look like a lesson.
 */
private fun Lesson.isEventPlaceholder(): Boolean =
    isEvent && subject?.name.isNullOrBlank()

/** Whether a placeholder row still carries something worth showing on its own. */
private fun Lesson.hasOwnLabel(): Boolean {
    val label = curriculum?.trim().orEmpty()
    val useful = label.isNotEmpty() &&
        !label.equals("event", ignoreCase = true) &&
        !RAW_ID.matches(label)
    return useful || !classes.isNullOrEmpty()
}

/** What the timetable should draw for one day once the event placeholders are resolved. */
internal data class DayPlan(
    val rows: List<Lesson>,
    val eventForRow: Map<Lesson, EduEvent>,
    val bannerEvents: List<EduEvent>,
    /**
     * The per-period blocks an event really occupies. The row keeps the whole span (so breaks
     * still add up), while these let the day view draw the event as one connected column of
     * blocks — the same way a double lesson joins up — instead of one tall card.
     */
    val eventSegments: Map<Lesson, List<Lesson>> = emptyMap(),
)

/**
 * Replaces EduPage's "Event" placeholders with a single row per school event that spans the
 * periods it occupies, and drops the placeholders that say nothing at all. An event with no
 * period of its own (a holiday, a multi-day break) stays in [DayPlan.bannerEvents] instead.
 */
internal fun planDayRows(lessons: List<Lesson>, dayEvents: List<EduEvent>): DayPlan {
    val placeholders = lessons.filter { it.isEventPlaceholder() }

    // Which event owns which placeholder rows. Events are matched by their period span, so a
    // four-period trip swallows the four (empty) "Event" rows EduPage left behind.
    val spans = LinkedHashMap<EduEvent, List<Lesson>>()
    dayEvents.forEach { event ->
        if (event.period == null) return@forEach
        val span = placeholders.filter { event.coversPeriod(it.period) }
        if (span.isNotEmpty()) spans[event] = span
    }

    val consumed = mutableSetOf<Lesson>()
    val rows = mutableListOf<Lesson>()
    val eventForRow = mutableMapOf<Lesson, EduEvent>()
    val eventSegments = mutableMapOf<Lesson, List<Lesson>>()
    lessons.forEach { lesson ->
        if (lesson in consumed) return@forEach
        val owner = if (lesson.isEventPlaceholder()) {
            spans.entries.firstOrNull { lesson in it.value }?.key
        } else {
            null
        }
        when {
            owner != null -> {
                val span = spans.getValue(owner)
                consumed += span
                val row = eventRowLesson(owner, span)
                rows += row
                eventForRow[row] = owner
                // One block per period: EduPage also leaves silent cancelled markers on the same
                // periods, so keep the placeholder that actually carries the lesson times.
                val blocks = span
                    .filter { it.period != null }
                    .groupBy { it.period!! }
                    .entries
                    .sortedBy { it.key }
                    .map { (_, candidates) ->
                        candidates.firstOrNull { it.startTime != null && it.endTime != null && !it.isCancelled }
                            ?: candidates.firstOrNull { it.startTime != null && it.endTime != null }
                            ?: candidates.first()
                    }
                if (blocks.isNotEmpty()) {
                    // Each block stands on its own: it knows the event it belongs to and its period.
                    eventSegments[row] = blocks.map { it.copy(curriculum = owner.name, type = EVENT_ROW_TYPE) }
                }
            }
            // A placeholder with nothing to say is the empty period the school never uses.
            lesson.isEventPlaceholder() && !lesson.hasOwnLabel() -> Unit
            else -> rows += lesson
        }
    }
    return DayPlan(
        rows = rows,
        eventForRow = eventForRow,
        bannerEvents = dayEvents - spans.keys,
        eventSegments = eventSegments,
    )
}

/** A synthetic lesson covering an event's whole span, so breaks before and after still add up. */
private fun eventRowLesson(event: EduEvent, span: List<Lesson>): Lesson {
    val timed = span.filter { it.startTime != null && it.endTime != null }
    val ordered = timed.ifEmpty { span }
    val first = ordered.minByOrNull { it.period ?: Int.MAX_VALUE } ?: span.first()
    val last = ordered.maxByOrNull { it.period ?: Int.MIN_VALUE } ?: first
    return first.copy(
        startTime = first.startTime,
        endTime = last.endTime ?: first.endTime,
        duration = span.sumOf { it.duration },
        subject = null,
        classes = null,
        groups = null,
        teachers = null,
        classrooms = null,
        curriculum = event.name,
        type = EVENT_ROW_TYPE,
        onlineLessonLink = null,
        isCancelled = false,
        isEvent = true,
        origSubject = null,
        origTeachers = null,
        origClassrooms = null,
    )
}

/**
 * The timetable block for a school event: one section, one title. When the event takes several
 * periods the block grows over exactly those periods and lists them inside the same surface, so
 * it reads as a single run instead of a tall card floating between empty rows.
 */
@Composable
private fun EventLessonCard(
    event: EduEvent,
    segments: List<Lesson>,
    compact: Boolean = false,
) {
    if (segments.isEmpty()) return
    val accent = eventAccent(event.kind)
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val single = segments.size == 1

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = accent.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = if (compact) 12.dp else 18.dp,
                ),
            verticalAlignment = Alignment.Top,
        ) {
            // A one-period event keeps the period and its times beside the title, like a lesson.
            if (single) {
                EventPeriodColumn(
                    lesson = segments.first(),
                    periodColor = accent,
                    timeColor = mutedText,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (single) 14.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = eventIcon(event.kind),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accent,
                    )
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                event.typeName?.takeIf { it.isNotBlank() }?.let { EventTypeChip(it) }
                val detail = eventDetailLine(event)
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (event.cancelsTimetable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accent,
                        )
                        Text(
                            text = stringResource(R.string.event_cancels_lessons),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                        )
                    }
                }
                if (!single) {
                    EventSpanRows(
                        segments = segments,
                        accent = accent,
                        mutedText = mutedText,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * The periods an event runs through, drawn as rows inside the event section so the whole span is
 * one block. The rail on the left joins the periods up the way the timetable column would.
 */
@Composable
private fun EventSpanRows(
    segments: List<Lesson>,
    accent: Color,
    mutedText: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        segments.forEachIndexed { index, segment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventSpanRail(
                    isFirst = index == 0,
                    isLast = index == segments.lastIndex,
                    accent = accent,
                )
                Text(
                    text = segment.period?.let { "$it." } ?: "•",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.width(30.dp),
                )
                Text(
                    text = segmentTimeRange(segment),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (index < segments.lastIndex) {
                HorizontalDivider(color = accent.copy(alpha = 0.22f))
            }
        }
    }
}

/** A dot per period with the connecting rail above and below it. */
@Composable
private fun EventSpanRail(
    isFirst: Boolean,
    isLast: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val rail = accent.copy(alpha = 0.55f)
    Column(
        modifier = modifier.width(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(7.dp)
                .background(if (isFirst) Color.Transparent else rail)
        )
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Box(
            Modifier
                .width(2.dp)
                .height(7.dp)
                .background(if (isLast) Color.Transparent else rail)
        )
    }
}

@Composable
private fun EventPeriodColumn(
    lesson: Lesson,
    periodColor: Color,
    timeColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(52.dp),
    ) {
        lesson.period?.let {
            Text(
                text = "$it.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = periodColor,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = lesson.startTime?.format(EVENT_TIME) ?: "?",
            style = MaterialTheme.typography.bodySmall,
            color = timeColor,
        )
        Text(
            text = lesson.endTime?.format(EVENT_TIME) ?: "?",
            style = MaterialTheme.typography.bodySmall,
            color = timeColor,
        )
    }
}

/** Start and end of one period of an event, e.g. "08:00 - 08:45". */
private fun segmentTimeRange(lesson: Lesson): String {
    val start = lesson.startTime?.format(EVENT_TIME)
    val end = lesson.endTime?.format(EVENT_TIME)
    return if (start != null && end != null) "$start – $end" else "—"
}

private val EVENT_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private data class BreakInfo(
    val minutes: Long,
    val isActive: Boolean,
)

private fun separatorBetween(
    prev: Lesson,
    next: Lesson,
    breakVisibility: BreakVisibility,
    selectedDate: LocalDate,
    currentTime: LocalTime,
): BreakInfo? {
    val prevEnd = prev.endTime ?: return null
    val thisStart = next.startTime ?: return null
    if (thisStart <= prevEnd) return null
    val breakMinutes = Duration.between(prevEnd, thisStart).toMinutes()
    val isBreakNow = selectedDate == LocalDate.now() &&
        !currentTime.isBefore(prevEnd) &&
        currentTime.isBefore(thisStart)
    val showBreak = when (breakVisibility) {
        BreakVisibility.ALL -> true
        BreakVisibility.ACTIVE_ONLY -> isBreakNow
        BreakVisibility.ACTIVE_OR_LONG ->
            isBreakNow || breakMinutes >= AppPreferences.LONG_BREAK_THRESHOLD_MINUTES
    }
    return if (showBreak) BreakInfo(breakMinutes, isBreakNow) else null
}

private fun shouldMergeDouble(prev: Lesson, next: Lesson): Boolean {
    if (prev.isCancelled != next.isCancelled) return false
    val prevEnd = prev.endTime ?: return false
    val nextStart = next.startTime ?: return false
    val gapMinutes = Duration.between(prevEnd, nextStart).toMinutes()
    if (gapMinutes < 0 || gapMinutes > 15) return false
    if (prev.subject?.name != next.subject?.name) return false
    val subjectBlank = prev.subject?.name.isNullOrBlank()
    if (subjectBlank && prev.curriculum != next.curriculum) return false
    if (prev.teachers?.map { it.name } != next.teachers?.map { it.name }) return false
    if (prev.classrooms?.map { it.name } != next.classrooms?.map { it.name }) return false
    return true
}

private fun mergeLessonGroups(
    startGroups: List<List<Lesson>>,
    mode: LessonGrouping,
    breakVisibility: BreakVisibility,
    selectedDate: LocalDate,
    currentTime: LocalTime,
): List<List<Lesson>> {
    if (startGroups.isEmpty() || mode == LessonGrouping.OFF) return startGroups
    val out = mutableListOf<List<Lesson>>()
    var current = startGroups[0].toMutableList()
    var chainable = startGroups[0].size == 1
    for (i in 1..startGroups.lastIndex) {
        val next = startGroups[i]
        val canExtend = chainable && next.size == 1 &&
            current.last().type != EVENT_ROW_TYPE && next.first().type != EVENT_ROW_TYPE &&
            when (mode) {
            LessonGrouping.OFF -> false
            LessonGrouping.DOUBLES -> shouldMergeDouble(current.last(), next.first())
            LessonGrouping.ALL -> separatorBetween(
                prev = current.last(),
                next = next.first(),
                breakVisibility = breakVisibility,
                selectedDate = selectedDate,
                currentTime = currentTime,
            ) == null
        }
        if (canExtend) {
            current.add(next.first())
        } else {
            out.add(current)
            current = next.toMutableList()
            chainable = next.size == 1
        }
    }
    out.add(current)
    return out
}

private fun <T> List<T>.groupConsecutiveBy(selector: (T) -> Any?): List<List<T>> {
    if (isEmpty()) return emptyList()
    val groups = mutableListOf<MutableList<T>>()
    var currentGroup = mutableListOf(first())
    var currentKey = selector(first())
    for (i in 1..lastIndex) {
        val key = selector(get(i))
        if (key == currentKey) {
            currentGroup.add(get(i))
        } else {
            groups.add(currentGroup)
            currentGroup = mutableListOf(get(i))
            currentKey = key
        }
    }
    groups.add(currentGroup)
    return groups
}

@Composable
private fun GroupedLessonCard(
    lessons: List<Lesson>,
    currentTime: LocalTime?,
    selectedDate: LocalDate,
    cancelledLessonStyle: CancelledLessonStyle,
    showSeconds: Boolean,
    compact: Boolean,
) {
    val first = lessons.first()
    val isCurrent = selectedDate == LocalDate.now() &&
        first.startTime != null && first.endTime != null &&
        !currentTime!!.isBefore(first.startTime) && currentTime.isBefore(first.endTime)

    

    RoundedCardContainer(
        modifier = Modifier.fillMaxWidth(),
        spacing = 2.dp,
        cornerRadius = 24.dp,
    ) {
        lessons.forEachIndexed { idx, lesson ->
            val lessonIsCurrent = isCurrent && idx == 0
            // Colours are resolved per segment: a substitution in one period must not leave
            // the period beside it with unreadable text.
            val colors = lessonCardColors(lesson, lessonIsCurrent, cancelledLessonStyle)
            Surface(
                color = colors.container,
                contentColor = colors.content
            ) {
                LessonCardContent(
                    lesson = lesson,
                    isCurrentLesson = lessonIsCurrent,
                    currentTime = if (lessonIsCurrent) currentTime else null,
                    cancelledLessonStyle = cancelledLessonStyle,
                    showSeconds = showSeconds,
                    compact = compact,
                    colors = colors,
                )
            }
        }
    }
}

/** The Monday..Friday (or ..Sunday) dates of the week containing [reference]. */
private fun weekDatesFor(reference: LocalDate, showWeekends: Boolean): List<LocalDate> {
    val monday = reference.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val days = if (showWeekends) 7 else 5
    return (0 until days).map { monday.plusDays(it.toLong()) }
}

@Composable
private fun WeekTimetableView(
    weekDates: List<LocalDate>,
    lessonsByDate: Map<LocalDate, List<Lesson>>,
    selectedDate: LocalDate,
    currentTime: LocalTime,
    cancelledLessonStyle: CancelledLessonStyle,
    compact: Boolean,
    events: List<Pair<LocalDate, EduEvent>>,
    onSelectDay: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    bottomPadding: PaddingValues,
) {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM")
    val weekLabel = if (weekDates.isNotEmpty()) {
        "${weekDates.first().format(dateFormatter)} – ${weekDates.last().format(dateFormatter)}"
    } else {
        ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.timetable_previous_week),
                )
            }
            Text(
                text = weekLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.timetable_next_week),
                )
            }
        }

        val maxLessons = weekDates.maxOfOrNull { lessonsByDate[it].orEmpty().size } ?: 0

        // The week's events ride along as chips; tapping one opens that day.
        if (events.isNotEmpty()) {
            EventsSectionHeader(
                title = stringResource(R.string.timetable_events_week),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(events, key = { (date, event) -> "$date-${event.id}" }) { (date, event) ->
                    EventWeekChip(
                        event = event,
                        dayLabel = weekChipDayLabel(date),
                        onClick = { onOpenDay(date) },
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            val spacing = 6.dp
            val headerHeight = 48.dp
            val blocksArea = maxHeight - headerHeight - bottomPadding.calculateBottomPadding() - 8.dp
            val blockHeight = if (maxLessons > 0) {
                ((blocksArea - spacing * (maxLessons - 1)) / maxLessons).coerceAtLeast(26.dp)
            } else {
                44.dp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weekDates.forEach { date ->
                    val lessons = lessonsByDate[date].orEmpty()
                        .sortedWith(
                            compareBy(
                                { it.startTime ?: LocalTime.MAX },
                                { it.period ?: 0 },
                            )
                        )
                    WeekDayColumn(
                        date = date,
                        lessons = lessons,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        currentTime = currentTime,
                        cancelledLessonStyle = cancelledLessonStyle,
                        compact = compact,
                        blockHeight = blockHeight,
                        headerHeight = headerHeight,
                        modifier = Modifier.weight(1f).clickable { onSelectDay(date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayColumn(
    date: LocalDate,
    lessons: List<Lesson>,
    isToday: Boolean,
    isSelected: Boolean,
    currentTime: LocalTime,
    cancelledLessonStyle: CancelledLessonStyle,
    compact: Boolean,
    blockHeight: Dp,
    headerHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            color = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isToday -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(headerHeight),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }

        if (lessons.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(blockHeight),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            lessons.forEach { lesson ->
                val start = lesson.startTime
                val end = lesson.endTime
                val isCurrent = isToday && start != null && end != null &&
                    !currentTime.isBefore(start) && currentTime.isBefore(end)
                WeekLessonBlock(
                    lesson = lesson,
                    isCurrent = isCurrent,
                    cancelledLessonStyle = cancelledLessonStyle,
                    compact = compact,
                    blockHeight = blockHeight,
                )
            }
        }
    }
}

@Composable
private fun WeekLessonBlock(
    lesson: Lesson,
    isCurrent: Boolean,
    cancelledLessonStyle: CancelledLessonStyle,
    compact: Boolean,
    blockHeight: Dp,
) {
    val isCancelled = lesson.isCancelled
    val isError = isCancelled && cancelledLessonStyle == CancelledLessonStyle.RED
    val greyed = isCancelled && cancelledLessonStyle == CancelledLessonStyle.GREYED_OUT

    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        lesson.hasChange() -> MaterialTheme.colorScheme.tertiaryContainer
        lesson.isOnlineLesson() -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceBright
    }
    val contentColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        lesson.hasChange() -> MaterialTheme.colorScheme.onTertiaryContainer
        lesson.isOnlineLesson() -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val alpha = if (greyed) 0.45f else 1f
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val start = lesson.startTime
    val end = lesson.endTime

    val tight = blockHeight < 42.dp

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(blockHeight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = if (tight) 2.dp else if (compact) 5.dp else 7.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = weekLessonName(lesson),
                style = if (tight) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = alpha),
                maxLines = if (blockHeight < 56.dp) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (start != null && !tight) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(start.format(timeFormatter))
                        end?.let {
                            append("–")
                            append(it.format(timeFormatter))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f * alpha),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun weekLessonName(lesson: Lesson): String {
    lesson.subject?.shortName?.takeIf { it.isNotBlank() }?.let { return it }
    lesson.subject?.name?.takeIf { it.isNotBlank() }?.let { return it }
    lesson.curriculum?.takeIf { it.isNotBlank() }?.let { return it }
    lesson.origSubject?.shortName?.takeIf { it.isNotBlank() }?.let { return it }
    return "—"
}

@Preview(name = "BreakSeparator – Inactive", showBackground = true, widthDp = 360)
@Composable
private fun BreakSeparatorInactivePreview() {
    Edupage2Theme {
        BreakSeparator(breakMinutes = 10)
    }
}

@Composable
private fun getDisplayName(lesson: Lesson): String {
    return when {
        lesson.isEvent && !lesson.curriculum.isNullOrBlank() -> lesson.curriculum!!
        lesson.isEvent -> {
            val classNames = lesson.classes?.joinToString(", ") { it.name ?: "" }?.takeIf { it.isNotBlank() }
            classNames ?: stringResource(R.string.timetable_event)
        }

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

