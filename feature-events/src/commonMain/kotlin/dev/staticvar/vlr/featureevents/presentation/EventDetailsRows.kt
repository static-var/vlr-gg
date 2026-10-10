/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.core.settings.EventDetailsPreferences
import dev.staticvar.vlr.core.settings.EventDetailsSection
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventTeam
import kotlinx.datetime.LocalDate

/** Participants show at most this many teams in their rail. */
internal const val ParticipantRailLimit: Int = 8

/** Sections this event has content for. Matches always shows, with an empty state before fixtures exist. */
internal val EventDetails.availableSections: Set<EventDetailsSection>
  get() = buildSet {
    if (teams.isNotEmpty()) add(EventDetailsSection.Participants)
    if (prizes.isNotEmpty()) add(EventDetailsSection.Prizes)
    if (standings.isNotEmpty()) add(EventDetailsSection.Standings)
    add(EventDetailsSection.Matches)
  }

/** Available sections in the user's order, including hidden ones, for the options sheet. */
internal fun EventDetailsPreferences.availableSectionsInOrder(event: EventDetails): List<EventDetailsSection> {
  val available = event.availableSections
  return orderedSections.filter { it in available }
}

/** A group of standings rows in VLR's ranked order. A null [name] means the server sent no group. */
internal data class EventStandingsGroup(val name: String?, val standings: List<EventStanding>)

internal fun List<EventStanding>.groupedStandings(): List<EventStandingsGroup> =
  groupBy { it.groupName?.trim()?.takeIf(String::isNotEmpty) }
    .map { (name, standings) -> EventStandingsGroup(name, standings) }

/** One item in the event details list below the hero. Keys stay stable across reorders and refreshes. */
internal sealed interface EventDetailsRow {
  val key: String
  val contentType: String

  data class Participants(val teams: List<EventTeam>) : EventDetailsRow {
    override val key: String = "participants"
    override val contentType: String = "participants"
  }

  data class Prizes(val prizes: List<EventPrize>) : EventDetailsRow {
    override val key: String = "prizes"
    override val contentType: String = "prizes"
  }

  data object StandingsTitle : EventDetailsRow {
    override val key: String = "standings-title"
    override val contentType: String = "section-title"
  }

  data object StandingsHidden : EventDetailsRow {
    override val key: String = "standings-hidden"
    override val contentType: String = "spoiler-notice"
  }

  data class StandingsTable(val index: Int, val group: EventStandingsGroup) : EventDetailsRow {
    override val key: String = "standings-$index"
    override val contentType: String = "standings-table"
  }

  data object MatchesTitle : EventDetailsRow {
    override val key: String = "matches-title"
    override val contentType: String = "matches-title"
  }

  data object MatchesEmpty : EventDetailsRow {
    override val key: String = "matches-empty"
    override val contentType: String = "matches-empty"
  }

  data class MatchDay(val date: LocalDate?) : EventDetailsRow {
    override val key: String = "match-day-${date ?: "unscheduled"}"
    override val contentType: String = "match-day"
  }

  data class Match(val match: ScheduledEventMatch, override val key: String) : EventDetailsRow {
    override val contentType: String = "match"
  }

  data object BottomSpacer : EventDetailsRow {
    override val key: String = "bottom-spacer"
    override val contentType: String = "bottom-spacer"
  }
}

/** Builds the list below the hero from the user's section order and visibility. */
internal fun buildEventDetailsRows(
  event: EventDetails,
  preferences: EventDetailsPreferences,
  matchDays: List<EventMatchDay>,
  spoilersHidden: Boolean,
): List<EventDetailsRow> = buildList {
  val available = event.availableSections
  preferences.orderedSections
    .filter { section -> section in available && preferences.isVisible(section) }
    .forEach { section ->
      when (section) {
        EventDetailsSection.Participants -> add(EventDetailsRow.Participants(event.teams.take(ParticipantRailLimit)))
        EventDetailsSection.Prizes -> add(EventDetailsRow.Prizes(event.prizes))
        EventDetailsSection.Standings -> {
          add(EventDetailsRow.StandingsTitle)
          if (spoilersHidden) {
            add(EventDetailsRow.StandingsHidden)
          } else {
            event.standings.groupedStandings().forEachIndexed { index, group ->
              add(EventDetailsRow.StandingsTable(index, group))
            }
          }
        }

        EventDetailsSection.Matches -> {
          add(EventDetailsRow.MatchesTitle)
          if (matchDays.isEmpty()) add(EventDetailsRow.MatchesEmpty)
          val usedKeys = mutableSetOf<String>()
          matchDays.forEach { day ->
            add(EventDetailsRow.MatchDay(day.date))
            day.matches.forEach { scheduled ->
              // Match ids should be unique, but a duplicated row must not crash the list.
              val baseKey = "match-${scheduled.match.matchId}"
              var key = baseKey
              var copy = 1
              while (!usedKeys.add(key)) key = "$baseKey-${copy++}"
              add(EventDetailsRow.Match(scheduled, key))
            }
          }
        }
      }
    }
  add(EventDetailsRow.BottomSpacer)
}

/**
 * Index of today's date heading among the rows, or null when no match falls on [today]. The list
 * places the hero first, so callers add one to scroll there.
 */
internal fun List<EventDetailsRow>.todayHeadingIndex(today: LocalDate): Int? =
  indexOfFirst { row -> row is EventDetailsRow.MatchDay && row.date == today }.takeIf { it >= 0 }
