package dev.staticvar.vlr.utils

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.staticvar.vlr.widget.ScoreWidget
import dev.staticvar.vlr.workers.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

private const val WIDGET_PERIODIC_WORK = "widget_update"
private const val WIDGET_REFRESH_WORK = "widget_refresh"
internal val widgetLastUpdateMillisKey = longPreferencesKey("last_widget_update_millis")

suspend fun Context.areWidgetsEnabled() =
  GlanceAppWidgetManager(this).getGlanceIds(ScoreWidget::class.java).isNotEmpty()

fun Context.queueWorker() {
  val work =
    PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
      .setConstraints(widgetNetworkConstraints())
      .build()
  WorkManager.getInstance(this)
    .enqueueUniquePeriodicWork(
      WIDGET_PERIODIC_WORK,
      ExistingPeriodicWorkPolicy.UPDATE,
      work,
    )
}

fun Context.queueWidgetRefresh() {
  val work =
    OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
      .setConstraints(widgetNetworkConstraints())
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .build()
  WorkManager.getInstance(this)
    .enqueueUniqueWork(WIDGET_REFRESH_WORK, ExistingWorkPolicy.REPLACE, work)
}

fun Context.stopWorker() {
  WorkManager.getInstance(this).apply {
    cancelUniqueWork(WIDGET_PERIODIC_WORK)
    cancelUniqueWork(WIDGET_REFRESH_WORK)
  }
}

internal suspend fun Context.recordWidgetUpdateMillis(updateMillis: Long) {
  GlanceAppWidgetManager(this).getGlanceIds(ScoreWidget::class.java).forEach { glanceId ->
    updateAppWidgetState(this, glanceId) { preferences ->
      preferences[widgetLastUpdateMillisKey] = updateMillis
    }
  }
}

private fun widgetNetworkConstraints() =
  Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
