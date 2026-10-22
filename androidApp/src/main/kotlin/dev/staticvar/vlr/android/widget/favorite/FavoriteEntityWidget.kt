/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import dev.staticvar.vlr.shared.widget.favorite.WidgetTap
import kotlinx.coroutines.flow.first

internal abstract class FavoriteEntityWidget(val kind: WidgetKind) : GlanceAppWidget() {
  final override val stateDefinition = PreferencesGlanceStateDefinition
  final override val sizeMode = SizeMode.Exact
  final override val previewSizeMode =
    SizeMode.Responsive(setOf(DpSize(280.dp, 136.dp), DpSize(280.dp, 213.dp), DpSize(280.dp, 330.dp)))

  final override suspend fun provideGlance(context: Context, id: GlanceId) {
    val runtime = FavoriteWidgetRuntime.get()
    val initialSelection = FavoriteWidgetBindings.selection(kind, getAppWidgetState(context, stateDefinition, id))
    val initial = runtime.frames(initialSelection).first()
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
    provideContent {
      val selection = FavoriteWidgetBindings.selection(kind, currentState<Preferences>())
      var widget by remember { mutableStateOf(initial) }
      LaunchedEffect(selection) { runtime.frames(selection).collect { widget = it } }
      val environment by FavoriteWidgetRuntime.invalidations.collectAsState()
      val renderContext = remember(environment) {
        context.createConfigurationContext(Configuration(context.applicationContext.resources.configuration))
      }
      CompositionLocalProvider(LocalContext provides renderContext) {
        Surface(GlanceModifier.clickable(actionStartActivity(widget.frame.tap.intent(context, appWidgetId)))) {
          Content(widget.frame, widget.logos)
        }
      }
    }
  }

  final override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val frame = FavoriteWidgetRuntime.get().preview(kind)
    provideContent {
      Surface(GlanceModifier) { Content(frame, WidgetBitmaps(emptyMap())) }
    }
  }

  @Composable
  private fun Surface(modifier: GlanceModifier, content: @Composable () -> Unit) {
    GlanceTheme {
      val background = GlanceModifier.fillMaxSize().appWidgetBackground()
        .background(GlanceTheme.colors.widgetBackground)
      val shape = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background.cornerRadius(android.R.dimen.system_app_widget_background_radius)
      } else {
        background.cornerRadius(20.dp)
      }
      Box(shape.then(modifier)) { content() }
    }
  }

  @Composable
  protected abstract fun Content(frame: FavoriteWidgetFrame, logos: WidgetBitmaps)
}

internal fun WidgetTap.intent(context: Context, appWidgetId: Int): Intent = when (this) {
  WidgetTap.Chooser -> Intent(context, FavoriteWidgetConfigurationActivity::class.java)
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

  WidgetTap.App -> Intent(context, MainActivity::class.java)

  is WidgetTap.Entity -> intent(context)
}

internal fun WidgetTap.Entity.intent(context: Context): Intent {
  val (path, id) = when (this) {
    is WidgetTap.Match -> "match" to id
    is WidgetTap.Event -> "event" to id
    is WidgetTap.Team -> "team" to id
  }
  return Intent(context, MainActivity::class.java).apply {
    data = Uri.Builder().scheme("https").authority("valesports.app").appendPath(path).appendPath(id).build()
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
  }
}
