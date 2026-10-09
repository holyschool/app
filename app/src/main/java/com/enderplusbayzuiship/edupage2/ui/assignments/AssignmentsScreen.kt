package com.enderplusbayzuiship.edupage2.ui.assignments

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.grades.Assignment
import com.edupage.api.model.grades.AssignmentType
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.util.ShimmerBox
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    onBack: () -> Unit,
    viewModel: AssignmentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val historyMonths by viewModel.historyMonths.collectAsState()
    val detailState by viewModel.detailState.collectAsState()
    val haptics = rememberAppHaptics()
    var detailAssignment by remember { mutableStateOf<Assignment?>(null) }
    var viewerAttachment by remember { mutableStateOf<com.edupage.api.model.MessageAttachment?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.assignments_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptics.virtualKey(); onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { haptics.virtualKey(); viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.assignments_refresh))
                    }
                    Spacer(Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            val isRefreshing = (uiState as? AssignmentsUiState.Success)?.isRefreshing == true
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            FilterSelector(
                selected = selectedFilter,
                onSelect = { haptics.virtualKey(); viewModel.setFilter(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (val state = uiState) {
                is AssignmentsUiState.Loading -> AssignmentsSkeleton(Modifier.fillMaxSize())
                is AssignmentsUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )
                is AssignmentsUiState.Success -> {
                    val filtered = filterAssignments(state.assignments, selectedFilter)
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Assignment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.assignments_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item(key = "assignments-list-${filtered.size}-${selectedFilter}") {
                                RoundedCardContainer {
                                    filtered.forEach { a ->
                                        key(a.id ?: "${a.title}-${a.date}-${a.type}") {
                                            AssignmentCard(
                                                assignment = a,
                                                onClick = {
                                                    haptics.virtualKey()
                                                    val superId = a.superId
                                                    if (superId != null) {
                                                        detailAssignment = a
                                                        viewModel.loadDetail(superId)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            if (historyMonths < 12) {
                                item(key = "assignments-load-more") {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { haptics.virtualKey(); viewModel.loadMore() },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.assignments_load_more),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
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

    detailAssignment?.let { assignment ->
        AssignmentDetailSheet(
            assignment = assignment,
            detailState = detailState,
            onDismiss = {
                detailAssignment = null
                viewModel.resetDetail()
            },
            onOpenAttachment = { viewerAttachment = it },
        )
    }

    viewerAttachment?.let { attachment ->
        com.enderplusbayzuiship.edupage2.ui.attachments.AttachmentViewerSheet(
            attachment = attachment,
            onDismiss = { viewerAttachment = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentDetailSheet(
    assignment: Assignment,
    detailState: AssignmentDetailState,
    onDismiss: () -> Unit,
    onOpenAttachment: (com.edupage.api.model.MessageAttachment) -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = assignment.title?.ifBlank { "–" } ?: "–",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            assignment.subjectName?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val meta = buildList {
                assignment.date?.let { add(it.format(dateFmt)) }
                add(typeLabel(assignment.type))
            }.joinToString(" · ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            assignment.details?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.assignments_attachments),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            when (detailState) {
                is AssignmentDetailState.Idle,
                is AssignmentDetailState.Loading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.assignments_attachments_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is AssignmentDetailState.Error -> Text(
                    text = detailState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                is AssignmentDetailState.Ready -> {
                    if (detailState.attachments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.assignments_no_attachments),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        detailState.attachments.forEach { attachment ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceBright,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenAttachment(attachment) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = attachment.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        Icons.Rounded.Download,
                                        contentDescription = stringResource(R.string.messages_attachment_open),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun filterAssignments(list: List<Assignment>, filter: AssignmentFilter): List<Assignment> {
    val sorted = list.sortedByDescending { it.date }
    return when (filter) {
        AssignmentFilter.ALL -> sorted
        AssignmentFilter.PENDING -> sorted.filter { !it.isFinished }
        AssignmentFilter.DONE -> sorted.filter { it.isFinished }
        AssignmentFilter.TESTS -> sorted.filter {
            it.type in setOf(AssignmentType.TEST, AssignmentType.EXAM, AssignmentType.SMALL_EXAM,
                AssignmentType.BIG_EXAM, AssignmentType.ORAL_EXAM, AssignmentType.REPORT_EXAM,
                AssignmentType.ETEST, AssignmentType.TESTING)
        }
    }
}

@Composable
private fun FilterSelector(
    selected: AssignmentFilter,
    onSelect: (AssignmentFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        AssignmentFilter.ALL to stringResource(R.string.assignments_filter_all),
        AssignmentFilter.PENDING to stringResource(R.string.assignments_filter_pending),
        AssignmentFilter.DONE to stringResource(R.string.assignments_filter_done),
        AssignmentFilter.TESTS to stringResource(R.string.assignments_filter_tests),
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (filter, label) ->
            SegmentedButton(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun AssignmentCard(assignment: Assignment, onClick: () -> Unit) {
    val accent = typeColor(assignment.type)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (assignment.superId != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.15f),
                contentColor = accent,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(typeIcon(assignment.type), contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.title?.ifBlank { "–" } ?: "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = assignment.subjectName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                val meta = buildList {
                    assignment.date?.let { add(it.format(dateFmt)) }
                    add(typeLabel(assignment.type))
                }.joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                assignment.details?.takeIf { it.isNotBlank() }?.let { details ->
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (assignment.isFinished) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.assignments_finished),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

@Composable
private fun typeColor(type: AssignmentType): androidx.compose.ui.graphics.Color = when (type) {
    AssignmentType.TEST, AssignmentType.ETEST, AssignmentType.TESTING ->
        MaterialTheme.colorScheme.error
    AssignmentType.HOMEWORK, AssignmentType.ETEST_HOMEWORK ->
        MaterialTheme.colorScheme.primary
    AssignmentType.EXAM, AssignmentType.BIG_EXAM, AssignmentType.SMALL_EXAM,
    AssignmentType.ORAL_EXAM, AssignmentType.REPORT_EXAM ->
        MaterialTheme.colorScheme.tertiary
    AssignmentType.PROJECT, AssignmentType.PROJECT_EXAM ->
        MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun typeIcon(type: AssignmentType): ImageVector = when (type) {
    AssignmentType.TEST, AssignmentType.ETEST, AssignmentType.TESTING,
    AssignmentType.EXAM, AssignmentType.BIG_EXAM, AssignmentType.SMALL_EXAM,
    AssignmentType.ORAL_EXAM, AssignmentType.REPORT_EXAM -> Icons.Rounded.Quiz
    else -> Icons.Rounded.Assignment
}

@Composable
private fun typeLabel(type: AssignmentType): String = when (type) {
    AssignmentType.TEST, AssignmentType.ETEST, AssignmentType.TESTING -> stringResource(R.string.assignment_type_test)
    AssignmentType.HOMEWORK, AssignmentType.ETEST_HOMEWORK -> stringResource(R.string.assignment_type_homework)
    AssignmentType.EXAM, AssignmentType.BIG_EXAM, AssignmentType.SMALL_EXAM,
    AssignmentType.ORAL_EXAM, AssignmentType.REPORT_EXAM -> stringResource(R.string.assignment_type_exam)
    AssignmentType.PROJECT, AssignmentType.PROJECT_EXAM -> stringResource(R.string.assignment_type_project)
    AssignmentType.LESSON -> stringResource(R.string.assignment_type_lesson)
    AssignmentType.CURRICULUM -> stringResource(R.string.assignment_type_curriculum)
    else -> stringResource(R.string.assignment_type_other)
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberAppHaptics()
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { haptics.virtualKey(); onRetry() },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.assignments_retry),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun AssignmentsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(Modifier.size(44.dp), height = 44.dp, cornerRadius = 12.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.7f), height = 16.dp)
                        ShimmerBox(Modifier.fillMaxWidth(0.4f), height = 11.dp)
                    }
                }
            }
        }
    }
}

