package dev.staticvar.vlr.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.e
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {

  private var parentJob = SupervisorJob()
  private val scope = CoroutineScope(parentJob)

  @Inject lateinit var repository: VlrRepository

  override val glanceAppWidget: GlanceAppWidget
    get() = ScoreWidget(repository)

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    e { "onUpdate" }
    isIfJobEndedRestart()
    scope.launch { glanceAppWidget.updateAll(context) }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    e { "onReceive ${intent.action}" }
    e { "onReceive ${intent.data}" }
    isIfJobEndedRestart()
    scope.launch { glanceAppWidget.updateAll(context) }
  }

  override fun onEnabled(context: Context?) {
    super.onEnabled(context)
    e { "onEnabled" }
    isIfJobEndedRestart()
    context?.let { scope.launch { glanceAppWidget.updateAll(context) } }
  }

  override fun onDisabled(context: Context?) {
    super.onDisabled(context)
    e { "onDisabled" }
    scope.cancel()
  }

  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    super.onDeleted(context, appWidgetIds)
    e { "onDeleted" }
    scope.cancel()
  }

  private fun isIfJobEndedRestart() {
    if (parentJob.isCancelled || parentJob.isCompleted) parentJob = SupervisorJob()
  }
}
