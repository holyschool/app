package com.wiffles.edupage.ui.chat

import com.google.gson.JsonParser
import com.wiffles.edupage.R

/**
 * The small "tool protocol" the chat uses: the assistant asks for something by writing a
 * fenced block, the app runs it and (for searches) sends the result back for a second pass.
 *
 * Two block shapes are supported:
 *
 * ```edupage-tool
 * {"tool":"web_search","query":"…"}
 * ```
 *
 * ```edupage-doc
 * filename: photosynthesis-notes.md
 * title: Photosynthesis
 * ---
 * # Photosynthesis
 * …
 * ```
 *
 * The JSON form is used for short, single-line requests; documents use the header form so
 * their body can contain Markdown, quotes and newlines without any escaping.
 */
internal sealed interface ToolCall {
    /** The exact block the assistant wrote, echoed back to it on the follow-up turn. */
    val raw: String

    /** True when the model has to be called again with the tool's result. */
    val needsRoundTrip: Boolean

    /** Label shown in the chat while the tool runs. */
    val statusRes: Int

    data class WebSearch(val query: String, override val raw: String) : ToolCall {
        override val needsRoundTrip = true
        override val statusRes = R.string.chat_tool_searching
    }

    data class ImageSearch(val query: String, val count: Int, override val raw: String) : ToolCall {
        override val needsRoundTrip = true
        override val statusRes = R.string.chat_tool_images
    }

    data class Document(
        val fileName: String,
        val title: String?,
        val content: String,
        override val raw: String,
    ) : ToolCall {
        override val needsRoundTrip = false
        override val statusRes = R.string.chat_tool_writing
    }
}

/** What is left of an assistant reply once its tool blocks are pulled out. */
internal data class ParsedToolReply(
    val visible: String,
    val calls: List<ToolCall>,
)

internal object ChatToolDialect {

    private const val TOOL_FENCE = "```edupage-tool"
    private const val DOC_FENCE = "```edupage-doc"

    /** Fenced blocks that must never be streamed to the student as raw text. */
    private val HIDDEN_FENCES = listOf(
        TOOL_FENCE,
        DOC_FENCE,
        "```edupage-quiz",
        "```edupage-chem",
    )

    private val TOOL_BLOCK = Regex("```edupage-tool\\s*([\\s\\S]*?)```")
    private val DOC_BLOCK = Regex("```edupage-doc\\s*([\\s\\S]*?)```")
    private val HEADER_LINE = Regex("^([A-Za-z_]+)\\s*:\\s*(.*)$")

    /**
     * The part of a still-streaming reply that is safe to show. Everything from a tool fence
     * onwards is held back, and a trailing partial fence ("```edup") is held back too, so the
     * raw JSON never flashes on screen while the model is asking for a tool.
     */
    fun streamablePrefix(text: String): String {
        var cut = text.length
        HIDDEN_FENCES.forEach { fence ->
            val index = text.indexOf(fence)
            if (index in 0 until cut) cut = index
        }
        val longest = HIDDEN_FENCES.maxOf { it.length }
        val scanFrom = (text.length - longest).coerceAtLeast(0)
        for (i in scanFrom until text.length) {
            val tail = text.substring(i)
            if (HIDDEN_FENCES.any { it.startsWith(tail) }) {
                cut = minOf(cut, i)
                break
            }
        }
        return text.substring(0, cut)
    }

    /** Splits a finished reply into the visible answer and the tool calls it requested. */
    fun parse(text: String): ParsedToolReply {
        val calls = mutableListOf<ToolCall>()

        TOOL_BLOCK.findAll(text).forEach { match ->
            parseJsonTool(match.value, match.groupValues[1])?.let { calls += it }
        }
        DOC_BLOCK.findAll(text).forEach { match ->
            parseDocument(match.value, match.groupValues[1])?.let { calls += it }
        }

        var visible = TOOL_BLOCK.replace(text, "")
        visible = DOC_BLOCK.replace(visible, "")
        visible = visible.trim().replace(Regex("\n{3,}"), "\n\n")
        return ParsedToolReply(visible, calls)
    }

    private fun parseJsonTool(raw: String, body: String): ToolCall? {
        val json = runCatching { JsonParser.parseString(body.trim()).asJsonObject }.getOrNull() ?: return null
        val query = json.get("query")?.asString?.trim().orEmpty()
        return when (json.get("tool")?.asString?.trim()?.lowercase()) {
            "web_search", "search", "websearch" ->
                if (query.isBlank()) null else ToolCall.WebSearch(query, raw)
            "image_search", "images", "image" ->
                if (query.isBlank()) {
                    null
                } else {
                    val count = json.get("count")?.asInt ?: 3
                    ToolCall.ImageSearch(query, count, raw)
                }
            else -> null
        }
    }

    private fun parseDocument(raw: String, body: String): ToolCall? {
        val lines = body.trim('\n').split('\n')
        var fileName = ""
        var title: String? = null
        var contentStart = 0

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed == "---") {
                contentStart = index + 1
                break
            }
            val header = HEADER_LINE.matchEntire(trimmed) ?: break
            when (header.groupValues[1].lowercase()) {
                "filename", "file", "name" -> fileName = header.groupValues[2].trim()
                "title" -> title = header.groupValues[2].trim().takeIf { it.isNotBlank() }
                "format" -> if (fileName.isBlank()) fileName = "document.${header.groupValues[2].trim()}"
                else -> break
            }
            contentStart = index + 1
        }

        val content = lines.drop(contentStart).joinToString("\n").trim('\n')
        if (content.isBlank()) return null

        val name = fileName.ifBlank { "document" }
        val withExtension = if (name.contains('.')) name else "$name.md"
        return ToolCall.Document(withExtension, title, content, raw)
    }
}