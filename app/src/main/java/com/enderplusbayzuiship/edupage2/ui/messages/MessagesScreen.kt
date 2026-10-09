package com.enderplusbayzuiship.edupage2.ui.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.PollAnswer
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.people.EduAccount
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.MessagesViewMode
import com.enderplusbayzuiship.edupage2.ui.core.cards.IconToggleItem
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelIcon
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelIcon
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate
import com.enderplusbayzuiship.edupage2.ui.modifiers.scrollMotionBlur
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
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
    FilterCategory(R.string.messages_type_album,        Icons.Default.Photo),
    FilterCategory(R.string.messages_type_notification, Icons.Default.Notifications),
)

private val CATEGORY_ORDER: List<Int> = listOf(
    R.string.messages_type_grade,
    R.string.messages_type_test,
    R.string.messages_type_homework,
    R.string.messages_type_message,
    R.string.messages_type_absence,
    R.string.messages_type_payment,
    R.string.messages_type_announcement,
    R.string.messages_type_event,
    R.string.messages_type_behaviour,
    R.string.messages_type_album,
    R.string.messages_type_notification,
)

private fun categoryInfo(labelRes: Int): FilterCategory =
    filterCategories.find { it.labelRes == labelRes }
        ?: FilterCategory(R.string.messages_type_notification, Icons.Default.Notifications)

private val HtmlTagRegex = Regex("<[^>]*>")

