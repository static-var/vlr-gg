package dev.staticvar.vlr.utils

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.staticvar.vlr.widget.ScoreWidget
import dev.staticvar.vlr.workers.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

suspend fun Context.areWidgetsEnabled() =
  GlanceAppWidgetManager(this).getGlanceIds(ScoreWidget::class.java).isNotEmpty()

fun Context.queueWorker() {
  val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
  val work =
    PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
      .setConstraints(constraints)
      .build()
  WorkManager.getInstance(this)
    .enqueueUniquePeriodicWork("widget_update", ExistingPeriodicWorkPolicy.UPDATE, work)
}

fun Context.stopWorker() {
  WorkManager.getInstance(this).cancelUniqueWork("widget_update")
}
