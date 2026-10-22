/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager

// The system keeps generated previews across launches and rate-limits updates, so publish once per install and locale.
internal suspend fun publishFavoriteWidgetPreviews(context: Context) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
  val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
  val stamp = "$installed|${context.resources.configuration.locales.toLanguageTags()}"
  val preferences = context.getSharedPreferences("favorite_widget_previews", Context.MODE_PRIVATE)
  if (preferences.getString("published", null) == stamp) return
  val manager = GlanceAppWidgetManager(context)
  val published = listOf(FavoriteTeamWidgetReceiver::class, FavoriteEventWidgetReceiver::class).all {
    manager.setWidgetPreviews(it) == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS
  }
  if (published) preferences.edit { putString("published", stamp) }
}
