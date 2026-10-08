package com.enderplusbayzuiship.edupage2.ui.messages

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.EduCloudFile
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.people.EduAccount
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.LocalHomeworkStore
import com.enderplusbayzuiship.edupage2.data.MessagesViewMode
import com.enderplusbayzuiship.edupage2.data.TimelineCache
import com.enderplusbayzuiship.edupage2.network.BackendRegistrationManager
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val PAGE_SIZE = 20

private val HIDDEN_TYPES = setOf(
    "h_timetable", "h_dailyplan", "h_clearplany", "h_pripravy",
    "h_clearcache", "h_cleardbi", "h_clearisicdata",
)

data class MessageGroup(
    val main: TimelineEvent,
    val replies: List<TimelineEvent> = emptyList()
)

sealed interface MessagesUiState {
    object Loading : MessagesUiState
    data class Error(val message: String) : MessagesUiState
    data class Success(
        val groups: List<MessageGroup>,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = true,
        val seenIds: Set<Int> = emptySet(),
        val isSending: Boolean = false,
        val sendError: String? = null,
    ) : MessagesUiState
}

sealed interface RecipientsState {
    object Idle : RecipientsState
    object Loading : RecipientsState
    data class Ready(val recipients: List<EduAccount>) : RecipientsState
    data class Error(val message: String) : RecipientsState
}

