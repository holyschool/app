package com.enderplusbayzuiship.edupage2.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A lightweight, dependency-free LaTeX math renderer for Compose. It supports the
 * subset of constructs an AI tutor typically emits: greek letters, common operators,
 * superscripts/subscripts, fractions, roots, sums and integrals. It is not a full
 * TeX engine but renders inline expressions legibly without any WebView.
 */
@Composable
fun MathFormula(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 17.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val node = remember(latex) { runCatching { MathParser(latex).parseTop() }.getOrNull() }
    if (node == null) {
        Text(text = latex, modifier = modifier, fontSize = fontSize, color = color)
        return
    }
    Box(modifier = modifier) {
        RenderMathNode(node, fontSize, color)
    }
}

private sealed interface MathNode {
    data class Row(val children: List<MathNode>) : MathNode
    data class Sym(val text: String, val italic: Boolean = false) : MathNode
    data class Frac(val num: MathNode, val den: MathNode) : MathNode
    data class Sqrt(val inner: MathNode) : MathNode
    data class Script(val base: MathNode, val sup: MathNode?, val sub: MathNode?) : MathNode
    data class BigOp(val symbol: String, val sub: MathNode?, val sup: MathNode?) : MathNode
}

@Composable
private fun RenderMathNode(node: MathNode, size: TextUnit, color: Color) {
    when (node) {
        is MathNode.Row -> Row(verticalAlignment = Alignment.CenterVertically) {
            node.children.forEach { RenderMathNode(it, size, color) }
        }

        is MathNode.Sym -> Text(
            text = node.text,
            fontSize = size,
            color = color,
            fontStyle = if (node.italic) FontStyle.Italic else FontStyle.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )

        is MathNode.Frac -> Column(
            modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RenderMathNode(node.num, size * 0.92f, color)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                thickness = 1.dp,
                color = color,
            )
            RenderMathNode(node.den, size * 0.92f, color)
        }

        is MathNode.Sqrt -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\u221A",
                fontSize = size * 1.15f,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.width(IntrinsicSize.Max).padding(top = 3.dp),
            ) {
                HorizontalDivider(thickness = 1.dp, color = color)
                RenderMathNode(node.inner, size, color)
            }
        }

        is MathNode.Script -> Row(verticalAlignment = Alignment.CenterVertically) {
            RenderMathNode(node.base, size, color)
            Column {
                node.sup?.let { RenderMathNode(it, size * 0.68f, color) }
                node.sub?.let {
                    Row(verticalAlignment = Alignment.Bottom) {
                        RenderMathNode(it, size * 0.68f, color)
                    }
                }
            }
        }

        is MathNode.BigOp -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            node.sup?.let {
                RenderMathNode(it, size * 0.66f, color)
            }
            Text(
                text = node.symbol,
                fontSize = size * 1.4f,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
            )
            node.sub?.let {
                RenderMathNode(it, size * 0.66f, color)
            }
        }
    }
}

private class MathParser(private val src: String) {
    private var i = 0

    fun parseTop(): MathNode = MathNode.Row(parseSequence())

    private fun parseSequence(): List<MathNode> {
        val nodes = mutableListOf<MathNode>()
        while (i < src.length) {
            val c = src[i]
            when {
                c == '}' -> break
                c == '^' || c == '_' -> {
                    i++
                    val arg = parseGroupOrAtom()
                    val base = nodes.removeLastOrNull() ?: MathNode.Sym("")
                    val existing = base as? MathNode.Script
                    val sup = if (c == '^') arg else existing?.sup
                    val sub = if (c == '_') arg else existing?.sub
                    val target = existing?.base ?: base
                    nodes.add(MathNode.Script(target, sup, sub))
                }
                else -> nodes.add(parseAtom())
            }
        }
        return nodes
    }

    private fun parseGroupOrAtom(): MathNode {
        skipSpaces()
        if (i >= src.length) return MathNode.Sym("")
        return if (src[i] == '{') {
            i++
            val seq = parseSequence()
            if (i < src.length && src[i] == '}') i++
            MathNode.Row(seq)
        } else {
            parseAtom()
        }
    }

    private fun skipSpaces() {
        while (i < src.length && src[i] == ' ') i++
    }

    private fun parseAtom(): MathNode {
        val c = src[i]
        return when {
            c == '\\' -> parseCommand()
            c == '{' -> parseGroupOrAtom()
            c == '~' -> {
                i++
                MathNode.Sym(" ")
            }
            c == ' ' -> {
                i++
                MathNode.Sym(" ")
            }
            else -> {
                i++
                val t = c.toString()
                MathNode.Sym(t, italic = t.length == 1 && t[0].isLetter())
            }
        }
    }

