package com.wiffles.edupage

import com.wiffles.edupage.ui.chat.ChatToolDialect
import com.wiffles.edupage.ui.chat.ToolCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The fenced-block protocol the assistant uses to ask for a web search, images or a file. */
class ChatToolDialectTest {

    @Test
    fun `parses a web search request`() {
        val reply = """
            Let me check that for you.
            ```edupage-tool
            {"tool":"web_search","query":"photosynthesis definition"}
            ```
        """.trimIndent()

        val parsed = ChatToolDialect.parse(reply)

        assertEquals("Let me check that for you.", parsed.visible)
        assertEquals(1, parsed.calls.size)
        val call = parsed.calls.first() as ToolCall.WebSearch
        assertEquals("photosynthesis definition", call.query)
        assertTrue(call.needsRoundTrip)
    }

    @Test
    fun `parses an image search request with a count`() {
        val reply = """
            ```edupage-tool
            {"tool":"image_search","query":"mitosis phases","count":4}
            ```
        """.trimIndent()

        val parsed = ChatToolDialect.parse(reply)

        assertEquals("", parsed.visible)
        val call = parsed.calls.first() as ToolCall.ImageSearch
        assertEquals("mitosis phases", call.query)
        assertEquals(4, call.count)
    }

    @Test
    fun `parses a document block and keeps its markdown body intact`() {
        val reply = """
            Here are your notes.
            ```edupage-doc
            filename: photosynthesis-notes.md
            title: Photosynthesis
            ---
            # Photosynthesis

            Plants turn **light** into sugar: `6 CO2 + 6 H2O -> C6H12O6 + 6 O2`.
            ```
        """.trimIndent()

        val parsed = ChatToolDialect.parse(reply)

        assertEquals("Here are your notes.", parsed.visible)
        val call = parsed.calls.first() as ToolCall.Document
        assertEquals("photosynthesis-notes.md", call.fileName)
        assertEquals("Photosynthesis", call.title)
        assertTrue(call.content.contains("# Photosynthesis"))
        assertTrue(call.content.contains("**light**"))
        // A document needs no follow-up turn: the app already has the file.
        assertFalse(call.needsRoundTrip)
    }

    @Test
    fun `defaults a bare file name to markdown`() {
        val reply = """
            ```edupage-doc
            filename: vocabulary
            ---
            hello = ahoj
            ```
        """.trimIndent()

        val call = ChatToolDialect.parse(reply).calls.first() as ToolCall.Document

        assertEquals("vocabulary.md", call.fileName)
        assertEquals("hello = ahoj", call.content)
    }

    @Test
    fun `ignores a tool block with no query`() {
        val reply = """
            ```edupage-tool
            {"tool":"web_search"}
            ```
        """.trimIndent()

        val parsed = ChatToolDialect.parse(reply)

        assertTrue(parsed.calls.isEmpty())
        assertEquals("", parsed.visible)
    }

    @Test
    fun `holds back a tool block while it is still streaming`() {
        val streaming = "Photosynthesis is the process.\n```edupage-tool\n{\"tool\":\"web_sea"

        assertEquals("Photosynthesis is the process.\n", ChatToolDialect.streamablePrefix(streaming))
    }

    @Test
    fun `holds back a partial fence that is not yet recognisable`() {
        assertEquals("Answer so far", ChatToolDialect.streamablePrefix("Answer so far```edu"))
    }

    @Test
    fun `keeps ordinary code fences visible while streaming`() {
        val text = "Try this:\n```kotlin\nprintln(\"hi\")\n```\ndone"

        assertEquals(text, ChatToolDialect.streamablePrefix(text))
    }
}