package com.wiffles.edupage.ui.subjects

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wiffles.edupage.data.SubjectStyle
import com.wiffles.edupage.ui.core.cards.PastelInitials
import com.wiffles.edupage.ui.core.cards.pastelPair

/**
 * Circular avatar used for subjects and homework. Shows [icon] when present,
 * otherwise the label's initials. Falls back to the shared pastel look when no
 * custom [colorArgb] is set.
 */
@Composable
fun StyledAvatar(
    label: String,
    icon: ImageVector?,
    colorArgb: Int?,
    modifier: Modifier = Modifier,
    containerSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    if (icon == null && colorArgb == null) {
        PastelInitials(text = label, modifier = modifier, containerSize = containerSize)
        return
    }

    val darkTheme = isSystemInDarkTheme()
    val (pastel, pastelOn) = pastelPair(label, darkTheme)
    val customColor = colorArgb?.let { Color(it) }
    val container = customColor ?: pastel
    val contentColor = if (colorArgb != null) Color(onColorFor(colorArgb)) else pastelOn

    Box(
        modifier = modifier
            .size(containerSize)
            .background(color = container, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(iconSize),
                tint = contentColor,
            )
        } else {
            Text(
                text = subjectInitials(label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

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
    val colorArgb = if (enabled) style?.colorArgb else null
    StyledAvatar(
        label = subjectName,
        icon = icon,
        colorArgb = colorArgb,
        modifier = modifier,
        containerSize = containerSize,
        iconSize = iconSize,
    )
}
