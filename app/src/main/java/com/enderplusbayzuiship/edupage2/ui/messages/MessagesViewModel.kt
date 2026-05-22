package com.enderplusbayzuiship.edupage2.ui.messages

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.people.EduAccount
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.TimelineCache
import com.enderplusbayzuiship.edupage2.network.BackendRegistrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
) : ViewModel() {

    companion object {
        private const val TAG = "MessagesViewModel"
    }

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private val _recipientsState = MutableStateFlow<RecipientsState>(RecipientsState.Idle)
    val recipientsState: StateFlow<RecipientsState> = _recipientsState.asStateFlow()

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
        viewModelScope.launch {
            backendRegistrationManager.markMessagesRead(ids)
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
        viewModelScope.launch {
            backendRegistrationManager.markMessagesRead(listOf(timelineId))
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
                    e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    fun sendMessage(recipients: List<EduAccount>, body: String) {
        if (recipients.isEmpty() || body.isBlank()) return
        val current = _uiState.value as? MessagesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSending = true, sendError = null)
            try {
                edupage.sendMessage(recipients, body)
                Log.i(TAG, "message sent, refreshing")

                historyMonthsBack = 1
                fetchAndUpdate(backgroundUpdate = true)
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed: ${e.message}", e)
                val afterSend = _uiState.value as? MessagesUiState.Success ?: current
                _uiState.value = afterSend.copy(
                    isSending = false,
                    sendError = e.message ?: context.getString(R.string.messages_error_failed_to_load)
                )
            }
        }
    }

    fun clearSendError() {
        val current = _uiState.value as? MessagesUiState.Success ?: return
        _uiState.value = current.copy(sendError = null)
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
        try {
            syncReadState()
            val events = edupage.getNotifications()
                .sortedByDescending { it.timestamp }
            allEvents = events
            cache.save(events)
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
