/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.text.format.DateFormat
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.shared.widget.favorite.DateSkeleton
import dev.staticvar.vlr.shared.widget.favorite.RelativeDay
import dev.staticvar.vlr.shared.widget.favorite.TimeSkeleton
import dev.staticvar.vlr.shared.widget.favorite.WidgetLocale
import dev.staticvar.vlr.shared.widget.favorite.WidgetTextKey
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.SimpleTimeZone
import kotlin.time.Instant
import java.util.TimeZone as JavaTimeZone

internal class AndroidWidgetLocale(
  context: Context,
  override val use24Hour: Boolean = DateFormat.is24HourFormat(context),
) : WidgetLocale {
  private val resources = context.resources
  private val locale: Locale = resources.configuration.locales[0]

  override fun text(key: WidgetTextKey, vararg arguments: String): String {
    val resource = when (key) {
      WidgetTextKey.TeamTitle -> R.string.widget_team_title
      WidgetTextKey.ChooseTeam -> R.string.widget_team_choose_team
      WidgetTextKey.NoFavoriteTeams -> R.string.widget_team_no_favorite_teams
      WidgetTextKey.TeamPick -> R.string.widget_team_pick
      WidgetTextKey.TeamFavoriteInstruction -> R.string.widget_team_favorite_instruction
      WidgetTextKey.TeamGhost -> R.string.widget_team_ghost
      WidgetTextKey.LoadingTeam -> R.string.widget_team_loading_team
      WidgetTextKey.EventTitle -> R.string.widget_event_title
      WidgetTextKey.ChooseEvent -> R.string.widget_event_choose_event
      WidgetTextKey.NoFavoriteEvents -> R.string.widget_event_no_favorite_events
      WidgetTextKey.EventPick -> R.string.widget_event_pick
      WidgetTextKey.EventFavoriteInstruction -> R.string.widget_event_favorite_instruction
      WidgetTextKey.EventGhost -> R.string.widget_event_ghost
      WidgetTextKey.TapToChoose -> R.string.widget_favorite_tap_to_choose
      WidgetTextKey.ToUseWidget -> R.string.widget_favorite_to_use_widget
      WidgetTextKey.OpenApp -> R.string.widget_favorite_open_app
      WidgetTextKey.Plays -> R.string.widget_team_plays
      WidgetTextKey.Playing -> R.string.widget_team_playing
      WidgetTextKey.WasDue -> R.string.widget_favorite_was_due
      WidgetTextKey.Live -> R.string.widget_favorite_live
      WidgetTextKey.LiveNow -> R.string.widget_favorite_live_now
      WidgetTextKey.ScoresHidden -> R.string.widget_favorite_scores_hidden
      WidgetTextKey.LastPlayed -> R.string.widget_team_last_played
      WidgetTextKey.Beat -> R.string.widget_team_beat
      WidgetTextKey.LostTo -> R.string.widget_team_lost_to
      WidgetTextKey.NoMatchScheduled -> R.string.widget_team_no_match_scheduled
      WidgetTextKey.HiddenNoMatchScheduled -> R.string.widget_team_hidden_no_match_scheduled
      WidgetTextKey.SeasonStartsSoon -> R.string.widget_team_season_starts_soon
      WidgetTextKey.NoMatches -> R.string.widget_team_no_matches
      WidgetTextKey.MatchNotListed -> R.string.widget_team_match_not_listed
      WidgetTextKey.AnyMinute -> R.string.widget_favorite_any_minute
      WidgetTextKey.Tbd -> R.string.widget_favorite_tbd
      WidgetTextKey.Recent -> R.string.widget_team_recent
      WidgetTextKey.Maps -> R.string.widget_team_maps
      WidgetTextKey.BeforeThat -> R.string.widget_team_before_that
      WidgetTextKey.WinMark -> R.string.widget_team_win_mark
      WidgetTextKey.LossMark -> R.string.widget_team_loss_mark
      WidgetTextKey.Win -> R.string.widget_team_win
      WidgetTextKey.Loss -> R.string.widget_team_loss
      WidgetTextKey.Won -> R.string.widget_team_won
      WidgetTextKey.Lost -> R.string.widget_team_lost
      WidgetTextKey.PlayingNow -> R.string.widget_team_playing_now
      WidgetTextKey.Next -> R.string.widget_team_next
      WidgetTextKey.MapNext -> R.string.widget_team_map_next
      WidgetTextKey.MapPick -> R.string.widget_team_map_pick
      WidgetTextKey.Decider -> R.string.widget_team_decider
      WidgetTextKey.WorldwideRank -> R.string.widget_team_worldwide_rank
      WidgetTextKey.RegionalRank -> R.string.widget_team_regional_rank
      WidgetTextKey.FirstMatch -> R.string.widget_event_first_match
      WidgetTextKey.Matchup -> R.string.widget_event_matchup
      WidgetTextKey.Starts -> R.string.widget_event_starts
      WidgetTextKey.EndedOn -> R.string.widget_event_ended_on
      WidgetTextKey.StartsOn -> R.string.widget_event_starts_on
      WidgetTextKey.NextMatch -> R.string.widget_event_next_match
      WidgetTextKey.TeamsNotListed -> R.string.widget_event_teams_not_listed
      WidgetTextKey.NextNotListed -> R.string.widget_event_next_not_listed
      WidgetTextKey.TeamBeatTeam -> R.string.widget_event_team_beat_team
      WidgetTextKey.NextAt -> R.string.widget_event_next_at
      WidgetTextKey.Champions -> R.string.widget_event_champions
      WidgetTextKey.BeatTeamScore -> R.string.widget_event_beat_team_score
      WidgetTextKey.EventEnded -> R.string.widget_event_ended_summary
      WidgetTextKey.WinnerHidden -> R.string.widget_event_winner_hidden
      WidgetTextKey.WinnerNotListed -> R.string.widget_event_winner_not_listed
      WidgetTextKey.PrizePool -> R.string.widget_event_prize_pool
      WidgetTextKey.UpNext -> R.string.widget_event_up_next
      WidgetTextKey.Today -> R.string.widget_favorite_today
      WidgetTextKey.Earlier -> R.string.widget_event_earlier
      WidgetTextKey.FinalStandings -> R.string.widget_event_final_standings
      WidgetTextKey.GroupHeading -> R.string.widget_event_group_heading
      WidgetTextKey.Ended -> R.string.widget_event_ended
    }
    return resources.getString(resource, *arguments)
  }

  override fun instant(value: Instant, skeleton: TimeSkeleton, zone: TimeZone): String {
    val pattern = when (skeleton) {
      TimeSkeleton.HourMinute -> if (use24Hour) "Hm" else "hm"
      TimeSkeleton.Hour -> if (use24Hour) "H" else "h"
    }
    val formatter = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, pattern), locale)
    formatter.timeZone = when (zone) {
      is FixedOffsetTimeZone -> SimpleTimeZone(zone.offset.totalSeconds * 1000, zone.id)
      else -> JavaTimeZone.getTimeZone(zone.id)
    }
    return formatter.format(Date(value.toEpochMilliseconds()))
  }

  override fun date(value: LocalDate, skeleton: DateSkeleton): String {
    val utc = JavaTimeZone.getTimeZone("UTC")
    val calendar = GregorianCalendar(utc, locale).apply {
      clear()
      set(value.year, value.month.number - 1, value.day, 12, 0, 0)
    }
    val formatter = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton.pattern), locale)
    formatter.timeZone = utc
    return formatter.format(calendar.time)
  }

  override fun relativeDay(day: RelativeDay): String = resources.getString(
    when (day) {
      RelativeDay.Yesterday -> R.string.widget_favorite_yesterday
      RelativeDay.Today -> R.string.widget_favorite_today
      RelativeDay.Tomorrow -> R.string.widget_favorite_tomorrow
    },
  )

  override fun integer(value: Long): String = NumberFormat.getIntegerInstance(locale).apply {
    isGroupingUsed = false
  }.format(value)
}
