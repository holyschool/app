package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

/**
 * Stores the AI study chat transcript on-device so the conversation survives restarts.
 * Nothing is uploaded anywhere but the AI provider itself.
 */
@Singleton
class ChatStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "ai_chat.json"
        private const val TAG = "ChatStore"
        private const val MAX_MESSAGES = 200
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    private val messages = java.util.concurrent.CopyOnWriteArrayList<ChatMessage>().apply {
        loadFromDisk()?.let { addAll(it) }
    }

    private val _messages = MutableStateFlow(messages.toList())
    val flow: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun all(): List<ChatMessage> = messages.toList()

    fun add(message: ChatMessage) {
        messages.add(message)
        trim()
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

    fun clear() {
        messages.clear()
        publish()
    }

    private fun trim() {
        while (messages.size > MAX_MESSAGES) {
            messages.removeAt(0)
        }
    }

    private fun publish() {
        persist()
        _messages.value = all()
    }

    private fun persist() {
        runCatching {
            file.writeText(gson.toJson(messages.toList()))
        }.onFailure { Log.e(TAG, "failed to persist chat", it) }
    }

    private fun loadFromDisk(): List<ChatMessage>? = runCatching {
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        val loaded = gson.fromJson<List<ChatMessage>>(file.readText(), type)
        loaded.map { it.sanitized() }
    }.getOrElse {
        Log.e(TAG, "failed to load chat", it)
        null
    }

    /**
     * Gson bypasses constructors, so transcripts written before a field existed can hold
     * nulls for Kotlin non-null properties (e.g. [ChatMessage.attachments]). Normalise
     * such values so the UI never sees a null collection.
     */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun ChatMessage.sanitized(): ChatMessage = copy(
        content = content ?: "",
        role = role ?: "assistant",
        attachments = attachments ?: emptyList(),
    )
}