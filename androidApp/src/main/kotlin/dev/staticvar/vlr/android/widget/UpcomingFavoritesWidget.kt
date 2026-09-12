/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class UpcomingFavoritesWidget : FavoriteMatchWidget(small = false)

internal class SmallUpcomingFavoritesWidget : FavoriteMatchWidget(small = true)

internal abstract class FavoriteMatchWidget(private val small: Boolean) : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Single

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snapshot = withContext(Dispatchers.IO) {
      WidgetSnapshotStore.read(context)?.let(::parseWidgetSnapshot)
    }
    val strings = WidgetStrings(context)
    val nowEpochMillis = System.currentTimeMillis()
    val openAppAction = actionStartActivity(Intent(context, MainActivity::class.java))
    val matchActions = snapshot?.matches.orEmpty().associate { match ->
      match.id to actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
          data = Uri.Builder()
            .scheme("vlr")
            .authority("match")
            .appendPath(match.id)
            .build()
          flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
      )
    }
    provideContent {
      GlanceTheme {
        WidgetContent(
          snapshot = snapshot,
          strings = strings,
          nowEpochMillis = nowEpochMillis,
          openAppAction = openAppAction,
          matchActions = matchActions,
          small = small,
        )
      }
    }
  }
}

@Composable
private fun WidgetContent(
  snapshot: WidgetSnapshot?,
  strings: WidgetStrings,
  nowEpochMillis: Long,
  openAppAction: Action,
  matchActions: Map<String, Action>,
  small: Boolean,
) {
  val theme = GlanceTheme.colors
  val currentMatches = snapshot?.matches.orEmpty().filter { match ->
    match.status == WidgetMatchStatus.LIVE ||
      match.status == WidgetMatchStatus.UPCOMING &&
      (match.startTimeEpochMillis == null || match.startTimeEpochMillis > nowEpochMillis)
  }

  Column(
    modifier = GlanceModifier
      .fillMaxSize()
      .appWidgetBackground()
      .cornerRadius(20.dp)
      .background(theme.widgetBackground)
      .clickable(openAppAction)
      .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    when {
      snapshot == null -> EmptyState(
        title = strings.notInitializedTitle,
        body = strings.notInitializedBody,
        theme = theme,
      )
      !snapshot.hasFavorites -> EmptyState(
        title = strings.noFavoritesTitle,
        body = strings.noFavoritesBody,
        theme = theme,
      )
      currentMatches.isEmpty() -> EmptyState(
        title = strings.noUpcomingTitle,
        body = strings.noUpcomingBody,
        theme = theme,
      )
      else -> {
        val match = currentMatches.first()
        val modifier = GlanceModifier.fillMaxWidth().defaultWeight()
          .clickable(matchActions.getValue(match.id))
        if (small) {
          SmallMatch(match, strings, theme, snapshot.spoilersHidden, modifier)
        } else {
          Scoreboard(match, strings, theme, snapshot.spoilersHidden, modifier)
        }
      }
    }
  }
}

@Composable
private fun SmallMatch(
  match: WidgetMatch,
  strings: WidgetStrings,
  theme: ColorProviders,
  spoilersHidden: Boolean,
  modifier: GlanceModifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    verticalAlignment = Alignment.Vertical.CenterVertically,
  ) {
    if (match.status == WidgetMatchStatus.LIVE) {
      WidgetText(strings.live, theme.primary, 11, bold = true)
    }
    Spacer(GlanceModifier.defaultWeight())
    WidgetText(match.team1, theme.onSurface, 14, maxLines = 2)
    if (match.status == WidgetMatchStatus.LIVE && spoilersHidden) {
      HiddenScores(strings, theme)
    } else {
      WidgetText(
        if (match.status == WidgetMatchStatus.LIVE) strings.score(match.score1, match.score2) else strings.versus,
        theme.onSurfaceVariant,
        12,
      )
    }
    WidgetText(match.team2, theme.onSurface, 14, maxLines = 2)
    Spacer(GlanceModifier.defaultWeight())
    if (match.status != WidgetMatchStatus.LIVE) {
      WidgetText(strings.startTime(match.startTimeEpochMillis, multiline = false), theme.onSurfaceVariant, 12)
    }
  }
}

