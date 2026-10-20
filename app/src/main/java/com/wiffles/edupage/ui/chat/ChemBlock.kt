package com.wiffles.edupage.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders a chemistry `edupage-chem` fenced block. Each line is either a reaction (drawn with
 * a proper arrow) or a structural formula (drawn as groups joined by bond lines); formulas get
 * real subscripts and charges. This is the "chemistry tool" the assistant is told to call, so a
 * reaction never has to fall back on overflowing `$...$` math.
 */
@Composable
internal fun ChemBlock(lines: List<String>, color: Color) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            lines.forEach { line -> ChemLine(line, color, accent) }
        }
    }
}

@Composable
private fun ChemLine(raw: String, color: Color, accent: Color) {
    val line = raw.trim()
    if (line.isEmpty()) return
    val arrow = findArrow(line)
    when {
        arrow != null -> ChemReaction(
            left = line.substring(0, arrow.first).trim(),
            arrow = arrow.second,
            right = line.substring(arrow.first + arrow.second.length).trim(),
            color = color,
            accent = accent,
        )
        isStructure(line) -> ChemStructure(line, color)
        else -> FormulaText(line, 20.sp, color, centered = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChemReaction(left: String, arrow: String, right: String, color: Color, accent: Color) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
    ) {
        FormulaText(left, 18.sp, color)
        Text(
            text = arrowGlyph(arrow),
            fontSize = 20.sp,
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        FormulaText(right, 18.sp, color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChemStructure(line: String, color: Color) {
    val inner = line.trim().removeSurrounding("(", ")").trim()
    val tokens = STRUCTURE_TOKEN.findAll(inner).map { it.value }.toList()
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
    ) {
        tokens.forEach { token ->
            if (token.length == 1 && token[0] in BOND_CHARS) {
                Bond(token[0], color)
            } else {
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = chemFormula(token),
                        fontSize = 15.sp,
                        color = color,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** A short horizontal line (double or triple for `=` / `≡` bonds). */
@Composable
private fun Bond(kind: Char, color: Color) {
    val rows = when (kind) {
        '=' -> 2
        '≡' -> 3
        else -> 1
    }
    Column(
        modifier = Modifier.width(20.dp).padding(horizontal = 1.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(rows) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(color.copy(alpha = 0.55f)),
            )
        }
    }
}

@Composable
private fun FormulaText(raw: String, size: TextUnit, color: Color, centered: Boolean = false) {
    Text(
        text = chemFormula(raw),
        fontSize = size,
        color = color,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
    )
}

// ------------------------------------------------------------------ parsing

private val BOND_CHARS = setOf('-', '\u2212', '=', '≡')
private val STRUCTURE_TOKEN = Regex("[A-Za-z0-9()\\[\\]]+|[-−=≡]")
private val ARROWS = listOf("⇌", "↔", "→", "<->", "<=>", "-->", "->", "=>")

/** A line is a structure when it is groups joined by single/double/triple bond markers. */
private val STRUCTURE = Regex(
    "^\\(?\\s*[A-Za-z0-9()\\[\\]]+(\\s*[-−=≡]\\s*[A-Za-z0-9()\\[\\]]+)+\\s*\\)?$",
)

private fun isStructure(line: String): Boolean = STRUCTURE.matches(line.trim())

private fun findArrow(line: String): Pair<Int, String>? {
    var best: Pair<Int, String>? = null
    ARROWS.forEach { token ->
        val index = line.indexOf(token)
        if (index >= 0 && (best == null || index < best!!.first)) best = index to token
    }
    return best
}

private fun arrowGlyph(arrow: String): String = when {
    arrow.contains('⇌') -> "⇌"
    arrow.contains('↔') || arrow.contains("<->") -> "↔"
    arrow.contains("<=") -> "⇌"
    else -> "⟶"
}

private val CHEMX_TEXT = Regex("""\\(?:text|mathrm|mathit|operatorname)\s*\{([^{}]*)\}""")
private val CHEMX_REPLACE = listOf(
    "\\longleftrightarrow" to "↔",
    "\\rightleftharpoons" to "⇌",
    "\\leftrightarrow" to "↔",
    "\\longrightarrow" to "→",
    "\\rightarrow" to "→",
    "\\to" to "→",
    "\\Delta" to "Δ",
    "\\triangle" to "Δ",
    "\\cdot" to "·",
    "\\times" to "×",
    "\\," to " ",
    "\\;" to " ",
    "\\!" to "",
)

internal fun normalizeChem(raw: String): String {
    var text = raw.trim().removePrefix("$$").removeSuffix("$$").trim()
    CHEMX_REPLACE.forEach { (from, to) -> text = text.replace(from, to) }
    text = CHEMX_TEXT.replace(text) { it.groupValues[1] }
    text = text.replace("$", "").replace("\\(", "").replace("\\)", "")
    return text.replace(Regex("\\s+"), " ").trim()
}

/**
 * Typesets a chemistry formula: digits after an element or group become subscripts, `_`/`^`
 * scripts are converted, and charges keep their sign. Leading coefficients stay upright.
 */
internal fun chemFormula(raw: String): AnnotatedString = buildAnnotatedString {
    val text = normalizeChem(raw)
    var index = 0
    var afterGroup = false
    while (index < text.length) {
        val c = text[index]
        when {
            c == '_' || c == '^' -> {
                val superscript = c == '^'
                index++
                val script = StringBuilder()
                if (index < text.length && text[index] == '{') {
                    val end = text.indexOf('}', index + 1)
                    if (end > index) {
                        script.append(text, index + 1, end)
                        index = end + 1
                    } else {
                        index++
                    }
                } else {
                    while (index < text.length && isScriptChar(text[index])) {
                        script.append(text[index])
                        index++
                    }
                }
                append(scripted(script.toString(), superscript))
                afterGroup = true
            }
            c.isDigit() && afterGroup -> {
                append(scripted(c.toString(), superscript = false))
                index++
            }
            else -> {
                append(c)
                afterGroup = c.isLetter() || c == ')' || c == ']'
                index++
            }
        }
    }
}

private fun isScriptChar(c: Char): Boolean =
    c.isLetterOrDigit() || c == '+' || c == '-' || c == '\u2212'

private fun scripted(raw: String, superscript: Boolean): String = buildString {
    raw.forEach { c ->
        val mapped = if (superscript) SUPERSCRIPT[c] else SUBSCRIPT[c]
        append(mapped ?: c)
    }
}

private val SUBSCRIPT = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
    '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '\u2212' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
)

private val SUPERSCRIPT = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '\u2212' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
    'n' to 'ⁿ', 'i' to 'ⁱ',
)