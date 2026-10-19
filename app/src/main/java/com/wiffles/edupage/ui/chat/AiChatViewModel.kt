package com.wiffles.edupage.ui.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AiCredentialsStore
import com.wiffles.edupage.data.AiQuiz
import com.wiffles.edupage.data.AiQuizStore
import com.wiffles.edupage.data.ChatAttachment
import com.wiffles.edupage.data.ChatMessage
import com.wiffles.edupage.data.ChatSession
import com.wiffles.edupage.data.ChatStore
import com.wiffles.edupage.data.LocalHomeworkStore
import com.wiffles.edupage.network.AiAttachment
import com.wiffles.edupage.network.AiMessage
import com.wiffles.edupage.network.AiService
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val quizStore: AiQuizStore,
    private val homeworkStore: LocalHomeworkStore,
) : ViewModel() {

    companion object {
        private const val TAG = "AiChatViewModel"
        private const val MAX_TEXT_ATTACHMENT_CHARS = 6000
        /** Fenced block the assistant emits to hand a quiz to the app. */
        private val QUIZ_BLOCK = Regex("```edupage-quiz[^\\n]*\\n?([\\s\\S]*?)```")
    }

    /** Messages belonging to the active session. */
    val messages: StateFlow<List<ChatMessage>> =
        combine(chatStore.allMessages, chatStore.currentSessionId) { all, sessionId ->
            all.filter { it.sessionId == sessionId }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sessions: StateFlow<List<ChatSession>> = chatStore.sessionsFlow

    val currentSessionId: StateFlow<String?> = chatStore.currentSessionId

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

        You can also build interactive quizzes for the student. When — and only when — the
        student asks you to make, generate or give them a quiz, a test, or practice questions,
        first write one short, friendly sentence inviting them to try it, then finish your reply
        with a fenced block in exactly this format (the app turns it into a tappable "start quiz"
        card):

        ```edupage-quiz
        {"topic":"…","subject":"…","difficulty":"medium","questions":[{"type":"multiple_choice","question":"…","options":["…","…","…","…"],"correctIndex":0,"explanation":"…"}]}
        ```

        Quiz block rules:
        - Use 5–10 questions and vary the question types where it fits.
        - Allowed "type" values and their shapes:
          - "multiple_choice": 4 options, exactly one correct, set "correctIndex".
          - "true_false": options must be exactly ["True","False"]; correctIndex 0 = True, 1 = False.
          - "fill_in": one sentence with a "___" gap; "options" are candidate words, one correct.
          - "open": no options; give the model answer in "answer".
        - Always set "subject" to a short subject label (e.g. "Biology") and "topic".
        - Put the block last, and only ever emit one block per reply.
        - Never mention the block, its JSON or its formatting to the student.
    """.trimIndent()

    /** Adds a short, local-only summary of pending homework so answers are more relevant. */
    private fun systemPrompt(): String {
        val study = aiCredentialsStore.studySettings.value
        val session = chatStore.session(chatStore.currentSessionId.value)
        return buildString {
            append(basePrompt)
            append("\n\n").append(study.promptBlock())
            session?.subjectTag?.takeIf { it.isNotBlank() }?.let { subject ->
                append("\n\nThis conversation is specific to the subject: ")
                append(subject)
                append(". Keep your answers focused on it.")
            }
            if (study.includeHomework) {
                val pending = homeworkStore.getAll()
                    .filter { !it.done && it.title.isNotBlank() }
                    .take(8)
                    .joinToString("; ") { item ->
                        if (item.subject.isNotBlank()) "${item.title} (${item.subject})" else item.title
                    }
                if (pending.isNotBlank()) {
                    append("\n\nThe student currently has these homework items they could ask about: ")
                    append(pending)
                    append('.')
                }
            }
        }
    }

    fun hasApiKey(): Boolean = aiCredentialsStore.current().apiKey.isNotBlank()

    /** True when the configured provider can read attachments (Gemini + vision model). */
    fun attachmentsEnabled(): Boolean = aiCredentialsStore.current().hasVision

    fun clearAttachError() {
        _attachError.value = null
    }

    // ------------------------------------------------------------------ sessions

    /** Starts a fresh conversation, optionally tagged with a subject. */
    fun newSession(subjectTag: String? = null) {
        chatStore.createSession(subjectTag = subjectTag)
    }

    fun selectSession(id: String) {
        chatStore.setCurrentSession(id)
    }

    fun deleteSession(id: String) {
        chatStore.deleteSession(id)
    }

    fun renameSession(id: String, title: String, subjectTag: String?) {
        chatStore.updateSession(id, title = title, subjectTag = subjectTag)
    }

    /** Clears the transcript of the active session. */
    fun clearCurrent() {
        _attachments.value.forEach { runCatching { File(it.localPath).delete() } }
        _attachments.value = emptyList()
        chatStore.clearSession(chatStore.currentSessionId.value)
    }

    // ---------------------------------------------------------------- attachments

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

    // ------------------------------------------------------------------- sending

    fun send(text: String) {
        val prompt = text.trim()
        val pending = _attachments.value
        if ((prompt.isBlank() && pending.isEmpty()) || _thinking.value) return
        val sessionId = chatStore.currentSessionId.value ?: chatStore.createSession().id
        if (!hasApiKey()) {
            chatStore.add(
                ChatMessage(
                    role = "assistant",
                    content = context.getString(R.string.chat_no_key),
                    error = true,
                    sessionId = sessionId,
                ),
            )
            return
        }
        val config = aiCredentialsStore.current()
        _attachments.value = emptyList()
        chatStore.add(
            ChatMessage(role = "user", content = prompt, attachments = pending, sessionId = sessionId),
        )
        if (prompt.isNotBlank()) chatStore.autoTitle(sessionId, prompt)
        viewModelScope.launch {
            _thinking.value = true
            val history = chatStore.messagesFor(sessionId)
                .filter { !it.error && it.content.isNotBlank() }
                .takeLast(20)
                .map { AiMessage(it.role, messageContentForModel(it)) }
            val payload = listOf(AiMessage("system", systemPrompt())) + history
            // Id of an in-progress streamed reply, so a failure can clear the empty bubble.
            var streamingId: String? = null
            try {
                // Photos and PDFs are sent inline to a Gemini vision model when configured;
                // text files are folded into the prompt by [messageContentForModel].
                val binaries = if (config.hasVision) {
                    pending.mapNotNull { readVisionAttachment(it) }
                } else {
                    emptyList()
                }
                val reply = if (binaries.isNotEmpty()) {
                    aiService.completeWithAttachments(config, payload, binaries, config.visionModel)
                } else {
                    // Print the answer as it arrives; fall back to a single request when the
                    // provider (e.g. some custom endpoints) refuses to stream.
                    val id = java.util.UUID.randomUUID().toString()
                    streamingId = id
                    chatStore.add(ChatMessage(id = id, role = "assistant", content = "", sessionId = sessionId))
                    val builder = StringBuilder()
                    try {
                        aiService.completeStream(config, payload) { delta ->
                            builder.append(delta)
                            chatStore.updateTransient(id) { it.copy(content = builder.toString()) }
                        }
                    } catch (streamError: Exception) {
                        if (builder.isEmpty()) {
                            aiService.complete(config, payload, jsonMode = false)
                        } else {
                            throw streamError
                        }
                    }
                }
                val (content, quizIds) = extractQuizBlocks(reply.trim())
                val id = streamingId
                if (id != null) {
                    chatStore.update(id) { it.copy(content = content, quizIds = quizIds) }
                } else {
                    chatStore.add(
                        ChatMessage(
                            role = "assistant",
                            content = content,
                            sessionId = sessionId,
                            quizIds = quizIds,
                        ),
                    )
                }
            } catch (e: Exception) {
                // Drop an empty streamed bubble so a failure never leaves a blank message behind.
                streamingId?.let { id ->
                    if (chatStore.all().firstOrNull { it.id == id }?.content.isNullOrBlank()) {
                        chatStore.remove(id)
                    }
                }
                val message = if (e.isNetworkError()) context.getString(R.string.network_error)
                else e.message ?: context.getString(R.string.cloud_error_loading)
                chatStore.add(
                    ChatMessage(role = "assistant", content = message, error = true, sessionId = sessionId),
                )
            } finally {
                _thinking.value = false
            }
        }
    }

    /** Pulls any quiz hand-off blocks out of a reply and saves the quizzes they describe. */
    private fun extractQuizBlocks(reply: String): Pair<String, List<String>> {
        val matches = QUIZ_BLOCK.findAll(reply).toList()
        if (matches.isEmpty()) return reply to emptyList()
        val quizIds = matches.mapNotNull { match ->
            match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let(::createQuizFromJson)?.id
        }
        val cleaned = QUIZ_BLOCK.replace(reply, "").trim().replace(Regex("\n{3,}"), "\n\n")
        return cleaned to quizIds
    }

    private fun createQuizFromJson(json: String): AiQuiz? = runCatching {
        val generated = aiService.parseGeneratedQuiz(json, expected = 50)
        val root = parseJsonObject(json)
        val topic = root?.get("topic")?.asString?.trim()?.takeIf { it.isNotBlank() }
            ?: generated.subject
            ?: context.getString(R.string.quiz_title)
        val difficulty = root?.get("difficulty")?.asString?.trim()?.takeIf { it.isNotBlank() } ?: "medium"
        val quiz = AiQuiz(
            topic = topic,
            questions = generated.questions,
            difficulty = difficulty,
            subjectTag = generated.subject,
        )
        quizStore.add(quiz)
        quiz
    }.getOrNull()

    private fun parseJsonObject(raw: String): JsonObject? = runCatching {
        val trimmed = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        val json = if (start in 0 until end) trimmed.substring(start, end + 1) else trimmed
        JsonParser.parseString(json).asJsonObject
    }.getOrNull()

    /** Reads an image or PDF attachment into bytes for the multimodal endpoint. */
    private fun readVisionAttachment(attachment: ChatAttachment): AiAttachment? {
        val mime = attachment.mimeType.lowercase().ifBlank { "application/octet-stream" }
        val supported = mime.startsWith("image/") || mime == "application/pdf"
        if (!supported) return null
        return runCatching {
            val file = File(attachment.localPath)
            if (!file.exists() || file.length() > 8_000_000) return null
            AiAttachment(mimeType = mime, data = file.readBytes())
        }.getOrNull()
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
}