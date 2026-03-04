package com.enderplusbayzuiship.edupage2.ui.timetable

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.timetable.Lesson
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.edupage.api.model.Classroom
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    bottomPadding: PaddingValues,
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Timetable",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                onPreviousDay = { viewModel.setDate(selectedDate.minusDays(1)) },
                onNextDay = { viewModel.setDate(selectedDate.plusDays(1)) },
                onToday = { viewModel.setDate(LocalDate.now()) }
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
                                onClick = { viewModel.refresh() },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Retry")
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
                                    text = "No lessons today",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 12.dp + bottomPadding.calculateBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.lessons) { lesson ->
                                LessonCard(lesson = lesson)
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
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val today = LocalDate.now()
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day")
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
            if (date != today) {
                AssistChip(
                    onClick = onToday,
                    label = { Text("Today", style = MaterialTheme.typography.labelMedium) },
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
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day")
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun LessonCard(lesson: Lesson) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val startStr = lesson.startTime?.format(timeFormatter) ?: "?"
    val endStr = lesson.endTime?.format(timeFormatter) ?: "?"

    val containerColor = when {
        lesson.isCancelled -> MaterialTheme.colorScheme.errorContainer
        lesson.hasChange() -> MaterialTheme.colorScheme.tertiaryContainer
        lesson.isOnlineLesson() -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = startStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = endStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                val subjectName = lesson.subject?.name ?: "Unknown subject"
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
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Teachers
                val teacherNames    = lesson.teachers?.joinToString(", ") { it.name ?: "?" } ?: ""
                val origTeacherNames = lesson.origTeachers?.joinToString(", ") { it.name ?: "?" }
                if (!origTeacherNames.isNullOrEmpty() && origTeacherNames != teacherNames) {
                    Text(
                        text = buildChangedText(old = origTeacherNames, new = teacherNames),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (teacherNames.isNotEmpty()) {
                    Text(
                        text = teacherNames,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Classrooms
                val classroomNames    = lesson.classrooms?.joinToString(", ") { it.name ?: "?" } ?: ""
                val origClassroomNames = lesson.origClassrooms?.joinToString(", ") { it.name ?: "?" }
                if (!origClassroomNames.isNullOrEmpty() && origClassroomNames != classroomNames) {
                    Text(
                        text = buildChangedText(old = origClassroomNames, new = classroomNames),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (classroomNames.isNotEmpty()) {
                    Text(
                        text = classroomNames,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!lesson.curriculum.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lesson.curriculum!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (lesson.isCancelled) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text(
                            "Cancelled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
                if (lesson.hasChange() && !lesson.isCancelled) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                        Text(
                            "Changed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
                if (lesson.isOnlineLesson()) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                        Text(
                            "Online",
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
            onPreviousDay = {}, onNextDay = {}, onToday = {}
        )
    }
}
