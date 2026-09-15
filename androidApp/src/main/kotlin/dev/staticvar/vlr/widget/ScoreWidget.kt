/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.glance.LocalSize
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.android.widget.LegacyMatchSnapshotStore
import dev.staticvar.vlr.android.widget.WidgetMatch
import dev.staticvar.vlr.android.widget.PrismMatchCard
import dev.staticvar.vlr.android.widget.PrismWidgetText
import dev.staticvar.vlr.android.widget.PrismTextTone
import dev.staticvar.vlr.android.widget.WidgetStrings
import dev.staticvar.vlr.android.widget.WidgetMatchStatus
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

class ScoreWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snapshot = withContext(Dispatchers.IO) { LegacyMatchSnapshotStore.read(context) }
    val spoilersHidden = snapshot?.spoilersHidden == true ||
      GlobalContext.get().get<SpoilerPreferencesRepository>().enabled.value
    val matches = snapshot?.matches.orEmpty().filter {
      it.status == WidgetMatchStatus.LIVE || it.status == WidgetMatchStatus.UPCOMING
    }.sortedWith(compareBy<WidgetMatch> { it.status != WidgetMatchStatus.LIVE }
      .thenBy { it.startTimeEpochMillis ?: Long.MAX_VALUE })
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    val actions = matches.associate { match ->
      match.id to actionStartActivity(Intent(context, MainActivity::class.java).apply {
        data = Uri.Builder().scheme("vlr").authority("match").appendPath(match.id).build()
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      })
    }
    val strings = WidgetStrings(context)
    provideContent {
      GlanceTheme {
        val compact = LocalSize.current.height < 180.dp
        val rowHeight = if (compact) (LocalSize.current.height - 32.dp).coerceAtLeast(76.dp) else 156.dp
        Column(
          modifier = GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(20.dp)
            .background(GlanceTheme.colors.widgetBackground).padding(12.dp),
        ) {
          if (!compact) PrismWidgetText(
            text = context.getString(R.string.legacy_matches_widget_name),
            size = 14,
            tone = PrismTextTone.Accent,
            modifier = GlanceModifier.fillMaxWidth().clickable(openApp).padding(bottom = 8.dp),
          )
          if (matches.isEmpty()) {
            PrismWidgetText(
              size = 14,
              tone = PrismTextTone.Secondary,
              text = context.getString(
                if (snapshot == null) R.string.legacy_matches_widget_loading else R.string.widget_no_upcoming_title,
              ),
              modifier = GlanceModifier.fillMaxSize().clickable(openApp),
            )
          } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
              items(matches) { match ->
                Column(GlanceModifier.fillMaxWidth().padding(vertical = if (compact) 4.dp else 8.dp)) {
                  PrismMatchCard(
                    match = match,
                    strings = strings,
                    spoilersHidden = spoilersHidden,
                    small = LocalSize.current.width < 240.dp,
                    compact = compact,
                    modifier = GlanceModifier.fillMaxWidth().height(rowHeight)
                      .clickable(actions.getValue(match.id)),
                  )
                  Spacer(GlanceModifier.fillMaxWidth().height(1.dp)
                    .background(GlanceTheme.colors.outline))
                }
              }
            }
          }
        }
      }
    }
  }
}
