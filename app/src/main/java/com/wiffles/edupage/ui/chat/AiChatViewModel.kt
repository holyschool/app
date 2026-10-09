package com.wiffles.edupage.ui.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AiCredentialsStore
import com.wiffles.edupage.data.ChatAttachment
import com.wiffles.edupage.data.ChatMessage
import com.wiffles.edupage.data.ChatStore
import com.wiffles.edupage.data.LocalHomeworkStore
import com.wiffles.edupage.network.AiMessage
import com.wiffles.edupage.network.AiService
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiService: AiService,
    private val aiCredentialsStore: AiCredentialsStore,
    private val chatStore: ChatStore,
    private val homeworkStore: LocalHomeworkStore,
) : ViewModel() {

    companion object {
        private const val TAG = "AiChatViewModel"
        private const val MAX_TEXT_ATTACHMENT_CHARS = 6000
    }

    val messages: StateFlow<List<ChatMessage>> = chatStore.flow

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    private val _attachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val attachments: StateFlow<List<ChatAttachment>> = _attachments.asStateFlow()

    private val _attachError = MutableStateFlow<String?>(null)
    val attachError: StateFlow<String?> = _attachError.asStateFlow()

    private val basePrompt = """
        You are a friendly, patient study assistant built into a school app.
        Help students understand their schoolwork: explain concepts step by step,
        give examples, summarise, and create practice questions on request.
        Prefer clear, age-appropriate language. If a question is unrelated to learning,
        gently steer back to studying. Be concise but genuinely educational.
        If you are unsure, say so rather than inventing facts.
        Format answers with Markdown. Write any maths as LaTeX inside $...$ (inline)
        or $$...$$ (block) so it renders nicely.
    """.trimIndent()

    /** Adds a short, local-only summary of pending homework so answers are more relevant. */
    private fun systemPrompt(): String {
        val pending = homeworkStore.getAll()
            .filter { !it.done && it.title.isNotBlank() }
            .take(8)
            .joinToString("; ") { item ->
                if (item.subject.isNotBlank()) "${item.title} (${item.subject})" else item.title
            }
        return buildString {
            append(basePrompt)
            if (pending.isNotBlank()) {
                append("\n\nThe student currently has these homework items they could ask about: ")
                append(pending)
                append('.')
            }
        }
    }

    fun hasApiKey(): Boolean = aiCredentialsStore.current().apiKey.isNotBlank()

    fun clearAttachError() {
        _attachError.value = null
    }

    /** Copies a picked document into app storage and queues it for the next message. */
    fun attachUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val name = queryDisplayName(uri) ?: "attachment"
                    val mime = resolver.getType(uri) ?: "application/octet-stream"
                    val dir = File(context.filesDir, "chat_attachments").apply { mkdirs() }
                    val target = uniqueFile(dir, name)
                    resolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("Could not read the selected file")
                    ChatAttachment(
                        name = name,
                        localPath = target.absolutePath,
                        mimeType = mime,
                        sizeBytes = target.length(),
                    )
                }
            }.onSuccess { attachment ->
                _attachments.value = _attachments.value + attachment
            }.onFailure { e ->
                Log.e(TAG, "attach failed: ${e.message}", e)
                _attachError.value = context.getString(R.string.chat_attach_failed)
            }
        }
    }

    fun removeAttachment(id: String) {
        val removed = _attachments.value.firstOrNull { it.id == id }
        _attachments.value = _attachments.value.filterNot { it.id == id }
        removed?.localPath?.let { runCatching { File(it).delete() } }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }

    private fun uniqueFile(dir: File, name: String): File {
        val candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (true) {
            val suffix = if (ext.isBlank()) "" else ".$ext"
            val f = File(dir, "$base-$i$suffix")
            if (!f.exists()) return f
            i++
        }
    }

    fun send(text: String) {
        val prompt = text.trim()
        val pending = _attachments.value
        if ((prompt.isBlank() && pending.isEmpty()) || _thinking.value) return
        if (!hasApiKey()) {
            chatStore.add(ChatMessage(role = "assistant", content = context.getString(R.string.chat_no_key), error = true))
            return
        }
        _attachments.value = emptyList()
        chatStore.add(ChatMessage(role = "user", content = prompt, attachments = pending))
        viewModelScope.launch {
            _thinking.value = true
            val history = chatStore.all()
                .filter { !it.error }
                .takeLast(20)
                .map { AiMessage(it.role, messageContentForModel(it)) }
            val payload = listOf(AiMessage("system", systemPrompt())) + history
            try {
                val reply = aiService.complete(aiCredentialsStore.current(), payload, jsonMode = false)
                chatStore.add(ChatMessage(role = "assistant", content = reply.trim()))
            } catch (e: Exception) {
                val message = if (e.isNetworkError()) context.getString(R.string.network_error)
                else e.message ?: context.getString(R.string.cloud_error_loading)
                chatStore.add(ChatMessage(role = "assistant", content = message, error = true))
            } finally {
                _thinking.value = false
            }
        }
    }

    /** Folds attachment names (and small text-file contents) into what the model sees. */
    private fun messageContentForModel(message: ChatMessage): String {
        if (message.attachments.isEmpty()) return message.content
        val sb = StringBuilder(message.content)
        message.attachments.forEach { attachment ->
            sb.append("\n\n[Attached file: ${attachment.name}]")
            val text = readTextAttachment(attachment)
            if (text != null) {
                sb.append("\n```\n").append(text).append("\n```")
            }
        }
        return sb.toString().trim()
    }

    private fun readTextAttachment(attachment: ChatAttachment): String? {
        val mime = attachment.mimeType.lowercase()
        val looksTextual = mime.startsWith("text/") ||
            mime.contains("json") || mime.contains("csv") || mime.contains("xml") ||
            attachment.name.substringAfterLast('.', "").lowercase() in
            setOf("txt", "md", "csv", "json", "xml", "html", "kt", "java", "py", "js", "ts")
        if (!looksTextual) return null
        return runCatching {
            val file = File(attachment.localPath)
            if (!file.exists() || file.length() > 512_000) return null
            file.readText().take(MAX_TEXT_ATTACHMENT_CHARS)
        }.getOrNull()
    }

    fun clear() {
        _attachments.value.forEach { runCatching { File(it.localPath).delete() } }
        _attachments.value = emptyList()
        chatStore.clear()
    }
}