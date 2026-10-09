package com.wiffles.edupage.ui.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiffles.edupage.R
import com.wiffles.edupage.data.SubjectStyle
import com.wiffles.edupage.ui.core.containers.SectionLabel
import com.wiffles.edupage.ui.core.sheets.AppBottomSheet
import com.wiffles.edupage.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectIconEditorSheet(
    onDismiss: () -> Unit,
    viewModel: SubjectIconsViewModel = hiltViewModel(),
) {
    val haptics = rememberAppHaptics()
    val styles by viewModel.styles.collectAsState()
    val subjectsState by viewModel.subjectsState.collectAsState()
    val aiAvailable by viewModel.aiAvailable.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    var selected by remember { mutableStateOf(listOf<String>()) }
    var editingIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        viewModel.ensureLoaded()
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        when (val state = subjectsState) {
            is SubjectsState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.subject_icons_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is SubjectsState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.subject_icons_error),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        haptics.virtualKey()
                        viewModel.loadSubjects()
                    }) {
                        Text(stringResource(R.string.messages_retry))
                    }
                }
            }

            is SubjectsState.Ready -> {
                val allSubjects = state.mine + state.others
                val current = selected.getOrNull(editingIndex)
                if (editingIndex < 0 || current == null) {
                    SelectSubjectsStep(
                        mine = state.mine,
                        others = state.others,
                        selected = selected,
                        aiAvailable = aiAvailable,
                        aiState = aiState,
                        onAutoAssign = {
                            haptics.virtualKey()
                            viewModel.autoAssignIcons()
                        },
                        onToggle = { name ->
                            haptics.virtualKey()
                            selected = if (name in selected) selected - name else selected + name
                        },
                        onToggleAll = {
                            haptics.virtualKey()
                            selected = if (allSubjects.isNotEmpty() && selected.size == allSubjects.size) {
                                emptyList()
                            } else {
                                allSubjects.map { it.name }
                            }
                        },
                        onNext = {
                            haptics.virtualKey()
                            editingIndex = 0
                        },
                    )
                } else {
                    val info = allSubjects.firstOrNull { it.name == current }
                        ?: SubjectInfo(name = current)
                    val style = styles[current] ?: SubjectStyle()
                    EditSubjectStep(
                        subject = info,
                        index = editingIndex,
                        total = selected.size,
                        style = style,
                        onStyleChange = { viewModel.setStyle(current, it) },
                        onBack = {
                            haptics.virtualKey()
                            editingIndex -= 1
                        },
                        onSkip = {
                            haptics.virtualKey()
                            if (editingIndex >= selected.lastIndex) onDismiss() else editingIndex += 1
                        },
                        onNext = {
                            haptics.virtualKey()
                            if (editingIndex >= selected.lastIndex) onDismiss() else editingIndex += 1
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectSubjectsStep(
    mine: List<SubjectInfo>,
    others: List<SubjectInfo>,
    selected: List<String>,
    aiAvailable: Boolean,
    aiState: SubjectAiState,
    onAutoAssign: () -> Unit,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onNext: () -> Unit,
) {
    val allNames = (mine + others).map { it.name }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.subject_icons_step_select),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.subject_icons_select_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onToggleAll) {
            Text(
                text = stringResource(
                    if (allNames.isNotEmpty() && selected.size == allNames.size) {
                        R.string.subject_icons_clear_all
                    } else {
                        R.string.subject_icons_select_all
                    }
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (mine.isNotEmpty()) {
                GroupHeader(text = stringResource(R.string.subject_icons_group_mine))
                mine.forEach { subject ->
                    SubjectSelectRow(subject = subject, checked = subject.name in selected, onToggle = onToggle)
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
            }

            if (others.isNotEmpty()) {
                GroupHeader(
                    text = stringResource(
                        if (mine.isEmpty()) {
                            R.string.subject_icons_group_all
                        } else {
                            R.string.subject_icons_group_more
                        }
                    ),
                )
                others.forEach { subject ->
                    SubjectSelectRow(subject = subject, checked = subject.name in selected, onToggle = onToggle)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (aiAvailable) {
            Button(
                onClick = onAutoAssign,
                enabled = aiState !is SubjectAiState.Running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (aiState is SubjectAiState.Running) {
                            R.string.subject_icons_ai_assigning
                        } else {
                            R.string.subject_icons_ai_assign
                        }
                    )
                )
            }
            when (aiState) {
                is SubjectAiState.Done -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.subject_icons_ai_assigned, aiState.count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is SubjectAiState.Error -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = aiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Unit
            }
            Spacer(Modifier.height(4.dp))
        }

        Button(
            onClick = onNext,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.subject_icons_next))
        }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
private fun SubjectSelectRow(
    subject: SubjectInfo,
    checked: Boolean,
    onToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle(subject.name) }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle(subject.name) },
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = subject.teachers.firstOrNull() ?: subject.shortName
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EditSubjectStep(
    subject: SubjectInfo,
    index: Int,
    total: Int,
    style: SubjectStyle,
    onStyleChange: (SubjectStyle) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val progress = if (total <= 0) 1f else (index + 1).toFloat() / total.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectAvatar(
                subjectName = subject.name,
                style = style,
                enabled = true,
                containerSize = 88.dp,
                iconSize = 44.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subject.shortName?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (subject.teachers.isNotEmpty()) {
                    Text(
                        text = subject.teachers.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel(text = stringResource(R.string.subject_icons_icon_label))
        Spacer(Modifier.height(8.dp))
        IconPickerRow(
            icons = SubjectIconCatalog.icons,
            selectedKey = style.iconKey,
            onSelect = { onStyleChange(style.copy(iconKey = it)) },
        )

        Spacer(Modifier.height(24.dp))

        SectionLabel(text = stringResource(R.string.subject_icons_color_label))
        Spacer(Modifier.height(8.dp))
        ColorPickerRow(
            selectedArgb = style.colorArgb,
            onSelect = { onStyleChange(style.copy(colorArgb = it)) },
            defaultLabel = subject.name,
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.subject_icons_back))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.subject_icons_skip))
            }
            Button(onClick = onNext) {
                Text(
                    stringResource(
                        if (index >= total - 1) R.string.subject_icons_finish else R.string.subject_icons_next
                    )
                )
            }
        }
    }
}

