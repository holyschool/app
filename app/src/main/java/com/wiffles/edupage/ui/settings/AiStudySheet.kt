package com.wiffles.edupage.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wiffles.edupage.R
import com.wiffles.edupage.ui.core.sheets.AppBottomSheet
import com.wiffles.edupage.ui.util.rememberAppHaptics

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AiStudySheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val study by viewModel.aiStudySettings.collectAsState()
    var text by remember { mutableStateOf(study.customInstructions) }
    var className by remember { mutableStateOf(study.studentClass) }
    val haptics = rememberAppHaptics()

    val presets = listOf(
        R.string.settings_ai_study_preset_stepbystep,
        R.string.settings_ai_study_preset_quiz,
        R.string.settings_ai_study_preset_simple,
    )

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_ai_study_custom_prompt),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.settings_ai_study_sheet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = className,
                onValueChange = { if (it.length <= 60) className = it },
                label = { Text(stringResource(R.string.settings_ai_study_class_label)) },
                placeholder = { Text(stringResource(R.string.settings_ai_study_class_hint)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.settings_ai_study_class_desc)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 2000) text = it },
                label = { Text(stringResource(R.string.settings_ai_study_prompt_label)) },
                minLines = 4,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.settings_ai_study_presets),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { res ->
                    val label = stringResource(res)
                    AssistChip(
                        onClick = {
                            haptics.virtualKey()
                            text = label
                        },
                        label = { Text(label) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        haptics.virtualKey()
                        viewModel.setAiStudyInstructions(text.trim())
                        viewModel.setAiStudyClass(className.trim())
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}