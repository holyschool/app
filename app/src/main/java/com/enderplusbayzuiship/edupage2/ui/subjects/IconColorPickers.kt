package com.enderplusbayzuiship.edupage2.ui.subjects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.cards.pastelPair
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

/**
 * Horizontal row of circular icon choices with a "None" option. Shared by the
 * subject-icon editor and the homework editor.
 */
@Composable
fun IconPickerRow(
    icons: List<SubjectIconOption>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    noneIcon: ImageVector = Icons.Rounded.Close,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconChoice(
            selected = selectedKey == null,
            onClick = {
                haptics.virtualKey()
                onSelect(null)
            },
        ) {
            Icon(
                imageVector = noneIcon,
                contentDescription = stringResource(R.string.subject_icons_none),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        icons.forEach { option ->
            val isSelected = selectedKey == option.key
            IconChoice(
                selected = isSelected,
                onClick = {
                    haptics.virtualKey()
                    onSelect(if (isSelected) null else option.key)
                },
            ) {
                Icon(
                    imageVector = option.vector,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/**
 * Horizontal row of color choices with a "default pastel" option. The default
 * swatch previews the pastel color derived from [defaultLabel].
 */
@Composable
fun ColorPickerRow(
    selectedArgb: Int?,
    onSelect: (Int?) -> Unit,
    defaultLabel: String,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    val darkTheme = isSystemInDarkTheme()
    val (defaultPastel, defaultOn) = pastelPair(defaultLabel, darkTheme)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ColorChoice(
            selected = selectedArgb == null,
            color = defaultPastel,
            onClick = {
                haptics.virtualKey()
                onSelect(null)
            },
        ) {
            Text(
                text = subjectInitials(defaultLabel).take(1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = defaultOn,
            )
        }
        subjectColorPaletteArgb.forEach { argb ->
            val isSelected = selectedArgb == argb
            ColorChoice(
                selected = isSelected,
                color = Color(argb),
                onClick = {
                    haptics.virtualKey()
                    onSelect(if (isSelected) null else argb)
                },
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(onColorFor(argb)),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconChoice(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceBright
        },
        modifier = Modifier.size(50.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun ColorChoice(
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        modifier = Modifier.size(48.dp),
        border = if (selected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            null
        },
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