    private fun parseCommand(): MathNode {
        i++ // consume backslash
        val start = i
        while (i < src.length && src[i].isLetter()) i++
        val name = src.substring(start, i)

        return when (name) {
            "frac", "dfrac", "tfrac" -> {
                val num = parseGroupOrAtom()
                val den = parseGroupOrAtom()
                MathNode.Frac(num, den)
            }
            "sqrt" -> MathNode.Sqrt(parseGroupOrAtom())
            "text", "mathrm", "operatorname" -> MathNode.Row(parseTextGroup())
            "left", "right" -> {
                skipSpaces()
                if (i < src.length) {
                    val d = src[i]
                    i++
                    MathNode.Sym(delimiterFor(d))
                } else MathNode.Sym("")
            }
            "sum" -> MathNode.BigOp("\u2211", null, null)
            "prod" -> MathNode.BigOp("\u220F", null, null)
            "int" -> MathNode.BigOp("\u222B", null, null)
            "iint" -> MathNode.BigOp("\u222C", null, null)
            "oint" -> MathNode.BigOp("\u222E", null, null)
            "lim" -> MathNode.Sym("lim")
            "cdot" -> MathNode.Sym("\u00B7")
            "times" -> MathNode.Sym("\u00D7")
            "div" -> MathNode.Sym("\u00F7")
            "pm" -> MathNode.Sym("\u00B1")
            "mp" -> MathNode.Sym("\u2213")
            "le", "leq" -> MathNode.Sym("\u2264")
            "ge", "geq" -> MathNode.Sym("\u2265")
            "neq", "ne" -> MathNode.Sym("\u2260")
            "approx" -> MathNode.Sym("\u2248")
            "equiv" -> MathNode.Sym("\u2261")
            "infty" -> MathNode.Sym("\u221E")
            "to", "rightarrow" -> MathNode.Sym("\u2192")
            "leftarrow" -> MathNode.Sym("\u2190")
            "leftrightarrow" -> MathNode.Sym("\u2194")
            "Rightarrow" -> MathNode.Sym("\u21D2")
            "in" -> MathNode.Sym("\u2208")
            "notin" -> MathNode.Sym("\u2209")
            "subset" -> MathNode.Sym("\u2282")
            "cup" -> MathNode.Sym("\u222A")
            "cap" -> MathNode.Sym("\u2229")
            "forall" -> MathNode.Sym("\u2200")
            "exists" -> MathNode.Sym("\u2203")
            "partial" -> MathNode.Sym("\u2202")
            "nabla" -> MathNode.Sym("\u2207")
            "angle" -> MathNode.Sym("\u2220")
            "degree" -> MathNode.Sym("\u00B0")
            "quad" -> MathNode.Sym("\u2003")
            "qquad" -> MathNode.Sym("\u2003\u2003")
            ",", ";", ":", "!", " " -> MathNode.Sym(" ")
            else -> {
                val greek = GREEK[name]
                when {
                    greek != null -> MathNode.Sym(greek)
                    name.isEmpty() -> MathNode.Sym(if (i < src.length) {
                        val d = src[i].also { i++ }
                        d.toString()
                    } else "")
                    else -> MathNode.Sym(name)
                }
            }
        }
    }

    private fun parseTextGroup(): List<MathNode> {
        skipSpaces()
        if (i >= src.length || src[i] != '{') return emptyList()
        i++
        val sb = StringBuilder()
        var depth = 1
        while (i < src.length && depth > 0) {
            val c = src[i]
            if (c == '{') depth++
            if (c == '}') {
                depth--
                if (depth == 0) { i++; break }
            }
            sb.append(c)
            i++
        }
        return listOf(MathNode.Sym(sb.toString()))
    }

    private fun delimiterFor(c: Char): String = when (c) {
        '.' -> ""
        '{' -> "{"
        '}' -> "}"
        else -> c.toString()
    }

    companion object {
        private val GREEK = mapOf(
            "alpha" to "\u03B1", "beta" to "\u03B2", "gamma" to "\u03B3",
            "delta" to "\u03B4", "epsilon" to "\u03B5", "varepsilon" to "\u03F5",
            "zeta" to "\u03B6", "eta" to "\u03B7", "theta" to "\u03B8",
            "iota" to "\u03B9", "kappa" to "\u03BA", "lambda" to "\u03BB",
            "mu" to "\u03BC", "nu" to "\u03BD", "xi" to "\u03BE",
            "pi" to "\u03C0", "rho" to "\u03C1", "sigma" to "\u03C3",
            "tau" to "\u03C4", "upsilon" to "\u03C5", "phi" to "\u03C6",
            "varphi" to "\u03D5", "chi" to "\u03C7", "psi" to "\u03C8",
            "omega" to "\u03C9",
            "Gamma" to "\u0393", "Delta" to "\u0394", "Theta" to "\u0398",
            "Lambda" to "\u039B", "Xi" to "\u039E", "Pi" to "\u03A0",
            "Sigma" to "\u03A3", "Phi" to "\u03A6", "Psi" to "\u03A8",
            "Omega" to "\u03A9",
        )
    }
}