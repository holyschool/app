package com.wiffles.edupage.ui.core.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wiffles.edupage.ui.util.rememberAppHaptics

@Composable
fun SettingsNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    val haptics = rememberAppHaptics()
    val contentColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        titleColor
    }
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                haptics.virtualKey()
                onClick()
            },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        },
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = icon?.let {
            {
                PastelIcon(icon = it, key = title)
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    )
}

