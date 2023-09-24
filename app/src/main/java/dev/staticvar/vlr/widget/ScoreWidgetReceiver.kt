package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.queueWorker
import dev.staticvar.vlr.utils.stopWorker

class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ScoreWidget()

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    e { "onReceive ${intent.action} | ${intent.data}" }
    if (
      intent.data.toString() != "android.appwidget.action.APPWIDGET_DISABLED" ||
      intent.data.toString() != "android.appwidget.action.APPWIDGET_DELETED"
    )
      context.queueWorker()
    else context.stopWorker()
  }
}
