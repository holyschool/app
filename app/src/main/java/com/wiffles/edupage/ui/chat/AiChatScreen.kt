@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.wiffles.edupage.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiffles.edupage.R
import com.wiffles.edupage.data.ChatAttachment
import com.wiffles.edupage.data.ChatMessage
import com.wiffles.edupage.ui.util.rememberAppHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val messages by viewModel.messages.collectAsState()
    val thinking by viewModel.thinking.collectAsState()
    val pendingAttachments by viewModel.attachments.collectAsState()
    val attachError by viewModel.attachError.collectAsState()
    var input by remember { mutableStateOf("") }
    val haptics = rememberAppHaptics()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            haptics.virtualKey()
            viewModel.attachUri(uri)
        }
    }

    LaunchedEffect(attachError) {
        attachError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearAttachError()
        }
    }

    LaunchedEffect(messages.size, thinking) {
        val target = messages.size + if (thinking) 1 else 0
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    fun send(text: String = input) {
        if (text.isBlank() && pendingAttachments.isEmpty()) return
        haptics.virtualKey()
        viewModel.send(text)
        input = ""
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Psychology,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.chat_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = stringResource(R.string.chat_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { haptics.virtualKey(); onBack() }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    actions = {
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { haptics.virtualKey(); viewModel.clear() }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.chat_clear),
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            },
            bottomBar = {
                ChatComposer(
                    value = input,
                    onValueChange = { input = it },
                    onSend = { send() },
                    onAttachFile = { picker.launch("*/*") },
                    onAttachPhoto = { picker.launch("image/*") },
                    attachmentsEnabled = viewModel.attachmentsEnabled(),
                    enabled = !thinking,
                    pendingAttachments = pendingAttachments,
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (messages.isEmpty() && !thinking) {
                    ChatEmptyState(onSuggestion = { send(it) })
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onOpenAttachment = { openChatAttachment(context, it) },
                                onCopy = { copyToClipboard(context, it) },
                                onShare = { shareMessage(context, it) },
                            )
                        }
                        if (thinking) {
                            item(key = "thinking") { ThinkingBubble() }
                        }
                        item(key = "disclaimer") {
                            Text(
                                text = stringResource(R.string.chat_disclaimer),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(onSuggestion: (String) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.chat_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        listOf(
            R.string.chat_suggestion_1,
            R.string.chat_suggestion_2,
            R.string.chat_suggestion_3,
        ).forEach { res ->
            Surface(
                onClick = { onSuggestion(context.getString(res)) },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onOpenAttachment: (ChatAttachment) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val isUser = message.role == "user"
    var menuOpen by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        if (!isUser) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 320.dp),
        ) {
            Surface(
                color = when {
                    isUser -> MaterialTheme.colorScheme.primary
                    message.error -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (isUser) 22.dp else 6.dp,
                    bottomEnd = if (isUser) 6.dp else 22.dp,
                ),
                modifier = Modifier
                    .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { menuOpen = true },
                    ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (!isUser && !message.error) {
                        MarkdownMessage(
                            text = message.content,
                            color = MaterialTheme.colorScheme.onSurface,
                            mathColor = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isUser -> MaterialTheme.colorScheme.onPrimary
                                message.error -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }

                    if (message.attachments.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        MessageAttachmentPreviews(
                            attachments = message.attachments,
                            isUser = isUser,
                            onOpen = { onOpenAttachment(it) },
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_copy)) },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = { menuOpen = false; onCopy(message.content) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_share)) },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = { menuOpen = false; onShare(message.content) },
                )
            }
        }
    }
}

@Composable
private fun MessageAttachmentPreviews(
    attachments: List<ChatAttachment>,
    isUser: Boolean,
    onOpen: (ChatAttachment) -> Unit,
) {
    val container = if (isUser) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val accent = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        attachments.forEach { attachment ->
            if (attachment.isImage()) {
                ImageAttachmentPreview(
                    attachment = attachment,
                    modifier = Modifier.size(132.dp),
                    onClick = { onOpen(attachment) },
                )
            } else {
                FileAttachmentChip(
                    attachment = attachment,
                    container = container,
                    content = content,
                    accent = accent,
                    onClick = { onOpen(attachment) },
                )
            }
        }
    }
}

