package com.enderplusbayzuiship.edupage2.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimetableWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val lessons = readWidgetLessons(context, today).filter { !it.cancelled }
        provideContent {
            GlanceTheme {
                WidgetTap(context = context, target = DeepLinkHelper.TARGET_TIMETABLE) {
                    TimetableContent(
                        context = context,
                        lessons = lessons,
                        tall = LocalSize.current.height.value >= 200,
                    )
                }
            }
        }
    }
}

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

@Composable
private fun TimetableContent(
    context: Context,
    lessons: List<WidgetLesson>,
    tall: Boolean,
) {
    val now = LocalTime.now()
    val upcoming = lessons.filter { it.end == null || it.end.isAfter(now) }
    val visible = (if (upcoming.size >= 2) upcoming else lessons).take(if (tall) 4 else 2)
    WidgetCardShell {
        WidgetHeader(
            iconRes = R.drawable.ic_notif_substitution,
            iconKey = "timetable",
            title = context.getString(R.string.tab_timetable),
            meta = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
            countText = lessons.size.takeIf { it > 0 }?.toString(),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (visible.isEmpty()) {
            WidgetEmptyHint(text = context.getString(R.string.widget_empty_lessons))
        } else {
            visible.forEach { lesson ->
                val isNow = lesson.start != null && lesson.end != null &&
                    !now.isBefore(lesson.start) && now.isBefore(lesson.end)
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isNow) {
                        Text(
                            text = context.getString(R.string.widget_now),
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimary,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.primary)
                                .cornerRadius(50.dp)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                    }
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = lesson.subject,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                        val meta = listOfNotNull(
                            lesson.start?.let { s ->
                                lesson.end?.let { e -> "$s–$e" } ?: s.toString()
                            },
                            lesson.room,
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
}

