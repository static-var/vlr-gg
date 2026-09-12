/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.staticvar.vlr.android.widget.LegacyWidgetRefreshScheduler

class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ScoreWidget()

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    LegacyWidgetRefreshScheduler.schedule(context)
    LegacyWidgetRefreshScheduler.refreshNow(context)
  }

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    LegacyWidgetRefreshScheduler.schedule(context)
    LegacyWidgetRefreshScheduler.refreshNow(context)
  }

  override fun onDisabled(context: Context) {
    LegacyWidgetRefreshScheduler.cancel(context)
    super.onDisabled(context)
  }
}
