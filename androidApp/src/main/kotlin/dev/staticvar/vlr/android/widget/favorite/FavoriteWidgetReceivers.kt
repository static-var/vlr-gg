/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class FavoriteEntityWidgetReceiver : GlanceAppWidgetReceiver() {
  abstract override val glanceAppWidget: FavoriteEntityWidget

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    requestFavoriteWidgetUpdate(context)
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    requestFavoriteWidgetUpdate(context)
  }

  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    super.onDeleted(context, appWidgetIds)
    requestFavoriteWidgetUpdate(context)
  }

  override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
    super.onRestored(context, oldWidgetIds, newWidgetIds)
    WorkManager.getInstance(context).enqueueUniqueWork(
      "favorite-entity-widgets-restore",
      ExistingWorkPolicy.APPEND_OR_REPLACE,
      OneTimeWorkRequestBuilder<FavoriteEntityRestoreWorker>()
        .setInputData(workDataOf("old" to oldWidgetIds, "new" to newWidgetIds))
        .build(),
    )
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    requestFavoriteWidgetUpdate(context)
  }
}

// Each provider gets its own restore broadcast, so the first one keeps the restored selections for both.
internal object FavoriteWidgetRestore {
  private val mutex = Mutex()
  private var selections: Map<Int, String>? = null

  suspend fun apply(context: Context, oldIds: IntArray, newIds: IntArray) = mutex.withLock {
    val bindings = FavoriteWidgetBindings(context)
    val sources = selections ?: bindings.restoredSelections().also { selections = it }
    bindings.restore(oldIds, newIds, sources)
  }
}

class FavoriteWidgetEnvironmentReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action in setOf(
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_LOCALE_CHANGED,
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
      )
    ) {
      requestFavoriteWidgetUpdate(context)
    }
  }
}

internal fun requestFavoriteWidgetUpdate(context: Context) {
  FavoriteWidgetRuntime.invalidate()
  WorkManager.getInstance(context).enqueueUniqueWork(
    "favorite-entity-widgets-reconcile-now",
    ExistingWorkPolicy.REPLACE,
    OneTimeWorkRequestBuilder<FavoriteEntityReconcileWorker>().addTag(FavoriteWidgetScheduler.RuntimeTag).build(),
  )
}
