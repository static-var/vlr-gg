package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.queueWorker
import javax.inject.Inject

@AndroidEntryPoint
class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {

  @Inject lateinit var repository: VlrRepository

  override val glanceAppWidget: GlanceAppWidget = ScoreWidget(repository)

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    e { "onReceive ${intent.action}" }
    e { "onReceive ${intent.data}" }
    context.queueWorker()
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    WorkManager.getInstance(context).cancelUniqueWork("widget_update")
  }
}
