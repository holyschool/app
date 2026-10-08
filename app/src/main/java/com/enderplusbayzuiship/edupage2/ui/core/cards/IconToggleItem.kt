package com.enderplusbayzuiship.edupage2.ui.core.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

@Composable
fun IconToggleItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
) {
    val haptics = rememberAppHaptics()
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                haptics.virtualKey()
                onCheckedChange(!checked)
            },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
        leadingContent = {
            PastelIcon(icon = icon, key = title)
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        if (enabled) {
                            haptics.virtualKey()
                            onCheckedChange(it)
                        }
                    },
                    enabled = enabled,
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    )
}