@Composable
private fun ImageAttachmentPreview(
    attachment: ChatAttachment,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val bitmap = rememberImageThumbnail(attachment.localPath, targetPx = 512)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = attachment.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FileAttachmentChip(
    attachment: ChatAttachment,
    container: Color,
    content: Color,
    accent: Color,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = container,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = if (trailing == null) 12.dp else 4.dp, top = 8.dp, bottom = 8.dp)
                .widthIn(max = 220.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Description,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    maxLines = 1,
                )
                if (attachment.sizeBytes > 0) {
                    Text(
                        text = formatBytes(attachment.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = content.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(2.dp))
                trailing()
            }
        }
    }
}

private fun ChatAttachment.isImage(): Boolean = mimeType.startsWith("image/")

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.0f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

/** Decodes a small, downsampled thumbnail from a local image file. */
@Composable
private fun rememberImageThumbnail(path: String, targetPx: Int): ImageBitmap? =
    produceState<ImageBitmap?>(initialValue = null, key1 = path, key2 = targetPx) {
        value = withContext(Dispatchers.IO) { decodeThumbnail(path, targetPx) }
    }.value

private fun decodeThumbnail(path: String, target: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > target || bounds.outHeight / sample > target) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return runCatching { BitmapFactory.decodeFile(path, options)?.asImageBitmap() }.getOrNull()
}

@Composable
private fun ThinkingBubble() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(30.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 6.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.chat_thinking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The merged chat composer: a single rounded container that holds the attachment
 * previews, the attach actions, the text field and the send button. Window insets are
 * applied here so the bar keeps comfortable space above the navigation bar / keyboard.
 */
@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit,
    onAttachPhoto: () -> Unit,
    attachmentsEnabled: Boolean,
    enabled: Boolean,
    pendingAttachments: List<ChatAttachment>,
    onRemoveAttachment: (String) -> Unit,
) {
    val canSend = enabled && (value.isNotBlank() || pendingAttachments.isNotEmpty())
    val scale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.92f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "sendScale",
    )

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 12.dp)
                .padding(top = 10.dp, bottom = 16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    AnimatedVisibility(
                        visible = pendingAttachments.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                    ) {
                        PendingAttachmentPreviews(
                            attachments = pendingAttachments,
                            onRemove = onRemoveAttachment,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ComposerAttachActions(
                            enabled = enabled && attachmentsEnabled,
                            onAttachFile = onAttachFile,
                            onAttachPhoto = onAttachPhoto,
                        )

                        TextField(
                            value = value,
                            onValueChange = onValueChange,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.chat_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            enabled = enabled,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f),
                        )

                        FilledIconButton(
                            onClick = onSend,
                            enabled = canSend,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.chat_send),
                                modifier = Modifier.scale(scale),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Compact, connected attach actions (file + photo) that live inside the composer. */
@Composable
private fun ComposerAttachActions(
    enabled: Boolean,
    onAttachFile: () -> Unit,
    onAttachPhoto: () -> Unit,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onAttachFile,
            enabled = enabled,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Rounded.AttachFile,
                contentDescription = stringResource(R.string.chat_attach),
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        VerticalDivider(
            modifier = Modifier.height(20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        IconButton(
            onClick = onAttachPhoto,
            enabled = enabled,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Rounded.Image,
                contentDescription = stringResource(R.string.chat_attach_photo),
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Attachment previews shown inside the top of the composer, each removable. */
@Composable
private fun PendingAttachmentPreviews(
    attachments: List<ChatAttachment>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        attachments.forEach { attachment ->
            if (attachment.isImage()) {
                Box(modifier = Modifier.size(72.dp)) {
                    ImageAttachmentPreview(
                        attachment = attachment,
                        modifier = Modifier.size(72.dp),
                    )
                    RemoveBadge(
                        onClick = { onRemove(attachment.id) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            } else {
                FileAttachmentChip(
                    attachment = attachment,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = MaterialTheme.colorScheme.onSurface,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = {},
                    trailing = { RemoveBadge(onClick = { onRemove(attachment.id) }) },
                )
            }
        }
    }
}

@Composable
private fun RemoveBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 1.dp,
        modifier = modifier.size(24.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.chat_attachment_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun openChatAttachment(context: Context, attachment: ChatAttachment) {
    try {
        val file = java.io.File(attachment.localPath)
        if (!file.exists()) {
            Toast.makeText(context, R.string.attachment_download_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, R.string.attachment_no_app, Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("chat", text))
    Toast.makeText(context, R.string.chat_copied, Toast.LENGTH_SHORT).show()
}

private fun shareMessage(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.chat_share_title)),
    )
}