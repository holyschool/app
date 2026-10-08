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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.SLOVAK_GRADE_MAP
import com.edupage.api.model.grades.Term
import com.edupage.api.model.grades.computeStats
import com.edupage.api.model.grades.SubjectAverage
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelInitials
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.ui.modifiers.scrollMotionBlur
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.ShimmerBox
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    bottomPadding: PaddingValues,
    viewModel: GradesViewModel = hiltViewModel()
) {
    val uiState                by viewModel.uiState.collectAsState()
    val selectedTerm           by viewModel.selectedTerm.collectAsState()
    val pendingHighlight       by viewModel.pendingHighlightSubject.collectAsState()
    val haptics                = rememberAppHaptics()
    val scrollBehavior         = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val refreshState           = rememberPullToRefreshState()
    val bottomSheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showPrediction          by remember { mutableStateOf(false) }
    var selectedGrade           by remember { mutableStateOf<EduGrade?>(null) }
    var selectedSubject         by remember { mutableStateOf<String?>(null) }
    var nextVirtualId           by remember { mutableStateOf(0) }
    val virtualGrades           = remember { mutableStateListOf<VirtualGradeInput>() }

    val isRefreshing = (uiState as? GradesUiState.Success)?.isRefreshing == true

    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val hasAnyNew = (uiState as? GradesUiState.Success)?.hasAnyNew == true

    LaunchedEffect(showPrediction, uiState) {
        if (showPrediction) {
            val subjects = (uiState as? GradesUiState.Success)?.subjects.orEmpty()
            selectedSubject = subjects.firstOrNull()?.subjectName
            virtualGrades.clear()
            nextVirtualId = 0
        }
    }

    val successState = uiState as? GradesUiState.Success
    val canPredict = successState?.subjects?.isNotEmpty() == true

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
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
                        if (hasAnyNew) {
                        FilledTonalIconButton(onClick = {
                            haptics.virtualKey()
                            viewModel.markAllRead()
                        }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.grades_mark_all_read)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    FilledTonalIconButton(onClick = { haptics.virtualKey(); viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.grades_refresh),
                            modifier = Modifier.rotate(if (isRefreshing) rotation else 0f)
                        )
                    }
                    if (canPredict) {
                        Spacer(Modifier.width(4.dp))
                        FilledTonalIconButton(onClick = {
                            haptics.virtualKey(); showPrediction = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.grades_predict_button)
                            )
                        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AnimatedVisibility(visible = isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                TermSelector(
                    selected = selectedTerm,
                    onSelect = { haptics.virtualKey(); viewModel.setTerm(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                when (val state = uiState) {
                    is GradesUiState.Loading -> {
                        GradesSkeleton(
                            bottomPadding = bottomPadding,
                            modifier = Modifier.fillMaxSize()
                        )
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
                                    onClick = { haptics.virtualKey(); viewModel.refresh() },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.grades_retry),
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
                            val expandedMap = remember(state.subjects) {
                                mutableStateMapOf(
                                    *state.subjects.map { it.subjectName to it.hasNewGrades }.toTypedArray()
                                )
                            }

                            val listState = rememberLazyListState()

                            LaunchedEffect(pendingHighlight, state.subjects) {
                                val target = pendingHighlight ?: return@LaunchedEffect
                                expandedMap[target] = true
                                val index = state.subjects.indexOfFirst { it.subjectName == target }
                                if (index >= 0) listState.animateScrollToItem(index)
                                viewModel.consumeHighlight()
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scrollMotionBlur(
                                        lazyListState = listState,
                                        enabled = com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate.forTabs(),
                                    ),
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp,
                                    top = 8.dp,
                                    bottom = 16.dp + bottomPadding.calculateBottomPadding()
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val allGrades = state.subjects.flatMap { it.grades }
                                item(key = "stats") {
                                    GradeStatsCard(stats = allGrades.computeStats())
                                }
                                items(state.subjects, key = { it.subjectName }) { group ->
                                    Box(Modifier.animateItem()) {
                                    val expanded = expandedMap[group.subjectName] ?: group.hasNewGrades
                                    SubjectCard(
                                        group = group,
                                        expanded = expanded,
                                        onGradeClick = { selectedGrade = it },
                                        onToggle = {
                                            haptics.virtualKey()
                                            val nowExpanded = !expanded
                                            expandedMap[group.subjectName] = nowExpanded
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
    }

    if (showPrediction) {
        val subjects = (uiState as? GradesUiState.Success)?.subjects.orEmpty()
        ModalBottomSheet(
            onDismissRequest = { showPrediction = false },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            GradePredictionSheet(
                subjects = subjects,
                selectedSubject = selectedSubject,
                onSelectSubject = { selectedSubject = it },
                virtualGrades = virtualGrades,
                onAddVirtualGrade = {
                    virtualGrades.add(VirtualGradeInput(nextVirtualId++, weight = "1"))
                },
                onRemoveVirtualGrade = { id ->
                    virtualGrades.removeAll { it.id == id }
                }
            )
        }
    }

    selectedGrade?.let { grade ->
        ModalBottomSheet(
            onDismissRequest = { selectedGrade = null },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            GradeDetailsBottomSheet(grade = grade, onDismiss = { selectedGrade = null })
        }
    }
}

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
                colors   = SegmentedButtonDefaults.colors(
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
                label    = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun GradeStatsCard(stats: com.edupage.api.model.grades.GradeStats) {
    val avg = stats.overallAverage
    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.grades_stats_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (avg != null) {
                    AverageChip(average = avg, allVerbal = false)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatsStat(
                    label = stringResource(R.string.grades_stats_count),
                    value = stats.totalGrades.toString()
                )
                StatsStat(
                    label = stringResource(R.string.grades_stats_subjects),
                    value = stats.bySubject.count { it.gradeCount > 0 }.toString()
                )
                StatsStat(
                    label = stringResource(R.string.grades_stats_verbal),
                    value = (stats.totalGrades - stats.trend.size).toString()
                )
            }
            val monthBuckets = stats.byMonth
            if (monthBuckets.size > 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = stringResource(R.string.grades_stats_trend),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    monthBuckets.takeLast(6).forEach { bucket ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = bucket.average?.let {
                                    if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it)
                                } ?: "–",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (bucket.average != null) gradeColor(bucket.average!!) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = bucket.month.name.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun StatsStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectCard(
    group: GradeSubjectGroup,
    expanded: Boolean,
    onGradeClick: (EduGrade) -> Unit = {},
    onToggle: () -> Unit,
) {
    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onToggle,
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            PastelInitials(text = group.subjectName)
            Spacer(Modifier.width(12.dp))
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
                group.classGradeAvg?.let { classAvg ->
                    Text(
                        text = stringResource(R.string.grades_class_average, if (classAvg == classAvg.toLong().toDouble()) classAvg.toLong().toString() else "%.2f".format(classAvg)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            AverageChip(average = group.average, allVerbal = group.allVerbal)

            Spacer(Modifier.width(8.dp))

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
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(150)),
            exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                group.grades.forEach { grade ->
                    GradeRow(grade = grade, onClick = { onGradeClick(grade) })
                }
            }
        }
    }
}

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

@Composable
private fun GradeRow(grade: EduGrade, onClick: () -> Unit = {}) {
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

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth()
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        val importance = grade.importance
        if (importance != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(
                    text = stringResource(R.string.grades_weight, formatWeightValue(importance)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

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
}

@Composable
private fun gradeColor(grade: Double): Color = when {
    grade <= 1.0 -> MaterialTheme.colorScheme.tertiary
    grade <= 2.0 -> MaterialTheme.colorScheme.primary
    grade <= 3.0 -> MaterialTheme.colorScheme.secondary
    grade <= 4.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    else         -> MaterialTheme.colorScheme.error
}

private class VirtualGradeInput(
    val id: Int,
    value: String = "",
    weight: String = ""
) {
    var value by mutableStateOf(value)
    var weight by mutableStateOf(weight)
}

private data class AverageResult(
    val average: Double,
    val totalWeight: Double,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradePredictionSheet(
    subjects: List<GradeSubjectGroup>,
    selectedSubject: String?,
    onSelectSubject: (String) -> Unit,
    virtualGrades: List<VirtualGradeInput>,
    onAddVirtualGrade: () -> Unit,
    onRemoveVirtualGrade: (Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    val subject = subjects.firstOrNull { it.subjectName == selectedSubject }
    val existingEntries = subject
        ?.grades
        ?.mapNotNull { grade ->
            val value = gradeNumericValue(grade) ?: return@mapNotNull null
            value to grade.importance
        }
        .orEmpty()
    val virtualEntries = virtualGrades.mapNotNull { entry ->
        val value = entry.value.toDoubleOrNull() ?: return@mapNotNull null
        val weight = entry.weight.toDoubleOrNull()
        value to weight
    }

    val currentResult = computeAverageResult(existingEntries)
    val predictedResult = computeAverageResult(existingEntries + virtualEntries)
    var subjectExpanded by remember { mutableStateOf(false) }
    var existingGradesExpanded by remember { mutableStateOf(false) }
    val haptics = rememberAppHaptics()
    val fieldShape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.grades_predict_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(
                text = stringResource(R.string.grades_predict_subject),
                modifier = Modifier.padding(bottom = 0.dp),
            )
            ExposedDropdownMenuBox(
                expanded = subjectExpanded,
                onExpandedChange = { subjectExpanded = !subjectExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSubject.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.grades_predict_subject)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors()
                )
                ExposedDropdownMenu(
                    expanded = subjectExpanded,
                    onDismissRequest = { subjectExpanded = false }
                ) {
                    subjects.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.subjectName) },
                            onClick = {
                                haptics.virtualKey()
                                onSelectSubject(group.subjectName)
                                subjectExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel(
                    text = stringResource(R.string.grades_predict_existing),
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 0.dp),
                )
                if (subject != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${subject.grades.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            when {
                subject == null -> Text(
                    text = stringResource(R.string.grades_predict_pick_subject),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                existingEntries.isEmpty() -> Text(
                    text = stringResource(R.string.grades_predict_no_subject_grades),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                else -> {
                    val allGrades = subject.grades
                    val previewCount = 3.coerceAtMost(allGrades.size)

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceBright,
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            allGrades.take(previewCount).forEachIndexed { index, grade ->
                                PredictionGradeRow(grade = grade)
                                if (index < previewCount - 1 || allGrades.size > previewCount || existingGradesExpanded) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }

                            if (allGrades.size > previewCount && !existingGradesExpanded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptics.virtualKey()
                                            existingGradesExpanded = true
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.grades_predict_show_all, allGrades.size),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = existingGradesExpanded,
                                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200)),
                            ) {
                                Column {
                                    allGrades.drop(previewCount).forEach { grade ->
                                        PredictionGradeRow(grade = grade)
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.virtualKey()
                                                existingGradesExpanded = false
                                            }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.grades_predict_show_less),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .rotate(180f),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(
                text = stringResource(R.string.grades_predict_virtual),
                modifier = Modifier.padding(bottom = 0.dp),
            )
            virtualGrades.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = entry.value,
                        onValueChange = { entry.value = it },
                        label = { Text(stringResource(R.string.grades_predict_value)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = fieldShape,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = entry.weight,
                        onValueChange = { entry.weight = it },
                        label = { Text(stringResource(R.string.grades_predict_weight)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = fieldShape,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        haptics.virtualKey()
                        onRemoveVirtualGrade(entry.id)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null
                        )
                    }
                }
            }
            Button(
                onClick = {
                    haptics.virtualKey()
                    onAddVirtualGrade()
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.grades_predict_add_virtual),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.grades_predict_current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatAverageOnly(currentResult),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.grades_predict_predicted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatAverageOnly(predictedResult),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.grades_predict_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PredictionGradeRow(grade: EduGrade) {
    val numeric = gradeNumericValue(grade)
    val gradeText = numeric?.let { formatAverageValue(it) }
        ?: grade.gradeN?.toString().orEmpty().ifBlank { "–" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = grade.title.ifBlank { gradeText },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = grade.date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val importance = grade.importance
        if (importance != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(
                    text = stringResource(R.string.grades_weight, formatWeightValue(importance)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Text(
                text = gradeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeDetailsBottomSheet(grade: EduGrade, onDismiss: () -> Unit) {
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    val gradeText = when (val n = grade.gradeN) {
        is Double -> if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
        is String -> n
        else -> "–"
    }
    val numeric = gradeNumericValue(grade)
    val scoreColor = if (grade.verbal || numeric == null) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        gradeColor(numeric)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = scoreColor.copy(alpha = 0.15f),
                contentColor = scoreColor,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = gradeText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grade.title.ifBlank { "–" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subjectName = grade.subjectName
                Text(
                    text = listOfNotNull(
                        subjectName,
                        grade.date.format(dateFmt),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        RoundedCardContainer {
            GradeDetailRow(
                label = stringResource(R.string.grade_details_subject),
                value = grade.subjectName ?: "–",
            )
            GradeDetailRow(
                label = stringResource(R.string.grade_details_assignment),
                value = grade.title.ifBlank { "–" },
            )
            GradeDetailRow(
                label = stringResource(R.string.grade_details_score),
                value = gradeText,
                valueColor = scoreColor,
            )
            grade.percent?.takeIf { !it.isInfinite() && !it.isNaN() }?.let {
                GradeDetailRow(
                    label = stringResource(R.string.grade_details_percentage),
                    value = "${"%.0f".format(it)}%",
                )
            }
            grade.maxPoints?.let {
                GradeDetailRow(
                    label = stringResource(R.string.grade_details_points),
                    value = formatWeightValue(it),
                )
            }
            grade.importance?.let {
                GradeDetailRow(
                    label = stringResource(R.string.grade_details_weight),
                    value = formatWeightValue(it),
                )
            }
            grade.classGradeAvg?.let {
                GradeDetailRow(
                    label = stringResource(R.string.grade_details_class_average),
                    value = "%.2f".format(it),
                )
            }
            grade.teacher?.name?.let {
                GradeDetailRow(
                    label = stringResource(R.string.grade_details_teacher),
                    value = it,
                )
            }
            GradeDetailRow(
                label = stringResource(R.string.grade_details_date),
                value = grade.date.format(dateFmt),
            )
        }

        if (!grade.comment.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.grade_details_note),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = grade.comment!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeDetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
        }
    }
}

private fun gradeNumericValue(grade: EduGrade): Double? {
    if (grade.verbal) return null
    return when (val n = grade.gradeN) {
        is Double -> n
        is String -> n.toDoubleOrNull() ?: SLOVAK_GRADE_MAP[n.lowercase()]
        else -> null
    }
}

private fun computeAverageResult(entries: List<Pair<Double, Double?>>): AverageResult? {
    if (entries.isEmpty()) return null
    val hasWeights = entries.any { it.second != null }
    return if (hasWeights) {
        val weightedSum = entries.sumOf { (v, w) -> v * (w ?: 1.0) }
        val totalWeight = entries.sumOf { (_, w) -> w ?: 1.0 }
        if (totalWeight == 0.0) null
        else AverageResult(roundTo2(weightedSum / totalWeight), totalWeight)
    } else {
        val sum = entries.sumOf { it.first }
        val avg = sum / entries.size
        AverageResult(roundTo2(avg), entries.size.toDouble())
    }
}

private fun roundTo2(value: Double): Double =
    (value * 100.0).roundToInt() / 100.0

private fun formatAverageValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else "%.2f".format(value)

private fun formatWeightValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else "%.2f".format(value)

private fun formatAverageOnly(result: AverageResult?): String {
    return if (result == null) "–"
    else formatAverageValue(result.average)
}

@Composable
private fun GradesSkeleton(
    bottomPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(
                start = 16.dp, end = 16.dp,
                top = 8.dp,
                bottom = 16.dp + bottomPadding.calculateBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) { SubjectCardSkeleton() }
    }
}

@Composable
private fun SubjectCardSkeleton() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f), height = 16.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f), height = 11.dp)
            }
            Spacer(Modifier.width(12.dp))
            ShimmerBox(modifier = Modifier.width(40.dp), height = 24.dp, cornerRadius = 50.dp)
        }
    }
}

@Preview(name = "Grades – Light", showBackground = true)
@Preview(name = "Grades – Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradesScreenPreview() {
    Edupage2Theme {
        GradesScreen(bottomPadding = PaddingValues())
    }
}

