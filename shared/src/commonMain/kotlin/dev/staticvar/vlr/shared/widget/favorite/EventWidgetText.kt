/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

internal object EventWidgetText {
  fun setup(reason: SetupReason, locale: WidgetLocale): FavoriteWidgetFrame.Setup = FavoriteWidgetFrame.Setup(
    WidgetKind.Event,
    reason,
    SetupText(
      title = locale.text(
        if (reason ==
          SetupReason.Choose
        ) {
          WidgetTextKey.ChooseEvent
        } else {
          WidgetTextKey.NoFavoriteEvents
        },
      ),
      subtitle = "Val Esports",
      lead = locale.text(WidgetTextKey.ToUseWidget),
      action = locale.text(
        if (reason == SetupReason.Choose) WidgetTextKey.TapToChoose else WidgetTextKey.OpenApp,
      ),
      instruction = locale.text(
        if (reason == SetupReason.Choose) WidgetTextKey.EventPick else WidgetTextKey.EventFavoriteInstruction,
      ),
      ghost = locale.text(WidgetTextKey.EventGhost),
    ),
  )

  fun placeholder(key: WidgetKey?, locale: WidgetLocale, entityId: String): FavoriteWidgetFrame.Placeholder =
    FavoriteWidgetFrame.Placeholder(
      key,
      PlaceholderText(
        locale.text(WidgetTextKey.EventTitle),
        locale.text(WidgetTextKey.Tbd),
        locale.text(WidgetTextKey.EventGhost),
      ),
      WidgetTap.Event(entityId),
    )

  fun frame(
    key: WidgetKey,
    content: EventContent,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): FavoriteWidgetFrame.Event {
    val medium = presentation(content, large = false, now, zone, locale)
    val large = presentation(content, large = true, now, zone, locale)
    return FavoriteWidgetFrame.Event(
      key,
      medium,
      large,
      WidgetTap.Event(key.entityId),
      content.logoSources(),
    )
  }

  private fun presentation(
    content: EventContent,
    large: Boolean,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): EventPresentation = EventPresentation(
    EventHeaderText(
      logoMark(WidgetTeam(content.event.id, content.event.title, logo = content.event.logo)),
      content.event.title,
      subtitle(content.subtitle, locale),
      content.progress?.coerceIn(0f, 1f),
      content.hasLiveMatch,
    ),
    when (val body = content.body) {
      is EventBody.Visible -> EventDisplayBody.Visible(visible(body.state, large, now, zone, locale))
      is EventBody.Hidden -> EventDisplayBody.Hidden(hidden(body.state, large, now, zone, locale))
    },
  )

  private fun visible(
    state: VisibleEventState,
    large: Boolean,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): VisibleEventText = when (state) {
    is VisibleEventState.Scheduled -> VisibleEventText.Scheduled(
      scheduled(state.role, state.hero, now, zone, locale),
      if (large) upcomingList(state.list, now, zone, locale) else null,
    )

    is VisibleEventState.Live -> {
      val live = locale.text(WidgetTextKey.Live)
      val other = state.otherLive.takeIf { it > 0 }?.let { signedInteger(it, locale) }
      VisibleEventText.Live(
        LiveEventText(
          matchup(state.hero.participants, locale),
          state.score?.text ?: "–",
          live,
          other,
          StageText(state.hero.round, state.hero.format?.label),
          WidgetSeriesRules.pills(state.maps, locale).map { PillText(it.text, pillTone(it.status)) },
          pair(state.hero.participants),
          WidgetTap.Match(state.hero.id),
        ),
        if (large) matchList(state.list, now, zone, locale) else null,
      )
    }

    is VisibleEventState.Between -> {
      val next = state.next.start?.let {
        val (day, time) = WidgetTemporalRules.rowClock(it, now, zone, locale)
        locale.text(WidgetTextKey.NextAt, "$day, $time")
      }
      VisibleEventText.Between(
        VictoryText(
          locale.text(WidgetTextKey.TeamBeatTeam, state.last.winner.name, state.last.loser.name),
          state.last.winnerFirstScore.text,
          dotJoin(state.last.round, next),
          LogoPair(logoMark(state.last.winner), logoMark(state.last.loser)),
          WidgetTap.Match(state.last.matchId),
        ),
        if (large) matchList(state.list, now, zone, locale) else null,
      )
    }

    is VisibleEventState.Gap -> VisibleEventText.Gap(
      gap(state.hero, locale),
      if (large) matchList(state.list, now, zone, locale) else null,
    )

    is VisibleEventState.Finished -> VisibleEventText.Finished(
      when (val hero = state.hero) {
        is EventFinish.Champion -> FinishedEventText.Champion(
          locale.text(WidgetTextKey.Champions),
          hero.team.name,
          dotJoin(
            hero.final?.let { locale.text(WidgetTextKey.BeatTeamScore, it.loser.name, it.winnerFirstScore.text) },
            hero.final?.round,
          ),
          logoMark(hero.team),
          teamTap(hero.team),
        )

        is EventFinish.Unconfirmed -> FinishedEventText.Unconfirmed(ended(hero.ended, false, locale))
      },
      if (large) finishedList(state.list, now, zone, locale) else null,
    )
  }

