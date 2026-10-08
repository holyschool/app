package com.enderplusbayzuiship.edupage2.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.enderplusbayzuiship.edupage2.data.SubjectStyle
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelInitials
import com.enderplusbayzuiship.edupage2.ui.core.cards.pastelPair

/**
 * Avatar for a subject. Falls back to the pastel initials when the subject-icons
 * feature is disabled or the subject has no icon/color configured.
 */
@Composable
fun SubjectAvatar(
    subjectName: String,
    style: SubjectStyle?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    containerSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    val icon = if (enabled) SubjectIconCatalog.vectorFor(style?.iconKey) else null
    val customColor = if (enabled) style?.colorArgb?.let { Color(it) } else null

    if (icon == null && customColor == null) {
        PastelInitials(text = subjectName, modifier = modifier, containerSize = containerSize)
        return
    }

    val darkTheme = isSystemInDarkTheme()
    val (pastel, pastelOn) = pastelPair(subjectName, darkTheme)
    val container = customColor ?: pastel
    val contentColor = if (customColor != null) {
        Color(onColorFor(customColor.toArgb()))
    } else {
        pastelOn
    }

    Box(
        modifier = modifier
            .size(containerSize)
            .background(color = container, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = subjectName,
                modifier = Modifier.size(iconSize),
                tint = contentColor,
            )
        } else {
            Text(
                text = subjectInitials(subjectName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}
