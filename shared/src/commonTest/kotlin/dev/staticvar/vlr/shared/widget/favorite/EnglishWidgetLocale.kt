/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal class EnglishWidgetLocale(override val use24Hour: Boolean = false) : WidgetLocale {
  override fun text(key: WidgetTextKey, vararg arguments: String): String {
    val format = when (key) {
      WidgetTextKey.TeamTitle -> "Team"
      WidgetTextKey.ChooseTeam -> "Choose a team"
      WidgetTextKey.NoFavoriteTeams -> "No favorite teams"
      WidgetTextKey.TeamPick -> "Pick a team from your favorites"
      WidgetTextKey.TeamFavoriteInstruction -> "Favorite a team, then pick it here"
      WidgetTextKey.TeamGhost -> "Recent results show here"
      WidgetTextKey.LoadingTeam -> "Loading team…"
      WidgetTextKey.EventTitle -> "Event"
      WidgetTextKey.ChooseEvent -> "Choose an event"
      WidgetTextKey.NoFavoriteEvents -> "No favorite events"
      WidgetTextKey.EventPick -> "Pick an event from your favorites"
      WidgetTextKey.EventFavoriteInstruction -> "Favorite an event, then pick it here"
      WidgetTextKey.EventGhost -> "Matches show here"
      WidgetTextKey.TapToChoose -> "Tap to choose"
      WidgetTextKey.ToUseWidget -> "To use this widget,"
      WidgetTextKey.OpenApp -> "Open Val Esports"
      WidgetTextKey.Plays -> "Plays {0}"
      WidgetTextKey.Playing -> "Playing {0}"
      WidgetTextKey.WasDue -> "Was due {0}"
      WidgetTextKey.Live -> "Live"
      WidgetTextKey.LiveNow -> "Live now"
      WidgetTextKey.ScoresHidden -> "Scores hidden"
      WidgetTextKey.LastPlayed -> "Last played {0}"
      WidgetTextKey.Beat -> "Beat {0}"
      WidgetTextKey.LostTo -> "Lost to {0}"
      WidgetTextKey.NoMatchScheduled -> "No match scheduled"
      WidgetTextKey.HiddenNoMatchScheduled -> "Scores hidden · No match scheduled"
      WidgetTextKey.SeasonStartsSoon -> "Season starts soon"
      WidgetTextKey.NoMatches -> "No matches"
      WidgetTextKey.MatchNotListed -> "VLR has not listed a match yet"
      WidgetTextKey.AnyMinute -> "Any minute"
      WidgetTextKey.Tbd -> "TBD"
      WidgetTextKey.Recent -> "Recent"
      WidgetTextKey.Maps -> "Maps"
      WidgetTextKey.BeforeThat -> "Before that"
      WidgetTextKey.WinMark -> "W"
      WidgetTextKey.LossMark -> "L"
      WidgetTextKey.Win -> "Win"
      WidgetTextKey.Loss -> "Loss"
      WidgetTextKey.Won -> "Won"
      WidgetTextKey.Lost -> "Lost"
      WidgetTextKey.PlayingNow -> "Playing now"
      WidgetTextKey.Next -> "Next"
      WidgetTextKey.MapNext -> "{0} next"
      WidgetTextKey.MapPick -> "{0} pick"
      WidgetTextKey.Decider -> "Decider"
      WidgetTextKey.WorldwideRank -> "#{0} worldwide"
      WidgetTextKey.RegionalRank -> "#{0} {1}"
      WidgetTextKey.FirstMatch -> "First match"
      WidgetTextKey.Matchup -> "{0} vs {1}"
      WidgetTextKey.Starts -> "Starts {0}"
      WidgetTextKey.EndedOn -> "Ended {0}"
      WidgetTextKey.StartsOn -> "Starts on"
      WidgetTextKey.NextMatch -> "Next match"
      WidgetTextKey.TeamsNotListed -> "Teams and matches not listed yet"
      WidgetTextKey.NextNotListed -> "VLR has not listed the next match"
      WidgetTextKey.TeamBeatTeam -> "{0} beat {1}"
      WidgetTextKey.NextAt -> "Next: {0}"
      WidgetTextKey.Champions -> "Champions"
      WidgetTextKey.BeatTeamScore -> "Beat {0} {1}"
      WidgetTextKey.EventEnded -> "Event ended"
      WidgetTextKey.WinnerHidden -> "Winner hidden"
      WidgetTextKey.WinnerNotListed -> "Winner not listed"
      WidgetTextKey.PrizePool -> "{0} prize pool"
      WidgetTextKey.UpNext -> "Up next"
      WidgetTextKey.Today -> "Today"
      WidgetTextKey.Earlier -> "Earlier"
      WidgetTextKey.FinalStandings -> "Final standings"
      WidgetTextKey.GroupHeading -> "{0} · W–L · maps"
      WidgetTextKey.Ended -> "Ended"
    }
    return arguments.foldIndexed(format) { index, text, argument -> text.replace("{$index}", argument) }
  }

  override fun instant(value: Instant, skeleton: TimeSkeleton, zone: TimeZone): String {
    val local = value.toLocalDateTime(zone)
    val hour = if (use24Hour) local.hour.toString().padStart(2, '0') else ((local.hour + 11) % 12 + 1).toString()
    val minutes = if (skeleton == TimeSkeleton.HourMinute) ":" + local.minute.toString().padStart(2, '0') else ""
    val period = if (use24Hour) "" else "\u202F" + if (local.hour < 12) "AM" else "PM"
    return hour + minutes + period
  }

  override fun date(value: LocalDate, skeleton: DateSkeleton): String {
    val month = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[
      value.month.number -
        1,
    ]
    val weekday = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[value.dayOfWeek.ordinal]
    return when (skeleton) {
      DateSkeleton.Weekday -> weekday
      DateSkeleton.MonthDay -> "$month ${value.day}"
      DateSkeleton.WeekdayMonthDay -> "$weekday, $month ${value.day}"
    }
  }

  override fun relativeDay(day: RelativeDay): String = when (day) {
    RelativeDay.Yesterday -> "Yesterday"
    RelativeDay.Today -> "Today"
    RelativeDay.Tomorrow -> "Tomorrow"
  }

  override fun integer(value: Long): String = value.toString()
}