@HiltViewModel
class MessagesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val prefs: AppPreferences,
    private val cache: TimelineCache,
    private val backendRegistrationManager: BackendRegistrationManager,
    private val homeworkStore: LocalHomeworkStore,
) : ViewModel() {

    companion object {
        private const val TAG = "MessagesViewModel"
    }

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private val _recipientsState = MutableStateFlow<RecipientsState>(RecipientsState.Idle)
    val recipientsState: StateFlow<RecipientsState> = _recipientsState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _importedHomework = MutableStateFlow(0)
    val importedHomework: StateFlow<Int> = _importedHomework.asStateFlow()

    fun consumeImportedHomework() {
        _importedHomework.value = 0
    }

    val viewModeFlow: Flow<MessagesViewMode> = prefs.messagesViewModeFlow
    val priorityFlow: Flow<Boolean> = prefs.messagesPriorityFlow
    val newOnTopFlow: Flow<Boolean> = prefs.messagesNewOnTopFlow

    fun setViewMode(mode: MessagesViewMode) {
        prefs.messagesViewMode = mode
    }

    fun setPriorityMessages(value: Boolean) {
        prefs.messagesPriority = value
    }

    fun setNewOnTop(value: Boolean) {
        prefs.messagesNewOnTop = value
    }

    private var allEvents: List<TimelineEvent> = emptyList()

    private var historyMonthsBack: Int = 1

    private var loadJob: Job? = null

    init {
        Log.i(TAG, "init: loading timeline")
        loadInitial()
    }

    fun refresh() {
        Log.i(TAG, "refresh triggered")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = _uiState.value
            if (current is MessagesUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            } else {
                _uiState.value = MessagesUiState.Loading
            }
            historyMonthsBack = 1
            fetchAndUpdate(backgroundUpdate = current is MessagesUiState.Success)
        }
    }

    fun loadMore() {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        if (current.isLoadingMore || !current.canLoadMore) return
        if (!com.enderplusbayzuiship.edupage2.ui.util.ConnectivityObserver.isOnline.value) return
        Log.i(TAG, "loadMore: historyMonthsBack=$historyMonthsBack")
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            try {
                val dateFrom = LocalDate.now().minusMonths(historyMonthsBack.toLong() + 1)
                historyMonthsBack++
                val newEvents = edupage.getNotificationHistory(dateFrom)
                    .sortedByDescending { it.timestamp }

                val existingIds = allEvents.map { it.timelineId }.toSet()
                val newUnique = newEvents.filter { it.timelineId !in existingIds }
                allEvents = (allEvents + newUnique).sortedByDescending { it.timestamp }

                cache.save(allEvents)
                updateUnreadCount()
                val canLoadMore = newUnique.size >= PAGE_SIZE
                Log.i(TAG, "loadMore: got ${newUnique.size} new items, canLoadMore=$canLoadMore")
                _uiState.value = current.copy(
                    groups        = allEvents.toGroups(),
                    isLoadingMore = false,
                    canLoadMore   = canLoadMore,
                    seenIds       = prefs.getSeenTimelineIds(),
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadMore failed: ${e.message}", e)
                _uiState.value = current.copy(isLoadingMore = false)
            }
        }
    }

    fun markAllSeen() {
        val current = _uiState.value as? MessagesUiState.Success ?: return

        val ids = allEvents.map { it.timelineId }
        prefs.markTimelineIdsSeen(ids)
        _uiState.value = current.copy(seenIds = prefs.getSeenTimelineIds())
        updateUnreadCount()
        viewModelScope.launch {
            backendRegistrationManager.markMessagesRead(ids)
            ids.forEach { id -> try { edupage.markMessageSeen(id) } catch (_: Exception) {} }
        }
    }

    fun markMessageSeen(timelineId: Int) {
        if (timelineId <= 0) return
        prefs.markTimelineIdsSeen(listOf(timelineId))
        if (timelineId > prefs.lastTimelineId) prefs.lastTimelineId = timelineId
        val current = _uiState.value as? MessagesUiState.Success
        if (current != null) {
            _uiState.value = current.copy(seenIds = prefs.getSeenTimelineIds())
        }
        updateUnreadCount()
        viewModelScope.launch {
            backendRegistrationManager.markMessagesRead(listOf(timelineId))
            try { edupage.markMessageSeen(timelineId) } catch (_: Exception) {}
        }
    }

    fun getEventById(timelineId: Int): TimelineEvent? {
        return allEvents.find { it.timelineId == timelineId }
    }

    fun loadRecipients() {
        if (_recipientsState.value is RecipientsState.Loading ||
            _recipientsState.value is RecipientsState.Ready) return
        viewModelScope.launch {
            _recipientsState.value = RecipientsState.Loading
            try {
                val studentsDeferred = async { edupage.getStudents() ?: emptyList() }
                val teachersDeferred = async { edupage.getTeachers() ?: emptyList() }
                val all: List<EduAccount> = studentsDeferred.await() + teachersDeferred.await()
                Log.i(TAG, "loaded ${all.size} recipients")
                _recipientsState.value = RecipientsState.Ready(all)
            } catch (e: Exception) {
                Log.e(TAG, "failed to load recipients: ${e.message}", e)
                _recipientsState.value = RecipientsState.Error(
                    if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    fun sendMessage(recipients: List<EduAccount>, body: String, important: Boolean = false, files: List<EduCloudFile> = emptyList()) {
        if (recipients.isEmpty() || body.isBlank()) return
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSending = true, sendError = null)
            try {
                edupage.sendMessage(recipients, body, important, files)
                Log.i(TAG, "message sent, refreshing")

                historyMonthsBack = 1
                fetchAndUpdate(backgroundUpdate = true)
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed: ${e.message}", e)
                val afterSend = _uiState.value as? MessagesUiState.Success ?: current
                _uiState.value = afterSend.copy(
                    isSending = false,
                    sendError = if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    fun clearSendError() {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        _uiState.value = current.copy(sendError = null)
    }

    fun getCurrentUserId(): String? = edupage.getUserId()

    fun sendReply(body: String, replyToTimelineId: Int, recipientUserString: String?) {
        if (body.isBlank()) return
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSending = true, sendError = null)
            try {
                edupage.sendReply(recipientUserString, body, replyToTimelineId)
                Log.i(TAG, "reply sent to $replyToTimelineId, refreshing")
                historyMonthsBack = 1
                fetchAndUpdate(backgroundUpdate = true)
            } catch (e: Exception) {
                Log.e(TAG, "sendReply failed: ${e.message}", e)
                val afterSend = _uiState.value as? MessagesUiState.Success ?: current
                _uiState.value = afterSend.copy(
                    isSending = false,
                    sendError = if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    fun deleteMessage(timelineId: Int) {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            try {
                edupage.deleteMessage(timelineId)
                allEvents = allEvents.filter { it.timelineId != timelineId }
                cache.save(allEvents)
                _uiState.value = current.copy(groups = allEvents.toGroups(), seenIds = prefs.getSeenTimelineIds())
                updateUnreadCount()
            } catch (e: Exception) {
                Log.e(TAG, "deleteMessage failed: ${e.message}", e)
            }
        }
    }

    fun likeMessage(timelineId: Int, liked: Boolean) {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            try {
                edupage.likeMessage(timelineId, liked)
                replaceEvent(timelineId) { it.copy(reactionCount = (it.reactionCount + if (liked) 1 else -1).coerceAtLeast(0)) }
            } catch (e: Exception) {
                Log.e(TAG, "likeMessage failed: ${e.message}", e)
            }
        }
    }

    fun toggleMessageDone(timelineId: Int, done: Boolean) {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            try {
                edupage.toggleMessageDone(timelineId, done)
                replaceEvent(timelineId) { it.copy(isDone = done) }
            } catch (e: Exception) {
                Log.e(TAG, "toggleMessageDone failed: ${e.message}", e)
            }
        }
    }

    fun toggleMessageStarred(timelineId: Int, starred: Boolean) {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            try {
                edupage.toggleMessageStarred(timelineId, starred)
                replaceEvent(timelineId) { it.copy(isStarred = starred) }
            } catch (e: Exception) {
                Log.e(TAG, "toggleMessageStarred failed: ${e.message}", e)
            }
        }
    }

    fun voteOnPoll(timelineId: Int, answerIds: List<String>) {
        if (answerIds.isEmpty()) return
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            try {
                edupage.voteOnPoll(timelineId, answerIds)
                replaceEvent(timelineId) { it.copy(myVotes = answerIds) }
                fetchAndUpdate(backgroundUpdate = true)
            } catch (e: Exception) {
                Log.e(TAG, "voteOnPoll failed: ${e.message}", e)
                val afterSend = _uiState.value as? MessagesUiState.Success ?: current
                _uiState.value = afterSend.copy(
                    sendError = if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    private fun replaceEvent(timelineId: Int, transform: (TimelineEvent) -> TimelineEvent) {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        allEvents = allEvents.map { if (it.timelineId == timelineId) transform(it) else it }
        cache.save(allEvents)
        _uiState.value = current.copy(groups = allEvents.toGroups(), seenIds = prefs.getSeenTimelineIds())
    }

    suspend fun uploadFile(file: java.io.File): EduCloudFile = edupage.cloudUpload(file)

    suspend fun sendPoll(
        recipients: List<EduAccount>,
        question: String,
        answers: List<String>,
        anonymous: Boolean = false,
        singleChoice: Boolean = false,
    ) {
        if (recipients.isEmpty() || question.isBlank() || answers.isEmpty()) return
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSending = true, sendError = null)
            try {
                edupage.createPoll(recipients, question, answers, anonymous, singleChoice)
                historyMonthsBack = 1
                fetchAndUpdate(backgroundUpdate = true)
            } catch (e: Exception) {
                Log.e(TAG, "sendPoll failed: ${e.message}", e)
                val afterSend = _uiState.value as? MessagesUiState.Success ?: current
                _uiState.value = afterSend.copy(
                    isSending = false,
                    sendError = if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    private fun updateUnreadCount() {
        _unreadCount.value = allEvents.count { it.timelineId !in prefs.getSeenTimelineIds() }
    }

    private fun List<TimelineEvent>.toGroups(): List<MessageGroup> {
        val filtered = filter { it.type?.lowercase() !in HIDDEN_TYPES }
        val mains = filtered.filter { it.reactionTo == null || it.reactionTo == 0 }
        val replies = filtered.filter { it.reactionTo != null && it.reactionTo != 0 }

        return mains.map { main ->
            MessageGroup(
                main = main,
                replies = replies.filter { it.reactionTo == main.timelineId }
                    .sortedBy { it.timestamp }
            )
        }.sortedByDescending { it.main.timestamp ?: it.replies.maxOfOrNull { r -> r.timestamp ?: java.time.LocalDateTime.MIN } ?: java.time.LocalDateTime.MIN }
    }

    private fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val cached = cache.load()
            if (cached != null) {
                val (events, isStale) = cached
                allEvents = events.sortedByDescending { it.timestamp }
                updateUnreadCount()
                val groups = allEvents.toGroups()
                Log.i(TAG, "cache hit: ${events.size} events, stale=$isStale")
                syncReadState()
                _uiState.value = MessagesUiState.Success(
                    groups       = groups,
                    isRefreshing = isStale,
                    canLoadMore  = true,
                    seenIds      = prefs.getSeenTimelineIds(),
                )
                if (!isStale) return@launch
            } else {
                _uiState.value = MessagesUiState.Loading
            }
            fetchAndUpdate(backgroundUpdate = cached != null)
        }
    }

    private suspend fun fetchAndUpdate(backgroundUpdate: Boolean) {
        if (!com.enderplusbayzuiship.edupage2.ui.util.ConnectivityObserver.isOnline.value) {
            val offline = _uiState.value
            if (offline is MessagesUiState.Success) {
                _uiState.value = offline.copy(isRefreshing = false, isLoadingMore = false)
            } else {
                _uiState.value = MessagesUiState.Error(context.getString(R.string.network_error))
            }
            return
        }
        try {
            syncReadState()
            val events = edupage.getNotifications()
                .sortedByDescending { it.timestamp }
            allEvents = events
            cache.save(events)
            val imported = homeworkStore.importFromMessages(events)
            if (imported > 0) _importedHomework.value = imported
            updateUnreadCount()
            val groups = events.toGroups()
            Log.i(TAG, "fetchAndUpdate: ${events.size} events")
            _uiState.value = MessagesUiState.Success(
                groups       = groups,
                isRefreshing = false,
                canLoadMore  = true,
                seenIds      = prefs.getSeenTimelineIds(),
            )
        } catch (e: Exception) {
            val current = _uiState.value
            if (backgroundUpdate && current is MessagesUiState.Success) {
                Log.w(TAG, "background fetch failed, keeping cache: ${e.message}")
                _uiState.value = current.copy(isRefreshing = false, isLoadingMore = false)
            } else {
                Log.e(TAG, "fetchAndUpdate failed: ${e.message}", e)
                _uiState.value = MessagesUiState.Error(
                    if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    private suspend fun syncReadState() {
        val localSeen = prefs.getSeenTimelineIds()
        val result = backendRegistrationManager.syncReadState(localSeen)
        if (result.ok && result.ids.isNotEmpty()) {
            prefs.markTimelineIdsSeen(result.ids)
            val current = _uiState.value as? MessagesUiState.Success
            if (current != null) {
                _uiState.value = current.copy(seenIds = prefs.getSeenTimelineIds())
            }
        }
    }
}

