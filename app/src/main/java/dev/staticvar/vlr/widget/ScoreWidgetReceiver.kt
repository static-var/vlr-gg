package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.queueWorker
import dev.staticvar.vlr.utils.stopWorker
import javax.inject.Inject

@AndroidEntryPoint
class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {

  @Inject lateinit var repository: VlrRepository

  override val glanceAppWidget: GlanceAppWidget
    get() = ScoreWidget(repository)

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    e { "onReceive ${intent.action} | ${intent.data}" }
    if (intent.data.toString() != "android.appwidget.action.APPWIDGET_DISABLED" ||
        intent.data.toString() != "android.appwidget.action.APPWIDGET_DELETED"
    )
      context.queueWorker()
    else context.stopWorker()
  }
}
