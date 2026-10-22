/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

internal object EventWidgetMapper {
  fun normalize(profile: EventProfile): EventData {
    val roster = mutableMapOf<String, WidgetTeam>()
    val conflicts = mutableSetOf<String>()
    for (tier in listOf(profile.teams, profile.prizes.mapNotNull { it.team })) {
      for ((key, values) in tier.filter { meaningfulText(it.name) != null }.groupBy { nameKey(it.name) }) {
        if (key in roster) continue
        val ids = values.mapNotNull { meaningfulText(it.id) }.toSet()
        if (ids.size > 1) conflicts += key
        roster[key] = WidgetTeam(
          id = ids.singleOrNull(),
          name = values.first().name.trim(),
          logo = if (ids.size > 1) null else values.firstNotNullOfOrNull { meaningfulText(it.logo) },
        )
      }
    }
    for (row in profile.standings) {
      val name = meaningfulText(row.team) ?: continue
      val key = nameKey(name)
      if (key in conflicts) continue
      val team = roster[key]
      roster[key] =
        team?.copy(logo = team.logo ?: meaningfulText(row.logo))
          ?: WidgetTeam(null, name, logo = meaningfulText(row.logo))
    }
    fun slot(name: String): WidgetParticipant {
      val cleaned = meaningfulText(name) ?: return WidgetParticipant.Undecided
      return WidgetParticipant.Team(roster[nameKey(cleaned)] ?: WidgetTeam(null, cleaned))
    }
    val matches = filterMatches(profile.matches).map { match ->
      val left = match.teams.getOrNull(0)
      val right = match.teams.getOrNull(1)
      val score = if (left?.score != null && right?.score != null && left.score >= 0 && right.score >= 0) {
        WidgetScore(left.score, right.score)
      } else {
        null
      }
      EventMatch(
        fixture = EventFixture(
          match.id,
          WidgetMatchPair(
            left?.let { slot(it.name) } ?: WidgetParticipant.Undecided,
            right?.let { slot(it.name) } ?: WidgetParticipant.Undecided,
          ),
          match.start,
          match.day?.date,
          match.round,
          match.stage,
          null,
        ),
        phase = when (match.phase) {
          RemoteMatchPhase.Upcoming -> EventMatchPhase.Upcoming
          RemoteMatchPhase.Live -> EventMatchPhase.Live(score)
          RemoteMatchPhase.Completed -> EventMatchPhase.Completed(score)
          RemoteMatchPhase.Unknown -> EventMatchPhase.Unknown
        },
      )
    }
    val groups = mutableListOf<EventGroup>()
    for (row in profile.standings) {
      val group = meaningfulText(row.group) ?: continue
      val wins = row.wins ?: continue
      val losses = row.losses ?: continue
      val ties = row.ties ?: 0
      val maps = row.mapDifference ?: continue
      if (wins < 0 || losses < 0 || ties < 0) continue
      val team = slot(row.team).team ?: continue
      val standing = EventStanding(team, wins, losses, ties, maps)
      val index = groups.indexOfFirst { nameKey(it.name) == nameKey(group) }
      if (index < 0) {
        groups += EventGroup(group, listOf(standing))
      } else {
        groups[index] = groups[index].copy(rows = groups[index].rows + standing)
      }
    }
    val prizes = profile.prizes.mapNotNull { prize ->
      val raw = prize.team ?: return@mapNotNull null
      val name = meaningfulText(raw.name) ?: return@mapNotNull null
      val known = roster[nameKey(name)]
      EventPrize(
        prize.position,
        meaningfulText(prize.prize).orEmpty(),
        WidgetTeam(meaningfulText(raw.id) ?: known?.id, name, logo = meaningfulText(raw.logo) ?: known?.logo),
      )
    }
    return EventData(
      event = EventMetadata(
        profile.id,
        profile.title,
        meaningfulText(profile.logo),
        profile.location,
        meaningfulText(profile.prize),
        profile.dates,
        when (profile.status) {
          RemoteEventStatus.Upcoming -> ReportedEventStatus.Upcoming
          RemoteEventStatus.Ongoing -> ReportedEventStatus.Ongoing
          RemoteEventStatus.Paused -> ReportedEventStatus.Paused
          RemoteEventStatus.Completed -> ReportedEventStatus.Completed
          RemoteEventStatus.Unknown -> ReportedEventStatus.Unknown
        },
      ),
      matches = matches,
      groups = groups,
      prizes = prizes,
      detail = null,
    )
  }

  fun enrich(data: EventData, matchId: String, details: MatchDetails?): EventData {
    if (details == null) return data
    val match = data.matches.firstOrNull { it.fixture.id == matchId } ?: return data
    fun enrich(slot: WidgetParticipant): WidgetParticipant {
      val team = slot.team ?: return slot
      val identity = team.id?.takeIf { it.isNotEmpty() }?.let { id -> details.teams.firstOrNull { it.id == id } }
      val detail = identity ?: details.teams.firstOrNull { nameKey(it.name) == nameKey(team.name) }
      return WidgetParticipant.Team(
        team.copy(
          logo = team.logo ?: meaningfulText(detail?.logo),
          tag =
          team.tag ?: meaningfulText(detail?.tag),
        ),
      )
    }
    val pair = WidgetMatchPair(enrich(match.fixture.participants.left), enrich(match.fixture.participants.right))
    val series = WidgetSeriesRules.make(details, pair, WidgetSide.Left)
    return data.copy(
      matches = data.matches.map {
        if (it.fixture.id ==
          matchId
        ) {
          it.copy(fixture = it.fixture.copy(participants = pair, format = series.format))
        } else {
          it
        }
      },
      detail = EventDetail(matchId, series.maps),
    )
  }

  fun filterMatches(matches: List<RemoteEventMatch>): List<RemoteEventMatch> {
    val seen = mutableSetOf<String>()
    return matches.mapNotNull { match ->
      val id = meaningfulText(match.id) ?: return@mapNotNull null
      if (!seen.add(id) || match.teams.size > 2) return@mapNotNull null
      match.copy(id = id)
    }
  }
}
