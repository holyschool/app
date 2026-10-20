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
import com.wiffles.edupage.data.DocumentWriter
import com.wiffles.edupage.data.LocalHomeworkStore
import com.wiffles.edupage.network.AiAttachment
import com.wiffles.edupage.network.AiConfig
import com.wiffles.edupage.network.AiMessage
import com.wiffles.edupage.network.AiService
import com.wiffles.edupage.network.AiTools
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    private val aiTools: AiTools,
    private val documentWriter: DocumentWriter,
) : ViewModel() {

    companion object {
        private const val TAG = "AiChatViewModel"
        private const val MAX_TEXT_ATTACHMENT_CHARS = 6000
        /** How many times the assistant may ask for a search before it must answer. */
        private const val MAX_TOOL_ROUNDS = 3
        /** Reveal pace for streamed replies, so the text types out at a steady, readable rate. */
        private const val STREAM_TICK_MS = 16L
        /** Characters per second typed at the tail; the backlog adds [STREAM_BACKLOG_GAIN] each. */
        private const val STREAM_BASE_CPS = 70f
        private const val STREAM_BACKLOG_GAIN = 5f
        private const val STREAM_MAX_CPS = 900f
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

    /** Label of the tool currently running (a string resource), or null when idle. */
    private val _toolStatus = MutableStateFlow<Int?>(null)
    val toolStatus: StateFlow<Int?> = _toolStatus.asStateFlow()

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

        For chemistry, a simple formula (H2O, CO2, NaCl) can stay as plain text, but whenever
        you write a reaction or a structural formula, put it in a fenced block so the app can
        typeset it properly. Write one item per line — a reaction with "->" between the sides
        and "+" between species, or a structure with the groups joined by "-", "=" or "≡":

        ```edupage-chem
        2 H2 + O2 -> 2 H2O
        CaCO3 -> CaO + CO2
        CH3-CH2-CH2-CH3
        CH3-CH=CH-CH2-OH
        ```

        Chem block rules:
        - Keep formulas in plain ASCII with digits right after each element (H2O, Ca(OH)2,
          SO4^2-); the app turns them into proper subscripts and charges.
        - Put the block on its own lines, right after the sentence that introduces it.
        - Never mention the block or its formatting to the student.

        You also have tools. To use one, write a fenced block and stop there — the app runs it
        and sends you the result, and you finish your answer in a follow-up turn.

        Search the web, for current facts, news, or anything you are not sure about:
        ```edupage-tool
        {"tool":"web_search","query":"…"}
        ```

        Find pictures for a topic:
        ```edupage-tool
        {"tool":"image_search","query":"…","count":3}
        ```

        Create a file the student can keep or send. The extension of "filename" picks the
        format (md, txt, csv, pdf or docx); everything after the --- line is the finished file:
        ```edupage-doc
        filename: photosynthesis-notes.md
        title: Photosynthesis
        ---
        # Photosynthesis
        …
        ```

        Tool rules:
        - Search instead of guessing when a question needs facts you are not certain about, and
          fetch pictures when the student asks for them or when a picture teaches it better.
        - Stop right after the tool block and wait: never invent the result.
        - At most one or two tool blocks per reply, and never repeat a search you already made.
        - The document block is used exactly as written, so make it the complete, final file.
        - Never mention the blocks, their JSON or this protocol to the student.
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
            _toolStatus.value = null
            val history = chatStore.messagesFor(sessionId)
                .filter { !it.error && it.content.isNotBlank() }
                .takeLast(20)
                .map { AiMessage(it.role, messageContentForModel(it)) }
            val payload = (listOf(AiMessage("system", systemPrompt())) + history).toMutableList()
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
                val id = java.util.UUID.randomUUID().toString()
                streamingId = id
                chatStore.add(ChatMessage(id = id, role = "assistant", content = "", sessionId = sessionId))

                // The assistant may ask for a search; each answer is streamed, then the tool
                // result is fed back for one more turn until it answers without tools.
                var visible = ""
                var round = 0
                val attachments = mutableListOf<ChatAttachment>()
                while (true) {
                    val raw = streamRound(
                        config = config,
                        payload = payload,
                        binaries = if (round == 0) binaries else emptyList(),
                        id = id,
                        base = visible,
                    )
                    val parsed = ChatToolDialect.parse(raw)
                    visible = listOf(visible, parsed.visible)
                        .filter { it.isNotBlank() }
                        .joinToString("\n\n")

                    // Documents are produced locally, so they never need another turn.
                    parsed.calls.filterIsInstance<ToolCall.Document>().forEach { call ->
                        _toolStatus.value = call.statusRes
                        attachments += documentWriter.write(call.fileName, call.title, call.content)
                    }

                    val remote = parsed.calls.filter { it.needsRoundTrip }
                    if (remote.isEmpty() || round >= MAX_TOOL_ROUNDS) break

                    _toolStatus.value = remote.first().statusRes
                    val results = remote.map { runTool(it) }
                    attachments += results.flatMap { it.attachments }
                    payload += AiMessage("assistant", raw)
                    payload += AiMessage("user", results.joinToString("\n\n") { it.text })
                    round++
                }
                _toolStatus.value = null

                val (content, quizIds) = extractQuizBlocks(visible.trim())
                chatStore.update(id) {
                    it.copy(content = content, quizIds = quizIds, attachments = attachments)
                }
            } catch (e: Exception) {
                _toolStatus.value = null
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

    // ---------------------------------------------------------------------- tools

    /** One streamed turn. Returns the complete raw text the model produced. */
    private suspend fun streamRound(
        config: AiConfig,
        payload: List<AiMessage>,
        binaries: List<AiAttachment>,
        id: String,
        base: String,
    ): String {
        val builder = StringBuilder()
        // How much of the streamable prefix has already been handed to the revealer.
        var shown = 0
        var streamFailure: Exception? = null
        val deltas = Channel<String>(Channel.UNLIMITED)

        coroutineScope {
            val revealer = launch { revealStreamed(id, deltas, base) }
            fun publish() {
                val prefix = ChatToolDialect.streamablePrefix(builder.toString())
                if (prefix.length > shown) {
                    deltas.trySend(prefix.substring(shown))
                    shown = prefix.length
                }
            }
            try {
                if (binaries.isNotEmpty()) {
                    builder.append(
                        aiService.completeWithAttachments(config, payload, binaries, config.visionModel),
                    )
                    publish()
                } else {
                    // Print the answer as it arrives; the visible part stops before any tool
                    // block, so the raw JSON never flashes on screen.
                    aiService.completeStream(config, payload) { delta ->
                        builder.append(delta)
                        publish()
                    }
                }
            } catch (e: Exception) {
                streamFailure = e
            }
            deltas.close()
            revealer.join()
        }

        streamFailure?.let { error ->
            // Nothing printed yet: give a provider that refuses to stream one more chance.
            if (builder.isNotEmpty()) throw error
            builder.append(aiService.complete(config, payload, jsonMode = false))
            val preview = ChatToolDialect.streamablePrefix(builder.toString())
            if (preview.isNotEmpty()) {
                val single = Channel<String>(Channel.UNLIMITED)
                coroutineScope {
                    val fallbackRevealer = launch { revealStreamed(id, single, base) }
                    single.trySend(preview)
                    single.close()
                    fallbackRevealer.join()
                }
            }
        }
        return builder.toString()
    }

    private data class ToolResult(val text: String, val attachments: List<ChatAttachment> = emptyList())

    private suspend fun runTool(call: ToolCall): ToolResult = when (call) {
        is ToolCall.WebSearch -> {
            val hits = aiTools.webSearch(call.query)
            ToolResult(formatWebResults(call.query, hits))
        }

        is ToolCall.ImageSearch -> {
            val hits = aiTools.imageSearch(call.query, call.count)
            val images = hits.mapNotNull { hit -> saveImage(hit) }
            ToolResult(formatImageResults(call.query, hits, images), images)
        }

        is ToolCall.Document -> ToolResult("The file was created.")
    }

    private fun formatWebResults(query: String, hits: List<AiTools.WebHit>): String {
        if (hits.isEmpty()) {
            return "web_search(\"$query\") returned no results. Answer from what you know and " +
                "say that you could not check it online."
        }
        val body = hits.mapIndexed { index, hit ->
            val snippet = if (hit.snippet.isBlank()) "" else " — ${hit.snippet.take(400)}"
            "${index + 1}. ${hit.title}$snippet (${hit.url})"
        }.joinToString("\n")
        return "web_search(\"$query\") results:\n$body\n\n" +
            "Use these to answer the student, in your own words. Do not mention the search " +
            "protocol, and do not run the same search again."
    }

    private fun formatImageResults(
        query: String,
        hits: List<AiTools.ImageHit>,
        saved: List<ChatAttachment>,
    ): String {
        if (saved.isEmpty()) {
            return "image_search(\"$query\") found nothing that could be shown."
        }
        val list = hits.take(saved.size).mapIndexed { index, hit -> "${index + 1}. ${hit.title}" }
            .joinToString("\n")
        return "image_search(\"$query\") — ${saved.size} picture(s) are now displayed in the " +
            "chat, in this order:\n$list\n\n" +
            "They are already visible to the student: refer to what each one shows in a short " +
            "sentence. Never write image URLs or Markdown image syntax."
    }

    private suspend fun saveImage(hit: AiTools.ImageHit): ChatAttachment? {
        val download = aiTools.downloadImage(hit.url) ?: return null
        return runCatching {
            documentWriter.saveBytes(download.name, download.bytes, download.mimeType)
        }.onFailure { Log.w(TAG, "could not save image: ${it.message}") }.getOrNull()
    }

    /**
     * Drains streamed deltas into a message at a steady reveal rate. Characters that arrive
     * faster than the pace are buffered, and the pace speeds up with the backlog so a long
     * answer still finishes promptly; the tail always types out at the base pace.
     */
    private suspend fun revealStreamed(id: String, deltas: ReceiveChannel<String>, base: String = "") {
        val pending = StringBuilder()
        val revealed = StringBuilder(base)
        // Fractional carry so a slow stream can reveal less than one character per frame.
        var carry = 0f
        try {
            while (true) {
                while (true) {
                    val delta = deltas.tryReceive().getOrNull() ?: break
                    pending.append(delta)
                }
                if (pending.isEmpty()) {
                    if (deltas.isClosedForReceive) break
                    pending.append(deltas.receiveCatching().getOrNull() ?: break)
                    continue
                }
                // Accelerate smoothly with the backlog so a long answer still finishes promptly,
                // while the tail always types at the gentle base pace.
                val cps = (STREAM_BASE_CPS + pending.length * STREAM_BACKLOG_GAIN)
                    .coerceAtMost(STREAM_MAX_CPS)
                carry += cps * (STREAM_TICK_MS / 1000f)
                val reveal = carry.toInt()
                if (reveal > 0) {
                    val take = minOf(reveal, pending.length)
                    revealed.append(pending, 0, take)
                    pending.delete(0, take)
                    // Keep the fractional remainder, but never let it snowball into a burst.
                    carry = (carry - take).coerceAtMost(2f)
                    chatStore.updateTransient(id) { it.copy(content = revealed.toString()) }
                }
                delay(STREAM_TICK_MS)
            }
        } finally {
            // Keep whatever was already typed if the reveal is cancelled mid-answer.
            chatStore.flush()
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