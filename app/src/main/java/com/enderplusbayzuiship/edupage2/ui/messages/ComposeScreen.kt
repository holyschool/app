package com.enderplusbayzuiship.edupage2.ui.messages

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.EduCloudFile
import com.edupage.api.model.people.EduAccount
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    recipientsState: RecipientsState,
    isSending: Boolean,
    sendError: String?,
    onDismiss: () -> Unit,
    onSend: (List<EduAccount>, String, Boolean, List<EduCloudFile>) -> Unit,
    onPollSend: (List<EduAccount>, String, List<String>, Boolean, Boolean) -> Unit,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isFullScreen by remember { mutableStateOf(false) }
    var isPollMode by remember { mutableStateOf(false) }
    var isImportant by remember { mutableStateOf(false) }
    var bodyText by remember { mutableStateOf(TextFieldValue("")) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedRecipients = remember { mutableStateListOf<EduAccount>() }
    var attachments = remember { mutableStateListOf<EduCloudFile>() }
    var pollAnswers = remember { mutableStateListOf<String>() }
    var pollQuestion by remember { mutableStateOf("") }
    var pollAnonymous by remember { mutableStateOf(false) }
    var pollSingleChoice by remember { mutableStateOf(false) }

    val filteredRecipients = remember(recipientsState, searchQuery) {
        derivedStateOf {
            val all = (recipientsState as? RecipientsState.Ready)?.recipients ?: emptyList()
            if (searchQuery.isBlank()) all
            else all.filter { it.name?.contains(searchQuery, ignoreCase = true) == true }
        }
    }.value

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val file = uriToFile(context, contentResolver, uri)
                        if (file != null) {
                            attachments.add(viewModel.uploadFile(file))
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        },
    )

    fun doSend() {
        if (selectedRecipients.isEmpty()) return
        if (isPollMode) {
            if (pollQuestion.isBlank() || pollAnswers.all { it.isBlank() }) return
            onPollSend(
                selectedRecipients.toList(), pollQuestion,
                pollAnswers.filter { it.isNotBlank() }, pollAnonymous, pollSingleChoice,
            )
        } else {
            if (bodyText.text.isBlank()) return
            onSend(selectedRecipients.toList(), bodyText.text, isImportant, attachments)
        }
        onDismiss()
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            FullScreenContent(
                isPollMode = isPollMode,
                onTogglePollMode = { haptics.virtualKey(); isPollMode = !isPollMode },
                isImportant = isImportant,
                onToggleImportant = { haptics.virtualKey(); isImportant = !isImportant },
                bodyText = bodyText,
                onBodyTextChange = { bodyText = it },
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                recipientsState = recipientsState,
                filteredRecipients = filteredRecipients,
                selectedRecipients = selectedRecipients,
                onToggleRecipient = { haptics.virtualKey(); if (it in selectedRecipients) selectedRecipients.remove(it) else selectedRecipients.add(it) },
                attachments = attachments,
                onPickAttachment = { attachmentLauncher.launch(arrayOf("*/*")) },
                onRemoveAttachment = { attachments.removeAt(it) },
                pollQuestion = pollQuestion,
                onPollQuestionChange = { pollQuestion = it },
                pollAnswers = pollAnswers,
                onPollAnswersChange = { pollAnswers.clear(); pollAnswers.addAll(it) },
                onAddPollAnswer = { haptics.virtualKey(); pollAnswers.add("") },
                onRemovePollAnswer = { pollAnswers.removeAt(it) },
                pollAnonymous = pollAnonymous,
                onTogglePollAnonymous = { haptics.virtualKey(); pollAnonymous = !pollAnonymous },
                pollSingleChoice = pollSingleChoice,
                onTogglePollSingleChoice = { haptics.virtualKey(); pollSingleChoice = !pollSingleChoice },
                isSending = isSending,
                onSend = { haptics.click(); doSend() },
                onExitFullScreen = { haptics.virtualKey(); isFullScreen = false },
            )
        }
    } else {
        AppBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
        ) {
            SheetContent(
                isPollMode = isPollMode,
                onTogglePollMode = { haptics.virtualKey(); isPollMode = !isPollMode },
                isImportant = isImportant,
                onToggleImportant = { haptics.virtualKey(); isImportant = !isImportant },
                bodyText = bodyText,
                onBodyTextChange = { bodyText = it },
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                recipientsState = recipientsState,
                filteredRecipients = filteredRecipients,
                selectedRecipients = selectedRecipients,
                onToggleRecipient = { haptics.virtualKey(); if (it in selectedRecipients) selectedRecipients.remove(it) else selectedRecipients.add(it) },
                attachments = attachments,
                onPickAttachment = { attachmentLauncher.launch(arrayOf("*/*")) },
                onRemoveAttachment = { attachments.removeAt(it) },
                pollQuestion = pollQuestion,
                onPollQuestionChange = { pollQuestion = it },
                pollAnswers = pollAnswers,
                onPollAnswersChange = { pollAnswers.clear(); pollAnswers.addAll(it) },
                onAddPollAnswer = { haptics.virtualKey(); pollAnswers.add("") },
                onRemovePollAnswer = { pollAnswers.removeAt(it) },
                pollAnonymous = pollAnonymous,
                onTogglePollAnonymous = { haptics.virtualKey(); pollAnonymous = !pollAnonymous },
                pollSingleChoice = pollSingleChoice,
                onTogglePollSingleChoice = { haptics.virtualKey(); pollSingleChoice = !pollSingleChoice },
                isSending = isSending,
                sendError = sendError,
                onSend = { haptics.click(); doSend() },
                onExpand = { haptics.virtualKey(); isFullScreen = true },
            )
        }
    }
}