private fun String.unescapeHtml(): String = this
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")
    .replace("&nbsp;", "\u00A0")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace(HtmlTagRegex, "")
    .trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    bottomPadding: PaddingValues,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val uiState         by viewModel.uiState.collectAsState()
    val recipientsState by viewModel.recipientsState.collectAsState()
    val importedHomework by viewModel.importedHomework.collectAsState()
    val viewMode        by viewModel.viewModeFlow.collectAsState(initial = MessagesViewMode.ALL)
    val priorityMessages by viewModel.priorityFlow.collectAsState(initial = false)
    val newMessagesOnTop by viewModel.newOnTopFlow.collectAsState(initial = false)
    val haptics         = rememberAppHaptics()
    val scope           = rememberCoroutineScope()
    val refreshState    = rememberPullToRefreshState()
    val messageListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val importedText = if (importedHomework > 0) {
        stringResource(R.string.homework_imported_snackbar, importedHomework)
    } else {
        ""
    }

    LaunchedEffect(importedText) {
        if (importedText.isNotEmpty()) {
            snackbarHostState.showSnackbar(importedText)
            viewModel.consumeImportedHomework()
        }
    }

    val barHidden by remember {
        derivedStateOf {
            messageListState.firstVisibleItemIndex > 0 ||
                messageListState.firstVisibleItemScrollOffset > 64
        }
    }

    var showCompose    by remember { mutableStateOf(false) }
    var detailGroup    by remember { mutableStateOf<MessageGroup?>(null) }
    var viewerAttachment by remember { mutableStateOf<com.edupage.api.model.MessageAttachment?>(null) }
    var selectedFilter by remember { mutableStateOf<Int?>(null) }
    var unreadOnly     by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery    by remember { mutableStateOf("") }
    var showSearch     by remember { mutableStateOf(false) }
    var starredOnly    by remember { mutableStateOf(false) }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(200)
            .collect { debouncedQuery = it }
    }

    val isRefreshing = (uiState as? MessagesUiState.Success)?.isRefreshing == true

    val hasUnread = remember(uiState) {
        val s = uiState as? MessagesUiState.Success ?: return@remember false
        s.groups.any { g ->
            g.main.timelineId !in s.seenIds || g.replies.any { it.timelineId !in s.seenIds }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = !barHidden,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
            TopAppBar(
                title = {},
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
                        val hasActiveFilters = selectedFilter != null || unreadOnly || starredOnly
                        FilledTonalIconButton(
                            onClick = {
                                haptics.virtualKey()
                                showFilterSheet = true
                            },
                            colors = if (hasActiveFilters)
                                androidx.compose.material3.IconButtonDefaults.filledIconButtonColors()
                            else
                                androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(),
                        ) {
                            Box {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.messages_filter),
                                )
                                if (hasActiveFilters) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (uiState is MessagesUiState.Success) {
                        FilledTonalIconButton(
                            onClick = {
                                haptics.virtualKey()
                                showSearch = !showSearch
                                if (!showSearch) searchQuery = ""
                            },
                            colors = if (searchQuery.isNotBlank())
                                androidx.compose.material3.IconButtonDefaults.filledIconButtonColors()
                            else
                                androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(),
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.messages_search),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    FilledTonalIconButton(onClick = { haptics.click(); viewModel.refresh() }) {
                        if (isRefreshing) {
                            RotatingRefreshIcon()
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.messages_refresh),
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            }
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
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState is MessagesUiState.Success) {
                        val hasActiveFilters = selectedFilter != null || unreadOnly || starredOnly
                        AnimatedVisibility(
                            visible = hasActiveFilters,
                            enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                        ) {
                            ActiveFilterChips(
                                selectedFilter = selectedFilter,
                                unreadOnly = unreadOnly,
                                starredOnly = starredOnly,
                                onClearType = { selectedFilter = null },
                                onToggleUnread = { unreadOnly = !unreadOnly },
                                onToggleStarred = { starredOnly = !starredOnly },
                                onClearAll = {
                                    selectedFilter = null
                                    unreadOnly = false
                                    starredOnly = false
                                },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showSearch,
                        enter = fadeIn(tween(150)) + androidx.compose.animation.expandVertically(tween(150)),
                        exit = fadeOut(tween(150)) + androidx.compose.animation.shrinkVertically(tween(150)),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.messages_search_hint)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    if (uiState is MessagesUiState.Success) {
                        MessagesViewModeRow(
                            viewMode = viewMode,
                            onSelect = { viewModel.setViewMode(it) },
                        )
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = RoundedCornerShape(20.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.messages_retry),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }

                        is MessagesUiState.Success -> {
                            val searchGroups = remember(
                                state.groups,
                                state.seenIds,
                                selectedFilter,
                                starredOnly,
                                unreadOnly,
                                debouncedQuery,
                            ) {
                                val byFilter = if (selectedFilter == null) state.groups
                                else state.groups.filter { typeInfoFor(it.main.type).labelRes == selectedFilter }
                                val byStarred = if (starredOnly) byFilter.filter { it.main.isStarred } else byFilter
                                val byUnread = if (unreadOnly) {
                                    byStarred.filter { g ->
                                        g.main.timelineId !in state.seenIds ||
                                            g.replies.any { it.timelineId !in state.seenIds }
                                    }
                                } else {
                                    byStarred
                                }
                                val q = debouncedQuery.trim().lowercase()
                                if (q.isBlank()) {
                                    byUnread
                                } else {
                                    byUnread.filter { g ->
                                        (g.main.title?.lowercase()?.contains(q) == true) ||
                                            (g.main.text?.lowercase()?.contains(q) == true) ||
                                            (g.main.authorName?.lowercase()?.contains(q) == true) ||
                                            g.replies.any { r ->
                                                (r.title?.lowercase()?.contains(q) == true) ||
                                                    (r.text?.lowercase()?.contains(q) == true) ||
                                                    (r.authorName?.lowercase()?.contains(q) == true)
                                            }
                                    }
                                }
                            }
                            val showEmpty = searchGroups.isEmpty() && (debouncedQuery.isNotBlank() || unreadOnly || !state.canLoadMore)
                            if (showEmpty) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stringResource(
                                            when {
                                                searchQuery.isNotBlank() -> R.string.messages_no_results
                                                unreadOnly -> R.string.messages_no_unread
                                                else -> R.string.messages_no_messages
                                            }
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                MessagesList(
                                    state = state.copy(groups = searchGroups),
                                    bottomPadding = bottomPadding,
                                    selectedFilter = selectedFilter,
                                    viewMode = viewMode,
                                    priority = priorityMessages,
                                    newOnTop = newMessagesOnTop,
                                    listState = messageListState,
                                    onLoadMore = { viewModel.loadMore() },
                                    onItemClick = { group ->
                                        haptics.virtualKey()
                                        viewModel.markMessageSeen(group.main.timelineId)
                                        group.replies.forEach { viewModel.markMessageSeen(it.timelineId) }
                                        detailGroup = group
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detailGroup?.let { group ->
        DetailSheet(
            group = group,
            onDismiss = { detailGroup = null },
            onDelete = { timelineId ->
                viewModel.deleteMessage(timelineId)
                detailGroup = null
            },
            onReply = { replyBody ->
                viewModel.sendReply(replyBody, group.main.timelineId, group.main.authorId)
            },
            onStarToggle = { timelineId, starred -> viewModel.toggleMessageStarred(timelineId, starred) },
            onVote = { timelineId, answerIds -> viewModel.voteOnPoll(timelineId, answerIds) },
            onOpenAttachment = { attachment -> viewerAttachment = attachment },
            currentUserId = viewModel.getCurrentUserId(),
        )
    }

    viewerAttachment?.let { attachment ->
        com.enderplusbayzuiship.edupage2.ui.attachments.AttachmentViewerSheet(
            attachment = attachment,
            onDismiss = { viewerAttachment = null },
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            selectedFilter = selectedFilter,
            unreadOnly = unreadOnly,
            starredOnly = starredOnly,
            onSelectType = { selectedFilter = it },
            onToggleUnread = { unreadOnly = !unreadOnly },
            onToggleStarred = { starredOnly = !starredOnly },
            onClearAll = {
                selectedFilter = null
                unreadOnly = false
                starredOnly = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showCompose) {
        ComposeScreen(
            recipientsState = recipientsState,
            isSending = (uiState as? MessagesUiState.Success)?.isSending == true,
            sendError = (uiState as? MessagesUiState.Success)?.sendError,
            onDismiss = { showCompose = false; viewModel.clearSendError() },
            onSend = { recipients, body, important, files ->
                viewModel.sendMessage(recipients, body, important, files)
                showCompose = false
            },
            onPollSend = { recipients, question, answers, anonymous, singleChoice ->
                scope.launch {
                    viewModel.sendPoll(recipients, question, answers, anonymous, singleChoice)
                }
                showCompose = false
            },
        )
    }
}

@Composable
private fun RotatingRefreshIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = stringResource(R.string.messages_refresh),
        modifier = Modifier.rotate(rotation),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesViewModeRow(
    viewMode: MessagesViewMode,
    onSelect: (MessagesViewMode) -> Unit,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val options = listOf(
            MessagesViewMode.CATEGORIES to R.string.messages_view_categories,
            MessagesViewMode.ALL to R.string.messages_view_all,
        )
        options.forEach { (mode, labelRes) ->
            FilterChip(
                selected = viewMode == mode,
                onClick = {
                    haptics.virtualKey()
                    onSelect(mode)
                },
                label = { Text(stringResource(labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveFilterChips(
    selectedFilter: Int?,
    unreadOnly: Boolean,
    starredOnly: Boolean,
    onClearType: () -> Unit,
    onToggleUnread: () -> Unit,
    onToggleStarred: () -> Unit,
    onClearAll: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selectedFilter != null) {
            val info = categoryInfo(selectedFilter)
            item(key = "active_type") {
                FilterChip(
                    selected = true,
                    onClick = { haptics.virtualKey(); onClearType() },
                    label = { Text(stringResource(info.labelRes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = info.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
        if (unreadOnly) {
            item(key = "active_unread") {
                FilterChip(
                    selected = true,
                    onClick = { haptics.virtualKey(); onToggleUnread() },
                    label = { Text(stringResource(R.string.messages_new_label)) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
        if (starredOnly) {
            item(key = "active_starred") {
                FilterChip(
                    selected = true,
                    onClick = { haptics.virtualKey(); onToggleStarred() },
                    label = { Text(stringResource(R.string.messages_filter_starred)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
        item(key = "active_clear") {
            FilterChip(
                selected = false,
                onClick = { haptics.virtualKey(); onClearAll() },
                label = { Text(stringResource(R.string.messages_filter_all)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    selectedFilter: Int?,
    unreadOnly: Boolean,
    starredOnly: Boolean,
    onSelectType: (Int?) -> Unit,
    onToggleUnread: () -> Unit,
    onToggleStarred: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.messages_filter),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    haptics.virtualKey()
                    onClearAll()
                }) {
                    Text(stringResource(R.string.messages_filter_all))
                }
            }
            SectionLabel(
                text = stringResource(R.string.messages_type_notification),
                color = MaterialTheme.colorScheme.primary,
            )
            RoundedCardContainer {
                FilterTypeRow(
                    labelRes = R.string.messages_filter_all,
                    icon = Icons.Default.FilterList,
                    selected = selectedFilter == null,
                    onClick = {
                        haptics.virtualKey()
                        onSelectType(null)
                    },
                )
                filterCategories.forEach { cat ->
                    FilterTypeRow(
                        labelRes = cat.labelRes,
                        icon = cat.icon,
                        selected = selectedFilter == cat.labelRes,
                        onClick = {
                            haptics.virtualKey()
                            onSelectType(if (selectedFilter == cat.labelRes) null else cat.labelRes)
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            RoundedCardContainer {
                IconToggleItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.messages_new_label),
                    checked = unreadOnly,
                    onCheckedChange = { onToggleUnread() },
                )
                IconToggleItem(
                    icon = Icons.Filled.Star,
                    title = stringResource(R.string.messages_filter_starred),
                    checked = starredOnly,
                    onCheckedChange = { onToggleStarred() },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterTypeRow(
    labelRes: Int,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.virtualKey()
                onClick()
            },
        headlineContent = {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        leadingContent = {
            PastelIcon(icon = icon, key = stringResource(labelRes))
        },
        trailingContent = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    )
}

private sealed interface MessageListItem {
    data class Section(val labelRes: Int, val unreadCount: Int) : MessageListItem
    data class Group(val group: MessageGroup, val isUnread: Boolean) : MessageListItem
    data object NewDivider : MessageListItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessagesList(
    state: MessagesUiState.Success,
    bottomPadding: PaddingValues,
    selectedFilter: Int?,
    viewMode: MessagesViewMode,
    priority: Boolean,
    newOnTop: Boolean,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onItemClick: (MessageGroup) -> Unit,
) {
    val haptics = rememberAppHaptics()
    var collapsedSections by remember { mutableStateOf(setOf<Int>()) }

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

    val mixed = priority || newOnTop
    val listItems = remember(state.groups, state.seenIds, selectedFilter, viewMode, priority, newOnTop) {
        val seenIds = state.seenIds
        fun isUnread(group: MessageGroup) =
            group.main.timelineId !in seenIds || group.replies.any { it.timelineId !in seenIds }

        val items = mutableListOf<MessageListItem>()
        val showCategories = selectedFilter == null && viewMode == MessagesViewMode.CATEGORIES && !mixed
        if (showCategories) {
            val byCategory = state.groups.groupBy { typeInfoFor(it.main.type).labelRes }
            val ordered = byCategory.toList().sortedBy { (labelRes, _) ->
                val idx = CATEGORY_ORDER.indexOf(labelRes)
                if (idx < 0) CATEGORY_ORDER.size else idx
            }
            ordered.forEach { (labelRes, groups) ->
                items.add(MessageListItem.Section(labelRes = labelRes, unreadCount = groups.count(::isUnread)))
                groups.forEach { items.add(MessageListItem.Group(it, isUnread(it))) }
            }
        } else {
            var ordered = state.groups
            if (priority) {
                ordered = ordered.sortedByDescending { it.main.isImportant || it.main.isStarred }
            }
            if (newOnTop) {
                ordered = ordered.sortedByDescending { isUnread(it) }
            }
            val insertDivider = selectedFilter != null || newOnTop
            val firstReadIdx = if (insertDivider) ordered.indexOfFirst { !isUnread(it) } else -1
            ordered.forEachIndexed { index, group ->
                if (firstReadIdx > 0 && index == firstReadIdx) {
                    items.add(MessageListItem.NewDivider)
                }
                items.add(MessageListItem.Group(group, isUnread(group)))
            }
        }
        items.toList()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .scrollMotionBlur(
                lazyListState = listState,
                enabled = MotionBlurGate.forTabs(),
            ),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp,
            bottom = 16.dp + bottomPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        var idx = 0
        while (idx < listItems.size) {
            when (val current = listItems[idx]) {
                is MessageListItem.Section -> {
                    val isCollapsed = current.labelRes in collapsedSections
                    val rows = mutableListOf<MessageListItem.Group>()
                    var j = idx + 1
                    while (j < listItems.size && listItems[j] is MessageListItem.Group) {
                        rows.add(listItems[j] as MessageListItem.Group)
                        j++
                    }
                    if (rows.isNotEmpty()) {
                        item(key = "sec_${current.labelRes}", contentType = "section") {
                            MessageSectionHeader(
                                labelRes = current.labelRes,
                                unreadCount = current.unreadCount,
                                totalCount = rows.size,
                                expanded = !isCollapsed,
                                onToggle = {
                                    haptics.virtualKey()
                                    collapsedSections = if (isCollapsed) {
                                        collapsedSections - current.labelRes
                                    } else {
                                        collapsedSections + current.labelRes
                                    }
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (!isCollapsed) {
                        messageGroupCards(
                            keyPrefix = "card_sec_${current.labelRes}",
                            rows = rows,
                            onItemClick = onItemClick,
                        )
                    }
                    idx = j
                }

                is MessageListItem.Group -> {
                    val rows = mutableListOf<MessageListItem.Group>()
                    var j = idx
                    while (j < listItems.size && listItems[j] is MessageListItem.Group) {
                        rows.add(listItems[j] as MessageListItem.Group)
                        j++
                    }
                    val firstId = rows.firstOrNull()?.group?.main?.timelineId
                    messageGroupCards(
                        keyPrefix = "card_grp_$firstId",
                        rows = rows,
                        onItemClick = onItemClick,
                    )
                    idx = j
                }

                is MessageListItem.NewDivider -> {
                    item(key = "new_divider", contentType = "divider") {
                        NewMessagesDivider(modifier = Modifier.animateItem())
                    }
                    idx++
                }
            }
        }

        if (state.isLoadingMore) {
            item(key = "loading_more", contentType = "loading") {
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

private fun LazyListScope.messageGroupCards(
    keyPrefix: String,
    rows: List<MessageListItem.Group>,
    onItemClick: (MessageGroup) -> Unit,
) {
    rows.chunked(8).forEachIndexed { chunkIdx, chunk ->
        item(key = "${keyPrefix}_$chunkIdx", contentType = "card") {
            RoundedCardContainer(Modifier.animateItem()) {
                chunk.forEach { row ->
                    MessageItem(
                        group = row.group,
                        isUnread = row.isUnread,
                        onClick = { onItemClick(row.group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageSectionHeader(
    labelRes: Int,
    unreadCount: Int,
    totalCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = categoryInfo(labelRes)
    val chevronAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(info.labelRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            if (unreadCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .rotate(chevronAngle),
        )
    }
}

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val fullTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
private fun MessageItem(group: MessageGroup, isUnread: Boolean, onClick: () -> Unit) {
    val event = group.main
    val typeInfo  = typeInfoFor(event.type)
    val typeLabel = stringResource(typeInfo.labelRes)
    val timeText = remember(event) {
        event.timestamp?.let { ts ->
            val today = java.time.LocalDate.now()
            if (ts.toLocalDate().year == today.year) ts.format(timeFmt) else ts.format(fullTimeFmt)
        }
    }

    val title = remember(event, typeLabel) {
        val displayText = event.text?.unescapeHtml()
        displayText?.takeIf { it.isNotBlank() }
            ?: event.title?.unescapeHtml()?.takeIf { it.isNotBlank() }
            ?: typeLabel
    }

    val sender = remember(event) {
        event.authorName?.takeIf { it.isNotBlank() }
    }

    val starredColor = Color(0xFFF9A825)

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
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
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val senderName = sender
                    ?: stringResource(R.string.messages_unknown_sender)
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (group.replies.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            text = "+${group.replies.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                TypeChip(label = typeLabel)
            }
        },
        leadingContent = {
            if (event.isStarred) {
                Surface(
                    shape = CircleShape,
                    color = starredColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = starredColor,
                        )
                    }
                }
            } else {
                PastelIcon(
                    icon = typeInfo.icon,
                    key = typeLabel,
                    containerSize = 40.dp,
                    iconSize = 20.dp,
                )
            }
        },
        trailingContent = {
            if (timeText != null) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    )
}

@Composable
private fun NewMessagesDivider(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
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
    group: MessageGroup,
    onDismiss: () -> Unit,
    onDelete: (Int) -> Unit,
    onReply: (String) -> Unit,
    onStarToggle: (Int, Boolean) -> Unit,
    onVote: (Int, List<String>) -> Unit,
    onOpenAttachment: (com.edupage.api.model.MessageAttachment) -> Unit,
    currentUserId: String?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope      = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isOwnMessage = currentUserId != null &&
        (group.main.authorId == currentUserId ||
         group.main.authorId?.removePrefix("-") == currentUserId?.removePrefix("-"))

    var event by remember(group.main.timelineId) { mutableStateOf(group.main) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        DetailSheetContent(
            event = event,
            group = group,
            isOwnMessage = isOwnMessage,
            onClose = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
            onDelete = {
                showDeleteConfirm = true
            },
            onStarToggle = { timelineId, starred ->
                event = event.copy(isStarred = starred)
                onStarToggle(timelineId, starred)
            },
            onReply = onReply,
            onVote = onVote,
            onOpenAttachment = onOpenAttachment,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.messages_delete_title)) },
            text = { Text(stringResource(R.string.messages_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(group.main.timelineId)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                ) {
                    Text(stringResource(R.string.messages_delete_action))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(stringResource(R.string.messages_detail_close))
                }
            },
        )
    }
}

@Composable
private fun DetailSheetContent(
    event: TimelineEvent,
    group: MessageGroup,
    isOwnMessage: Boolean,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onStarToggle: (Int, Boolean) -> Unit,
    onReply: (String) -> Unit,
    onVote: (Int, List<String>) -> Unit,
    onOpenAttachment: (com.edupage.api.model.MessageAttachment) -> Unit,
) {
    val typeInfo  = typeInfoFor(event.type)
    val typeLabel = stringResource(typeInfo.labelRes)
    val displayText = event.text?.unescapeHtml()
    var replyText by remember { mutableStateOf("") }
    val haptics = rememberAppHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
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
                if (event.isImportant) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.messages_important_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                ActionIconButton(
                    icon = if (event.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                    tint = if (event.isStarred) Color(0xFFF9A825) else MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = stringResource(R.string.messages_star),
                    onClick = { haptics.virtualKey(); onStarToggle(event.timelineId, !event.isStarred) },
                )
                if (isOwnMessage) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.messages_delete_action),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.messages_detail_close))
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            RoundedCardContainer {
                val sender = event.authorName?.takeIf { it.isNotBlank() }
                if (sender != null) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = sender,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.messages_detail_from),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    )
                }

                val dateStr = event.timestamp?.format(detailDateFmt)
                if (dateStr != null) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.messages_detail_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    )
                }
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

            if (event.attachments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                AttachmentsCard(
                    attachments = event.attachments,
                    onOpen = onOpenAttachment,
                )
            }

            val hasPoll = !event.pollAnswers.isNullOrEmpty()
            if (hasPoll) {
                Spacer(Modifier.height(16.dp))
                PollCard(
                    event = event,
                    onVote = { answerIds -> onVote(event.timelineId, answerIds) },
                )
            }

            if (group.replies.isNotEmpty()) {
                SectionLabel(
                    text = stringResource(R.string.messages_replies_label, group.replies.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    startPadding = 16.dp,
                    topPadding = 16.dp,
                )
                RoundedCardContainer {
                    group.replies.forEach { reply ->
                        ReplyItem(reply = reply, onOpenAttachment = onOpenAttachment)
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text(stringResource(R.string.messages_reply_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            FilledTonalIconButton(
                onClick = {
                    if (replyText.isNotBlank()) {
                        haptics.click()
                        onReply(replyText)
                        replyText = ""
                    }
                },
                enabled = replyText.isNotBlank(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.messages_reply),
                )
            }
        }
    }
}

@Composable
private fun PollCard(event: TimelineEvent, onVote: (List<String>) -> Unit) {
    val answers = event.pollAnswers ?: return
    val totalVotes = answers.sumOf { it.votes.size }
    val hasVoted = event.myVotes.isNotEmpty()

    var selectedAnswers by remember(event.timelineId) { mutableStateOf(event.myVotes) }
    val multiple = event.pollMultiple
    val haptics = rememberAppHaptics()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.messages_poll_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            val badges = mutableListOf<String>()
            if (event.pollAnonymous) badges.add(stringResource(R.string.messages_poll_anonymous))
            else badges.add(stringResource(R.string.messages_poll_public))
            if (event.pollMultiple) badges.add(stringResource(R.string.messages_poll_multiple))
            else badges.add(stringResource(R.string.messages_poll_single))
            Text(
                text = badges.joinToString(" \u00b7 "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (hasVoted) {
                answers.forEach { answer ->
                    val pct = if (totalVotes > 0) answer.votes.size.toFloat() / totalVotes else 0f
                    val isSelected = answer.id in event.myVotes
                    PollAnswerResult(
                        text = answer.text,
                        percentage = pct,
                        voteCount = answer.votes.size,
                        isSelected = isSelected,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = stringResource(R.string.messages_poll_total_votes, totalVotes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                answers.forEach { answer ->
                    val isSelected = answer.id in selectedAnswers
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                 else null,
                        onClick = {
                            haptics.virtualKey()
                            selectedAnswers = if (multiple) {
                                if (isSelected) selectedAnswers - answer.id else selectedAnswers + answer.id
                            } else {
                                if (isSelected) emptyList() else listOf(answer.id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (isSelected) "${answer.text} \u2713" else answer.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        haptics.click()
                        onVote(selectedAnswers)
                    },
                    enabled = selectedAnswers.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.messages_poll_vote),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PollAnswerResult(
    text: String,
    percentage: Float,
    voteCount: Int,
    isSelected: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isSelected) "$text \u2713" else text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$voteCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percentage)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun ReplyItem(
    reply: TimelineEvent,
    onOpenAttachment: (com.edupage.api.model.MessageAttachment) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reply.authorName ?: stringResource(R.string.messages_unknown_sender),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                reply.timestamp?.let {
                    Text(
                        text = it.format(detailDateFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = (reply.text ?: reply.title ?: "").unescapeHtml(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (reply.attachments.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                AttachmentsCard(attachments = reply.attachments, onOpen = onOpenAttachment)
            }
        }
    }
}

/** A tappable list of message/reply attachments. */
@Composable
private fun AttachmentsCard(
    attachments: List<com.edupage.api.model.MessageAttachment>,
    onOpen: (com.edupage.api.model.MessageAttachment) -> Unit,
) {
    val haptics = rememberAppHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { haptics.virtualKey(); onOpen(attachment) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeSheet(
    recipientsState: RecipientsState,
    isSending: Boolean,
    sendError: String?,
    onDismiss: () -> Unit,
    onSend: (List<EduAccount>, String, Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.imePadding(),
    ) {
        ComposeSheetContent(
            recipientsState = recipientsState,
            isSending = isSending,
            sendError = sendError,
            onSend = { recipients, body, important ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onSend(recipients, body, important)
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
    onSend: (List<EduAccount>, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedRecipients = remember { mutableStateListOf<EduAccount>() }
    var searchQuery by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var important by remember { mutableStateOf(false) }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
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
            RoundedCardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                                onClick = { haptics.virtualKey(); selectedRecipients.remove(recipient) },
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
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            ),
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
                    RoundedCardContainer(
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
                                    MaterialTheme.colorScheme.surfaceBright,
                                onClick = {
                                    haptics.virtualKey()
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
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Spacer(Modifier.height(8.dp))

        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            IconToggleItem(
                icon = Icons.Filled.PriorityHigh,
                title = stringResource(R.string.messages_important),
                checked = important,
                onCheckedChange = { important = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

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
                onSend(selectedRecipients.toList(), bodyText, important)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = !isSending,
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
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

@Preview(name = "Messages – Light", showBackground = true)
@Preview(name = "Messages – Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessagesScreenPreview() {
    Edupage2Theme {
        MessagesScreen(bottomPadding = PaddingValues())
    }
}

