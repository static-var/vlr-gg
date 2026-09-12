/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.staticvar.vlr.widget.ScoreWidgetReceiver
import dev.staticvar.vlr.workers.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

internal object LegacyWidgetRefreshScheduler {
  private const val PERIODIC_WORK_NAME = "widget_update"
  private const val IMMEDIATE_WORK_NAME = "all-matches-widget-refresh-now"
  private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

  fun hasInstalledWidgets(context: Context): Boolean = AppWidgetManager.getInstance(context)
    .getAppWidgetIds(ComponentName(context, ScoreWidgetReceiver::class.java)).isNotEmpty()

  fun schedule(context: Context) {
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).setConstraints(network).build(),
    )
  }

  fun refreshNow(context: Context) {
    WorkManager.getInstance(context).enqueueUniqueWork(
      IMMEDIATE_WORK_NAME,
      ExistingWorkPolicy.KEEP,
      OneTimeWorkRequestBuilder<WidgetUpdateWorker>().setConstraints(network).build(),
    )
  }

  fun cancel(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
  }

  fun restore(context: Context) {
    if (hasInstalledWidgets(context)) {
      schedule(context)
      refreshNow(context)
    }
  }
}

class WidgetUpgradeReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) LegacyWidgetRefreshScheduler.restore(context)
  }
}
