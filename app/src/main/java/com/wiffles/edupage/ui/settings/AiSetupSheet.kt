package com.wiffles.edupage.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wiffles.edupage.R
import com.wiffles.edupage.network.AiConfig
import com.wiffles.edupage.network.AiProvider
import com.wiffles.edupage.ui.core.sheets.AppBottomSheet
import com.wiffles.edupage.ui.util.rememberAppHaptics

private const val AI_STEP_PROVIDER = 0
private const val AI_STEP_ACCESS = 1
private const val AI_STEP_MODEL = 2
private const val AI_STEP_VERIFY = 3
private const val AI_STEP_COUNT = 4

@Composable
fun aiProviderLabel(provider: AiProvider): String = stringResource(
    when (provider) {
        AiProvider.GEMINI -> R.string.settings_ai_provider_gemini
        AiProvider.OPENROUTER -> R.string.settings_ai_provider_openrouter
        AiProvider.OPENAI -> R.string.settings_ai_provider_openai
        AiProvider.CUSTOM -> R.string.settings_ai_provider_custom
    }
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AiSetupSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val config by viewModel.aiConfig.collectAsState()
    val modelsState by viewModel.aiModels.collectAsState()
    val testState by viewModel.aiTestState.collectAsState()
    val haptics = rememberAppHaptics()
    val context = LocalContext.current

    var step by remember { mutableStateOf(AI_STEP_PROVIDER) }
    var provider by remember { mutableStateOf(config.provider) }
    var key by remember { mutableStateOf(config.apiKey) }
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var model by remember { mutableStateOf(config.model) }

    fun selectProvider(next: AiProvider) {
        if (next == provider) return
        haptics.virtualKey()
        provider = next
        val stored: AiConfig = viewModel.aiConfigFor(next)
        key = stored.apiKey
        baseUrl = stored.baseUrl
        model = stored.model
    }

    fun persistAccess() {
        viewModel.setAiProvider(provider)
        viewModel.setAiApiKey(provider, key)
        viewModel.setAiCustomBaseUrl(baseUrl)
    }

    fun goNext() {
        haptics.virtualKey()
        when (step) {
            AI_STEP_PROVIDER -> step = AI_STEP_ACCESS
            AI_STEP_ACCESS -> {
                persistAccess()
                viewModel.loadAiModels()
                step = AI_STEP_MODEL
            }
            AI_STEP_MODEL -> {
                viewModel.setAiModel(provider, model)
                step = AI_STEP_VERIFY
            }
        }
    }

    LaunchedEffect(testState) {
        when (val state = testState) {
            is SettingsViewModel.AiTestState.Ok -> {
                Toast.makeText(context, context.getString(R.string.settings_ai_test_ok), Toast.LENGTH_SHORT).show()
                viewModel.consumeAiTestState()
            }
            is SettingsViewModel.AiTestState.Failed -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_ai_test_failed, state.message),
                    Toast.LENGTH_LONG,
                ).show()
                viewModel.consumeAiTestState()
            }
            else -> Unit
        }
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_experimental_ai_quiz),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            StepIndicator(step = step)
            Text(
                text = stringResource(
                    when (step) {
                        AI_STEP_PROVIDER -> R.string.settings_ai_step_provider
                        AI_STEP_ACCESS -> R.string.settings_ai_step_access
                        AI_STEP_MODEL -> R.string.settings_ai_step_model
                        else -> R.string.settings_ai_step_verify
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            when (step) {
                AI_STEP_PROVIDER -> {
                    AiProvider.entries.forEach { option ->
                        ProviderOption(
                            provider = option,
                            selected = provider == option,
                            onClick = { selectProvider(option) },
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_ai_provider_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AI_STEP_ACCESS -> {
                    if (provider.needsBaseUrl) {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text(stringResource(R.string.settings_ai_base_url)) },
                            placeholder = { Text(stringResource(R.string.settings_ai_base_url_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.settings_ai_base_url_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text(stringResource(R.string.settings_ai_api_key)) },
                        placeholder = { Text(stringResource(R.string.settings_ai_api_key_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.settings_ai_access_note, aiProviderLabel(provider)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AI_STEP_MODEL -> {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text(stringResource(R.string.settings_ai_model)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { haptics.virtualKey(); viewModel.loadAiModels() },
                        enabled = modelsState !is SettingsViewModel.AiModelsState.Loading,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        if (modelsState is SettingsViewModel.AiModelsState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(10.dp))
                            Text(stringResource(R.string.settings_ai_models_loading))
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.settings_ai_load_models))
                        }
                    }
                    when (val state = modelsState) {
                        is SettingsViewModel.AiModelsState.Failed -> Text(
                            text = stringResource(R.string.settings_ai_models_failed, state.message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        is SettingsViewModel.AiModelsState.Loaded -> {
                            if (state.models.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.settings_ai_models_empty),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    state.models.forEach { option ->
                                        ModelOption(
                                            model = option,
                                            selected = model == option,
                                            onClick = {
                                                haptics.virtualKey()
                                                model = option
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }

                AI_STEP_VERIFY -> {
                    Text(
                        text = stringResource(R.string.settings_ai_verify_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SummaryRow(stringResource(R.string.settings_ai_provider), aiProviderLabel(provider))
                            SummaryRow(stringResource(R.string.settings_ai_model), model.ifBlank { provider.defaultModel })
                            if (provider.needsBaseUrl) {
                                SummaryRow(stringResource(R.string.settings_ai_base_url), baseUrl)
                            }
                            SummaryRow(
                                stringResource(R.string.settings_ai_api_key),
                                if (key.isBlank()) "—" else "••••" + key.takeLast(4),
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { haptics.virtualKey(); viewModel.testAiConnection() },
                        enabled = testState !is SettingsViewModel.AiTestState.Testing,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        if (testState is SettingsViewModel.AiTestState.Testing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(10.dp))
                            Text(stringResource(R.string.settings_ai_testing))
                        } else {
                            Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.settings_ai_test))
                        }
                    }
                    val testError = (testState as? SettingsViewModel.AiTestState.Failed)?.message
                    AnimatedVisibility(visible = testError != null) {
                        Text(
                            text = testError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > AI_STEP_PROVIDER) {
                    OutlinedButton(
                        onClick = {
                            haptics.virtualKey()
                            step -= 1
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) {
                        Text(stringResource(R.string.settings_ai_back))
                    }
                }
                if (step < AI_STEP_VERIFY) {
                    Button(
                        onClick = { goNext() },
                        enabled = when (step) {
                            AI_STEP_ACCESS -> key.isNotBlank() && (!provider.needsBaseUrl || baseUrl.isNotBlank())
                            AI_STEP_MODEL -> model.isNotBlank()
                            else -> true
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) {
                        Text(stringResource(R.string.settings_ai_next), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            haptics.virtualKey()
                            viewModel.setAiModel(provider, model)
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_ai_key_saved),
                                Toast.LENGTH_SHORT,
                            ).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) {
                        Text(stringResource(R.string.settings_ai_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(AI_STEP_COUNT) { index ->
            val active = index <= step
            val fraction by animateFloatAsState(
                targetValue = if (active) 1f else 0f,
                label = "step",
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
            ) {}
        }
    }
}

@Composable
private fun ProviderOption(
    provider: AiProvider,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Cloud,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = aiProviderLabel(provider),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        when (provider) {
                            AiProvider.GEMINI -> R.string.settings_ai_provider_gemini_desc
                            AiProvider.OPENROUTER -> R.string.settings_ai_provider_openrouter_desc
                            AiProvider.OPENAI -> R.string.settings_ai_provider_openai_desc
                            AiProvider.CUSTOM -> R.string.settings_ai_provider_custom_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ModelOption(
    model: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = model,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}