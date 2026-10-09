package com.wiffles.edupage.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wiffles.edupage.MainActivity
import com.wiffles.edupage.notification.DeepLinkHelper

@Composable
fun WidgetTap(
    context: Context,
    target: String,
    content: @Composable () -> Unit,
) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(DeepLinkHelper.EXTRA_DEEP_LINK_TARGET, target)
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(intent)),
    ) {
        content()
    }
}

private fun tonalContainer(key: String, colors: GlanceThemeColors): ColorProvider {
    val bucket = (key.hashCode() and Int.MAX_VALUE) % 3
    return when (bucket) {
        0 -> colors.primaryContainer
        1 -> colors.secondaryContainer
        else -> colors.tertiaryContainer
    }
}

private fun tonalOnContainer(key: String, colors: GlanceThemeColors): ColorProvider {
    val bucket = (key.hashCode() and Int.MAX_VALUE) % 3
    return when (bucket) {
        0 -> colors.onPrimaryContainer
        1 -> colors.onSecondaryContainer
        else -> colors.onTertiaryContainer
    }
}

private typealias GlanceThemeColors = androidx.glance.color.ColorProviders

@Composable
fun WidgetCardShell(content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
fun WidgetHeader(
    @DrawableRes iconRes: Int,
    iconKey: String,
    title: String,
    meta: String? = null,
    countText: String? = null,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .background(tonalContainer(iconKey, GlanceTheme.colors))
                .cornerRadius(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tonalOnContainer(iconKey, GlanceTheme.colors)),
                modifier = GlanceModifier.size(22.dp),
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            if (meta != null) {
                Text(
                    text = meta,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1,
                )
            }
        }
        if (countText != null) {
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = countText,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(50.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun WidgetProgressBar(progress: Float, barWidth: androidx.compose.ui.unit.Dp) {
    val fraction = progress.coerceIn(0f, 1f)
    val safeWidth = barWidth.coerceAtLeast(0.dp)
    Box(
        modifier = GlanceModifier
            .width(safeWidth)
            .height(6.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(50.dp),
    ) {
        Box(
            modifier = GlanceModifier
                .width((safeWidth.value * fraction).dp)
                .fillMaxHeight()
                .background(GlanceTheme.colors.primary)
                .cornerRadius(50.dp),
        ) {}
    }
}

@Composable
fun WidgetEmptyHint(text: String) {
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        maxLines = 2,
        modifier = GlanceModifier.padding(vertical = 4.dp),
    )
}

