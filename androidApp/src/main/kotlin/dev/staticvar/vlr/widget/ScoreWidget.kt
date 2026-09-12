/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.android.widget.LegacyMatchSnapshotStore
import dev.staticvar.vlr.android.widget.WidgetMatch
import dev.staticvar.vlr.android.widget.WidgetMatchStatus
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val times = matches.associate { match ->
      match.id to (match.startTimeEpochMillis?.let { epochMillis ->
        val date = Date(epochMillis)
        SimpleDateFormat("EEE", Locale.getDefault()).format(date) + "\n" +
          android.text.format.DateFormat.getTimeFormat(context).format(date)
      } ?: context.getString(R.string.widget_time_tbd))
    }
    provideContent {
      GlanceTheme {
        Column(
          modifier = GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(20.dp)
            .background(GlanceTheme.colors.widgetBackground).padding(12.dp),
        ) {
          Text(
            text = context.getString(R.string.legacy_matches_widget_name),
            modifier = GlanceModifier.fillMaxWidth().clickable(openApp).padding(bottom = 8.dp),
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp),
          )
          if (matches.isEmpty()) {
            Text(
              text = context.getString(
                if (snapshot == null) R.string.legacy_matches_widget_loading else R.string.widget_no_upcoming_title,
              ),
              modifier = GlanceModifier.fillMaxSize().clickable(openApp),
              style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
            )
          } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
              items(matches) { match ->
                MatchRow(
                  match = match,
                  time = times.getValue(match.id),
                  liveLabel = context.getString(R.string.widget_live),
                  hiddenLabel = context.getString(R.string.widget_scores_hidden),
                  spoilersHidden = spoilersHidden,
                  openMatch = actions.getValue(match.id),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MatchRow(
  match: WidgetMatch,
  time: String,
  liveLabel: String,
  hiddenLabel: String,
  spoilersHidden: Boolean,
  openMatch: Action,
) {
  val colors = GlanceTheme.colors
  Column(GlanceModifier.fillMaxWidth().clickable(openMatch).padding(vertical = 8.dp)) {
    Text(match.event, style = TextStyle(color = colors.primary, fontSize = 12.sp), maxLines = 1)
    Row(
      modifier = GlanceModifier.fillMaxWidth().padding(vertical = 8.dp),
      verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
      Team(match.team1, match.score1, match.status, spoilersHidden, GlanceModifier.defaultWeight())
      Column(
        modifier = GlanceModifier.width(76.dp).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
      ) {
        Text(
          text = if (match.status == WidgetMatchStatus.LIVE) liveLabel else time,
          style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
          maxLines = 2,
        )
        if (match.status == WidgetMatchStatus.LIVE && spoilersHidden) {
          Image(
            ImageProvider(R.drawable.ic_widget_visibility_off), hiddenLabel,
            modifier = GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant),
          )
        }
      }
      Team(match.team2, match.score2, match.status, spoilersHidden, GlanceModifier.defaultWeight())
    }
    val details = listOf(match.format, match.stage).filter(String::isNotBlank).joinToString(" · ")
    if (details.isNotEmpty()) {
      Text(details, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp), maxLines = 1)
      Spacer(GlanceModifier.height(8.dp))
    }
    Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(colors.outline))
  }
}

@Composable
private fun Team(name: String, score: Int?, status: WidgetMatchStatus, spoilersHidden: Boolean, modifier: GlanceModifier) {
  Column(modifier, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
    Text(
      text = if (status == WidgetMatchStatus.LIVE && !spoilersHidden) score?.toString() ?: "-" else "-",
      style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 28.sp, fontFamily = FontFamily.SansSerif),
    )
    Text(
      name,
      style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, textAlign = TextAlign.Center),
      maxLines = 2,
    )
  }
}