  private fun hidden(
    state: HiddenEventState,
    large: Boolean,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): HiddenEventText = when (state) {
    is HiddenEventState.Scheduled -> HiddenEventText.Scheduled(
      scheduled(state.role, state.hero, now, zone, locale),
      if (large) neutralList(state.list, now, zone, locale) else null,
    )

    is HiddenEventState.Live -> HiddenEventText.Live(
      NeutralLiveText(
        matchup(state.hero.participants, locale),
        locale.text(WidgetTextKey.LiveNow),
        dotJoin(locale.text(WidgetTextKey.ScoresHidden), state.hero.round),
        locale.text(WidgetTextKey.Live),
        pair(state.hero.participants),
        WidgetTap.Match(state.hero.id),
      ),
      if (large) neutralList(state.list, now, zone, locale) else null,
    )

    is HiddenEventState.Gap -> HiddenEventText.Gap(
      gap(state.hero, locale),
      if (large) neutralList(state.list, now, zone, locale) else null,
    )

    is HiddenEventState.Finished -> HiddenEventText.Finished(
      ended(state.ended, true, locale),
      if (large) neutralList(state.list, now, zone, locale) else null,
    )
  }

  private fun scheduled(
    role: ScheduledRole,
    fixture: EventFixture,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): ScheduledEventText {
    val kickoff = WidgetTemporalRules.kickoff(fixture.start, now, zone, locale)
    val matchup = matchup(fixture.participants, locale)
    val first = role == ScheduledRole.First
    val lead: String
    val context: String
    when (kickoff) {
      is Kickoff.Delayed -> {
        lead = matchup
        context = dotJoin(locale.text(WidgetTextKey.WasDue, kickoff.originalTime), fixture.round)
      }

      is Kickoff.Scheduled -> when {
        first -> {
          lead = locale.text(WidgetTextKey.FirstMatch)
          context = dotJoin(matchup, fixture.round)
        }

        fixture.participants.teams.size < 2 -> {
          lead = fixture.round
          context = dotJoin(matchup, fixture.format?.label)
        }

        else -> {
          lead = matchup
          context = dotJoin(fixture.round, fixture.format?.label)
        }
      }

      Kickoff.Unknown -> {
        lead = if (first) locale.text(WidgetTextKey.FirstMatch) else matchup
        context = if (first) dotJoin(matchup, fixture.round) else dotJoin(fixture.round, fixture.format?.label)
      }
    }
    val day = (kickoff as? Kickoff.Scheduled)?.day
    return ScheduledEventText(lead, day, kickoff.hero(locale), context, pair(fixture.participants), WidgetTap.Match(fixture.id))
  }

  private fun gap(value: EventGap, locale: WidgetLocale): GapText = when (value) {
    is EventGap.NotListed -> GapText(
      locale.text(WidgetTextKey.StartsOn),
      dayText(value.start, locale),
      locale.text(WidgetTextKey.TeamsNotListed),
    )

    EventGap.NextUnlisted -> GapText(
      locale.text(WidgetTextKey.NextMatch),
      locale.text(WidgetTextKey.Tbd),
      locale.text(WidgetTextKey.NextNotListed),
    )
  }

  private fun ended(value: EventEnded, hidden: Boolean, locale: WidgetLocale): EndedText = EndedText(
    locale.text(WidgetTextKey.EventEnded),
    dayText(value.end, locale),
    dotJoin(
      locale.text(if (hidden) WidgetTextKey.WinnerHidden else WidgetTextKey.WinnerNotListed),
      value.prizePool?.let { locale.text(WidgetTextKey.PrizePool, it) },
    ),
  )

  private fun subtitle(value: EventSubtitle, locale: WidgetLocale): String = when (value) {
    is EventSubtitle.Starts -> dotJoin(locale.text(WidgetTextKey.Starts, dayText(value.day, locale)), value.location)

    is EventSubtitle.Ended -> dotJoin(locale.text(WidgetTextKey.EndedOn, dayText(value.day, locale)), value.location)

    is EventSubtitle.Stage -> dotJoin(value.stage, value.location)

    is EventSubtitle.Dates -> {
      val start = dayText(value.bounds.start?.date, locale)
      val range = value.bounds.endInclusive?.let { start + " – " + dayText(it.date, locale) } ?: start
      dotJoin(range, value.location)
    }
  }

