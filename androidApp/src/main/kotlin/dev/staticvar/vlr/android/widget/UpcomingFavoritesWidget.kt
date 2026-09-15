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
import androidx.glance.GlanceTheme
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
      )
      !snapshot.hasFavorites -> EmptyState(
        title = strings.noFavoritesTitle,
        body = strings.noFavoritesBody,
      )
      currentMatches.isEmpty() -> EmptyState(
        title = strings.noUpcomingTitle,
        body = strings.noUpcomingBody,
      )
      else -> {
        val match = currentMatches.first()
        val modifier = GlanceModifier.fillMaxWidth().defaultWeight()
          .clickable(matchActions.getValue(match.id))
        if (small) {
          PrismMatchCard(match, strings, snapshot.spoilersHidden, small = true, modifier = modifier)
        } else {
          PrismMatchCard(match, strings, snapshot.spoilersHidden, small = false, modifier = modifier)
        }
      }
    }
  }
}

@Composable
internal fun PrismMatchCard(
  match: WidgetMatch,
  strings: WidgetStrings,
  spoilersHidden: Boolean,
  small: Boolean,
  modifier: GlanceModifier = GlanceModifier,
  compact: Boolean = false,
) {
  Column(modifier) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
      PrismWidgetText(
        if (match.status == WidgetMatchStatus.LIVE) strings.live else strings.upcoming,
        11, PrismTextTone.Accent, modifier = GlanceModifier.defaultWeight(),
      )
      PrismWidgetText(
        when {
          match.status == WidgetMatchStatus.LIVE -> match.format
          small -> strings.weekday(match.startTimeEpochMillis)
          else -> strings.startTime(match.startTimeEpochMillis)
        },
        11, PrismTextTone.Secondary, align = androidx.glance.text.TextAlign.End,
        modifier = GlanceModifier.width(if (small) 48.dp else 130.dp),
      )
    }
    Column(
      GlanceModifier.fillMaxWidth().defaultWeight(),
      verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
      PrismTeamRow(match.team1, match.score1, match.status, spoilersHidden, strings, small, compact)
      Spacer(GlanceModifier.height(5.dp))
      PrismTeamRow(match.team2, match.score2, match.status, spoilersHidden, strings, small, compact)
    }
    if (compact) return@Column
    if (small) {
      if (match.status != WidgetMatchStatus.LIVE) {
        PrismWidgetText(strings.clockTime(match.startTimeEpochMillis), 12, PrismTextTone.Secondary)
      } else if (spoilersHidden) {
        PrismWidgetText(strings.scoresHidden, 10, PrismTextTone.Secondary)
      }
    } else {
      PrismWidgetText(
        listOf(match.event, match.format, match.stage).filter(String::isNotBlank).joinToString(" · "),
        11, PrismTextTone.Secondary, modifier = GlanceModifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun PrismTeamRow(
  name: String,
  score: Int?,
  status: WidgetMatchStatus,
  spoilersHidden: Boolean,
  strings: WidgetStrings,
  small: Boolean,
  compact: Boolean,
) {
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    PrismWidgetText(name, if (compact) 14 else if (small) 16 else 20, maxLines = 2, modifier = GlanceModifier.defaultWeight())
    Spacer(GlanceModifier.width(8.dp))
    val visible = status == WidgetMatchStatus.LIVE && !spoilersHidden
    PrismWidgetText(
      if (visible) score?.toString() ?: "—" else "—",
      if (compact) 20 else 28, PrismTextTone.Accent, align = androidx.glance.text.TextAlign.End,
      description = if (status == WidgetMatchStatus.LIVE && spoilersHidden) strings.scoresHidden else null,
      modifier = GlanceModifier.width(32.dp),
    )
  }
}

@Composable
private fun EmptyState(title: String, body: String) {
  Column(
    modifier = GlanceModifier.fillMaxSize(),
    verticalAlignment = Alignment.Vertical.CenterVertically,
    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
  ) {
    PrismWidgetText(title, 16, maxLines = 2, modifier = GlanceModifier.fillMaxWidth())
    Spacer(GlanceModifier.height(6.dp))
    PrismWidgetText(body, 12, PrismTextTone.Secondary, maxLines = 3, modifier = GlanceModifier.fillMaxWidth())
  }
}

internal class WidgetStrings(private val context: Context) {
  val title: String = context.getString(R.string.widget_title)
  val notInitializedTitle: String = context.getString(R.string.widget_not_initialized_title)
  val notInitializedBody: String = context.getString(R.string.widget_not_initialized_body)
  val noFavoritesTitle: String = context.getString(R.string.widget_no_favorites_title)
  val noFavoritesBody: String = context.getString(R.string.widget_no_favorites_body)
  val noUpcomingTitle: String = context.getString(R.string.widget_no_upcoming_title)
  val noUpcomingBody: String = context.getString(R.string.widget_no_upcoming_body)
  val upcoming: String = context.getString(R.string.widget_upcoming)
  val live: String = context.getString(R.string.widget_live)
  val scoresHidden: String = context.getString(R.string.widget_scores_hidden)

  fun weekday(epochMillis: Long?): String = epochMillis?.let {
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it))
  } ?: context.getString(R.string.widget_time_tbd)

  fun clockTime(epochMillis: Long?): String = epochMillis?.let {
    android.text.format.DateFormat.getTimeFormat(context).format(Date(it))
  } ?: context.getString(R.string.widget_time_tbd)

  fun startTime(epochMillis: Long?): String =
    if (epochMillis == null) context.getString(R.string.widget_time_tbd)
    else "${weekday(epochMillis)} · ${clockTime(epochMillis)}"
}
