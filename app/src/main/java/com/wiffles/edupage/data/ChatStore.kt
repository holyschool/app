package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ChatAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val error: Boolean = false,
    val attachments: List<ChatAttachment> = emptyList(),
    /** The conversation this message belongs to. */
    val sessionId: String? = null,
    /** Ids of quizzes the assistant created in this message (rendered as "take quiz" cards). */
    val quizIds: List<String> = emptyList(),
)

/** A saved conversation thread, optionally tagged with a school subject. */
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val subjectTag: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
)

/**
 * Stores the AI study chat on-device, split into subject-tagged sessions, so several
 * conversations can be kept side by side and survive restarts. Nothing is uploaded
 * anywhere but the AI provider itself.
 */
@Singleton
class ChatStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "ai_chat.json"
        private const val SESSIONS_FILE_NAME = "ai_chat_sessions.json"
        private const val TAG = "ChatStore"
        private const val MAX_PER_SESSION = 200
        private const val DEFAULT_TITLE = "Study chat"
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val sessionsFile: File get() = File(context.filesDir, SESSIONS_FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val messages = java.util.concurrent.CopyOnWriteArrayList<ChatMessage>().apply {
        loadMessages()?.let { addAll(it) }
    }

    private val sessions = java.util.concurrent.CopyOnWriteArrayList<ChatSession>().apply {
        loadSessions()?.let { addAll(it) }
    }

    private val _messages = MutableStateFlow(messages.toList())
    val allMessages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow(sessions.sortedByDescending { it.updatedAtMs })
    val sessionsFlow: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    init {
        migrateAndSelect()
    }

    /** Assigns legacy (unowned) messages to a default session and picks the active one. */
    private fun migrateAndSelect() {
        if (sessions.isEmpty()) {
            val default = ChatSession(title = DEFAULT_TITLE)
            sessions.add(default)
            if (messages.isNotEmpty()) {
                messages.replaceAll { if (it.sessionId.isNullOrBlank()) it.copy(sessionId = default.id) else it }
            }
        }
        // Any message still without an owner joins the oldest session.
        val fallbackId = sessions.minByOrNull { it.createdAtMs }?.id
        if (fallbackId != null) {
            messages.replaceAll { if (it.sessionId.isNullOrBlank()) it.copy(sessionId = fallbackId) else it }
        }
        _currentSessionId.value = sessions.maxByOrNull { it.updatedAtMs }?.id ?: sessions.firstOrNull()?.id
        publish()
    }

    fun all(): List<ChatMessage> = messages.toList()

    fun messagesFor(sessionId: String?): List<ChatMessage> =
        messages.filter { it.sessionId == sessionId }

    fun session(id: String?): ChatSession? = sessions.firstOrNull { it.id == id }

    fun subjects(): List<String> =
        sessions.mapNotNull { it.subjectTag?.takeIf { tag -> tag.isNotBlank() } }.distinct().sorted()

    /** Creates a new, empty session and makes it the active one. */
    fun createSession(subjectTag: String? = null, title: String = ""): ChatSession {
        val session = ChatSession(
            title = title.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE,
            subjectTag = subjectTag?.trim()?.takeIf { it.isNotBlank() },
        )
        sessions.add(session)
        _currentSessionId.value = session.id
        publish()
        return session
    }

    fun setCurrentSession(id: String) {
        if (sessions.none { it.id == id }) return
        _currentSessionId.value = id
        _sessions.value = sessions.sortedByDescending { it.updatedAtMs }
    }

    fun updateSession(id: String, title: String? = null, subjectTag: String? = null) {
        val idx = sessions.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = sessions[idx]
        sessions[idx] = current.copy(
            title = title?.takeIf { it.isNotBlank() } ?: current.title,
            subjectTag = subjectTag?.trim()?.takeIf { it.isNotBlank() },
            updatedAtMs = System.currentTimeMillis(),
        )
        publish()
    }

    /** Renames a session from its first user message, unless the user set a title already. */
    fun autoTitle(id: String, from: String) {
        val idx = sessions.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = sessions[idx]
        if (current.title.isNotBlank() && current.title != DEFAULT_TITLE) return
        val title = from.trim().replace('\n', ' ').take(48).ifBlank { DEFAULT_TITLE }
        sessions[idx] = current.copy(title = title, updatedAtMs = System.currentTimeMillis())
        publish()
    }

    fun deleteSession(id: String) {
        sessions.removeIf { it.id == id }
        messages.removeIf { it.sessionId == id }
        if (_currentSessionId.value == id) {
            _currentSessionId.value = sessions.maxByOrNull { it.updatedAtMs }?.id
        }
        if (sessions.isEmpty()) {
            val fresh = ChatSession(title = DEFAULT_TITLE)
            sessions.add(fresh)
            _currentSessionId.value = fresh.id
        }
        publish()
    }

    fun add(message: ChatMessage) {
        messages.add(message)
        touchSession(message.sessionId)
        trim(message.sessionId)
        publish()
    }

    fun update(id: String, transform: (ChatMessage) -> ChatMessage) {
        val idx = messages.indexOfFirst { it.id == id }
        if (idx < 0) return
        messages[idx] = transform(messages[idx])
        publish()
    }

    fun remove(id: String) {
        messages.removeIf { it.id == id }
        publish()
    }

    /** Clears the messages of one session, keeping the session itself. */
    fun clearSession(sessionId: String?) {
        messages.removeIf { it.sessionId == sessionId }
        publish()
    }

    private fun touchSession(sessionId: String?) {
        val idx = sessions.indexOfFirst { it.id == sessionId }
        if (idx < 0) return
        sessions[idx] = sessions[idx].copy(updatedAtMs = System.currentTimeMillis())
    }

    private fun trim(sessionId: String?) {
        val owned = messages.filter { it.sessionId == sessionId }
        if (owned.size <= MAX_PER_SESSION) return
        val excess = owned.sortedBy { it.timestampMs }.take(owned.size - MAX_PER_SESSION).map { it.id }.toSet()
        messages.removeIf { it.id in excess }
    }

    private fun publish() {
        persist()
        _messages.value = messages.toList()
        _sessions.value = sessions.sortedByDescending { it.updatedAtMs }
    }

    private fun persist() {
        val messageSnapshot = messages.toList()
        val sessionSnapshot = sessions.toList()
        ioScope.launch {
            runCatching {
                file.writeText(gson.toJson(messageSnapshot))
                sessionsFile.writeText(gson.toJson(sessionSnapshot))
            }.onFailure { Log.e(TAG, "failed to persist chat", it) }
        }
    }

    private fun loadMessages(): List<ChatMessage>? = runCatching {
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        val loaded = gson.fromJson<List<ChatMessage>>(file.readText(), type)
        loaded.map { it.sanitized() }
    }.getOrElse {
        Log.e(TAG, "failed to load chat", it)
        null
    }

    private fun loadSessions(): List<ChatSession>? = runCatching {
        if (!sessionsFile.exists()) return emptyList()
        val type = object : TypeToken<List<ChatSession>>() {}.type
        val loaded = gson.fromJson<List<ChatSession>>(sessionsFile.readText(), type)
        loaded.map { it.sanitized() }
    }.getOrElse {
        Log.e(TAG, "failed to load chat sessions", it)
        null
    }

    /**
     * Gson bypasses constructors, so data written before a field existed can hold nulls
     * for Kotlin non-null properties. Normalise such values so the UI never sees a null.
     */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun ChatMessage.sanitized(): ChatMessage = copy(
        content = content ?: "",
        role = role ?: "assistant",
        attachments = attachments ?: emptyList(),
        quizIds = quizIds ?: emptyList(),
        sessionId = sessionId?.takeIf { it.isNotBlank() },
    )

    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun ChatSession.sanitized(): ChatSession = copy(
        title = title ?: "",
        subjectTag = subjectTag?.takeIf { it.isNotBlank() },
    )
}