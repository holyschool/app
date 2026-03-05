package com.enderplusbayzuiship.edupage2.ui.grades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.SLOVAK_GRADE_MAP
import com.edupage.api.model.grades.Term
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import java.time.format.DateTimeFormatter

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    bottomPadding: PaddingValues,
    viewModel: GradesViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val selectedTerm by viewModel.selectedTerm.collectAsState()
    val haptics      = rememberAppHaptics()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val hasAnyNew = (uiState as? GradesUiState.Success)?.hasAnyNew == true

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.grades_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Mark-all-read button — visible only when there are new grades
                    if (hasAnyNew) {
                        FilledTonalIconButton(onClick = {
                            haptics.click()
                            viewModel.markAllRead()
                        }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.grades_mark_all_read)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    FilledTonalIconButton(onClick = { haptics.click(); viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.grades_refresh))
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
            // ── Term selector ─────────────────────────────────────────────────
            TermSelector(
                selected = selectedTerm,
                onSelect = { haptics.tick(); viewModel.setTerm(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Content ───────────────────────────────────────────────────────
            when (val state = uiState) {
                is GradesUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                is GradesUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { haptics.click(); viewModel.refresh() },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(stringResource(R.string.grades_retry))
                            }
                        }
                    }
                }

                is GradesUiState.Success -> {
                    if (state.subjects.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.grades_no_grades),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Start expanded only for subjects that have new grades.
                        // Re-keyed whenever the subject list identity changes (term switch / refresh).
                        val expandedMap = remember(state.subjects) {
                            mutableStateMapOf(
                                *state.subjects.map { it.subjectName to it.hasNewGrades }.toTypedArray()
                            )
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 8.dp,
                                bottom = 16.dp + bottomPadding.calculateBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.subjects, key = { it.subjectName }) { group ->
                                val expanded = expandedMap[group.subjectName] ?: group.hasNewGrades
                                SubjectCard(
                                    group = group,
                                    expanded = expanded,
                                    onToggle = {
                                        haptics.tick()
                                        val nowExpanded = !expanded
                                        expandedMap[group.subjectName] = nowExpanded
                                        // When the user collapses a card, mark its grades as read
                                        if (!nowExpanded && group.hasNewGrades) {
                                            viewModel.markSubjectRead(group.subjectName)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Term selector ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermSelector(
    selected: Term,
    onSelect: (Term) -> Unit,
    modifier: Modifier = Modifier,
) {
    val terms = listOf(
        Term.FIRST  to stringResource(R.string.grades_term_first),
        Term.SECOND to stringResource(R.string.grades_term_second),
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        terms.forEachIndexed { index, (term, label) ->
            SegmentedButton(
                selected = selected == term,
                onClick  = { onSelect(term) },
                shape    = SegmentedButtonDefaults.itemShape(index = index, count = terms.size),
                label    = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

// ── Subject card ──────────────────────────────────────────────────────────────

@Composable
private fun SubjectCard(
    group: GradeSubjectGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Subject name + grade count
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // New-grade dot badge
                    if (group.hasNewGrades) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.grades_count, group.grades.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            // Average chip
            AverageChip(average = group.average, allVerbal = group.allVerbal)

            Spacer(Modifier.width(8.dp))

            // Expand/collapse arrow
            val arrowAngle by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = tween(200),
                label = "arrow"
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(arrowAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Expanded grade rows ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(150)),
            exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                group.grades.forEachIndexed { index, grade ->
                    GradeRow(grade = grade)
                    if (index < group.grades.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ── Average chip ──────────────────────────────────────────────────────────────

@Composable
private fun AverageChip(average: Double?, allVerbal: Boolean) {
    val (text, color) = when {
        allVerbal || average == null -> stringResource(R.string.grades_verbal) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> {
            val label = if (average == average.toLong().toDouble()) average.toLong().toString()
                        else "%.2f".format(average)
            label to gradeColor(average)
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ── Grade row ─────────────────────────────────────────────────────────────────

@Composable
private fun GradeRow(grade: EduGrade) {
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    val gradeText = when (val n = grade.gradeN) {
        is Double -> if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
        is String -> n
        else      -> "–"
    }
    val color = if (grade.verbal) MaterialTheme.colorScheme.onSurfaceVariant
                else gradeColor(
                    when (val n = grade.gradeN) {
                        is Double -> n
                        is String -> n.toDoubleOrNull()
                            ?: SLOVAK_GRADE_MAP[n.lowercase()]
                            ?: 0.0
                        else -> 0.0
                    }
                )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grade value bubble
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.15f),
            contentColor = color,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = gradeText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (gradeText.length > 2) 14.sp else 20.sp,
                    maxLines = 1
                )
            }
        }

        // Title, teacher, date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = grade.title.ifBlank { "–" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = buildList {
                grade.teacher?.name?.let { add(it) }
                add(grade.date.format(dateFmt))
            }.joinToString(" · ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val gradeComment = grade.comment
            if (!gradeComment.isNullOrBlank()) {
                Text(
                    text = gradeComment,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Percent / points chip
        val gradePercent = grade.percent
        val gradeMaxPoints = grade.maxPoints
        val trailingText: String? = when {
            gradePercent != null && !gradePercent.isInfinite() && !gradePercent.isNaN() -> {
                val pct = "%.0f".format(gradePercent)
                stringResource(R.string.grades_percent, pct)
            }
            gradeMaxPoints != null && grade.gradeN != null -> {
                val pts = when (val n = grade.gradeN) {
                    is Double -> if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
                    is String -> n
                    else -> "?"
                }
                val max = if (gradeMaxPoints == gradeMaxPoints.toLong().toDouble())
                    gradeMaxPoints.toLong().toString() else gradeMaxPoints.toString()
                stringResource(R.string.grades_points, pts, max)
            }
            else -> null
        }
        if (trailingText != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Color helper ──────────────────────────────────────────────────────────────

/**
 * Maps a Slovak/Czech numeric grade (1=best, 5=worst) to a theme color.
 * 1 → tertiary (green-ish)
 * ≤ 2 → primary (blue)
 * ≤ 3 → secondary (teal / neutral)
 * ≤ 4 → error dimmed (orange-ish)
 * > 4 → error (red)
 */
@Composable
private fun gradeColor(grade: Double): Color = when {
    grade <= 1.0 -> MaterialTheme.colorScheme.tertiary
    grade <= 2.0 -> MaterialTheme.colorScheme.primary
    grade <= 3.0 -> MaterialTheme.colorScheme.secondary
    grade <= 4.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    else         -> MaterialTheme.colorScheme.error
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Grades – Light", showBackground = true)
@Preview(name = "Grades – Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradesScreenPreview() {
    Edupage2Theme {
        GradesScreen(bottomPadding = PaddingValues())
    }
}