@Composable
private fun Scoreboard(
  match: WidgetMatch,
  strings: WidgetStrings,
  theme: ColorProviders,
  spoilersHidden: Boolean,
  modifier: GlanceModifier,
) {
  Column(modifier = modifier) {
    WidgetText(match.event.ifBlank { strings.title }, theme.primary, 12, align = TextAlign.Start)
    Spacer(GlanceModifier.height(6.dp))
    Separator(theme)
    Row(
      modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
      verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
      ScoreboardTeam(match.team1, match.score1, match.status, spoilersHidden, theme, GlanceModifier.defaultWeight())
      Column(
        modifier = GlanceModifier.width(76.dp).padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
      ) {
        WidgetText(
          if (match.status == WidgetMatchStatus.LIVE) strings.live else strings.startTime(match.startTimeEpochMillis),
          if (match.status == WidgetMatchStatus.LIVE) theme.primary else theme.onSurfaceVariant,
          12,
          maxLines = 2,
        )
        if (match.status == WidgetMatchStatus.LIVE && spoilersHidden) HiddenScores(strings, theme)
      }
      ScoreboardTeam(match.team2, match.score2, match.status, spoilersHidden, theme, GlanceModifier.defaultWeight())
    }
    WidgetText(listOf(match.format, match.stage).filter(String::isNotBlank).joinToString(" · "), theme.onSurfaceVariant, 11)
  }
}

@Composable
private fun ScoreboardTeam(
  name: String,
  score: Int?,
  status: WidgetMatchStatus,
  spoilersHidden: Boolean,
  theme: ColorProviders,
  modifier: GlanceModifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
    WidgetText(if (status == WidgetMatchStatus.LIVE && !spoilersHidden) score?.toString() ?: "-" else "-", theme.onSurface, 32)
    WidgetText(name, theme.onSurface, 14, maxLines = 2)
  }
}

@Composable
private fun HiddenScores(strings: WidgetStrings, theme: ColorProviders) {
  Image(
    provider = ImageProvider(R.drawable.ic_widget_visibility_off),
    contentDescription = strings.scoresHidden,
    modifier = GlanceModifier.size(18.dp),
    colorFilter = ColorFilter.tint(theme.onSurfaceVariant),
  )
}

@Composable
private fun Separator(theme: ColorProviders) {
  Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(theme.outline))
}

@Composable
private fun WidgetText(
  text: String,
  color: ColorProvider,
  size: Int,
  maxLines: Int = 1,
  bold: Boolean = false,
  align: TextAlign = TextAlign.Center,
) {
  Text(
    text = text,
    modifier = GlanceModifier.fillMaxWidth(),
    maxLines = maxLines,
    style = TextStyle(
      color = color,
      fontFamily = FontFamily.SansSerif,
      fontSize = size.sp,
      fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
      textAlign = align,
    ),
  )
}

@Composable
private fun EmptyState(title: String, body: String, theme: ColorProviders) {
  Column(
    modifier = GlanceModifier.fillMaxSize(),
    verticalAlignment = Alignment.Vertical.CenterVertically,
    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
  ) {
    WidgetText(title, theme.onSurface, 16, maxLines = 2, bold = true)
    Spacer(GlanceModifier.height(6.dp))
    WidgetText(body, theme.onSurfaceVariant, 12, maxLines = 3)
  }
}

private class WidgetStrings(private val context: Context) {
  val title: String = context.getString(R.string.widget_title)
  val notInitializedTitle: String = context.getString(R.string.widget_not_initialized_title)
  val notInitializedBody: String = context.getString(R.string.widget_not_initialized_body)
  val noFavoritesTitle: String = context.getString(R.string.widget_no_favorites_title)
  val noFavoritesBody: String = context.getString(R.string.widget_no_favorites_body)
  val noUpcomingTitle: String = context.getString(R.string.widget_no_upcoming_title)
  val noUpcomingBody: String = context.getString(R.string.widget_no_upcoming_body)
  val versus: String = context.getString(R.string.widget_versus)
  val live: String = context.getString(R.string.widget_live)
  val scoresHidden: String = context.getString(R.string.widget_scores_hidden)

  fun score(score1: Int?, score2: Int?): String =
    context.getString(R.string.widget_live_score, score1?.toString() ?: "-", score2?.toString() ?: "-")

  fun startTime(epochMillis: Long?, multiline: Boolean = true): String {
    if (epochMillis == null) return context.getString(R.string.widget_time_tbd)
    val date = Date(epochMillis)
    val day = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
    val time = android.text.format.DateFormat.getTimeFormat(context).format(date)
    return day + (if (multiline) "\n" else ", ") + time
  }
}
