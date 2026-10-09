package com.wiffles.edupage.ui.timetable

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.draw.rotate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.edupage.api.model.Classroom
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.timetable.Lesson
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.BreakVisibility
import com.wiffles.edupage.data.CancelledLessonStyle
import com.wiffles.edupage.data.LessonGrouping

import com.wiffles.edupage.ui.theme.Edupage2Theme
import com.wiffles.edupage.ui.core.containers.RoundedCardContainer
import com.wiffles.edupage.ui.modifiers.MotionBlurGate
import com.wiffles.edupage.ui.modifiers.scrollMotionBlur
import com.wiffles.edupage.ui.util.ShimmerBox
import com.wiffles.edupage.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    bottomPadding: PaddingValues,
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
    val haptics = rememberAppHaptics()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val refreshState   = rememberPullToRefreshState()

    val isRefreshing = (uiState as? TimetableUiState.Success)?.isRefreshing == true

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
            LargeTopAppBar(
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
                    DateNavigationBar(
                        date = selectedDate,
                        showWeekends = showWeekends,
                        onPreviousDay = { haptics.virtualKey(); viewModel.setDate(selectedDate.minusDays(1)) },
                        onNextDay = { haptics.virtualKey(); viewModel.setDate(selectedDate.plusDays(1)) },
                        onToday = { haptics.virtualKey(); viewModel.setDate(LocalDate.now()) }
                    )

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
                            if (state.lessons.isEmpty()) {
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
                                val startGroups = state.lessons.groupConsecutiveBy { it.startTime }
                                val displayGroups = mergeLessonGroups(
                                    startGroups = startGroups,
                                    mode = lessonGrouping,
                                    breakVisibility = breakVisibility,
                                    selectedDate = selectedDate,
                                    currentTime = currentTime,
                                )
                                val listState = rememberLazyListState()

                                LaunchedEffect(state.lessons, selectedDate, displayGroups) {
                                    if (selectedDate != LocalDate.now()) return@LaunchedEffect
                                    val lessons = state.lessons
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
                                        listState.animateScrollToItem(targetItem)
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
                                        bottom = if (compactTimetable) 8.dp else (16.dp + bottomPadding.calculateBottomPadding())
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(if (compactTimetable) 8.dp else 14.dp)
                                ) {
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
                                            val isCurrentLesson = selectedDate == LocalDate.now() &&
                                                lesson.startTime != null && lesson.endTime != null &&
                                                !currentTime.isBefore(lesson.startTime) &&
                                                currentTime.isBefore(lesson.endTime)
                                            LessonCard(
                                                lesson = lesson,
                                                isCurrentLesson = isCurrentLesson,
                                                currentTime = if (isCurrentLesson) currentTime else null,
                                                cancelledLessonStyle = cancelledLessonStyle,
                                                showSeconds = showSeconds,
                                                compact = compactTimetable
                                            )
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
    val isCurrent = isCurrentLesson
    val isError = lesson.isCancelled && cancelledLessonStyle == CancelledLessonStyle.RED
    val isTertiary = lesson.hasChange()
    val isSecondary = lesson.isOnlineLesson()

    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        lesson.isCancelled -> MaterialTheme.colorScheme.surfaceBright
        isTertiary -> MaterialTheme.colorScheme.tertiaryContainer
        isSecondary -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceBright
    }

    val contentColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isTertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        isSecondary -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        LessonCardContent(lesson, isCurrentLesson, currentTime, cancelledLessonStyle, showSeconds, compact)
    }
}

@Composable
private fun LessonCardContent(
    lesson: Lesson,
    isCurrentLesson: Boolean = false,
    currentTime: LocalTime? = null,
    cancelledLessonStyle: CancelledLessonStyle = CancelledLessonStyle.RED,
    showSeconds: Boolean = false,
    compact: Boolean = false,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val startStr = lesson.startTime?.format(timeFormatter) ?: "?"
    val endStr = lesson.endTime?.format(timeFormatter) ?: "?"

    val isCurrent = isCurrentLesson
    val isError = lesson.isCancelled && cancelledLessonStyle == CancelledLessonStyle.RED
    val isTertiary = lesson.hasChange()
    val isSecondary = lesson.isOnlineLesson()

    val primaryTextColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isTertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        isSecondary -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val secondaryTextColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
        isError -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.82f)
        isTertiary -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.82f)
        isSecondary -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val periodColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isTertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        isSecondary -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }

    val isGreyedOut = lesson.isCancelled && cancelledLessonStyle == CancelledLessonStyle.GREYED_OUT
    val contentAlpha = if (isGreyedOut) 0.45f else 1f

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
        val canExtend = chainable && next.size == 1 && when (mode) {
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

    val containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceBright
    val contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    RoundedCardContainer(
        modifier = Modifier.fillMaxWidth(),
        spacing = 2.dp,
        cornerRadius = 24.dp,
    ) {
        lessons.forEachIndexed { idx, lesson ->
            val lessonIsCurrent = isCurrent && idx == 0
            Surface(
                color = containerColor,
                contentColor = contentColor
            ) {
                LessonCardContent(
                    lesson = lesson,
                    isCurrentLesson = lessonIsCurrent,
                    currentTime = if (lessonIsCurrent) currentTime else null,
                    cancelledLessonStyle = cancelledLessonStyle,
                    showSeconds = showSeconds,
                    compact = compact
                )
            }
        }
    }
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

