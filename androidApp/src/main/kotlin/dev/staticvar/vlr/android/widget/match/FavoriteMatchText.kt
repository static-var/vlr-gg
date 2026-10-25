/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.match

import android.content.Context
import android.text.format.DateFormat
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.shared.widget.FavoriteMatchState
import dev.staticvar.vlr.shared.widget.MatchSide
import dev.staticvar.vlr.shared.widget.UpcomingWidgetMatch
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.winner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Everything the favorite match widget draws, already localized. */
internal sealed interface FavoriteMatchCopy {
  val logoSources: List<String> get() = emptyList()

  data class Match(
    val matchId: String,
    val status: FavoriteMatchStatus,
    val event: String,
    val team1: FavoriteMatchTeam,
    val team2: FavoriteMatchTeam,
    val center: FavoriteMatchCenter,
    val footer: String?,
    val next: FavoriteMatchNext?,
  ) : FavoriteMatchCopy {
    override val logoSources: List<String> get() = listOfNotNull(team1.logo.source, team2.logo.source)
  }

  data class Message(val title: String, val caption: String, val captionFirst: Boolean = false) : FavoriteMatchCopy
}

internal data class FavoriteMatchStatus(val text: String, val tone: FavoriteMatchStatusTone)

internal enum class FavoriteMatchStatusTone { Live, Final, Kickoff }

internal data class FavoriteMatchTeam(val name: String, val logo: LogoMark.Named, val tone: FavoriteMatchTeamTone)

internal enum class FavoriteMatchTeamTone { Neutral, Winner, Loser }

internal sealed interface FavoriteMatchCenter {
  data class Score(val team1: String, val team2: String) : FavoriteMatchCenter
  data class Versus(val text: String) : FavoriteMatchCenter

  /** No score to show: a dash, described as hidden when spoiler protection removed it. */
  data class Unknown(val description: String?) : FavoriteMatchCenter
}

internal data class FavoriteMatchNext(val matchId: String, val text: String)

