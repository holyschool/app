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
import java.time.format.DateTimeFormatter
import java.util.Locale

class CanteenWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (meals, credit) = readWidgetMeals(context, LocalDate.now())
        provideContent {
            GlanceTheme {
                WidgetTap(context = context, target = DeepLinkHelper.TARGET_MEALS) {
                    CanteenContent(
                        context = context,
                        meals = meals,
                        credit = credit,
                        tall = LocalSize.current.height.value >= 200,
                    )
                }
            }
        }
    }
}

class CanteenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CanteenWidget()
}

@Composable
private fun CanteenContent(
    context: Context,
    meals: List<WidgetMeal>,
    credit: String?,
    tall: Boolean,
) {
    WidgetCardShell {
        WidgetHeader(
            iconRes = R.drawable.ic_notif_homework,
            iconKey = "canteen",
            title = context.getString(R.string.tab_meals),
            meta = if (!credit.isNullOrBlank()) {
                context.getString(R.string.widget_credit, credit)
            } else {
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
            },
            countText = meals.size.takeIf { it > 0 }?.toString(),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (meals.isEmpty()) {
            WidgetEmptyHint(text = context.getString(R.string.widget_empty_meals))
        } else {
            meals.take(if (tall) 4 else 2).forEach { meal ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (meal.ordered) "✓" else "·",
                        style = TextStyle(
                            color = if (meal.ordered) GlanceTheme.colors.onPrimary
                            else GlanceTheme.colors.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier
                            .background(
                                if (meal.ordered) GlanceTheme.colors.primary
                                else GlanceTheme.colors.surfaceVariant
                            )
                            .cornerRadius(50.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = meal.name,
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

