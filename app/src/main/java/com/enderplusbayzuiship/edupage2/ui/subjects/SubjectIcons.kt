package com.enderplusbayzuiship.edupage2.ui.subjects

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb

data class SubjectIconOption(
    val key: String,
    val vector: ImageVector,
)

/**
 * Preset icons a user can assign to a subject. Keys are stable and persisted in
 * [com.enderplusbayzuiship.edupage2.data.SubjectStyleStore].
 */
object SubjectIconCatalog {
    val icons: List<SubjectIconOption> = listOf(
        SubjectIconOption("book", Icons.Rounded.MenuBook),
        SubjectIconOption("language", Icons.Rounded.Language),
        SubjectIconOption("math", Icons.Rounded.Calculate),
        SubjectIconOption("science", Icons.Rounded.Science),
        SubjectIconOption("physics", Icons.Rounded.Bolt),
        SubjectIconOption("biology", Icons.Rounded.Biotech),
        SubjectIconOption("history", Icons.Rounded.HistoryEdu),
        SubjectIconOption("geography", Icons.Rounded.Public),
        SubjectIconOption("art", Icons.Rounded.Palette),
        SubjectIconOption("music", Icons.Rounded.MusicNote),
        SubjectIconOption("sport", Icons.Rounded.SportsSoccer),
        SubjectIconOption("fitness", Icons.Rounded.FitnessCenter),
        SubjectIconOption("computer", Icons.Rounded.Computer),
        SubjectIconOption("code", Icons.Rounded.Code),
        SubjectIconOption("economics", Icons.Rounded.AccountBalance),
        SubjectIconOption("technical", Icons.Rounded.Architecture),
        SubjectIconOption("health", Icons.Rounded.MedicalServices),
        SubjectIconOption("religion", Icons.Rounded.Church),
        SubjectIconOption("agriculture", Icons.Rounded.Agriculture),
        SubjectIconOption("drama", Icons.Rounded.TheaterComedy),
        SubjectIconOption("law", Icons.Rounded.Gavel),
        SubjectIconOption("psychology", Icons.Rounded.Psychology),
        SubjectIconOption("ideas", Icons.Rounded.Lightbulb),
        SubjectIconOption("design", Icons.Rounded.Brush),
    )

    fun byKey(key: String?): SubjectIconOption? =
        key?.let { k -> icons.firstOrNull { it.key == k } }

    fun vectorFor(key: String?): ImageVector? = byKey(key)?.vector
}

/** Palette offered when customizing a subject color. */
val subjectColorPalette: List<Color> = listOf(
    Color(0xFFE53935), // red
    Color(0xFFF4511E), // deep orange
    Color(0xFFFB8C00), // orange
    Color(0xFFFDD835), // yellow
    Color(0xFF7CB342), // light green
    Color(0xFF43A047), // green
    Color(0xFF00897B), // teal
    Color(0xFF00ACC1), // cyan
    Color(0xFF1E88E5), // blue
    Color(0xFF3949AB), // indigo
    Color(0xFF8E24AA), // purple
    Color(0xFFD81B60), // pink
    Color(0xFF6D4C41), // brown
    Color(0xFF546E7A), // blue grey
)

val subjectColorPaletteArgb: List<Int> = subjectColorPalette.map { it.toArgb() }

/** First letters of up to two words, e.g. "Anglický jazyk" -> "AJ". */
fun subjectInitials(text: String): String =
    text.trim().split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

/** Returns black or white depending on the background luminance, for readable contrast. */
fun onColorFor(backgroundArgb: Int): Int =
    if (androidx.compose.ui.graphics.Color(backgroundArgb).luminance() > 0.5f) {
        0xFF1B1B1B.toInt()
    } else {
        0xFFFFFFFF.toInt()
    }

/** Light pastel color derived from the subject name (matches the in-app default). */
fun defaultSubjectColorArgb(name: String): Int {
    val hue = (((name.hashCode() % 360) + 360) % 360).toFloat()
    return Color.hsv(hue, 0.36f, 0.93f).toArgb()
}

