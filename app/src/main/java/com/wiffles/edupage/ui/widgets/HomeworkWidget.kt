package com.wiffles.edupage.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wiffles.edupage.R
import com.wiffles.edupage.notification.DeepLinkHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeworkWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = readWidgetHomework(context)
        provideContent {
            GlanceTheme {
                WidgetTap(context = context, target = DeepLinkHelper.TARGET_HOMEWORK) {
                    HomeworkContent(
                        context = context,
                        items = items,
                        tall = LocalSize.current.height.value >= 200,
                    )
                }
            }
        }
    }
}

class HomeworkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeworkWidget()
}

@Composable
private fun HomeworkContent(
    context: Context,
    items: List<WidgetHomework>,
    tall: Boolean,
) {
    WidgetCardShell {
        WidgetHeader(
            iconRes = R.drawable.ic_notif_message,
            iconKey = "homework",
            title = context.getString(R.string.widget_homework_title),
            meta = context.getString(R.string.widget_homework_desc),
            countText = items.size.takeIf { it > 0 }?.toString(),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (items.isEmpty()) {
            WidgetEmptyHint(text = context.getString(R.string.widget_empty_homework))
        } else {
            items.take(if (tall) 4 else 2).forEach { item ->
                Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = item.title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    val meta = listOfNotNull(
                        item.subject.takeIf { it.isNotBlank() },
                        runCatching {
                            LocalDate.parse(item.date)
                                .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
                        }.getOrNull(),
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