internal class FavoriteMatchText(
  private val context: Context,
  private val use24Hour: Boolean = DateFormat.is24HourFormat(context),
  private val timeZone: TimeZone = TimeZone.getDefault(),
) {
  private val locale: Locale = context.resources.configuration.locales[0]
  private val separator = context.getString(R.string.widget_match_metadata_separator)

  fun copy(state: FavoriteMatchState, spoilersHidden: Boolean, nowEpochMillis: Long): FavoriteMatchCopy = when (state) {
    is FavoriteMatchState.Live -> match(
      state.match,
      FavoriteMatchStatus(context.getString(R.string.widget_live), FavoriteMatchStatusTone.Live),
      spoilersHidden,
      footer = join(mapProgress(state.match), formatLabel(state.match)).ifEmpty { state.match.stage },
      tones = false,
    )

    is FavoriteMatchState.Upcoming -> match(
      state.match,
      FavoriteMatchStatus(kickoff(state.match.startTimeEpochMillis, nowEpochMillis), FavoriteMatchStatusTone.Kickoff),
      spoilersHidden,
      footer = join(state.match.stage, formatLabel(state.match)),
      tones = false,
    )

    is FavoriteMatchState.Completed -> match(
      state.match,
      FavoriteMatchStatus(context.getString(R.string.widget_match_final).uppercase(locale), FavoriteMatchStatusTone.Final),
      spoilersHidden,
      footer = state.match.stage,
      tones = true,
      next = state.next?.let { next ->
        val matchup = context.getString(R.string.widget_event_matchup, next.team1, next.team2)
        FavoriteMatchNext(
          next.id,
          context.getString(R.string.widget_event_next_at, join(matchup, kickoff(next.startTimeEpochMillis, nowEpochMillis))),
        )
      },
    )

    FavoriteMatchState.NeedsApp -> FavoriteMatchCopy.Message(
      context.getString(R.string.widget_favorite_open_app),
      context.getString(R.string.widget_favorite_to_use_widget),
      captionFirst = true,
    )

    FavoriteMatchState.NoFavorites -> FavoriteMatchCopy.Message(
      context.getString(R.string.widget_match_pick_favorites),
      context.getString(R.string.widget_match_pick_favorites_caption),
    )

    FavoriteMatchState.NoMatches -> FavoriteMatchCopy.Message(
      context.getString(R.string.widget_match_no_matches),
      context.getString(R.string.widget_match_no_matches_caption),
    )
  }

  private fun match(
    match: UpcomingWidgetMatch,
    status: FavoriteMatchStatus,
    spoilersHidden: Boolean,
    footer: String,
    tones: Boolean,
    next: FavoriteMatchNext? = null,
  ): FavoriteMatchCopy.Match {
    val winner = match.winner.takeIf { tones }
    fun tone(side: MatchSide) = when (winner) {
      null -> FavoriteMatchTeamTone.Neutral
      side -> FavoriteMatchTeamTone.Winner
      else -> FavoriteMatchTeamTone.Loser
    }
    val score1 = match.score1
    val score2 = match.score2
    val center = when {
      status.tone == FavoriteMatchStatusTone.Kickoff -> FavoriteMatchCenter.Versus(context.getString(R.string.widget_match_versus))
      score1 != null && score2 != null -> FavoriteMatchCenter.Score(integer(score1), integer(score2))
      else -> FavoriteMatchCenter.Unknown(context.getString(R.string.widget_favorite_scores_hidden).takeIf { spoilersHidden })
    }
    return FavoriteMatchCopy.Match(
      matchId = match.id,
      status = status,
      event = match.event,
      team1 = FavoriteMatchTeam(match.team1, logo(match.team1, match.team1Logo), tone(MatchSide.Team1)),
      team2 = FavoriteMatchTeam(match.team2, logo(match.team2, match.team2Logo), tone(MatchSide.Team2)),
      center = center,
      footer = footer.takeIf(String::isNotBlank),
      next = next,
    )
  }

  /** The map in play, counted from the series score; empty while scores are hidden or the format is unknown. */
  private fun mapProgress(match: UpcomingWidgetMatch): String {
    val total = bestOf(match) ?: return ""
    val score1 = match.score1 ?: return ""
    val score2 = match.score2 ?: return ""
    if (total < 2) return ""
    return context.getString(R.string.widget_match_map_progress, minOf(score1 + score2 + 1, total), total)
  }

  private fun formatLabel(match: UpcomingWidgetMatch): String = bestOf(match)?.let { "Bo$it" }.orEmpty()

  private fun bestOf(match: UpcomingWidgetMatch): Int? =
    match.format.uppercase(Locale.ROOT).takeIf { it.startsWith("BO") }?.drop(2)?.toIntOrNull()

  private fun kickoff(start: Long?, nowEpochMillis: Long): String {
    if (start == null) return context.getString(R.string.widget_favorite_tbd)
    val day = when (dayOffset(nowEpochMillis, start)) {
      0 -> context.getString(R.string.widget_favorite_today)
      1 -> context.getString(R.string.widget_favorite_tomorrow)
      else -> format(start, "EEE")
    }
    return "$day ${format(start, if (use24Hour) "Hm" else "hm")}"
  }

  private fun dayOffset(from: Long, to: Long): Int {
    fun day(epochMillis: Long) = Calendar.getInstance(timeZone, locale).apply {
      timeInMillis = epochMillis
      set(Calendar.HOUR_OF_DAY, 12)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return Math.round((day(to) - day(from)) / DayMillis.toDouble()).toInt()
  }

  private fun format(epochMillis: Long, skeleton: String): String =
    SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
      .apply { timeZone = this@FavoriteMatchText.timeZone }
      .format(Date(epochMillis))

  private fun integer(value: Int): String = String.format(locale, "%d", value)

  private fun join(vararg parts: String): String = parts.filter(String::isNotBlank).joinToString(separator)
}

private fun logo(name: String, source: String?): LogoMark.Named {
  val trimmed = name.trim()
  val end = trimmed.offsetByCodePoints(0, minOf(MonogramLength, trimmed.codePointCount(0, trimmed.length)))
  return LogoMark.Named(name, tag = null, source = source, monogram = trimmed.substring(0, end).trimEnd().uppercase())
}

private const val MonogramLength = 3
private const val DayMillis = 24 * 60 * 60 * 1000L