@Composable
private fun SheetContent(
    isPollMode: Boolean,
    onTogglePollMode: () -> Unit,
    isImportant: Boolean,
    onToggleImportant: () -> Unit,
    bodyText: TextFieldValue,
    onBodyTextChange: (TextFieldValue) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    recipientsState: RecipientsState,
    filteredRecipients: List<EduAccount>,
    selectedRecipients: List<EduAccount>,
    onToggleRecipient: (EduAccount) -> Unit,
    attachments: MutableList<EduCloudFile>,
    onPickAttachment: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    pollQuestion: String,
    onPollQuestionChange: (String) -> Unit,
    pollAnswers: MutableList<String>,
    onPollAnswersChange: (MutableList<String>) -> Unit,
    onAddPollAnswer: () -> Unit,
    onRemovePollAnswer: (Int) -> Unit,
    pollAnonymous: Boolean,
    onTogglePollAnonymous: () -> Unit,
    pollSingleChoice: Boolean,
    onTogglePollSingleChoice: () -> Unit,
    isSending: Boolean,
    sendError: String?,
    onSend: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isPollMode) stringResource(R.string.messages_poll)
                else stringResource(R.string.messages_compose),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onPickAttachment, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = stringResource(R.string.messages_attachment_pick),
                    tint = if (attachments.isNotEmpty()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onExpand, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.messages_fullscreen),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = !isPollMode,
                onClick = { if (isPollMode) onTogglePollMode() },
                label = { Text(stringResource(R.string.messages_compose)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
            FilterChip(
                selected = isPollMode,
                onClick = { if (!isPollMode) onTogglePollMode() },
                label = { Text(stringResource(R.string.messages_poll)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.HowToReg,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
            Spacer(Modifier.weight(1f))
            FilterChip(
                selected = isImportant && !isPollMode,
                enabled = !isPollMode,
                onClick = onToggleImportant,
                label = { Text(stringResource(R.string.messages_important)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            )
        }

        Spacer(Modifier.height(12.dp))

        SectionLabel(
            text = if (selectedRecipients.isNotEmpty()) {
                stringResource(R.string.messages_recipient_hint) + " (${selectedRecipients.size})"
            } else {
                stringResource(R.string.messages_recipient_hint)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            startPadding = 4.dp,
        )

        if (selectedRecipients.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                selectedRecipients.forEach { recipient ->
                    val name = recipient.name ?: return@forEach
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            IconButton(
                                onClick = { onToggleRecipient(recipient) },
                                modifier = Modifier.size(18.dp),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        RecipientPicker(
            searchQuery = searchQuery,
            onSearchChange = onSearchChange,
            recipientsState = recipientsState,
            filteredRecipients = filteredRecipients,
            selectedRecipients = selectedRecipients,
            onToggleRecipient = onToggleRecipient,
        )

        Spacer(Modifier.height(8.dp))

        if (!isPollMode) {
            OutlinedTextField(
                value = bodyText,
                onValueChange = onBodyTextChange,
                placeholder = { Text(stringResource(R.string.messages_message_body_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                minLines = 2,
                maxLines = 6,
            )
        } else {
            PollForm(
                question = pollQuestion,
                onQuestionChange = onPollQuestionChange,
                answers = pollAnswers,
                onAnswersChange = onPollAnswersChange,
                onAddAnswer = onAddPollAnswer,
                onRemoveAnswer = onRemovePollAnswer,
                anonymous = pollAnonymous,
                onAnonymousChange = onTogglePollAnonymous,
                singleChoice = pollSingleChoice,
                onSingleChoiceChange = onTogglePollSingleChoice,
            )
        }

        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                attachments.forEachIndexed { index, file ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(file.fileName, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.width(100.dp))
                            IconButton(onClick = { onRemoveAttachment(index) }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        if (sendError != null) {
            Spacer(Modifier.height(8.dp))
            Text(sendError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = !isSending && selectedRecipients.isNotEmpty(),
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.messages_sending),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = stringResource(R.string.messages_send),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenContent(
    isPollMode: Boolean,
    onTogglePollMode: () -> Unit,
    isImportant: Boolean,
    onToggleImportant: () -> Unit,
    bodyText: TextFieldValue,
    onBodyTextChange: (TextFieldValue) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    recipientsState: RecipientsState,
    filteredRecipients: List<EduAccount>,
    selectedRecipients: List<EduAccount>,
    onToggleRecipient: (EduAccount) -> Unit,
    attachments: MutableList<EduCloudFile>,
    onPickAttachment: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    pollQuestion: String,
    onPollQuestionChange: (String) -> Unit,
    pollAnswers: MutableList<String>,
    onPollAnswersChange: (MutableList<String>) -> Unit,
    onAddPollAnswer: () -> Unit,
    onRemovePollAnswer: (Int) -> Unit,
    pollAnonymous: Boolean,
    onTogglePollAnonymous: () -> Unit,
    pollSingleChoice: Boolean,
    onTogglePollSingleChoice: () -> Unit,
    isSending: Boolean,
    onSend: () -> Unit,
    onExitFullScreen: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isPollMode) stringResource(R.string.messages_poll)
                        else stringResource(R.string.messages_compose),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onExitFullScreen) {
                        Icon(Icons.Filled.FullscreenExit, contentDescription = null)
                    }
                },
                actions = {
                    if (!isPollMode) {
                        IconButton(onClick = onToggleImportant) {
                            Icon(
                                Icons.Filled.PriorityHigh,
                                contentDescription = stringResource(R.string.messages_important),
                                tint = if (isImportant) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = onTogglePollMode) {
                        Icon(
                            imageVector = if (isPollMode) Icons.Default.Email else Icons.Default.HowToReg,
                            contentDescription = if (isPollMode) stringResource(R.string.messages_compose) else stringResource(R.string.messages_poll),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.imePadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Button(
                    onClick = onSend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isSending && selectedRecipients.isNotEmpty(),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.messages_sending),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.messages_send),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionLabel(
                text = stringResource(R.string.messages_recipient_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RecipientPicker(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                recipientsState = recipientsState,
                filteredRecipients = filteredRecipients,
                selectedRecipients = selectedRecipients,
                onToggleRecipient = onToggleRecipient,
            )
            Spacer(Modifier.height(12.dp))

            if (!isPollMode) {
                FormattingBar(value = bodyText, onValueChange = onBodyTextChange)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bodyText,
                    onValueChange = onBodyTextChange,
                    placeholder = { Text(stringResource(R.string.messages_message_body_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    minLines = 6,
                    maxLines = 20,
                )
            } else {
                PollForm(
                    question = pollQuestion,
                    onQuestionChange = onPollQuestionChange,
                    answers = pollAnswers,
                    onAnswersChange = onPollAnswersChange,
                    onAddAnswer = onAddPollAnswer,
                    onRemoveAnswer = onRemovePollAnswer,
                    anonymous = pollAnonymous,
                    onAnonymousChange = onTogglePollAnonymous,
                    singleChoice = pollSingleChoice,
                    onSingleChoiceChange = onTogglePollSingleChoice,
                )
            }

            if (attachments.isNotEmpty()) {
                SectionLabel(
                    text = stringResource(R.string.messages_attachment_count, attachments.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    startPadding = 16.dp,
                    topPadding = 16.dp,
                )
                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    attachments.forEachIndexed { index, file ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    file.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onRemoveAttachment(index) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                            ),
                        )
                    }
                }
            }

            IconButton(onClick = onPickAttachment, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AttachFile, contentDescription = stringResource(R.string.messages_attachment_pick), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.messages_attachment_pick), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RecipientPicker(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    recipientsState: RecipientsState,
    filteredRecipients: List<EduAccount>,
    selectedRecipients: List<EduAccount>,
    onToggleRecipient: (EduAccount) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.messages_search_recipients)) },
            label = { Text(stringResource(R.string.messages_recipient_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        when (recipientsState) {
            is RecipientsState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            is RecipientsState.Error -> {
                Text(
                    text = (recipientsState as RecipientsState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            is RecipientsState.Ready, is RecipientsState.Idle -> {
                if (filteredRecipients.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(
                            items = filteredRecipients,
                            key = { it.getId() },
                        ) { account ->
                            PersonCard(
                                account = account,
                                selected = account in selectedRecipients,
                                onToggle = { onToggleRecipient(account) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonCard(
    account: EduAccount,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val name = account.name
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.width(88.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FormattingBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val start = value.selection.start
    val end = value.selection.end

    fun wrap(marker: String) {
        if (start == end) {
            val newText = value.text.substring(0, start) + marker + value.text.substring(start)
            val newCursor = start + marker.length
            onValueChange(value.copy(text = newText, selection = TextRange(newCursor, newCursor)))
        } else {
            val selected = value.text.substring(start, end)
            val newText = value.text.substring(0, start) + marker + selected + marker + value.text.substring(end)
            val newCursor = end + marker.length * 2
            onValueChange(value.copy(text = newText, selection = TextRange(newCursor, newCursor)))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FormatChip(label = "I", onClick = { wrap("*") })
        FormatChip(label = "U", onClick = { wrap("<u>") })
    }
}

@Composable
private fun FormatChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollForm(
    question: String,
    onQuestionChange: (String) -> Unit,
    answers: MutableList<String>,
    onAnswersChange: (MutableList<String>) -> Unit,
    onAddAnswer: () -> Unit,
    onRemoveAnswer: (Int) -> Unit,
    anonymous: Boolean,
    onAnonymousChange: () -> Unit,
    singleChoice: Boolean,
    onSingleChoiceChange: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            placeholder = { Text(stringResource(R.string.messages_poll_question)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        answers.forEachIndexed { index, answer ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { new ->
                        val updated = ArrayList(answers)
                        updated[index] = new
                        onAnswersChange(updated)
                    },
                    placeholder = { Text(stringResource(R.string.messages_poll_option_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                )
                if (answers.size > 2) {
                    IconButton(onClick = { onRemoveAnswer(index) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onAddAnswer) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.messages_poll_add_answer))
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterChip(
                selected = anonymous,
                onClick = onAnonymousChange,
                label = { Text(stringResource(R.string.messages_poll_anonymous), style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
            FilterChip(
                selected = singleChoice,
                onClick = onSingleChoiceChange,
                label = { Text(stringResource(R.string.messages_poll_single_choice), style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

private fun uriToFile(context: Context, contentResolver: ContentResolver, uri: Uri): File? {
    return try {
        val fileName = getFileName(contentResolver, uri) ?: "attachment_${System.currentTimeMillis()}"
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, fileName)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (_: Exception) {
        null
    }
}

private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            name = it.getString(idx)
        }
    }
    return name
}

