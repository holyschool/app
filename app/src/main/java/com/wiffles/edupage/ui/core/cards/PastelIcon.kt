package com.wiffles.edupage.ui.core.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun pastelPair(key: String, darkTheme: Boolean): Pair<Color, Color> {
    val hue = (((key.hashCode() % 360) + 360) % 360).toFloat()
    return if (darkTheme) {
        Pair(
            Color.hsv(hue, 0.45f, 0.32f),
            Color.hsv(hue, 0.35f, 0.92f),
        )
    } else {
        Pair(
            Color.hsv(hue, 0.36f, 0.93f),
            Color.hsv(hue, 0.72f, 0.38f),
        )
    }
}

@Composable
fun PastelIcon(
    icon: ImageVector,
    key: String,
    modifier: Modifier = Modifier,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val (pastel, vibrant) = pastelPair(key, darkTheme)
    Box(
        modifier = modifier
            .size(containerSize)
            .background(color = pastel, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription ?: key,
            modifier = Modifier.size(iconSize),
            tint = vibrant,
        )
    }
}

@Composable
fun PastelInitials(
    text: String,
    modifier: Modifier = Modifier,
    containerSize: Dp = 44.dp,
) {
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val (pastel, vibrant) = pastelPair(text, darkTheme)
    val initials = remember(text) {
        text.trim().split(Regex("\\s+"))
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }
    }
    Box(
        modifier = modifier
            .size(containerSize)
            .background(color = pastel, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = vibrant,
        )
    }
}

