package com.enderplusbayzuiship.edupage2.ui.messages

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.people.EduAccount
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private data class TypeInfo(val icon: ImageVector, val labelRes: Int)

private fun typeInfoFor(type: String?): TypeInfo = when (type?.lowercase()) {
    "sprava"                                    -> TypeInfo(Icons.Default.Email,                      R.string.messages_type_message)
    "hw", "homework", "h_homework"              -> TypeInfo(Icons.AutoMirrored.Filled.Assignment,     R.string.messages_type_homework)
    "test", "bexam", "oexam", "sexam",
    "rexam", "pexam", "testing",
    "testpridelenie"                            -> TypeInfo(Icons.Default.Quiz,                       R.string.messages_type_test)
    "oznam", "news"                             -> TypeInfo(Icons.Default.Campaign,                   R.string.messages_type_announcement)
    "znamka", "znamkydoc", "h_znamky",
    "settings"                                  -> TypeInfo(Icons.Default.Star,                       R.string.messages_type_grade)
    "absent", "student_absent",
    "ospravedlnenka"                            -> TypeInfo(Icons.Default.EventBusy,                  R.string.messages_type_absence)
    "event", "schoolevent", "culture",
    "excursion", "trip", "parentsevening",
    "meeting", "bmeeting",
    "signin", "confirmation"                    -> TypeInfo(Icons.Default.Event,                      R.string.messages_type_event)
    "payments", "h_financie"                    -> TypeInfo(Icons.Default.AccountBalanceWallet,       R.string.messages_type_payment)
    "h_album"                                   -> TypeInfo(Icons.Default.Photo,                      R.string.messages_type_album)
    "vcelicka"                                  -> TypeInfo(Icons.Default.HowToReg,                   R.string.messages_type_behaviour)
    else                                        -> TypeInfo(Icons.Default.Notifications,              R.string.messages_type_notification)
}

private data class FilterCategory(val labelRes: Int, val icon: ImageVector)

private val filterCategories: List<FilterCategory> = listOf(
    FilterCategory(R.string.messages_type_message,      Icons.Default.Email),
    FilterCategory(R.string.messages_type_homework,     Icons.AutoMirrored.Filled.Assignment),
    FilterCategory(R.string.messages_type_test,         Icons.Default.Quiz),
    FilterCategory(R.string.messages_type_announcement, Icons.Default.Campaign),
    FilterCategory(R.string.messages_type_grade,        Icons.Default.Star),
    FilterCategory(R.string.messages_type_absence,      Icons.Default.EventBusy),
    FilterCategory(R.string.messages_type_event,        Icons.Default.Event),
    FilterCategory(R.string.messages_type_payment,      Icons.Default.AccountBalanceWallet),
    FilterCategory(R.string.messages_type_behaviour,    Icons.Default.HowToReg),
    FilterCategory(R.string.messages_type_notification, Icons.Default.Notifications),
)

private fun String.unescapeHtml(): String = this
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")
    .replace("&nbsp;", "\u00A0")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace(Regex("<[^>]*>"), "")
    .trim()

