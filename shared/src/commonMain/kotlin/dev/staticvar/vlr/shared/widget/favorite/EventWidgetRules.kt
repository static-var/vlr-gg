/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal object EventWidgetRules {
  fun project(
    data: EventData,
    fetchedAt: Instant,
    favoriteTeamIds: Set<String>,
    spoilersHidden: Boolean,
    now: Instant,
    zone: TimeZone,
  ): EventContent {
    val phase = phase(data, fetchedAt, now, zone)
    val visible = selection(data, fetchedAt, favoriteTeamIds, phase, now, zone)
    val body = if (spoilersHidden) {
      EventBody.Hidden(
        hide(visible, data, fetchedAt, now, zone),
      )
    } else {
      EventBody.Visible(visible)
    }
    val fixture = when (body) {
      is EventBody.Visible -> when (val state = body.state) {
        is VisibleEventState.Scheduled -> state.hero
        is VisibleEventState.Live -> state.hero
        is VisibleEventState.Between -> data.matches.firstOrNull { it.fixture.id == state.last.matchId }?.fixture
        else -> data.matches.sortedWith(chronological).lastOrNull()?.fixture
      }

      is EventBody.Hidden -> when (val state = body.state) {
        is HiddenEventState.Scheduled -> state.hero
        is HiddenEventState.Live -> state.hero
        else -> data.matches.sortedWith(chronological).lastOrNull()?.fixture
      }
    }
    val subtitle = when (phase) {
      EventPhase.Finished -> EventSubtitle.Ended(data.event.dates.endInclusive?.date, data.event.location)

      EventPhase.NotStarted -> if (data.matches.isEmpty()) {
        EventSubtitle.Dates(data.event.dates, data.event.location)
      } else {
        EventSubtitle.Starts(data.event.dates.start?.date, data.event.location)
      }

      EventPhase.InProgress -> EventSubtitle.Stage(fixture?.stage.orEmpty(), data.event.location)
    }
    return EventContent(
      data.event,
      subtitle,
      progress(data.event.dates, phase, now, zone),
      data.matches.any { it.phase is EventMatchPhase.Live && eligible(it, fetchedAt, now) },
      body,
    )
  }

  fun eligible(match: EventMatch, fetchedAt: Instant, now: Instant): Boolean = when (match.phase) {
    is EventMatchPhase.Live -> now - fetchedAt <= 10800.seconds
    EventMatchPhase.Upcoming -> upcomingExpiry(match.fixture)?.let { now <= it } ?: true
    is EventMatchPhase.Completed -> true
    EventMatchPhase.Unknown -> false
  }

  private fun upcomingExpiry(fixture: EventFixture): Instant? = fixture.start?.plus(21600.seconds)
    ?: fixture.floatingCalendarDay?.plus(1, DateTimeUnit.DAY)?.atStartOfDayIn(TimeZone.UTC)?.plus(21600.seconds)

  fun phase(data: EventData, fetchedAt: Instant, now: Instant, zone: TimeZone): EventPhase = when {
    data.matches.any { it.phase is EventMatchPhase.Live && eligible(it, fetchedAt, now) } -> EventPhase.InProgress

    data.event.reportedStatus == ReportedEventStatus.Completed -> EventPhase.Finished

    data.matches.any {
      it.phase is EventMatchPhase.Live || it.phase is EventMatchPhase.Completed
    } -> EventPhase.InProgress

    data.event.reportedStatus in listOf(
      ReportedEventStatus.Ongoing,
      ReportedEventStatus.Paused,
    ) -> EventPhase.InProgress

    data.event.dates.start?.let { now >= it.start(zone) } == true -> EventPhase.InProgress

    else -> EventPhase.NotStarted
  }

  val chronological: Comparator<EventMatch> = compareBy<EventMatch> {
    it.fixture.floatingCalendarDay
      ?: LocalDate(9999, 12, 31)
  }
    .thenBy { it.fixture.start ?: Instant.DISTANT_FUTURE }.thenBy { it.fixture.id }

  private fun selection(
    data: EventData,
    fetchedAt: Instant,
    favorites: Set<String>,
    phase: EventPhase,
    now: Instant,
    zone: TimeZone,
  ): VisibleEventState {
    val available = data.matches.filter { eligible(it, fetchedAt, now) }
    val priority = compareByDescending<EventMatch> {
      it.fixture.participants.teams.any { team -> team.id in favorites }
    }
      .then(chronological)
    fun list(heroId: String?, heading: ListHeading) = matchList(data, heroId, heading, fetchedAt, now, zone)
    val live = available.filter { it.phase is EventMatchPhase.Live }.sortedWith(priority)
    live.firstOrNull()?.let { match ->
      val score = (match.phase as EventMatchPhase.Live).score
      return VisibleEventState.Live(
        match.fixture,
        score,
        data.detail?.takeIf {
          it.matchId == match.fixture.id
        }?.maps.orEmpty(),
        live.size - 1,
        list(match.fixture.id, ListHeading.Today),
      )
    }
    val last = data.matches.filter {
      it.phase is EventMatchPhase.Completed
    }.sortedWith(chronological).lastOrNull()?.let(::victory)
    if (phase == EventPhase.Finished) {
      val champion = data.prizes.firstOrNull { it.position.trim().equals("1st", true) }?.team ?: last?.winner
      val hero = champion?.let { EventFinish.Champion(it, last?.takeIf { result -> sameTeam(result.winner, it) }) }
        ?: EventFinish.Unconfirmed(EventEnded(data.event.dates.endInclusive?.date, data.event.prizePool))
      val prizes = data.prizes.filter { !it.position.trim().equals("1st", true) }.take(4)
      return VisibleEventState.Finished(
        hero,
        if (prizes.isEmpty()) {
          EventFinishedList.Matches(list(null, ListHeading.Earlier))
        } else {
          EventFinishedList.Prizes(prizes)
        },
      )
    }
    val next = available.filter { it.phase == EventMatchPhase.Upcoming }.sortedWith(priority).firstOrNull()?.fixture
      ?: return VisibleEventState.Gap(
        if (phase ==
          EventPhase.NotStarted
        ) {
          EventGap.NotListed(data.event.dates.start?.date)
        } else {
          EventGap.NextUnlisted
        },
        list(null, ListHeading.Earlier),
      )
    val start = last?.scheduledStart
    val nextStart = next.start
    if (last != null && start != null && nextStart != null && now >= start && now - start < 43200.seconds &&
      nextStart.toLocalDateTime(zone).date > now.toLocalDateTime(zone).date
    ) {
      return VisibleEventState.Between(last, next, list(last.matchId, ListHeading.UpNext))
    }
    val first = data.matches.filter { it.phase != EventMatchPhase.Unknown }.sortedWith(chronological).firstOrNull()
    val role = if (phase == EventPhase.NotStarted &&
      first?.fixture?.id == next.id
    ) {
      ScheduledRole.First
    } else {
      ScheduledRole.Next
    }
    return VisibleEventState.Scheduled(role, next, upcomingList(data, next, favorites, fetchedAt, now, zone))
  }

  fun detailMatchId(data: EventData, favorites: Set<String>, now: Instant, zone: TimeZone): String? =
    when (val state = selection(data, now, favorites, phase(data, now, now, zone), now, zone)) {
      is VisibleEventState.Scheduled -> state.hero.id
      is VisibleEventState.Live -> state.hero.id
      is VisibleEventState.Between -> state.next.id
      is VisibleEventState.Gap, is VisibleEventState.Finished -> null
    }

  fun hide(
    state: VisibleEventState,
    data: EventData,
    fetchedAt: Instant,
    now: Instant,
    zone: TimeZone,
  ): HiddenEventState {
    fun following(hero: EventFixture) = neutral(upcomingMatches(data, hero, fetchedAt, now, zone))
    return when (state) {
      is VisibleEventState.Scheduled -> HiddenEventState.Scheduled(state.role, state.hero, following(state.hero))

      is VisibleEventState.Live -> HiddenEventState.Live(state.hero, neutral(state.list))

      is VisibleEventState.Between -> HiddenEventState.Scheduled(ScheduledRole.Next, state.next, following(state.next))

      is VisibleEventState.Gap -> HiddenEventState.Gap(state.hero, neutral(state.list))

      is VisibleEventState.Finished -> HiddenEventState.Finished(
        EventEnded(data.event.dates.endInclusive?.date, data.event.prizePool),
        neutral(matchList(data, null, ListHeading.Earlier, fetchedAt, now, zone)),
      )
    }
  }

  fun upcomingList(
    data: EventData,
    hero: EventFixture,
    favorites: Set<String>,
    fetchedAt: Instant,
    now: Instant,
    zone: TimeZone,
  ): EventUpcomingList {
    val exact = data.groups.firstOrNull { it.name.equals(hero.round, true) }
    val playoffWords = listOf(
      "final", "playoff", "upper", "lower", "quarter", "semi", "opening",
      "elimination", "decider", "knockout", "round of",
    )
    val groupStage = hero.stage.contains("group", true) || playoffWords.none { hero.round.contains(it, true) }
    val containing = if (groupStage && hero.participants.teams.size == 2) {
      data.groups.filter { group ->
        hero.participants.teams.all { team -> group.rows.any { sameTeam(it.team, team) } }
      }
    } else {
      emptyList()
    }
    val group = exact ?: containing.singleOrNull()
    if (group != null) {
      val firstFavorite = group.rows.indexOfFirst { it.team.id in favorites }.coerceAtLeast(0)
      val start = minOf(maxOf(0, firstFavorite - 4), maxOf(0, group.rows.size - 5))
      return EventUpcomingList.Group(
        group.name,
        group.rows.withIndex().drop(start).take(5).map {
          EventGroupRow(it.index + 1, it.value, it.value.team.id in favorites)
        },
      )
    }
    return EventUpcomingList.Matches(upcomingMatches(data, hero, fetchedAt, now, zone))
  }

  private fun upcomingMatches(
    data: EventData,
    hero: EventFixture,
    fetchedAt: Instant,
    now: Instant,
    zone: TimeZone,
  ): EventMatchList {
    val later = data.matches.filter {
      it.phase == EventMatchPhase.Upcoming && eligible(it, fetchedAt, now) &&
        chronological.compare(EventMatch(hero, EventMatchPhase.Upcoming), it) < 0
    }.sortedWith(chronological)
    if (later.isNotEmpty()) return EventMatchList(ListHeading.UpNext, later.take(3))
    return matchList(data, hero.id, ListHeading.Earlier, fetchedAt, now, zone)
  }

  private fun matchList(
    data: EventData,
    heroId: String?,
    preferred: ListHeading,
    fetchedAt: Instant,
    now: Instant,
    zone: TimeZone,
  ): EventMatchList {
    val available = data.matches.filter { it.fixture.id != heroId && eligible(it, fetchedAt, now) }
    if (preferred == ListHeading.Today) {
      val today = available.filter {
        it.fixture.start?.toLocalDateTime(zone)?.date == now.toLocalDateTime(zone).date
      }.sortedWith(chronological)
      if (today.isNotEmpty()) return EventMatchList(ListHeading.Today, today.take(3))
    }
    if (preferred != ListHeading.Earlier) {
      val upcoming = available.filter { it.phase == EventMatchPhase.Upcoming }.sortedWith(chronological)
      if (upcoming.isNotEmpty()) return EventMatchList(ListHeading.UpNext, upcoming.take(3))
    }
    return EventMatchList(
      ListHeading.Earlier,
      available.filter {
        it.phase is EventMatchPhase.Completed
      }.sortedWith(chronological.reversed()).take(3),
    )
  }

  private fun neutral(list: EventMatchList): NeutralEventList = NeutralEventList(
    list.heading,
    list.rows.map {
      NeutralEventMatch(
        it.fixture,
        when (it.phase) {
          EventMatchPhase.Upcoming -> NeutralPhase.Upcoming
          is EventMatchPhase.Live -> NeutralPhase.Live
          is EventMatchPhase.Completed, EventMatchPhase.Unknown -> NeutralPhase.Ended
        },
      )
    },
  )

  fun sameTeam(left: WidgetTeam, right: WidgetTeam): Boolean =
    if (left.id != null && right.id != null) left.id == right.id else left.name.trim().equals(right.name.trim(), true)

  fun victory(match: EventMatch): EventVictory? {
    val score = (match.phase as? EventMatchPhase.Completed)?.score ?: return null
    if (score.team < 0 || score.opponent < 0 || score.team == score.opponent) return null
    val left = match.fixture.participants.left.team ?: return null
    val right = match.fixture.participants.right.team ?: return null
    return EventVictory(
      match.fixture.id,
      if (score.team > score.opponent) left else right,
      if (score.team >
        score.opponent
      ) {
        right
      } else {
        left
      },
      WidgetScore(maxOf(score.team, score.opponent), minOf(score.team, score.opponent)),
      match.fixture.round,
      match.fixture.start,
    )
  }

  /** The API has no end time. The result window uses match start and can end early. */
  fun resultWindowEnd(victory: EventVictory): Instant? = victory.scheduledStart?.plus(43200.seconds)

  fun progress(bounds: EventDates, phase: EventPhase, now: Instant, zone: TimeZone): Float? {
    if (phase == EventPhase.Finished) return 1f
    if (phase != EventPhase.InProgress) return null
    val first = bounds.start?.date ?: return null
    val last = bounds.endInclusive?.date ?: return null
    if (last < first) return null
    val start = first.atStartOfDayIn(zone)
    val end = last.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
    if (end <= start) return null
    return ((now - start) / (end - start)).coerceIn(0.0, 1.0).toFloat().takeIf { it > 0f }
  }

  fun renderAt(data: EventData, content: EventContent, fetchedAt: Instant, now: Instant, zone: TimeZone): Instant? {
    val dates = mutableListOf(WidgetTemporalRules.nextMidnight(now, zone))
    if (data.matches.any { it.phase is EventMatchPhase.Live }) dates += fetchedAt + 10801.seconds
    for (fixture in content.fixtures(large = true)) {
      fixture.start?.let { dates += it }
      if (data.matches.any { it.fixture.id == fixture.id && it.phase == EventMatchPhase.Upcoming }) {
        upcomingExpiry(fixture)?.let { dates += it + 1.seconds }
      }
    }
    val body = content.body
    if (body is EventBody.Visible &&
      body.state is VisibleEventState.Between
    ) {
      resultWindowEnd(body.state.last)?.let { dates += it }
    }
    return dates.filter { it > now }.minOrNull()
  }

  fun refreshAt(data: EventData, content: EventContent, fetchedAt: Instant, now: Instant): Instant {
    val scheduled = content.fixtures(large = false).firstOrNull()
    val base = when (val body = content.body) {
      is EventBody.Visible -> when (val state = body.state) {
        is VisibleEventState.Live -> now + 900.seconds

        is VisibleEventState.Finished -> return now + 43200.seconds

        is VisibleEventState.Scheduled, is VisibleEventState.Between -> WidgetTemporalRules.scheduled(
          scheduled?.start,
          now,
          900.seconds,
        )

        is VisibleEventState.Gap -> now + (if (state.hero is EventGap.NotListed) 21600 else 7200).seconds
      }

      is EventBody.Hidden -> when (val state = body.state) {
        is HiddenEventState.Live -> now + 900.seconds
        is HiddenEventState.Finished -> return now + 43200.seconds
        is HiddenEventState.Scheduled -> WidgetTemporalRules.scheduled(scheduled?.start, now, 900.seconds)
        is HiddenEventState.Gap -> now + (if (state.hero is EventGap.NotListed) 21600 else 7200).seconds
      }
    }
    val starts = data.matches.filter {
      it.phase == EventMatchPhase.Upcoming && eligible(it, fetchedAt, now)
    }.mapNotNull { it.fixture.start }
    var result = if (starts.any { it <= now }) minOf(base, now + 900.seconds) else base
    starts.filter { it > now }.minOrNull()?.let { result = minOf(result, it) }
    val body = content.body
    if (body is EventBody.Visible && body.state is VisibleEventState.Between) {
      resultWindowEnd(body.state.last)?.takeIf { it > now }?.let { result = minOf(result, it) }
    }
    return result
  }
}

