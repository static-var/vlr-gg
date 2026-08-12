package dev.staticvar.vlr.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.staticvar.vlr.utils.queueWorker
import dev.staticvar.vlr.utils.queueWidgetRefresh
import dev.staticvar.vlr.utils.stopWorker

class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ScoreWidget()

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    context.queueWorker()
    context.queueWidgetRefresh()
  }

  override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
    super.onRestored(context, oldWidgetIds, newWidgetIds)
    context.queueWorker()
    context.queueWidgetRefresh()
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    context.stopWorker()
  }
}
