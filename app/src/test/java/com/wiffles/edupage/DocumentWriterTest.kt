package com.wiffles.edupage

import com.wiffles.edupage.data.DocumentWriter
import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The file formats the assistant can hand back: format detection, Markdown flattening, DOCX. */
class DocumentWriterTest {

    @Test
    fun `format follows the file extension`() {
        assertEquals(DocumentWriter.Format.MARKDOWN, DocumentWriter.formatOf("notes.md"))
        assertEquals(DocumentWriter.Format.TEXT, DocumentWriter.formatOf("notes.TXT"))
        assertEquals(DocumentWriter.Format.CSV, DocumentWriter.formatOf("words.csv"))
        assertEquals(DocumentWriter.Format.PDF, DocumentWriter.formatOf("sheet.pdf"))
        assertEquals(DocumentWriter.Format.DOCX, DocumentWriter.formatOf("essay.docx"))
        // Anything unknown falls back to Markdown, the friendliest thing to read back.
        assertEquals(DocumentWriter.Format.MARKDOWN, DocumentWriter.formatOf("summary"))
    }

    @Test
    fun `plain text drops the markdown syntax`() {
        val markdown = "# Heading\n- first **bold** item\n> quoted and `code`\nplain *italic* text"

        val plain = DocumentWriter.toPlainText(markdown)

        assertTrue(plain.contains("Heading"))
        assertTrue(plain.contains("• first bold item"))
        assertTrue(plain.contains("quoted and code"))
        assertTrue(plain.contains("plain italic text"))
        assertTrue(!plain.contains("**"))
        assertTrue(!plain.contains("`"))
    }

    @Test
    fun `docx package has the parts Word needs`() {
        val file = File.createTempFile("edupage-test", ".docx")
        try {
            DocumentWriter.writeDocx(file, "Photosynthesis", "# Photosynthesis\n\nPlants! & <light>")

            ZipFile(file).use { zip ->
                assertTrue(zip.getEntry("[Content_Types].xml") != null)
                assertTrue(zip.getEntry("_rels/.rels") != null)
                val document = zip.getInputStream(zip.getEntry("word/document.xml"))
                    .readBytes().toString(Charsets.UTF_8)
                assertTrue(document.contains("Photosynthesis"))
                assertTrue(document.contains("Plants!"))
                // XML-unsafe characters must survive as entities, or Word refuses the file.
                assertTrue(document.contains("&amp;"))
                assertTrue(document.contains("&lt;light&gt;"))
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `xml escaping keeps the document valid`() {
        assertEquals("a &amp; b &lt;c&gt; &quot;d&quot; &apos;e&apos;", DocumentWriter.escapeXml("a & b <c> \"d\" 'e'"))
    }
}