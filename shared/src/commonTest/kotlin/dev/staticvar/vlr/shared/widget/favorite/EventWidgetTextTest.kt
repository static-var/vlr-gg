/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EventWidgetTextTest {
  private val now = Instant.parse("2026-10-20T12:00:00Z")
  private val locale = EnglishWidgetLocale()
  private val zone = TimeZone.UTC
  private val key = requireNotNull(WidgetKey.parse(WidgetKind.Event, "2766", "client"))
  private val loud = WidgetTeam("loud", "LOUD", logo = "loud-logo")
  private val vitality = WidgetTeam("vitality", "Team Vitality", logo = "vitality-logo")
  private val pair = WidgetMatchPair(WidgetParticipant.Team(loud), WidgetParticipant.Team(vitality))
  private val event = EventMetadata(
    "2766",
    "Champions 2026",
    "event-logo",
    "Shanghai",
    "$2,250,000",
    EventDates(WidgetDay.parse("2026-09-24"), WidgetDay.parse("2026-10-18")),
    ReportedEventStatus.Ongoing,
  )
  private val empty = EventMatchList(ListHeading.UpNext, emptyList())
  private val neutralEmpty = NeutralEventList(ListHeading.UpNext, emptyList())

  @Test
  fun `first match keeps a separate day and lead and matchup context with or without a time`() {
    data class Case(val offset: Long?, val lead: String, val day: String?, val time: String)
    for ((offset, lead, day, time) in listOf(
      Case(172800L, "First match", "Thu, Oct 22", "12:00\u202FPM"),
      Case(null, "First match", null, "TBD"),
    )) {
      val frame = frame(
        VisibleEventState.Scheduled(
          ScheduledRole.First,
          fixture(offset = offset, round = "Upper Round 1"),
          EventUpcomingList.Matches(empty),
        ),
      )
      for (presentation in listOf(frame.medium, frame.large)) {
        val hero = assertIs<VisibleEventText.Scheduled>(visible(presentation)).hero
        assertEquals(lead, hero.lead)
        assertEquals(day, hero.day)
        assertEquals(time, hero.time)
        assertEquals("LOUD vs Team Vitality · Upper Round 1", hero.context)
      }
    }
  }

  @Test
  fun `next match includes round and format while undecided participants use the round as lead`() {
    val cases = listOf(
      Triple(pair, "LOUD vs Team Vitality", "Lower Final · Bo5"),
      Triple(pair.copy(right = WidgetParticipant.Undecided), "Lower Final", "LOUD vs TBD · Bo5"),
      Triple(
        WidgetMatchPair(WidgetParticipant.Undecided, WidgetParticipant.Undecided),
        "Lower Final",
        "TBD vs TBD · Bo5",
      ),
    )
    for ((participants, lead, context) in cases) {
      val state = VisibleEventState.Scheduled(
        ScheduledRole.Next,
        fixture(offset = 21600, round = "Lower Final", participants = participants),
        EventUpcomingList.Matches(empty),
      )
      val hero = assertIs<VisibleEventText.Scheduled>(visible(frame(state).medium)).hero
      assertEquals(lead, hero.lead)
      assertEquals("Today", hero.day)
      assertEquals("6:00\u202FPM", hero.time)
      assertEquals(context, hero.context)
      assertEquals(WidgetTap.Event("2766"), frame(state).tap)
    }
  }

  @Test
  fun `unknown time keeps the matchup lead even when both teams are undecided`() {
    val value = fixture(
      offset = null,
      participants = WidgetMatchPair(
        WidgetParticipant.Undecided,
        WidgetParticipant.Undecided,
      ),
    )
    val hero = assertIs<VisibleEventText.Scheduled>(
      visible(
        frame(
          VisibleEventState.Scheduled(
            ScheduledRole.Next,
            value,
            EventUpcomingList.Matches(empty),
          ),
        ).medium,
      ),
    ).hero
    assertEquals("TBD vs TBD", hero.lead)
    assertNull(hero.day)
    assertEquals("TBD", hero.time)
    assertEquals("Grand Final · Bo5", hero.context)
    assertEquals(LogoPair(LogoMark.Undecided, LogoMark.Undecided), hero.pair)
  }

  @Test
  fun `overdue and kickoff instants show the original time without a day or format`() {
    for (offset in listOf(-1L, 0L)) {
      for (role in ScheduledRole.entries) {
        val hero = assertIs<VisibleEventText.Scheduled>(
          visible(
            frame(
              VisibleEventState.Scheduled(
                role,
                fixture(offset = offset, round = "Lower Final"),
                EventUpcomingList.Matches(empty),
              ),
            ).medium,
          ),
        ).hero
        assertEquals("LOUD vs Team Vitality", hero.lead)
        assertNull(hero.day)
        assertEquals("Any minute", hero.time)
        assertEquals(
          if (offset ==
            -1L
          ) {
            "Was due 11:59\u202FAM · Lower Final"
          } else {
            "Was due 12:00\u202FPM · Lower Final"
          },
          hero.context,
        )
      }
    }
  }

  @Test
  fun `live keeps matchup accents and stage in separate display slots`() {
    for ((count, other) in listOf(
      0 to null,
      1 to "+1",
    )) {
      val frame = frame(VisibleEventState.Live(fixture(), WidgetScore(2, 1), emptyList(), count, empty))
      for (presentation in listOf(frame.medium, frame.large)) {
        val hero = assertIs<VisibleEventText.Live>(visible(presentation)).hero
        assertEquals("LOUD vs Team Vitality", hero.lead)
        assertEquals("2–1", hero.score)
        assertEquals(StageText("Grand Final", "Bo5"), hero.beside)
        assertEquals("Live", hero.liveLabel)
        assertEquals(other, hero.otherLive)
      }
    }
    val missing = assertIs<VisibleEventText.Live>(
      visible(
        frame(
          VisibleEventState.Live(
            fixture().copy(round = "", format = null),
            null,
            emptyList(),
            0,
            empty,
          ),
        ).medium,
      ),
    ).hero
    assertEquals("–", missing.score)
    assertEquals(StageText("", null), missing.beside)
    assertEquals(emptyList(), missing.pills)
  }

  @Test
  fun `bo5 pills preserve the live map and show only the first next map in both sizes`() {
    val cases = listOf(
      Triple(1, false, listOf("Ascent 13–9", "Sunset 6–4", "Haven next")),
      Triple(3, false, listOf("13–9", "9–13", "13–9", "Lotus 6–4", "Split next")),
      Triple(4, false, listOf("13–9", "9–13", "13–9", "9–13", "Split 6–4")),
      Triple(3, true, listOf("13–9", "9–13", "13–9", "Lotus next")),
    )
    for ((index, between, expected) in cases) {
      val frame = frame(VisibleEventState.Live(fixture(), WidgetScore(2, 1), maps(index, between), 0, empty))
      for (presentation in listOf(frame.medium, frame.large)) {
        val hero = assertIs<VisibleEventText.Live>(visible(presentation)).hero
        assertEquals(expected, hero.pills.map { it.text })
      }
    }
    val long = maps(4).mapIndexed { index, map ->
      if (index == 4) map.copy(name = "Extraordinarily Long Map Name") else map
    }
    val hero = assertIs<VisibleEventText.Live>(
      visible(
        frame(
          VisibleEventState.Live(
            fixture(),
            WidgetScore(2, 2),
            long,
            0,
            empty,
          ),
        ).medium,
      ),
    ).hero
    assertEquals("Extraordinarily Long Map Name 6–4", hero.pills.last().text)
    assertEquals(
      listOf(PillTone.Won, PillTone.Lost, PillTone.Won, PillTone.Lost, PillTone.Live),
      hero.pills.map { it.tone },
    )
  }

  @Test
  fun `between matches shows winner first and complete next day and clock context`() {
    val state = VisibleEventState.Between(victory(), fixture(offset = 86400), empty)
    for (presentation in listOf(frame(state).medium, frame(state).large)) {
      val hero = assertIs<VisibleEventText.Between>(visible(presentation)).hero
      assertEquals("Team Vitality beat LOUD", hero.lead)
      assertEquals("2–1", hero.score)
      assertEquals("Lower Round 3 · Next: Tomorrow, 12\u202FPM", hero.context)
      assertEquals("Team Vitality", assertIs<LogoMark.Named>(hero.pair.left).name)
      assertEquals("LOUD", assertIs<LogoMark.Named>(hero.pair.right).name)
    }
    val missing = assertIs<VisibleEventText.Between>(
      visible(frame(state.copy(next = fixture(offset = null))).medium),
    ).hero
    assertEquals("Lower Round 3", missing.context)
  }

  @Test
  fun `champion and matching final compose their result while an absent final leaves no context`() {
    val final = victory().copy(
      winner = loud,
      loser = vitality,
      winnerFirstScore = WidgetScore(3, 1),
      round = "Grand Final",
    )
    for ((result, context) in listOf(final to "Beat Team Vitality 3–1 · Grand Final", null to "")) {
      val frame = frame(
        VisibleEventState.Finished(
          EventFinish.Champion(loud, result),
          EventFinishedList.Matches(empty),
        ),
      )
      for (presentation in listOf(frame.medium, frame.large)) {
        val hero = assertIs<FinishedEventText.Champion>(
          assertIs<VisibleEventText.Finished>(visible(presentation)).hero,
        )
        assertEquals("Champions", hero.label)
        assertEquals("LOUD", hero.name)
        assertEquals(context, hero.context)
        assertEquals("loud-logo", hero.logo.source)
      }
    }
  }

  @Test
  fun `unconfirmed and hidden finishes show dates prize pool and the correct winner message`() {
    for ((end, date) in listOf(LocalDate.parse("2026-10-18") to "Oct 18", null to "TBD")) {
      for ((pool, visibleContext, hiddenContext) in listOf(
        Triple("$2,250,000", "Winner not listed · $2,250,000 prize pool", "Winner hidden · $2,250,000 prize pool"),
        Triple(null, "Winner not listed", "Winner hidden"),
      )) {
        val ended = EventEnded(end, pool)
        val visible = assertIs<FinishedEventText.Unconfirmed>(
          assertIs<VisibleEventText.Finished>(
            visible(
              frame(
                VisibleEventState.Finished(EventFinish.Unconfirmed(ended), EventFinishedList.Matches(empty)),
              ).medium,
            ),
          ).hero,
        ).ended
        val hidden = assertIs<HiddenEventText.Finished>(
          hidden(frame(HiddenEventState.Finished(ended, neutralEmpty)).medium),
        ).hero
        assertEquals(EndedText("Event ended", date, visibleContext), visible)
        assertEquals(EndedText("Event ended", date, hiddenContext), hidden)
      }
    }
  }

  @Test
  fun `schedule gaps show a floating start date or next match message in visible and hidden bodies`() {
    val cases = listOf(
      EventGap.NotListed(LocalDate.parse("2027-02-20")) to
        GapText("Starts on", "Feb 20", "Teams and matches not listed yet"),
      EventGap.NotListed(null) to GapText("Starts on", "TBD", "Teams and matches not listed yet"),
      EventGap.NextUnlisted to GapText("Next match", "TBD", "VLR has not listed the next match"),
    )
    for ((gap, expected) in cases) {
      assertEquals(
        expected,
        assertIs<VisibleEventText.Gap>(visible(frame(VisibleEventState.Gap(gap, empty)).medium)).hero,
      )
      assertEquals(
        expected,
        assertIs<HiddenEventText.Gap>(hidden(frame(HiddenEventState.Gap(gap, neutralEmpty)).medium)).hero,
      )
    }
  }

  @Test
  fun `hidden live has only neutral matchup live label and composed hidden context`() {
    val frame = frame(
      HiddenEventState.Live(
        fixture(),
        NeutralEventList(
          ListHeading.Today,
          listOf(
            NeutralEventMatch(fixture(), NeutralPhase.Ended),
          ),
        ),
      ),
    )
    for (presentation in listOf(frame.medium, frame.large)) {
      val hero = assertIs<HiddenEventText.Live>(hidden(presentation)).hero
      assertEquals("LOUD vs Team Vitality", hero.lead)
      assertEquals("Live now", hero.label)
      assertEquals("Live", hero.liveLabel)
      assertEquals("Scores hidden · Grand Final", hero.context)
    }
    assertNull(assertIs<HiddenEventText.Live>(hidden(frame.medium)).list)
    val row = requireNotNull(assertIs<HiddenEventText.Live>(hidden(frame.large)).list).rows.single()
    assertEquals("Ended", row.day)
    assertEquals("LOUD", row.leftName)
    assertEquals("Team Vitality", row.rightName)
    assertEquals(false, row.live)
    assertEquals(listOf("event-logo", "loud-logo", "vitality-logo"), frame.logoSources)
  }

  @Test
  fun `hidden scheduled state shares schedule text and excludes all medium lists`() {
    val hero = fixture(offset = 21600, round = "Lower Final")
    val hidden = frame(
      HiddenEventState.Scheduled(
        ScheduledRole.Next,
        hero,
        NeutralEventList(
          ListHeading.UpNext,
          listOf(NeutralEventMatch(fixture(offset = 86400), NeutralPhase.Upcoming)),
        ),
      ),
    )
    val visible = frame(VisibleEventState.Scheduled(ScheduledRole.Next, hero, EventUpcomingList.Matches(empty)))
    assertEquals(
      assertIs<VisibleEventText.Scheduled>(visible(visible.medium)).hero,
      assertIs<HiddenEventText.Scheduled>(hidden(hidden.medium)).hero,
    )
    assertNull(assertIs<HiddenEventText.Scheduled>(hidden(hidden.medium)).list)
    val list = requireNotNull(assertIs<HiddenEventText.Scheduled>(hidden(hidden.large)).list)
    assertEquals("Up next", list.heading)
    assertEquals("Tomorrow", list.rows.single().day)
    assertEquals("12\u202FPM", list.rows.single().time)
  }

  @Test
  fun `today rows use phase labels localized scores and loser styling only for completed unequal scores`() {
    val rows = listOf(
      EventMatch(fixture(offset = -14400, round = "Lower Round 2"), EventMatchPhase.Completed(WidgetScore(2, 1))),
      EventMatch(fixture(offset = -3600, round = "Lower Round 2"), EventMatchPhase.Completed(WidgetScore(1, 2))),
      EventMatch(fixture(), EventMatchPhase.Live(WidgetScore(0, 1))),
      EventMatch(fixture(offset = 32400), EventMatchPhase.Upcoming),
      EventMatch(fixture(offset = 32400), EventMatchPhase.Completed(WidgetScore(1, 1))),
      EventMatch(fixture(offset = null), EventMatchPhase.Unknown),
      EventMatch(fixture(offset = null), EventMatchPhase.Completed(null)),
    )
    val frame = frame(VisibleEventState.Live(fixture(), null, emptyList(), 0, EventMatchList(ListHeading.Today, rows)))
    assertNull(assertIs<VisibleEventText.Live>(visible(frame.medium)).list)
    val list = requireNotNull(assertIs<VisibleEventText.Live>(visible(frame.large)).list)
    assertEquals("Today", list.heading)
    assertEquals(listOf("Ended", "Ended", "Live", "Next", "Ended", "Next", "Ended"), list.rows.map { it.day })
    assertEquals(
      listOf("8\u202FAM", "11\u202FAM", "1\u202FPM", "9\u202FPM", "9\u202FPM", "TBD", "TBD"),
      list.rows.map { it.time },
    )
    assertEquals(listOf("2", "1", "0", null, "1", null, null), list.rows.map { it.leftScore })
    assertEquals(listOf("1", "2", "1", null, "1", null, null), list.rows.map { it.rightScore })
    assertEquals(listOf(LoserSide.Right, LoserSide.Left, null, null, null, null, null), list.rows.map { it.loser })
    assertEquals(listOf(false, false, true, false, false, false, false), list.rows.map { it.live })
    assertEquals("Lower Round 2", list.rows.first().round)
  }

  @Test
  fun `up next and earlier rows use relative dates and floating calendar day when the time is missing`() {
    val fixtures = listOf(
      fixture(offset = 32400, round = "Lower Round 3"),
      fixture(offset = 86400, round = "Lower Final", participants = pair.copy(left = WidgetParticipant.Undecided)),
      fixture(
        offset = 172800,
        participants = WidgetMatchPair(WidgetParticipant.Undecided, WidgetParticipant.Undecided),
      ),
      fixture(offset = null).copy(floatingCalendarDay = LocalDate.parse("2026-10-18")),
      fixture(offset = null).copy(floatingCalendarDay = null),
    )
    val state = VisibleEventState.Scheduled(
      ScheduledRole.Next,
      fixture(),
      EventUpcomingList.Matches(
        EventMatchList(ListHeading.UpNext, fixtures.map { EventMatch(it, EventMatchPhase.Upcoming) }),
      ),
    )
    assertNull(assertIs<VisibleEventText.Scheduled>(visible(frame(state).medium)).list)
    val list = assertIs<UpcomingListText.Matches>(
      assertIs<VisibleEventText.Scheduled>(visible(frame(state).large)).list,
    ).value
    assertEquals("Up next", list.heading)
    assertEquals(listOf("Today", "Tomorrow", "Thu", "Oct 18", "TBD"), list.rows.map { it.day })
    assertEquals(listOf("9\u202FPM", "12\u202FPM", "12\u202FPM", "TBD", "TBD"), list.rows.map { it.time })
    assertEquals(listOf("LOUD", "TBD", "TBD", "LOUD", "LOUD"), list.rows.map { it.leftName })
    assertEquals(LogoMark.Undecided, list.rows[1].left)
    assertEquals(
      listOf("Lower Round 3", "Lower Final", "Grand Final", "Grand Final", "Grand Final"),
      list.rows.map { it.round },
    )
    val hiddenState = HiddenEventState.Finished(
      EventEnded(null, null),
      NeutralEventList(
        ListHeading.Earlier,
        listOf(
          NeutralEventMatch(fixtures.first().copy(start = Instant.parse("2026-10-18T06:00:00Z")), NeutralPhase.Ended),
        ),
      ),
    )
    val midnight = EventWidgetText.frame(
      key,
      content(EventBody.Hidden(hiddenState)),
      Instant.parse("2026-10-19T00:00:00Z"),
      zone,
      locale,
    )
    val earlier = requireNotNull(assertIs<HiddenEventText.Finished>(hidden(midnight.large)).list)
    assertEquals("Earlier", earlier.heading)
    assertEquals("Yesterday", earlier.rows.single().day)
    assertEquals("6\u202FAM", earlier.rows.single().time)
  }

  @Test
  fun `group table uses literal rank record signed maps and favorite styling only in large`() {
    val table = EventUpcomingList.Group(
      "Group Omega",
      listOf(
        EventGroupRow(2, EventStanding(loud, 4, 1, 2, 4), false),
        EventGroupRow(6, EventStanding(vitality, 1, 4, 0, -3), true),
        EventGroupRow(7, EventStanding(WidgetTeam(null, "Other"), 0, 0, 0, 0), false),
      ),
    )
    val frame = frame(VisibleEventState.Scheduled(ScheduledRole.Next, fixture(), table))
    assertNull(assertIs<VisibleEventText.Scheduled>(visible(frame.medium)).list)
    val list = assertIs<UpcomingListText.Group>(assertIs<VisibleEventText.Scheduled>(visible(frame.large)).list)
    assertEquals("Group Omega · W–L · maps", list.heading)
    assertEquals(listOf("2", "6", "7"), list.rows.map { it.rank })
    assertEquals(listOf("4–1", "1–4", "0–0"), list.rows.map { it.record })
    assertEquals(listOf("+4", "−3", "0"), list.rows.map { it.maps })
    assertEquals(listOf(false, true, false), list.rows.map { it.favorite })
  }

  @Test
  fun `finished prizes keep api positions amounts and team names only in visible large frames`() {
    val frame = frame(
      VisibleEventState.Finished(
        EventFinish.Champion(loud, null),
        EventFinishedList.Prizes(
          listOf(
            EventPrize("2nd", "$400,000", vitality),
            EventPrize("3rd–4th", "$200,000", WidgetTeam("g2", "G2 Esports", logo = "g2-logo")),
          ),
        ),
      ),
    )
    assertNull(assertIs<VisibleEventText.Finished>(visible(frame.medium)).list)
    val list = assertIs<FinishedListText.Prizes>(assertIs<VisibleEventText.Finished>(visible(frame.large)).list)
    assertEquals("Final standings", list.heading)
    assertEquals(listOf("2nd", "3rd–4th"), list.rows.map { it.position })
    assertEquals(listOf("Team Vitality", "G2 Esports"), list.rows.map { it.team.name })
    assertEquals(listOf("$400,000", "$200,000"), list.rows.map { it.amount })
    assertEquals(listOf("event-logo", "loud-logo", "vitality-logo", "g2-logo"), frame.logoSources)
    val hidden = frame(HiddenEventState.Finished(EventEnded(null, null), neutralEmpty))
    assertEquals(listOf("event-logo"), hidden.logoSources)
    assertNull(assertIs<HiddenEventText.Finished>(hidden(hidden.medium)).list)
    assertNull(assertIs<HiddenEventText.Finished>(hidden(hidden.large)).list)
  }

  @Test
  fun `header formats final subtitle dates without zone shifts and clamps progress`() {
    val cases = listOf(
      EventSubtitle.Starts(LocalDate.parse("2026-10-22"), "São Paulo") to "Starts Oct 22 · São Paulo",
      EventSubtitle.Ended(LocalDate.parse("2026-10-18"), "Shanghai") to "Ended Oct 18 · Shanghai",
      EventSubtitle.Stage("Phase 2", "") to "Phase 2",
      EventSubtitle.Stage("", "Shanghai") to "Shanghai",
      EventSubtitle.Dates(EventDates(WidgetDay.parse("2027-02-20"), WidgetDay.parse("2027-03-03")), "Seoul") to
        "Feb 20 – Mar 3 · Seoul",
      EventSubtitle.Dates(EventDates(WidgetDay.parse("2027-02-20"), null), "") to "Feb 20",
      EventSubtitle.Starts(null, "") to "Starts TBD",
    )
    for (zone in listOf("UTC", "America/Los_Angeles", "Asia/Kolkata")) {
      for ((subtitle, expected) in cases) {
        val value = content(EventBody.Hidden(HiddenEventState.Gap(EventGap.NextUnlisted, neutralEmpty)))
          .copy(subtitle = subtitle, progress = 1.2f, hasLiveMatch = true)
        val frame = EventWidgetText.frame(key, value, now, TimeZone.of(zone), locale)
        assertEquals(expected, frame.medium.header.subtitle)
        assertEquals(frame.medium.header, frame.large.header)
        assertEquals("Champions 2026", frame.medium.header.title)
        assertEquals("CHA", frame.medium.header.event.monogram)
        assertEquals(1f, frame.medium.header.progress)
        assertEquals(true, frame.medium.header.live)
      }
    }
    val value = content(EventBody.Hidden(HiddenEventState.Gap(EventGap.NextUnlisted, neutralEmpty)))
    assertEquals(0f, EventWidgetText.frame(key, value.copy(progress = -1f), now, zone, locale).medium.header.progress)
    assertNull(EventWidgetText.frame(key, value.copy(progress = null), now, zone, locale).medium.header.progress)
  }

  @Test
  fun `supplied locale controls clock text row scores group numbers and live counts`() {
    val custom = object : WidgetLocale by locale {
      override fun text(key: WidgetTextKey, vararg arguments: String): String = when (key) {
        WidgetTextKey.Tbd -> "Unknown"
        WidgetTextKey.Live -> "Playing"
        else -> locale.text(key, *arguments)
      }
      override fun integer(value: Long): String = "[$value]"
    }
    val live = VisibleEventState.Live(
      fixture(),
      null,
      emptyList(),
      2,
      EventMatchList(
        ListHeading.Today,
        listOf(EventMatch(fixture(), EventMatchPhase.Live(WidgetScore(1, 2)))),
      ),
    )
    val frame = EventWidgetText.frame(key, content(EventBody.Visible(live)), now, zone, custom)
    val state = assertIs<VisibleEventText.Live>(visible(frame.large))
    assertEquals("LOUD vs Team Vitality", state.hero.lead)
    assertEquals("Playing", state.hero.liveLabel)
    val hiddenFrame = EventWidgetText.frame(
      key,
      content(EventBody.Hidden(HiddenEventState.Live(fixture(), neutralEmpty))),
      now,
      zone,
      custom,
    )
    assertEquals("Playing", assertIs<HiddenEventText.Live>(hidden(hiddenFrame.medium)).hero.liveLabel)
    assertEquals("+[2]", state.hero.otherLive)
    assertEquals("Playing", requireNotNull(state.list).rows.single().day)
    assertEquals("[1]", requireNotNull(state.list).rows.single().leftScore)
    assertEquals("[2]", requireNotNull(state.list).rows.single().rightScore)
    val scheduled = VisibleEventState.Scheduled(
      ScheduledRole.Next,
      fixture(offset = 23400),
      EventUpcomingList.Matches(empty),
    )
    val clock = EventWidgetText.frame(
      key,
      content(EventBody.Visible(scheduled)),
      now,
      zone,
      EnglishWidgetLocale(use24Hour = true),
    )
    assertEquals("18:30", assertIs<VisibleEventText.Scheduled>(visible(clock.medium)).hero.time)
    val unknown = scheduled.copy(
      hero = fixture(offset = null, participants = pair.copy(right = WidgetParticipant.Undecided)),
    )
    val text = assertIs<VisibleEventText.Scheduled>(
      visible(EventWidgetText.frame(key, content(EventBody.Visible(unknown)), now, zone, custom).medium),
    ).hero
    assertEquals("LOUD vs Unknown", text.lead)
    assertEquals("Unknown", text.time)
  }

  @Test
  fun `explicit zone controls scheduled day and clock in visible and hidden frames`() {
    val fixture = fixture(offset = 21600)
    for ((zoneId, day, time) in listOf(
      Triple("UTC", "Today", "6:00\u202FPM"),
      Triple("Asia/Tokyo", "Tomorrow", "3:00\u202FAM"),
      Triple("America/Los_Angeles", "Today", "11:00\u202FAM"),
    )) {
      for (body in listOf(
        EventBody.Visible(
          VisibleEventState.Scheduled(ScheduledRole.Next, fixture, EventUpcomingList.Matches(empty)),
        ),
        EventBody.Hidden(HiddenEventState.Scheduled(ScheduledRole.Next, fixture, neutralEmpty)),
      )) {
        val frame = EventWidgetText.frame(key, content(body), now, TimeZone.of(zoneId), locale)
        val hero = when (body) {
          is EventBody.Visible -> assertIs<VisibleEventText.Scheduled>(visible(frame.medium)).hero
          is EventBody.Hidden -> assertIs<HiddenEventText.Scheduled>(hidden(frame.medium)).hero
        }
        assertEquals("LOUD vs Team Vitality", hero.lead)
        assertEquals(day, hero.day)
        assertEquals(time, hero.time)
      }
    }
  }

  @Test
  fun `setup and placeholder use existing Event keys and typed tap targets`() {
    for ((reason, title, instruction) in listOf(
      Triple(SetupReason.Choose, "Choose an event", "Pick an event from your favorites"),
      Triple(SetupReason.NoFavorites, "No favorite events", "Favorite an event, then pick it here"),
    )) {
      val frame = EventWidgetText.setup(reason, locale)
      assertEquals(
        SetupText(
          title,
          "Val Esports",
          "To use this widget,",
          if (reason == SetupReason.Choose) "Tap to choose" else "Open Val Esports",
          instruction,
          "Matches show here",
        ),
        frame.text,
      )
      assertEquals(WidgetKind.Event, frame.kind)
      assertEquals(reason, frame.reason)
      assertEquals(if (reason == SetupReason.Choose) WidgetTap.Chooser else WidgetTap.App, frame.tap)
      assertEquals(emptyList(), frame.logoSources)
    }
    val placeholder = EventWidgetText.placeholder(key, locale, key.entityId)
    assertEquals(PlaceholderText("Event", "TBD", "Matches show here"), placeholder.text)
    assertEquals(WidgetTap.Event("2766"), placeholder.tap)
    assertEquals(emptyList(), placeholder.logoSources)
  }

  @Test
  fun `displayed logos include group and history rows with unique sources and monogram fallback`() {
    val other = WidgetTeam("other", "Other", "OTH", "a-other-logo")
    val state = VisibleEventState.Gap(
      EventGap.NextUnlisted,
      EventMatchList(
        ListHeading.Earlier,
        listOf(
          EventMatch(
            fixture(participants = pair.copy(right = WidgetParticipant.Team(other))),
            EventMatchPhase.Completed(null),
          ),
        ),
      ),
    )
    val frame = frame(state)
    assertEquals(listOf("event-logo", "loud-logo", "a-other-logo"), frame.logoSources)
    val list = requireNotNull(assertIs<VisibleEventText.Gap>(visible(frame.large)).list)
    assertEquals("OTH", assertIs<LogoMark.Named>(list.rows.single().right).monogram)
    val between = frame(
      VisibleEventState.Between(
        victory(),
        fixture(participants = pair.copy(right = WidgetParticipant.Team(other))),
        EventMatchList(ListHeading.UpNext, listOf(EventMatch(fixture(), EventMatchPhase.Upcoming))),
      ),
    )
    assertEquals(listOf("event-logo", "vitality-logo", "loud-logo", "a-other-logo"), between.logoSources)
    val group = frame(
      VisibleEventState.Scheduled(
        ScheduledRole.Next,
        fixture(),
        EventUpcomingList.Group(
          "Group A",
          listOf(
            EventGroupRow(1, EventStanding(loud, 1, 0, 0, 1), false),
            EventGroupRow(2, EventStanding(other, 0, 1, 0, -1), false),
          ),
        ),
      ),
    )
    assertEquals(listOf("event-logo", "loud-logo", "vitality-logo", "a-other-logo"), group.logoSources)
    val noLogo = other.copy(tag = null, logo = null)
    val next = frame(
      VisibleEventState.Scheduled(
        ScheduledRole.Next,
        fixture(participants = pair.copy(right = WidgetParticipant.Team(noLogo))),
        EventUpcomingList.Matches(empty),
      ),
    )
    val logo = assertIs<LogoMark.Named>(assertIs<VisibleEventText.Scheduled>(visible(next.medium)).hero.pair.right)
    assertEquals("OTH", logo.monogram)
    assertNull(logo.source)
    assertEquals(listOf("event-logo", "loud-logo"), next.logoSources)
  }

  private fun fixture(
    id: String = "match",
    offset: Long? = 3600,
    round: String = "Grand Final",
    participants: WidgetMatchPair = pair,
  ): EventFixture = EventFixture(
    id,
    participants,
    offset?.let { now + it.seconds },
    LocalDate.parse("2026-10-20"),
    round,
    "Playoffs",
    WidgetSeriesFormat.Bo5,
  )

  private fun victory(): EventVictory = EventVictory(
    "last",
    vitality,
    loud,
    WidgetScore(2, 1),
    "Lower Round 3",
    now - 3600.seconds,
  )
  private fun content(body: EventBody): EventContent =
    EventContent(event, EventSubtitle.Stage("Playoffs", "Shanghai"), 0.82f, false, body)
  private fun frame(state: VisibleEventState): FavoriteWidgetFrame.Event =
    EventWidgetText.frame(key, content(EventBody.Visible(state)), now, zone, locale)
  private fun frame(state: HiddenEventState): FavoriteWidgetFrame.Event =
    EventWidgetText.frame(key, content(EventBody.Hidden(state)), now, zone, locale)
  private fun visible(value: EventPresentation): VisibleEventText = assertIs<EventDisplayBody.Visible>(value.body).state
  private fun hidden(value: EventPresentation): HiddenEventText = assertIs<EventDisplayBody.Hidden>(value.body).state

  private fun maps(index: Int, between: Boolean = false): List<WidgetMap> =
    listOf("Ascent", "Sunset", "Haven", "Lotus", "Split").mapIndexed { offset, name ->
      val won = offset % 2 == 0
      val status = when {
        offset < index -> if (won) WidgetMapStatus.Won else WidgetMapStatus.Lost
        offset == index && !between -> WidgetMapStatus.Live
        else -> WidgetMapStatus.Next
      }
      WidgetMap(
        name,
        WidgetMapPick.Decider,
        status,
        when (status) {
          WidgetMapStatus.Live -> WidgetScore(6, 4)
          WidgetMapStatus.Won -> WidgetScore(13, 9)
          WidgetMapStatus.Lost -> WidgetScore(9, 13)
          WidgetMapStatus.Next -> null
        },
      )
    }
}
