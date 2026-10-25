/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.match

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dev.staticvar.vlr.android.widget.FavoriteSnapshotWidgetReceiver
import dev.staticvar.vlr.android.widget.WidgetSnapshotStore
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetLogoLoader
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetSurface
import dev.staticvar.vlr.android.widget.favorite.WidgetBitmaps
import dev.staticvar.vlr.android.widget.parseUpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.FavoriteMatchState
import dev.staticvar.vlr.shared.widget.UpcomingWidgetMatch
import dev.staticvar.vlr.shared.widget.favoriteMatchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform

internal class FavoriteMatchWidget : GlanceAppWidget() {
  // Exact sizes let team names ellipsize against the real column width instead of the smallest bucket's.
  override val sizeMode: SizeMode = SizeMode.Exact
  override val previewSizeMode = SizeMode.Responsive(
    setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 180.dp)),
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val loadedRevision = WidgetSnapshotStore.revisions.value
    val initial = load(context)
    provideContent {
      var content by remember { mutableStateOf(initial) }
      // update() on an open session does not run provideGlance again, so reload when the snapshot changes.
      LaunchedEffect(Unit) {
        WidgetSnapshotStore.revisions.filter { it != loadedRevision }.collectLatest { content = load(context) }
      }
      FavoriteWidgetSurface(GlanceModifier) { FavoriteMatchLayout(content.copy, content.logos) }
    }
  }

  private suspend fun load(context: Context): LoadedMatch {
    val json = KoinPlatform.getKoin().get<Json>()
    val snapshot = withContext(Dispatchers.IO) {
      WidgetSnapshotStore.read(context)?.let { parseUpcomingMatchesSnapshot(it, json) }
    }
    val now = System.currentTimeMillis()
    val copy = FavoriteMatchText(context.localized())
      .copy(favoriteMatchState(snapshot, now), snapshot?.spoilersHidden == true, now)
    return LoadedMatch(copy, FavoriteWidgetLogoLoader(context).load(copy.logoSources))
  }

  override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val now = System.currentTimeMillis()
    val copy = FavoriteMatchText(context.localized()).copy(FavoriteMatchState.Live(PreviewMatch), false, now)
    provideContent {
      FavoriteWidgetSurface(GlanceModifier) { FavoriteMatchLayout(copy, WidgetBitmaps(emptyMap())) }
    }
  }
}

private data class LoadedMatch(val copy: FavoriteMatchCopy, val logos: WidgetBitmaps)

internal class FavoriteMatchWidgetReceiver : FavoriteSnapshotWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = FavoriteMatchWidget()
}

private fun Context.localized(): Context = createConfigurationContext(Configuration(applicationContext.resources.configuration))

private val PreviewMatch = UpcomingWidgetMatch(
  id = "preview",
  event = "Masters London",
  team1 = "Sentinels",
  team2 = "FNATIC",
  status = "LIVE",
  score1 = 1,
  score2 = 1,
  format = "BO3",
  stage = "Upper final",
)