internal fun EventContent.fixtures(large: Boolean): List<EventFixture> {
  fun rows(list: EventMatchList) = if (large) list.rows.map { it.fixture } else emptyList()
  fun neutral(list: NeutralEventList) = if (large) list.rows.map { it.fixture } else emptyList()
  return when (val value = body) {
    is EventBody.Visible -> when (val state = value.state) {
      is VisibleEventState.Scheduled -> listOf(state.hero) +
        ((state.list as? EventUpcomingList.Matches)?.let { rows(it.list) }.orEmpty())

      is VisibleEventState.Live -> listOf(state.hero) + rows(state.list)

      is VisibleEventState.Between -> listOf(state.next) + rows(state.list)

      is VisibleEventState.Gap -> rows(state.list)

      is VisibleEventState.Finished -> (state.list as? EventFinishedList.Matches)?.let { rows(it.list) }.orEmpty()
    }

    is EventBody.Hidden -> when (val state = value.state) {
      is HiddenEventState.Scheduled -> listOf(state.hero) + neutral(state.list)
      is HiddenEventState.Live -> listOf(state.hero) + neutral(state.list)
      is HiddenEventState.Gap -> neutral(state.list)
      is HiddenEventState.Finished -> neutral(state.list)
    }
  }
}

internal fun EventContent.logoSources(): List<String> {
  val state = (body as? EventBody.Visible)?.state
  val hero = when (state) {
    is VisibleEventState.Between -> listOf(state.last.winner.logo, state.last.loser.logo)
    is VisibleEventState.Finished -> listOf((state.hero as? EventFinish.Champion)?.team?.logo)
    else -> emptyList()
  }
  val rows = when (state) {
    is VisibleEventState.Finished -> (state.list as? EventFinishedList.Prizes)?.rows?.map { it.team.logo }.orEmpty()
    is VisibleEventState.Scheduled -> (state.list as? EventUpcomingList.Group)?.rows?.map { it.standing.team.logo }.orEmpty()
    else -> emptyList()
  }
  val fixtures = fixtures(large = true).flatMap { it.participants.teams.map { team -> team.logo } }
  return (listOf(event.logo) + hero + fixtures + rows).filterNotNull().distinct()
}