  private fun upcomingList(
    value: EventUpcomingList,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): UpcomingListText? = when (value) {
    is EventUpcomingList.Matches -> matchList(value.list, now, zone, locale)?.let(UpcomingListText::Matches)

    is EventUpcomingList.Group -> value.rows.takeIf { it.isNotEmpty() }?.let { rows ->
      UpcomingListText.Group(
        locale.text(WidgetTextKey.GroupHeading, value.name),
        rows.map {
          GroupRowText(
            locale.integer(it.rank),
            logoMark(it.standing.team),
            locale.integer(it.standing.wins) + "–" + locale.integer(it.standing.losses),
            signedInteger(it.standing.maps, locale),
            it.favorite,
            teamTap(it.standing.team),
          )
        },
      )
    }
  }

  private fun finishedList(
    value: EventFinishedList,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): FinishedListText? = when (value) {
    is EventFinishedList.Matches -> matchList(value.list, now, zone, locale)?.let(FinishedListText::Matches)

    is EventFinishedList.Prizes -> value.rows.takeIf { it.isNotEmpty() }?.let { rows ->
      FinishedListText.Prizes(
        locale.text(WidgetTextKey.FinalStandings),
        rows.map { PrizeRowText(it.position, logoMark(it.team), it.amount, teamTap(it.team)) },
      )
    }
  }

  private fun matchList(value: EventMatchList, now: Instant, zone: TimeZone, locale: WidgetLocale): MatchListText? =
    value.rows.takeIf { it.isNotEmpty() }?.let { rows ->
      MatchListText(
        heading(value.heading, locale),
        rows.map { match ->
          val phase: NeutralPhase
          val score: WidgetScore?
          when (val state = match.phase) {
            is EventMatchPhase.Live -> {
              phase = NeutralPhase.Live
              score = state.score
            }

            is EventMatchPhase.Completed -> {
              phase = NeutralPhase.Ended
              score = state.score
            }

            EventMatchPhase.Upcoming, EventMatchPhase.Unknown -> {
              phase = NeutralPhase.Upcoming
              score = null
            }
          }
          val neutral = row(NeutralEventMatch(match.fixture, phase), value.heading, now, zone, locale)
          MatchRowText(
            neutral.day, neutral.time, neutral.left, neutral.right, neutral.leftName, neutral.rightName, neutral.round,
            score?.let { locale.integer(it.team) },
            score?.let { locale.integer(it.opponent) },
            if (phase == NeutralPhase.Ended && score != null && score.team != score.opponent) {
              if (score.team < score.opponent) LoserSide.Left else LoserSide.Right
            } else {
              null
            },
            neutral.live,
            neutral.tap,
          )
        },
      )
    }

  private fun neutralList(
    value: NeutralEventList,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): NeutralListText? = value.rows.takeIf { it.isNotEmpty() }?.let { rows ->
    NeutralListText(heading(value.heading, locale), rows.map { row(it, value.heading, now, zone, locale) })
  }

  private fun row(
    match: NeutralEventMatch,
    heading: ListHeading,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): NeutralRowText {
    val fixture = match.fixture
    val clock = fixture.start?.let { WidgetTemporalRules.rowClock(it, now, zone, locale) }
    val day = if (heading == ListHeading.Today) {
      locale.text(
        when (match.phase) {
          NeutralPhase.Upcoming -> WidgetTextKey.Next
          NeutralPhase.Live -> WidgetTextKey.Live
          NeutralPhase.Ended -> WidgetTextKey.Ended
        },
      )
    } else {
      clock?.first ?: dayText(fixture.floatingCalendarDay, locale)
    }
    return NeutralRowText(
      day,
      clock?.second ?: locale.text(WidgetTextKey.Tbd),
      mark(fixture.participants.left),
      mark(fixture.participants.right),
      name(fixture.participants.left, locale),
      name(fixture.participants.right, locale),
      fixture.round,
      match.phase == NeutralPhase.Live,
      WidgetTap.Match(fixture.id),
    )
  }

  private fun heading(value: ListHeading, locale: WidgetLocale): String = locale.text(
    when (value) {
      ListHeading.Today -> WidgetTextKey.Today
      ListHeading.UpNext -> WidgetTextKey.UpNext
      ListHeading.Earlier -> WidgetTextKey.Earlier
    },
  )

  private fun matchup(value: WidgetMatchPair, locale: WidgetLocale): String =
    locale.text(WidgetTextKey.Matchup, name(value.left, locale), name(value.right, locale))
  private fun name(value: WidgetParticipant, locale: WidgetLocale): String =
    value.team?.name ?: locale.text(WidgetTextKey.Tbd)
  private fun pair(value: WidgetMatchPair): LogoPair = LogoPair(mark(value.left), mark(value.right))
  private fun mark(value: WidgetParticipant): LogoMark = value.team?.let(::logoMark) ?: LogoMark.Undecided
}
