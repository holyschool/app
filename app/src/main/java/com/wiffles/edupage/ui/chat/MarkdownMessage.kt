@file:OptIn(ExperimentalLayoutApi::class)

package com.wiffles.edupage.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders a Markdown subset (headings, lists, code, quotes, emphasis, links) plus
 * inline/block LaTeX math, using native Compose text. Designed for AI answers, which
 * routinely mix Markdown structure with `$...$` / `$$...$$` formulas.
 */
@Composable
fun MarkdownMessage(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    mathColor: Color = color,
) {
    val blocks = remember(text) { MarkdownParser(text).parse() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block -> MarkdownBlockView(block, color, mathColor) }
    }
}

@Composable
private fun MarkdownBlockView(block: MdBlock, color: Color, mathColor: Color) {
    when (block) {
        is MdBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.titleLarge
                2 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            InlineText(
                spans = block.spans,
                style = style.copy(fontWeight = FontWeight.Bold, color = color),
                baseColor = color,
                mathColor = mathColor,
            )
        }

        is MdBlock.Paragraph -> InlineText(
            spans = block.spans,
            style = MaterialTheme.typography.bodyMedium.copy(color = color),
            baseColor = color,
            mathColor = mathColor,
        )

        is MdBlock.Quote -> Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .padding(vertical = 2.dp),
            ) {}
            Spacer(4.dp)
            InlineText(
                spans = block.spans,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                ),
                baseColor = MaterialTheme.colorScheme.onSurfaceVariant,
                mathColor = mathColor,
            )
        }

        is MdBlock.Bullet -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        modifier = Modifier.width(16.dp),
                    )
                    InlineText(
                        spans = item,
                        style = MaterialTheme.typography.bodyMedium.copy(color = color),
                        baseColor = color,
                        mathColor = mathColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        is MdBlock.Ordered -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        modifier = Modifier.width(22.dp),
                    )
                    InlineText(
                        spans = item,
                        style = MaterialTheme.typography.bodyMedium.copy(color = color),
                        baseColor = color,
                        mathColor = mathColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        is MdBlock.Code -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(12.dp),
        ) {
            Text(
                text = block.code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = color,
                ),
            )
        }

        is MdBlock.Divider -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        is MdBlock.MathBlock -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            MathFormula(
                latex = block.latex,
                modifier = Modifier.padding(vertical = 4.dp),
                fontSize = 19.sp,
                color = mathColor,
            )
        }
    }
}

@Composable
private fun Spacer(width: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.width(width))
}

@Composable
private fun InlineText(
    spans: List<MdSpan>,
    style: TextStyle,
    baseColor: Color,
    mathColor: Color,
    modifier: Modifier = Modifier,
) {
    val hasMath = spans.any { it is MdSpan.Math }
    if (!hasMath) {
        val annotated = remember(spans, style, baseColor) { buildAnnotated(spans, baseColor) }
        Text(text = annotated, style = style, modifier = modifier)
        return
    }

    // With inline math we lay out each token in a FlowRow so lines still wrap.
    val tokens = remember(spans, baseColor) { expandForFlow(spans, baseColor) }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        tokens.forEach { token ->
            when (token) {
                is MdSpan.Math -> Box(modifier = Modifier.padding(horizontal = 1.dp)) {
                    MathFormula(latex = token.latex, fontSize = 16.sp, color = mathColor)
                }
                is MdSpan.Text -> Text(
                    text = remember(token) { buildAnnotated(listOf(token), baseColor) },
                    style = style,
                )
            }
        }
    }
}

private fun buildAnnotated(spans: List<MdSpan>, baseColor: Color): AnnotatedString =
    buildAnnotatedString {
        spans.forEach { span ->
            if (span !is MdSpan.Text) return@forEach
            val link = span.link
            if (link != null) {
                withLink(LinkAnnotation.Url(link)) {
                    withStyle(spanStyle(span, baseColor, link = true)) { append(span.text) }
                }
            } else {
                withStyle(spanStyle(span, baseColor, link = false)) { append(span.text) }
            }
        }
    }

