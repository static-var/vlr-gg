/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

enum class WidgetTextKey {
  TeamTitle,
  ChooseTeam,
  NoFavoriteTeams,
  TeamPick,
  TeamFavoriteInstruction,
  TeamGhost,
  LoadingTeam,
  EventTitle,
  ChooseEvent,
  NoFavoriteEvents,
  EventPick,
  EventFavoriteInstruction,
  EventGhost,
  TapToChoose,
  ToUseWidget,
  OpenApp,
  Plays,
  Playing,
  WasDue,
  Live,
  LiveNow,
  ScoresHidden,
  LastPlayed,
  Beat,
  LostTo,
  NoMatchScheduled,
  HiddenNoMatchScheduled,
  SeasonStartsSoon,
  NoMatches,
  MatchNotListed,
  AnyMinute,
  Tbd,
  Recent,
  Maps,
  BeforeThat,
  WinMark,
  LossMark,
  Win,
  Loss,
  Won,
  Lost,
  PlayingNow,
  Next,
  MapNext,
  MapPick,
  Decider,
  WorldwideRank,
  RegionalRank,
  FirstMatch,
  Matchup,
  Starts,
  EndedOn,
  StartsOn,
  NextMatch,
  TeamsNotListed,
  NextNotListed,
  TeamBeatTeam,
  NextAt,
  Champions,
  BeatTeamScore,
  EventEnded,
  WinnerHidden,
  WinnerNotListed,
  PrizePool,
  UpNext,
  Today,
  Earlier,
  FinalStandings,
  GroupHeading,
  Ended,
}

enum class TimeSkeleton { HourMinute, Hour }

enum class DateSkeleton(val pattern: String) {
  Weekday("EEE"),
  MonthDay("MMMd"),
  WeekdayMonthDay("EEEMMMd"),
}

enum class RelativeDay { Yesterday, Today, Tomorrow }

interface WidgetLocale {
  val use24Hour: Boolean
  fun text(key: WidgetTextKey, vararg arguments: String): String

  /** ICU skeletons jm and j use [use24Hour]. */
  fun instant(value: Instant, skeleton: TimeSkeleton, zone: TimeZone): String

  /** Format a calendar date without shifting it to an adjacent day. */
  fun date(value: LocalDate, skeleton: DateSkeleton): String
  fun relativeDay(day: RelativeDay): String

  /** Format a non-negative magnitude with locale digits and no grouping separator. */
  fun integer(value: Long): String
}

internal fun WidgetLocale.integer(value: Int): String = integer(value.toLong())
