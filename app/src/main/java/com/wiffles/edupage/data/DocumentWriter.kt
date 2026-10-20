package com.wiffles.edupage.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns text the assistant produced into a real file the student can open, keep or send.
 *
 * Markdown, plain text and CSV are written verbatim; PDF is laid out with Android's own
 * [PdfDocument]; DOCX is a hand-written minimal OOXML package, so no extra dependency is
 * needed for any of it.
 */
@Singleton
class DocumentWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    enum class Format(val extension: String, val mimeType: String) {
        MARKDOWN("md", "text/markdown"),
        TEXT("txt", "text/plain"),
        CSV("csv", "text/csv"),
        PDF("pdf", "application/pdf"),
        DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    }

    /**
     * Writes [content] into the chat attachment folder and returns the attachment that can be
     * hung on the assistant message.
     */
    fun write(fileName: String, title: String?, content: String): ChatAttachment {
        val format = formatOf(fileName)
        val baseName = sanitize(fileName.substringBeforeLast('.', fileName)).ifBlank { "document" }
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val target = uniqueFile(dir, "$baseName.${format.extension}")
        val body = content.replace("\r\n", "\n").trim('\n')

        when (format) {
            Format.MARKDOWN, Format.TEXT, Format.CSV -> target.writeText(body, Charsets.UTF_8)
            Format.PDF -> writePdf(target, title, body)
            Format.DOCX -> writeDocx(target, title, body)
        }

        Log.i(TAG, "wrote ${format.name} document ${target.name} (${target.length()} bytes)")
        return ChatAttachment(
            id = java.util.UUID.randomUUID().toString(),
            name = target.name,
            localPath = target.absolutePath,
            mimeType = format.mimeType,
            sizeBytes = target.length(),
        )
    }

    /** Saves raw bytes (a picture the assistant fetched) as a chat attachment. */
    fun saveBytes(name: String, bytes: ByteArray, mimeType: String): ChatAttachment {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val clean = sanitize(name).ifBlank { "image" }
        val withExtension = if (clean.contains('.')) clean else "$clean.jpg"
        val target = uniqueFile(dir, withExtension)
        target.writeBytes(bytes)
        Log.i(TAG, "saved image ${target.name} (${target.length()} bytes)")
        return ChatAttachment(
            id = java.util.UUID.randomUUID().toString(),
            name = target.name,
            localPath = target.absolutePath,
            mimeType = mimeType.ifBlank { "image/jpeg" },
            sizeBytes = target.length(),
        )
    }

    // ----------------------------------------------------------------------- pdf

    private fun writePdf(file: File, title: String?, content: String) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val usableWidth = pageWidth - margin * 2
        val lineGap = 5.5f

        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.SANS_SERIF
            textSize = 11.5f
            color = Color.rgb(24, 24, 24)
        }
        val heading = Paint(body).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 16f
        }

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
        )
        var canvas = page.canvas
        var y = margin + body.textSize

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin + body.textSize
        }

        fun draw(text: String, paint: Paint) {
            if (y > pageHeight - margin) newPage()
            canvas.drawText(text, margin, y, paint)
            y += paint.textSize + lineGap
        }

        title?.takeIf { it.isNotBlank() }?.let {
            draw(it.trim(), heading)
            y += 4f
        }

        toPlainText(content).split('\n').forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                y += body.textSize * 0.6f
                return@forEach
            }
            var remaining = line
            while (remaining.isNotEmpty()) {
                val count = body.breakText(remaining, true, usableWidth, null)
                if (count <= 0) break
                draw(remaining.substring(0, count).trimEnd(), body)
                remaining = remaining.substring(count).trimStart()
            }
        }

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
    }

    // ------------------------------------------------------------------- helpers

    private fun sanitize(name: String): String =
        name.trim().replace(Regex("[^A-Za-z0-9 _.-]"), "-").replace(Regex("\\s+"), "-").take(60)

    private fun uniqueFile(dir: File, name: String): File {
        val candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (true) {
            val suffix = if (ext.isBlank()) "" else ".$ext"
            val next = File(dir, "$base-$i$suffix")
            if (!next.exists()) return next
            i++
        }
    }

    companion object {
        private const val TAG = "DocumentWriter"
        private const val DIR_NAME = "chat_attachments"

        /** Picks the output format from the file name the assistant asked for. */
        fun formatOf(fileName: String): Format =
            when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
                "txt" -> Format.TEXT
                "csv" -> Format.CSV
                "pdf" -> Format.PDF
                "docx" -> Format.DOCX
                else -> Format.MARKDOWN
            }

        /** Strips the light Markdown syntax so a PDF or DOCX reads as normal prose. */
        internal fun toPlainText(markdown: String): String =
            markdown.lineSequence().joinToString("\n") { line ->
                line
                    .replace(Regex("^\\s{0,3}#{1,6}\\s*"), "")
                    .replace(Regex("^\\s{0,3}[-*+]\\s+"), "• ")
                    .replace(Regex("^\\s{0,3}>\\s?"), "")
                    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                    .replace(Regex("__(.+?)__"), "$1")
                    .replace(Regex("(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\w*])"), "$1")
                    .replace(Regex("`([^`]*)`"), "$1")
            }

        /** Writes a minimal but valid OOXML package: no dependency, opens in Word and Docs. */
        internal fun writeDocx(file: File, title: String?, content: String) {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zip ->
                fun put(name: String, body: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(body.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                put("[Content_Types].xml", CONTENT_TYPES)
                put("_rels/.rels", RELATIONSHIPS)
                put("word/document.xml", documentXml(title, content))
            }
        }

        internal fun documentXml(title: String?, content: String): String {
            val paragraphs = StringBuilder()
            title?.takeIf { it.isNotBlank() }?.let {
                paragraphs.append(paragraph(it.trim(), bold = true, sizeHalfPoints = 36))
            }
            content.split('\n').forEach { line ->
                val heading = line.trimStart().startsWith("#")
                val text = toPlainText(line).trimEnd()
                if (text.isBlank()) {
                    paragraphs.append("<w:p/>")
                } else {
                    paragraphs.append(
                        paragraph(text, bold = heading, sizeHalfPoints = if (heading) 30 else 22),
                    )
                }
            }
            return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                |<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                |<w:body>
                |$paragraphs
                |<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr>
                |</w:body>
                |</w:document>
                |""".trimMargin()
        }

        internal fun escapeXml(text: String): String = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        private fun paragraph(text: String, bold: Boolean, sizeHalfPoints: Int): String {
            val runProps = buildString {
                if (bold) append("<w:b/>")
                append("<w:sz w:val=\"$sizeHalfPoints\"/>")
            }
            return "<w:p><w:pPr><w:spacing w:after=\"120\"/></w:pPr>" +
                "<w:r><w:rPr>$runProps</w:rPr>" +
                "<w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r></w:p>"
        }

        private val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            |<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            |<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            |<Default Extension="xml" ContentType="application/xml"/>
            |<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            |</Types>
            |""".trimMargin()

        private val RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            |<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            |<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            |</Relationships>
            |""".trimMargin()
    }
}