package com.enderplusbayzuiship.edupage2.ui.timetable

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.edupage.api.model.timetable.Lesson
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
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CancelledLessonStyle
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

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
    val showWeekends by viewModel.showWeekends.collectAsState()
    val haptics = rememberAppHaptics()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    FilledTonalIconButton(onClick = { haptics.click(); viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.timetable_refresh))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DateNavigationBar(
                date = selectedDate,
                showWeekends = showWeekends,
                onPreviousDay = { haptics.tick(); viewModel.setDate(selectedDate.minusDays(1)) },
                onNextDay = { haptics.tick(); viewModel.setDate(selectedDate.plusDays(1)) },
                onToday = { haptics.tick(); viewModel.setDate(LocalDate.now()) }
            )

            when (val state = uiState) {
                is TimetableUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
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
                                onClick = { haptics.click(); viewModel.refresh() },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(stringResource(R.string.timetable_retry))
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
                        val listState = rememberLazyListState()

                        // Auto-scroll to the active lesson or active break on first load today
                        LaunchedEffect(state.lessons, selectedDate) {
                            if (selectedDate != LocalDate.now()) return@LaunchedEffect
                            val lessons = state.lessons
                            // Find active lesson index
                            val activeLessonIndex = lessons.indexOfFirst { lesson ->
                                lesson.startTime != null && lesson.endTime != null &&
                                    !currentTime.isBefore(lesson.startTime) &&
                                    currentTime.isBefore(lesson.endTime)
                            }
                            val targetIndex = if (activeLessonIndex >= 0) {
                                activeLessonIndex
                            } else {
                                // Find active break: we're between lessons[i-1].endTime and lessons[i].startTime
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
                            if (targetIndex >= 0) {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 12.dp,
                                bottom = 12.dp + bottomPadding.calculateBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(state.lessons) { index, lesson ->
                                // Break separator: filtered by breakVisibility setting
                                if (index > 0) {
                                    val prev = state.lessons[index - 1]
                                    val prevEnd = prev.endTime
                                    val thisStart = lesson.startTime
                                    if (prevEnd != null && thisStart != null && thisStart > prevEnd) {
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
                                        if (showBreak) {
                                            BreakSeparator(
                                                breakMinutes = breakMinutes,
                                                isActive = isBreakNow,
                                                breakEndsAt = if (isBreakNow) thisStart else null,
                                                currentTime = currentTime
                                            )
                                        }
                                    }
                                }
                                val isCurrentLesson = selectedDate == LocalDate.now() &&
                                    lesson.startTime != null && lesson.endTime != null &&
                                    !currentTime.isBefore(lesson.startTime) &&
                                    currentTime.isBefore(lesson.endTime)
                                LessonCard(
                                    lesson = lesson,
                                    isCurrentLesson = isCurrentLesson,
                                    currentTime = if (isCurrentLesson) currentTime else null,
                                    cancelledLessonStyle = cancelledLessonStyle
                                )
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalIconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.timetable_previous_day))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    HorizontalDivider()
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    isCurrentLesson: Boolean = false,
    currentTime: LocalTime? = null,
    cancelledLessonStyle: CancelledLessonStyle = CancelledLessonStyle.RED,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val startStr = lesson.startTime?.format(timeFormatter) ?: "?"
    val endStr = lesson.endTime?.format(timeFormatter) ?: "?"

    val containerColor = when {
        isCurrentLesson -> MaterialTheme.colorScheme.primaryContainer
        lesson.isCancelled && cancelledLessonStyle == CancelledLessonStyle.RED -> MaterialTheme.colorScheme.errorContainer
        lesson.isCancelled -> MaterialTheme.colorScheme.surfaceContainerLow
        lesson.hasChange() -> MaterialTheme.colorScheme.tertiaryContainer
        lesson.isOnlineLesson() -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val isGreyedOut = lesson.isCancelled && cancelledLessonStyle == CancelledLessonStyle.GREYED_OUT
    val contentAlpha = if (isGreyedOut) 0.45f else 1f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isCurrentLesson) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Period + time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                lesson.period?.let {
                    Text(
                        text = "$it.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = startStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
                Text(
                    text = endStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }

            // Subject + teacher + classroom (with diff rendering)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Subject
                val subjectName = lesson.subject?.name ?: stringResource(R.string.timetable_unknown_subject)
                val origSubjectName = lesson.origSubject?.name
                if (origSubjectName != null) {
                    Text(
                        text = buildChangedText(old = origSubjectName, new = subjectName),
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                }

                // Teachers
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }

                // Classrooms
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }

                if (!lesson.curriculum.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lesson.curriculum!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }
            }

            // Status badges
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
                    val minsLeft = if (currentTime != null && lesson.endTime != null)
                        Duration.between(currentTime, lesson.endTime).toMinutes()
                    else null
                    if (minsLeft != null && minsLeft >= 0) {
                        TimeLeftPill(
                            label = stringResource(R.string.timetable_time_left, formatTimeLeft(minsLeft)),
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
}

/**
 * Dotted separator shown for every gap between lessons.
 * Highlighted (bold, primary colour) only when the break is currently active.
 */
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
        Duration.between(currentTime, breakEndsAt).toMinutes()
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
            TimeLeftPill(
                label = stringResource(R.string.timetable_time_left, formatTimeLeft(minsLeft)),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Formats a minute count into a short human-readable string, e.g. "35m" or "1h 5m". */
private fun formatTimeLeft(minutes: Long): String = when {
    minutes < 1    -> "<1m"
    minutes < 60   -> "${minutes}m"
    minutes % 60 == 0L -> "${minutes / 60}h"
    else           -> "${minutes / 60}h ${minutes % 60}m"
}

/**
 * A small rounded pill showing a time-left label.
 */
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

/**
 * Builds an AnnotatedString showing "~~old~~ → **new**".
 * The old value is rendered with strikethrough in a muted colour;
 * the arrow separator is plain; the new value is bold.
 */
@Composable
private fun buildChangedText(old: String, new: String) = buildAnnotatedString {
    withStyle(
        SpanStyle(
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    ) { append(old) }
    append("  →  ")
    withStyle(
        SpanStyle(fontWeight = FontWeight.Bold)
    ) { append(new) }
}

// ---------------------------------------------------------------------------
// Preview helpers
// ---------------------------------------------------------------------------

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
    onlineLessonLink = if (isOnline) "https://meet.example.com/abc" else null,
    isCancelled = isCancelled,
    isEvent = false,
    origSubject = origSubjectName?.let { Subject(subjectId = 2, name = it, shortName = it.take(3)) },
    origTeachers = origTeacherName?.let { listOf(previewTeacher(it)) },
    origClassrooms = origClassroomName?.let { listOf(Classroom(classroomId = 2, name = it, shortName = it)) },
)

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

@Preview(name = "BreakSeparator – Inactive", showBackground = true, widthDp = 360)
@Composable
private fun BreakSeparatorInactivePreview() {
    Edupage2Theme {
        BreakSeparator(breakMinutes = 10)
    }
}