private fun extractSubjectFromGradeText(text: String?): String? {
    if (text == null) return null

    val dashIdx = text.indexOf(" - ")
    val colonIdx = text.lastIndexOf(":")
    return if (dashIdx >= 0 && colonIdx > dashIdx) {
        text.substring(dashIdx + 3, colonIdx).trim()
    } else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    bottomPadding: PaddingValues,
    viewModel: MessagesViewModel = hiltViewModel(),
    onGradeClick: (subjectHint: String) -> Unit = {},
) {
    val uiState         by viewModel.uiState.collectAsState()
    val recipientsState by viewModel.recipientsState.collectAsState()
    val haptics         = rememberAppHaptics()
    val scope           = rememberCoroutineScope()
    val scrollBehavior  = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val refreshState    = rememberPullToRefreshState()

    var showCompose    by remember { mutableStateOf(false) }
    var detailEvent    by remember { mutableStateOf<TimelineEvent?>(null) }
    var selectedFilter by remember { mutableStateOf<Int?>(null) }
    var showFilterRow  by remember { mutableStateOf(false) }

    val isRefreshing = (uiState as? MessagesUiState.Success)?.isRefreshing == true

    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val hasUnread = remember(uiState) {
        val s = uiState as? MessagesUiState.Success ?: return@remember false
        s.items.any { it.timelineId !in s.seenIds }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.messages_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (hasUnread) {
                        FilledTonalIconButton(onClick = {
                            haptics.click()
                            viewModel.markAllSeen()
                        }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.messages_mark_all_read),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    if (uiState is MessagesUiState.Success) {
                        FilledTonalIconButton(
                            onClick = {
                                haptics.click()
                                showFilterRow = !showFilterRow
                            },
                            colors = if (selectedFilter != null)
                                androidx.compose.material3.IconButtonDefaults.filledIconButtonColors()
                            else
                                androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(),
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.messages_filter),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    FilledTonalIconButton(onClick = { haptics.click(); viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.messages_refresh),
                            modifier = Modifier.rotate(if (isRefreshing) rotation else 0f)
                        )
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
        floatingActionButton = {
            if (uiState is MessagesUiState.Success) {
                FloatingActionButton(
                    onClick = {
                        haptics.click()
                        viewModel.loadRecipients()
                        showCompose = true
                    },
                    modifier = Modifier.padding(bottom = bottomPadding.calculateBottomPadding()),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.messages_compose))
                }
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = refreshState,
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState is MessagesUiState.Success) {
                    AnimatedVisibility(
                        visible = showFilterRow,
                        enter = fadeIn(tween(150)) + androidx.compose.animation.expandVertically(tween(150)),
                        exit = fadeOut(tween(150)) + androidx.compose.animation.shrinkVertically(tween(150)),
                    ) {
                        FilterRow(
                            selected = selectedFilter,
                            onSelect = { filter ->
                                selectedFilter = filter
                                showFilterRow = false
                            },
                        )
                    }
                }

                when (val state = uiState) {
                    is MessagesUiState.Loading -> {
                        MessagesSkeleton(
                            bottomPadding = bottomPadding,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is MessagesUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { haptics.click(); viewModel.refresh() },
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text(stringResource(R.string.messages_retry))
                                }
                            }
                        }
                    }

                    is MessagesUiState.Success -> {
                        val filteredItems = if (selectedFilter == null) state.items
                        else state.items.filter { typeInfoFor(it.type).labelRes == selectedFilter }
                        if (filteredItems.isEmpty() && !state.canLoadMore) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.messages_no_messages),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            MessagesList(
                                state = state.copy(items = filteredItems),
                                bottomPadding = bottomPadding,
                                onLoadMore = { viewModel.loadMore() },
                                onItemClick = { event ->
                                    haptics.tick()
                                    viewModel.markMessageSeen(event.timelineId)
                                    val type = event.type?.lowercase()
                                    if (type == "znamka" || type == "znamkydoc" || type == "h_znamky" || type == "settings") {
                                        val subject = extractSubjectFromGradeText(event.text)
                                        if (subject != null) {
                                            onGradeClick(subject)
                                        } else {
                                            onGradeClick("")
                                        }
                                    } else {
                                        detailEvent = event
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    detailEvent?.let { event ->
        DetailSheet(
            event = event,
            onDismiss = { detailEvent = null },
        )
    }

    if (showCompose) {
        ComposeSheet(
            recipientsState = recipientsState,
            isSending = (uiState as? MessagesUiState.Success)?.isSending == true,
            sendError = (uiState as? MessagesUiState.Success)?.sendError,
            onDismiss = { showCompose = false; viewModel.clearSendError() },
            onSend = { recipients, body ->
                viewModel.sendMessage(recipients, body)
                showCompose = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        item(key = "filter_all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.messages_filter_all)) },
            )
        }
        items(filterCategories, key = { it.labelRes }) { cat ->
            FilterChip(
                selected = selected == cat.labelRes,
                onClick = { onSelect(if (selected == cat.labelRes) null else cat.labelRes) },
                label = { Text(stringResource(cat.labelRes)) },
                leadingIcon = {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

private sealed interface MessageListItem {
    data class Event(val event: TimelineEvent, val isUnread: Boolean) : MessageListItem
    data object NewDivider : MessageListItem
}

@Composable
private fun MessagesList(
    state: MessagesUiState.Success,
    bottomPadding: PaddingValues,
    onLoadMore: () -> Unit,
    onItemClick: (TimelineEvent) -> Unit,
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val totalItems = layoutInfo.totalItemsCount
            lastVisible >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }
            .collect { nearEnd ->
                if (nearEnd && state.canLoadMore && !state.isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    val listItems = remember(state.items, state.seenIds) {
        val seenIds = state.seenIds
        val firstReadIdx = state.items.indexOfFirst { it.timelineId in seenIds }
        val items = mutableListOf<MessageListItem>()
        state.items.forEachIndexed { index, event ->
            if (firstReadIdx > 0 && index == firstReadIdx) {
                items.add(MessageListItem.NewDivider)
            }
            items.add(MessageListItem.Event(event, event.timelineId !in seenIds))
        }
        items.toList()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp,
            bottom = 16.dp + bottomPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(listItems, key = { when (it) {
            is MessageListItem.Event -> "evt_${it.event.timelineId}"
            is MessageListItem.NewDivider -> "new_divider"
        } }) { item ->
            when (item) {
                is MessageListItem.Event -> MessageItem(
                    event = item.event,
                    isUnread = item.isUnread,
                    onClick = { onItemClick(item.event) }
                )
                is MessageListItem.NewDivider -> NewMessagesDivider()
            }
        }

        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
            }
        }
    }
}

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val fullTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
private fun MessageItem(event: TimelineEvent, isUnread: Boolean, onClick: () -> Unit) {
    val typeInfo  = typeInfoFor(event.type)
    val typeLabel = stringResource(typeInfo.labelRes)
    val timeText  = event.timestamp?.let { ts ->
        val today = java.time.LocalDate.now()
        if (ts.toLocalDate().year == today.year) ts.format(timeFmt) else ts.format(fullTimeFmt)
    }

    val displayText = event.text?.unescapeHtml()
    val title = displayText?.takeIf { it.isNotBlank() }
        ?: event.title?.unescapeHtml()?.takeIf { it.isNotBlank() }
        ?: typeLabel

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeInfo.icon,
                        contentDescription = typeLabel,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isUnread) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val sender = event.authorName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.messages_unknown_sender)
                    Text(
                        text = sender,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    TypeChip(label = typeLabel)
                }
            }

            if (timeText != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NewMessagesDivider() {
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = primary.copy(alpha = 0.3f)
        )
        Surface(
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = primary.copy(alpha = 0.1f),
            contentColor = primary
        ) {
            Text(
                text = stringResource(R.string.messages_new_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = primary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun TypeChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

private val detailDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    event: TimelineEvent,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope      = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        DetailSheetContent(
            event = event,
            onClose = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
        )
    }
}

@Composable
private fun DetailSheetContent(
    event: TimelineEvent,
    onClose: () -> Unit,
) {
    val typeInfo  = typeInfoFor(event.type)
    val typeLabel = stringResource(typeInfo.labelRes)
    val displayText = event.text?.unescapeHtml()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeInfo.icon,
                        contentDescription = typeLabel,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.messages_detail_close))
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))

        val sender = event.authorName?.takeIf { it.isNotBlank() }
        if (sender != null) {
            DetailRow(label = stringResource(R.string.messages_detail_from), value = sender)
            Spacer(Modifier.height(10.dp))
        }

        val dateStr = event.timestamp?.format(detailDateFmt)
        if (dateStr != null) {
            DetailRow(label = stringResource(R.string.messages_detail_date), value = dateStr)
            Spacer(Modifier.height(10.dp))
        }

        val body = displayText?.takeIf { it.isNotBlank() }
            ?: event.title?.unescapeHtml()?.takeIf { it.isNotBlank() }
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeSheet(
    recipientsState: RecipientsState,
    isSending: Boolean,
    sendError: String?,
    onDismiss: () -> Unit,
    onSend: (List<EduAccount>, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
    ) {
        ComposeSheetContent(
            recipientsState = recipientsState,
            isSending = isSending,
            sendError = sendError,
            onSend = { recipients, body ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onSend(recipients, body)
                }
            },
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
        )
    }
}

@Composable
private fun ComposeSheetContent(
    recipientsState: RecipientsState,
    isSending: Boolean,
    sendError: String?,
    onSend: (List<EduAccount>, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedRecipients = remember { mutableStateListOf<EduAccount>() }
    var searchQuery by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val haptics = rememberAppHaptics()

    val filteredRecipients by remember(recipientsState, searchQuery) {
        derivedStateOf {
            val all = (recipientsState as? RecipientsState.Ready)?.recipients ?: emptyList()
            if (searchQuery.isBlank()) all
            else all.filter { it.name?.contains(searchQuery, ignoreCase = true) == true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.messages_compose),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (selectedRecipients.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                selectedRecipients.forEach { recipient ->
                    val name = recipient.name ?: return@forEach
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { haptics.tick(); selectedRecipients.remove(recipient) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.messages_search_recipients)) },
            label = { Text(stringResource(R.string.messages_recipient_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        Spacer(Modifier.height(6.dp))

        when (recipientsState) {
            is RecipientsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            is RecipientsState.Error -> {
                Text(
                    text = recipientsState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            is RecipientsState.Ready, is RecipientsState.Idle -> {
                if (filteredRecipients.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    ) {
                        filteredRecipients.take(10).forEachIndexed { index, account ->
                            val isSelected = account in selectedRecipients
                            val name = account.name ?: return@forEachIndexed
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.surface,
                                onClick = {
                                    haptics.tick()
                                    if (isSelected) selectedRecipients.remove(account)
                                    else selectedRecipients.add(account)
                                },
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                            if (index < filteredRecipients.take(10).lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = bodyText,
            onValueChange = { bodyText = it },
            placeholder = { Text(stringResource(R.string.messages_message_body_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(16.dp),
        )

        val displayError = validationError ?: sendError
        AnimatedVisibility(
            visible = displayError != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            if (displayError != null) {
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                haptics.click()
                if (selectedRecipients.isEmpty()) {
                    validationError = "Select at least one recipient"
                    return@Button
                }
                validationError = null
                onSend(selectedRecipients.toList(), bodyText)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            enabled = !isSending,
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.messages_sending))
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.messages_send))
            }
        }
    }
}

@Preview(name = "Messages – Light", showBackground = true)
@Preview(name = "Messages – Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessagesScreenPreview() {
    Edupage2Theme {
        MessagesScreen(bottomPadding = PaddingValues())
    }
}