private fun spanStyle(span: MdSpan.Text, baseColor: Color, link: Boolean): SpanStyle {
    var style = SpanStyle(
        color = if (link) Color(0xFF3B82F6) else baseColor,
        fontWeight = if (span.bold) FontWeight.Bold else null,
        fontStyle = if (span.italic) FontStyle.Italic else null,
        textDecoration = when {
            link -> TextDecoration.Underline
            span.strike -> TextDecoration.LineThrough
            else -> null
        },
        fontFamily = if (span.code) FontFamily.Monospace else null,
    )
    if (span.code) style = style.copy(background = baseColor.copy(alpha = 0.08f))
    return style
}

/** Splits text spans into word-ish tokens so a FlowRow can wrap around inline math. */
private fun expandForFlow(spans: List<MdSpan>, baseColor: Color): List<MdSpan> {
    val out = mutableListOf<MdSpan>()
    spans.forEach { span ->
        if (span is MdSpan.Text) {
            val parts = span.text.split(Regex("(?<=\\s)"))
            parts.forEach { part ->
                if (part.isNotEmpty()) out.add(span.copy(text = part))
            }
        } else {
            out.add(span)
        }
    }
    return out
}

// ------------------------------------------------------------------ model

private sealed interface MdSpan {
    data class Text(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val code: Boolean = false,
        val strike: Boolean = false,
        val link: String? = null,
    ) : MdSpan
    data class Math(val latex: String) : MdSpan
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data class Bullet(val items: List<List<MdSpan>>) : MdBlock
    data class Ordered(val items: List<List<MdSpan>>) : MdBlock
    data class Code(val code: String) : MdBlock
    data class Quote(val spans: List<MdSpan>) : MdBlock
    data object Divider : MdBlock
    data class MathBlock(val latex: String) : MdBlock
}

private class MarkdownParser(private val source: String) {

    fun parse(): List<MdBlock> {
        val lines = source.replace("\r\n", "\n").split("\n")
        val blocks = mutableListOf<MdBlock>()
        var idx = 0

        while (idx < lines.size) {
            val raw = lines[idx]
            val line = raw.trimEnd()

            when {
                line.isBlank() -> idx++

                line.startsWith("```") -> {
                    val builder = StringBuilder()
                    idx++
                    while (idx < lines.size && !lines[idx].trimStart().startsWith("```")) {
                        builder.appendLine(lines[idx])
                        idx++
                    }
                    idx++ // closing fence
                    blocks.add(MdBlock.Code(builder.toString().trimEnd()))
                }

                line.trim() == "$$" -> {
                    val builder = StringBuilder()
                    idx++
                    while (idx < lines.size && lines[idx].trim() != "$$") {
                        builder.append(' ').append(lines[idx].trim())
                        idx++
                    }
                    idx++
                    blocks.add(MdBlock.MathBlock(builder.toString().trim()))
                }

                line.trimStart().startsWith("$$") && line.trimEnd().endsWith("$$") &&
                    line.trim().length > 4 -> {
                    val latex = line.trim().removePrefix("$$").removeSuffix("$$").trim()
                    blocks.add(MdBlock.MathBlock(latex))
                    idx++
                }

                Regex("^#{1,6}\\s+.*").matches(line) -> {
                    val level = line.takeWhile { it == '#' }.length
                    val content = line.drop(level).trim()
                    blocks.add(MdBlock.Heading(level, parseInline(content)))
                    idx++
                }

                line.trim() == "---" || line.trim() == "***" || line.trim() == "___" -> {
                    blocks.add(MdBlock.Divider)
                    idx++
                }

                isBullet(line) -> {
                    val items = mutableListOf<List<MdSpan>>()
                    while (idx < lines.size && isBullet(lines[idx])) {
                        items.add(parseInline(stripBullet(lines[idx])))
                        idx++
                    }
                    blocks.add(MdBlock.Bullet(items))
                }

                isOrdered(line) -> {
                    val items = mutableListOf<List<MdSpan>>()
                    while (idx < lines.size && isOrdered(lines[idx])) {
                        items.add(parseInline(stripOrdered(lines[idx])))
                        idx++
                    }
                    blocks.add(MdBlock.Ordered(items))
                }

                line.trimStart().startsWith(">") -> {
                    val builder = StringBuilder()
                    while (idx < lines.size && lines[idx].trimStart().startsWith(">")) {
                        builder.append(lines[idx].trimStart().removePrefix(">").trim()).append(' ')
                        idx++
                    }
                    blocks.add(MdBlock.Quote(parseInline(builder.toString().trim())))
                }

                else -> {
                    val builder = StringBuilder()
                    while (idx < lines.size) {
                        val l = lines[idx]
                        if (l.isBlank() || isBullet(l) || isOrdered(l) ||
                            Regex("^#{1,6}\\s+.*").matches(l.trimEnd()) ||
                            l.trimStart().startsWith(">") || l.trimStart().startsWith("```") ||
                            l.trim() == "$$" || l.trim() == "---"
                        ) break
                        builder.append(if (builder.isEmpty()) "" else " ").append(l.trim())
                        idx++
                    }
                    if (builder.isNotEmpty()) blocks.add(MdBlock.Paragraph(parseInline(builder.toString())))
                }
            }
        }
        return blocks
    }

