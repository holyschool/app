package com.enderplusbayzuiship.edupage2.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrepareWidget : GlanceAppWidget() {

    companion object {
        val TotalKey = intPreferencesKey("prepare_total")
        val DateKey = stringPreferencesKey("prepare_date")
    }

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val total = currentState(key = TotalKey) ?: -1
            val dateIso = currentState(key = DateKey)
            val targetDate = runCatching {
                if (dateIso != null) LocalDate.parse(dateIso) else LocalDate.now().plusDays(1)
            }.getOrDefault(LocalDate.now().plusDays(1))
            val prepared = readWidgetPrepared(context, targetDate).size
            GlanceTheme {
                WidgetTap(context = context, target = DeepLinkHelper.TARGET_OVERVIEW) {
                    PrepareContent(
                        context = context,
                        targetDate = targetDate,
                        total = total,
                        prepared = prepared,
                    )
                }
            }
        }
    }
}

class PrepareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrepareWidget()
}

@Composable
private fun PrepareContent(
    context: Context,
    targetDate: LocalDate,
    total: Int,
    prepared: Int,
) {
    val done = total >= 0 && prepared >= total
    WidgetCardShell {
        WidgetHeader(
            iconRes = R.drawable.ic_notif_grade,
            iconKey = "prepare",
            title = context.getString(R.string.widget_prepare_title),
            meta = targetDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
            countText = if (total >= 0) "$prepared/$total" else null,
        )
        Spacer(GlanceModifier.height(10.dp))
        if (total < 0) {
            WidgetEmptyHint(text = context.getString(R.string.widget_prepare_tap))
        } else {
            val barWidth = LocalSize.current.width - 28.dp
            WidgetProgressBar(
                progress = if (total == 0) 1f else prepared.coerceAtMost(total).toFloat() / total,
                barWidth = barWidth,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = context.getString(
                    R.string.widget_prepare_progress,
                    prepared.coerceAtMost(total),
                    total,
                ),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

