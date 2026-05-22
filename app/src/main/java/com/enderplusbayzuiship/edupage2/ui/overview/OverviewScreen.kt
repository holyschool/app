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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.timetable.Lesson
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
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
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    val timetableState by viewModel.timetableState.collectAsState()
    val gradesState    by viewModel.gradesState.collectAsState()
    val messagesState  by viewModel.messagesState.collectAsState()
    val isRefreshing   by viewModel.isRefreshing.collectAsState()
    val currentTime    by viewModel.currentTime.collectAsState()
    val haptics        = rememberAppHaptics()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val refreshState   = rememberPullToRefreshState()

    val isInitialLoading = timetableState is TimetableOverviewState.Loading &&
        gradesState is GradesOverviewState.Loading &&
        messagesState is MessagesOverviewState.Loading

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
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.overview_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    FilledTonalIconButton(onClick = { haptics.click(); viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.overview_refresh),
                            modifier = Modifier.rotate(if (isRefreshing) rotation else 0f)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(onClick = { haptics.click(); onSettings() }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.overview_settings))
                    }
                    Spacer(Modifier.width(8.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isInitialLoading || isRefreshing) {
                    OverviewSkeleton(
                        bottomPadding = bottomPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp + bottomPadding.calculateBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Spacer(Modifier.height(8.dp))

                        TimetableCard(
                            state = timetableState,
                            currentTime = currentTime,
                            onGoToTimetable = onGoToTimetable,
                            haptics = haptics,
                        )

                        GradesCard(
                            state = gradesState,
                            onGoToGrades = onGoToGrades,
                            haptics = haptics,
                        )

                        MessagesCard(
                            state = messagesState,
                            onGoToMessages = onGoToMessages,
                            haptics = haptics,
                        )

                        QuickActionsCard(
                            onCompose   = onGoToMessages,
                            onTimetable = onGoToTimetable,
                            onGrades    = onGoToGrades,
                            haptics     = haptics,
                        )
                    }
                }
            }

            if (isRefreshing) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
private fun TimetableCard(
    state: TimetableOverviewState,
    currentTime: LocalTime,
    onGoToTimetable: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    OverviewCard {
        when (state) {
            is TimetableOverviewState.Loading -> TimetableCardLoading()
            is TimetableOverviewState.Error   -> TimetableCardError()
            is TimetableOverviewState.Success -> TimetableCardContent(
                state        = state,
                currentTime  = currentTime,
                expanded     = expanded,
                onToggle     = { haptics.tick(); expanded = !expanded },
                onGoToTimetable = onGoToTimetable,
                haptics      = haptics,
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
private fun TimetableCardError() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardSectionHeader(
            title = stringResource(R.string.overview_today_schedule),
            icon  = Icons.Default.DateRange,
        )
        Text(
            text  = stringResource(R.string.overview_error_timetable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        if (currentLesson != null) {
            CurrentLessonBanner(lesson = currentLesson, currentTime = currentTime)
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
                    onClick = { haptics.tick(); onToggle() },
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
                        onClick = { haptics.tick(); onGoToTimetable() },
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
                    onClick = { haptics.tick(); onGoToTimetable() },
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
private fun CurrentLessonBanner(lesson: Lesson, currentTime: LocalTime) {
    Surface(
        shape = RoundedCornerShape(14.dp),
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
                val minsLeft = java.time.Duration.between(currentTime, end).toMinutes()
                if (minsLeft >= 0) {
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text       = formatTimeLeft(minsLeft),
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
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text       = lesson.period?.let { "$it." } ?: "–",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color      = if (isCurrent) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            modifier   = Modifier.width(26.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = getDisplayName(lesson),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent || isNext) FontWeight.SemiBold else FontWeight.Normal,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
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
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
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
                            m.clickable { haptics.tick(); expanded = !expanded }
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
                    Text(
                        text  = stringResource(R.string.overview_no_grades),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                                        onClick = { haptics.tick(); expanded = !expanded },
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
                                        onClick = { haptics.tick(); onGoToGrades() },
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
private fun MessagesCard(
    state: MessagesOverviewState,
    onGoToMessages: (() -> Unit)?,
    haptics: com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
) {
    OverviewCard {
        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { m -> if (onGoToMessages != null) m.clip(RoundedCornerShape(12.dp)).clickable { haptics.tick(); onGoToMessages() } else m },
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
                    Text(
                        text  = stringResource(R.string.overview_no_messages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                                    onClick = { haptics.tick(); onGoToMessages() },
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
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text  = initials,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

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
private fun QuickActionsCard(
    onCompose:   (() -> Unit)?,
    onTimetable: (() -> Unit)?,
    onGrades:    (() -> Unit)?,
    haptics:     com.enderplusbayzuiship.edupage2.ui.util.AppHaptics,
) {
    OverviewCard {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            CardSectionHeader(
                title = stringResource(R.string.overview_quick_actions),
                icon  = Icons.AutoMirrored.Filled.Assignment,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onCompose != null) {
                    QuickActionButton(
                        label    = stringResource(R.string.overview_action_compose),
                        icon     = Icons.Default.Edit,
                        modifier = Modifier.weight(1f),
                        onClick  = { haptics.click(); onCompose() },
                    )
                }
                if (onTimetable != null) {
                    QuickActionButton(
                        label    = stringResource(R.string.overview_action_timetable),
                        icon     = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        onClick  = { haptics.click(); onTimetable() },
                    )
                }
                if (onGrades != null) {
                    QuickActionButton(
                        label    = stringResource(R.string.overview_action_grades),
                        icon     = Icons.Default.Star,
                        modifier = Modifier.weight(1f),
                        onClick  = { haptics.click(); onGrades() },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label:    String,
    icon:     ImageVector,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit,
) {
    FilledTonalButton(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
            )
        }
    }
}

private const val TIMETABLE_COLLAPSED_COUNT = 4
private const val GRADES_COLLAPSED_COUNT    = 3

@Composable
private fun OverviewCard(content: @Composable () -> Unit) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun CardSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint     = MaterialTheme.colorScheme.primary,
        )
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun gradeColor(grade: Double): Color = when {
    grade <= 1.0 -> MaterialTheme.colorScheme.tertiary
    grade <= 2.0 -> MaterialTheme.colorScheme.primary
    grade <= 3.0 -> MaterialTheme.colorScheme.secondary
    grade <= 4.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    else         -> MaterialTheme.colorScheme.error
}

private fun formatTimeLeft(minutes: Long): String = when {
    minutes < 1        -> "<1m"
    minutes < 60       -> "${minutes}m"
    minutes % 60 == 0L -> "${minutes / 60}h"
    else               -> "${minutes / 60}h ${minutes % 60}m"
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