    private fun isBullet(l: String): Boolean {
        val t = l.trimStart()
        return t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ")
    }

    private fun isOrdered(l: String): Boolean = Regex("^\\s*\\d+\\.\\s+.*").matches(l)

    private fun stripBullet(l: String): String = l.trimStart().drop(2)

    private fun stripOrdered(l: String): String =
        l.trimStart().replaceFirst(Regex("^\\d+\\.\\s+"), "")

    private fun parseInline(text: String): List<MdSpan> {
        val spans = mutableListOf<MdSpan>()
        val sb = StringBuilder()
        var i = 0

        fun flush() {
            if (sb.isNotEmpty()) {
                spans.add(MdSpan.Text(sb.toString()))
                sb.clear()
            }
        }

        while (i < text.length) {
            val c = text[i]
            when {
                c == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        flush()
                        spans.add(MdSpan.Text(text.substring(i + 1, end), code = true))
                        i = end + 1
                    } else { sb.append(c); i++ }
                }
                c == '$' && !(i + 1 < text.length && text[i + 1] == '$') -> {
                    val end = text.indexOf('$', i + 1)
                    if (end > i + 1) {
                        flush()
                        spans.add(MdSpan.Math(text.substring(i + 1, end)))
                        i = end + 1
                    } else { sb.append(c); i++ }
                }
                c == '*' && i + 1 < text.length && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        flush()
                        spans.addAll(parseStyled(text.substring(i + 2, end), bold = true))
                        i = end + 2
                    } else { sb.append(c); i++ }
                }
                c == '~' && i + 1 < text.length && text[i + 1] == '~' -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i) {
                        flush()
                        spans.addAll(parseStyled(text.substring(i + 2, end), strike = true))
                        i = end + 2
                    } else { sb.append(c); i++ }
                }
                (c == '*' || c == '_') -> {
                    val marker = c
                    val end = text.indexOf(marker, i + 1)
                    if (end > i + 1) {
                        flush()
                        spans.addAll(parseStyled(text.substring(i + 1, end), italic = true))
                        i = end + 1
                    } else { sb.append(c); i++ }
                }
                c == '[' -> {
                    val close = text.indexOf(']', i + 1)
                    val open = if (close > 0) text.indexOf('(', close) else -1
                    val endParen = if (open > 0) text.indexOf(')', open) else -1
                    if (close > 0 && open == close + 1 && endParen > open) {
                        flush()
                        val label = text.substring(i + 1, close)
                        val url = text.substring(open + 1, endParen)
                        spans.add(MdSpan.Text(label, link = url))
                        i = endParen + 1
                    } else { sb.append(c); i++ }
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        flush()
        return spans
    }

    private fun parseStyled(
        text: String,
        bold: Boolean = false,
        italic: Boolean = false,
        strike: Boolean = false,
    ): List<MdSpan> {
        // Styled text may still contain code/math; recurse then apply flags to Text spans.
        val inner = if (text.contains('`') || text.contains('$')) parseInline(text)
        else listOf(MdSpan.Text(text))
        return inner.map { span ->
            when (span) {
                is MdSpan.Text -> span.copy(
                    bold = span.bold || bold,
                    italic = span.italic || italic,
                    strike = span.strike || strike,
                )
                else -> span
            }
        }
    }
}