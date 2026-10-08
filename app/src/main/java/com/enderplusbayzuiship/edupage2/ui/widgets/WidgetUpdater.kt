package com.enderplusbayzuiship.edupage2.ui.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(TimetableWidget::class.java).forEach { id ->
                TimetableWidget().update(context, id)
            }
            manager.getGlanceIds(CanteenWidget::class.java).forEach { id ->
                CanteenWidget().update(context, id)
            }
            manager.getGlanceIds(HomeworkWidget::class.java).forEach { id ->
                HomeworkWidget().update(context, id)
            }
            manager.getGlanceIds(PrepareWidget::class.java).forEach { id ->
                PrepareWidget().update(context, id)
            }
        }
    }

    suspend fun setPrepareState(context: Context, dateIso: String, total: Int) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(PrepareWidget::class.java).forEach { id ->
                updateAppWidgetState(
                    context,
                    PreferencesGlanceStateDefinition,
                    id,
                ) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[PrepareWidget.TotalKey] = total
                        this[PrepareWidget.DateKey] = dateIso
                    }
                }
                PrepareWidget().update(context, id)
            }
        }
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "edupage_widget_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.refreshAll(applicationContext)
        return Result.success()
    }
}

