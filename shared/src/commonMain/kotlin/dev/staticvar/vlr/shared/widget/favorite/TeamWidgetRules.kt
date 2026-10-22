/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal object TeamWidgetRules {
  fun choose(teamId: String, previews: List<MatchPreview>): MatchPreview? = previews
    .filter {
      (it.left.id == teamId || it.right.id == teamId) &&
        it.phase in listOf(RemoteMatchPhase.Live, RemoteMatchPhase.Upcoming)
    }
    .sortedWith(
      compareBy<MatchPreview> { it.phase != RemoteMatchPhase.Live }
        .thenComparator { left, right ->
          when {
            left.start == null && right.start == null -> 0
            left.start == null -> 1
            right.start == null -> -1
            else -> left.start.compareTo(right.start)
          }
        },
    )
    .firstOrNull()

  fun content(profile: TeamProfile, preview: MatchPreview?, details: MatchDetails?): TeamContent {
    val recent = profile.recent.take(5)
    val history = recent.map { row ->
      TeamResult(
        id = row.id,
        day = row.day?.date,
        opponent = row.opponent.widgetTeam(),
        result = if (row.won) ResultKind.Win else ResultKind.Loss,
        score = score(row.teamScore, row.opponentScore),
      )
    }
    val form = List(5 - recent.size) { FormMark.Empty } +
      recent.asReversed().map { if (it.won) FormMark.Win else FormMark.Loss }
    val header = TeamHeader(profile.team.widgetTeam(), profile.rank, profile.region, profile.regionRank)
    if (preview == null) return TeamContent(header, form, history, TeamState.Idle(history.firstOrNull()))
    val ownFirst = preview.left.id == profile.team.id
    val own = if (ownFirst) preview.left else preview.right
    val opponent = if (ownFirst) preview.right else preview.left
    val opponentDetails = details?.teams?.firstOrNull { it.id == opponent.id }
    val opponentTeam = if (!opponent.id.isNullOrEmpty() && !opponent.name.trim().equals("TBD", ignoreCase = true)) {
      opponent.widgetTeam().copy(
        tag = opponentDetails?.tag ?: opponent.tag,
        logo =
        opponentDetails?.logo ?: opponent.logo,
      )
    } else {
      null
    }
    val previewIsFormat = preview.series.replace(" ", "").lowercase() in listOf("bo1", "bo3", "bo5")
    val fixture = TeamFixture(
      id = preview.id,
      opponent = opponentTeam,
      start = preview.start,
      event = preview.event,
      stage = details?.eventStage?.takeIf {
        it.isNotEmpty()
      } ?: preview.series.takeUnless { previewIsFormat }.orEmpty(),
      format = WidgetSeriesRules.format(details, preview.series),
    )
    val state = if (preview.phase == RemoteMatchPhase.Live) {
      val seriesOpponent = opponent.widgetTeam()
      val pair = if (ownFirst) {
        WidgetMatchPair(WidgetParticipant.Team(header.team), WidgetParticipant.Team(seriesOpponent))
      } else {
        WidgetMatchPair(WidgetParticipant.Team(seriesOpponent), WidgetParticipant.Team(header.team))
      }
      TeamState.Live(
        fixture = fixture,
        score = score(own.score, opponent.score),
        maps = WidgetSeriesRules.make(
          details,
          pair,
          if (ownFirst) WidgetSide.Left else WidgetSide.Right,
          preview.currentMap,
          preview.series,
          listOfNotNull(own.id, own.name, own.tag),
        ).maps,
      )
    } else {
      TeamState.Upcoming(fixture)
    }
    return TeamContent(header, if (state is TeamState.Live) form + FormMark.Live else form, history, state)
  }

  fun expire(content: TeamContent, fetchedAt: Instant, now: Instant): TeamContent {
    val expired = when (val state = content.state) {
      is TeamState.Live -> now - fetchedAt > 10800.seconds
      is TeamState.Upcoming -> state.fixture.start?.let { now - it > 21600.seconds } ?: false
      is TeamState.Idle -> false
    }
    if (!expired) return content
    return content.copy(
      form = content.form.let { if (it.lastOrNull() == FormMark.Live) it.dropLast(1) else it },
      state = TeamState.Idle(content.history.firstOrNull()),
    )
  }

  fun hide(content: TeamContent): HiddenTeamContent = HiddenTeamContent(
    header = content.header,
    form = content.form.map { if (it == FormMark.Live) NeutralFormMark.Live else NeutralFormMark.Neutral },
    history = content.history.map { it.neutral() },
    state = when (val state = content.state) {
      is TeamState.Upcoming -> HiddenTeamState.Upcoming(state.fixture)
      is TeamState.Live -> HiddenTeamState.Live(state.fixture)
      is TeamState.Idle -> HiddenTeamState.Idle(state.latest?.neutral())
    },
  )

  fun history(content: TeamContent): List<TeamResult> = when (val state = content.state) {
    is TeamState.Live -> if (state.maps.isNotEmpty()) emptyList() else content.history.take(4)
    is TeamState.Idle -> content.history.drop(1).take(4)
    is TeamState.Upcoming -> content.history.take(4)
  }

  fun history(content: HiddenTeamContent): List<NeutralTeamResult> = when (content.state) {
    is HiddenTeamState.Idle -> content.history.drop(1).take(4)
    else -> content.history.take(4)
  }

  fun logoSources(content: TeamContent): List<String> {
    val opponent = when (val state = content.state) {
      is TeamState.Idle -> state.latest?.opponent
      is TeamState.Live -> state.fixture.opponent
      is TeamState.Upcoming -> state.fixture.opponent
    }
    return (
      listOf(content.header.team.logo, opponent?.logo) + history(content).map {
        it.opponent.logo
      }
      ).filterNotNull()
  }

  fun logoSources(content: HiddenTeamContent): List<String> {
    val opponent = when (val state = content.state) {
      is HiddenTeamState.Idle -> state.latest?.opponent
      is HiddenTeamState.Live -> state.fixture.opponent
      is HiddenTeamState.Upcoming -> state.fixture.opponent
    }
    return (
      listOf(content.header.team.logo, opponent?.logo) + history(content).map {
        it.opponent.logo
      }
      ).filterNotNull()
  }

  fun refreshAt(content: TeamContent?, now: Instant): Instant = when (val state = content?.state) {
    null, is TeamState.Live -> now + 900.seconds
    is TeamState.Upcoming -> WidgetTemporalRules.scheduled(state.fixture.start, now, 300.seconds)
    is TeamState.Idle -> now + 7200.seconds
  }

  fun renderAt(
    content: TeamContent,
    fetchedAt: Instant,
    cachedFallback: Boolean,
    now: Instant,
    zone: TimeZone,
    fallbackAt: Instant,
  ): Instant? {
    val kickoff = (content.state as? TeamState.Upcoming)?.fixture?.start
    val limit = when (val state = content.state) {
      is TeamState.Live -> fetchedAt + 10801.seconds
      is TeamState.Upcoming -> state.fixture.start?.plus(21601.seconds)
      is TeamState.Idle -> null
    }
    val expiry = limit?.let { if (cachedFallback) it else maxOf(it, fallbackAt) }
    return (WidgetTemporalRules.kickoffDates(kickoff, now, zone) + listOfNotNull(expiry)).filter {
      it > now
    }.minOrNull()
  }

  private fun RemoteTeam.widgetTeam(): WidgetTeam = WidgetTeam(id, name, tag, logo)

  private fun TeamResult.neutral(): NeutralTeamResult = NeutralTeamResult(id, day, opponent)

  private fun score(team: Int?, opponent: Int?): WidgetScore? =
    if (team != null && opponent != null) WidgetScore(team, opponent) else null
}
