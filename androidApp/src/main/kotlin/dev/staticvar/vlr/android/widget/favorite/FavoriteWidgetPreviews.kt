/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// Runs apart from the runtime work, which is cancelled while no favorite widget is placed.
internal class FavoriteWidgetPreviewWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    publishFavoriteWidgetPreviews(applicationContext)
    return Result.success()
  }
}

// The system keeps generated previews across launches and rate-limits updates, so publish once per install and locale.
private suspend fun publishFavoriteWidgetPreviews(context: Context) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
  val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
  val stamp = "$installed|${context.resources.configuration.locales.toLanguageTags()}"
  val preferences = context.getSharedPreferences("favorite_widget_previews", Context.MODE_PRIVATE)
  val manager = GlanceAppWidgetManager(context)
  for (receiver in listOf(FavoriteTeamWidgetReceiver::class, FavoriteEventWidgetReceiver::class)) {
    val name = receiver.java.name
    if (preferences.getString(name, null) == stamp) continue
    if (manager.setWidgetPreviews(receiver) == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
      preferences.edit { putString(name, stamp) }
    }
  }
}
