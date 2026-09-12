/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll

internal class UpcomingFavoritesWidgetReceiver : FavoriteMatchWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = UpcomingFavoritesWidget()
}

internal class SmallUpcomingFavoritesWidgetReceiver : FavoriteMatchWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SmallUpcomingFavoritesWidget()
}

internal abstract class FavoriteMatchWidgetReceiver : GlanceAppWidgetReceiver() {

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    FavoriteWidgetRefreshScheduler.schedule(context)
    FavoriteWidgetRefreshScheduler.refreshNow(context)
  }

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    FavoriteWidgetRefreshScheduler.schedule(context)
    FavoriteWidgetRefreshScheduler.refreshNow(context)
  }

  override fun onDisabled(context: Context) {
    if (!FavoriteMatchWidgets.hasInstalledWidgets(context)) {
      FavoriteWidgetRefreshScheduler.cancel(context)
    }
    super.onDisabled(context)
  }
}

internal object FavoriteMatchWidgets {
  suspend fun updateAll(context: Context) {
    UpcomingFavoritesWidget().updateAll(context)
    SmallUpcomingFavoritesWidget().updateAll(context)
  }

  fun hasInstalledWidgets(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    return listOf(
      UpcomingFavoritesWidgetReceiver::class.java,
      SmallUpcomingFavoritesWidgetReceiver::class.java,
    ).any { receiver -> manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty() }
  }
}
