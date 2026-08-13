package dev.staticvar.vlr.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dev.staticvar.vlr.utils.queueWidgetRefresh

class RefreshWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    context.queueWidgetRefresh()
  }
}
