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

internal class TeamWidgetTextTest {
  private val now = Instant.fromEpochSeconds(1_792_152_000)
  private val locale = EnglishWidgetLocale()
  private val key = WidgetKey.parse(WidgetKind.Team, "2", "client")!!
  private val own = WidgetTeam("2", "Sentinels", "SEN", "own")
  private val opponent = WidgetTeam("1", "FNATIC", "FNC", "opponent")
  private val header = TeamHeader(own, 4, "Americas", 1)
  private val history = listOf(
    result("1", "2026-10-15", "Paper Rex", ResultKind.Win, WidgetScore(2, 1)),
    result("2", "2026-10-12", "DRX", ResultKind.Win, WidgetScore(2, 0)),
    result("3", "2026-10-09", "G2 Esports", ResultKind.Loss, WidgetScore(1, 2)),
    result("4", "2026-10-05", "FNATIC", ResultKind.Win, WidgetScore(2, 1)),
    result("5", "2026-09-28", "LOUD", ResultKind.Win, WidgetScore(2, 0)),
  )
  private val fixture =
    TeamFixture("100", opponent, now + 8_040.seconds, "Masters London", "Upper final", WidgetSeriesFormat.Bo3)

  @Test
  fun `upcoming sample has complete hero and size specific rankings`() {
    val frame = frame(TeamState.Upcoming(fixture))
    assertEquals("Sentinels", frame.medium.header.title)
    assertEquals("#4 worldwide", frame.medium.header.ranking)
    assertEquals("#4 worldwide · #1 Americas", frame.large.header.ranking)
    for (presentation in listOf(frame.medium, frame.large)) {
      val hero = assertIs<TeamHeroText.Upcoming>(assertIs<TeamDisplayBody.Visible>(presentation.body).hero)
      assertEquals("Plays FNATIC", hero.lead)
      assertEquals("Today", hero.day)
      assertEquals("2:14\u202FPM", hero.value)
      assertEquals("Masters London · Upper final · Bo3", hero.context)
      assertEquals(LogoMark.Named("FNATIC", "FNC", "opponent", "FNC"), hero.opponent)
    }
    assertNull(assertIs<TeamDisplayBody.Visible>(frame.medium.body).detail)
    val recent = assertIs<TeamDetail.History>(assertIs<TeamDisplayBody.Visible>(frame.large.body).detail)
    assertEquals("Recent", recent.heading)
    assertEquals(listOf("Paper Rex", "DRX", "G2 Esports", "FNATIC"), recent.rows.map { it.name })
    assertEquals(listOf("2–1", "2–0", "1–2", "2–1"), recent.rows.map { it.score })
    assertEquals(listOf("1", "2", "3", "4").map(WidgetTap::Match), recent.rows.map { it.tap })
    assertEquals(WidgetTap.Match("100"), frame.heroTap())
    assertEquals(WidgetTap.Team("2"), frame.tap)
  }

  @Test
  fun `upcoming hero uses relative days clock times and overdue copy`() {
    data class Case(
      val start: Instant?,
      val reference: Instant,
      val day: String?,
      val value: String,
      val context: String,
    )
    val cases = listOf(
      Case(now + 19_800.seconds, now, "Today", "5:30\u202FPM", "Masters London · Upper final · Bo3"),
      Case(
        now + 19_800.seconds,
        now + 20_000.seconds,
        null,
        "Any minute",
        "Was due 5:30\u202FPM · Masters London",
      ),
      Case(now + 86_400.seconds, now, "Tomorrow", "12:00\u202FPM", "Masters London · Upper final · Bo3"),
      Case(
        Instant.parse("2026-10-22T20:00:00Z"),
        now,
        "Thu, Oct 22",
        "8:00\u202FPM",
        "Masters London · Upper final · Bo3",
      ),
      Case(null, now, null, "TBD", "Masters London · Upper final · Bo3"),
    )
    for (case in cases) {
      val content = content(TeamState.Upcoming(fixture.copy(opponent = null, start = case.start)))
      val frame = TeamWidgetText.frame(key, content, false, case.reference, TimeZone.UTC, locale)
      for (presentation in listOf(frame.medium, frame.large)) {
        val hero = assertIs<TeamHeroText.Upcoming>(assertIs<TeamDisplayBody.Visible>(presentation.body).hero)
        assertEquals("Plays TBD", hero.lead)
        assertEquals(case.day, hero.day)
        assertEquals(case.value, hero.value)
        assertEquals(case.context, hero.context)
        assertEquals(LogoMark.Undecided, hero.opponent)
      }
    }
  }

  @Test
  fun `projection uses the supplied zone and locale clock setting for every kickoff word`() {
    val zone = TimeZone.of("America/Los_Angeles")
    val reference = Instant.parse("2026-10-15T06:30:00Z")
    val start = Instant.parse("2026-10-15T08:30:00Z")
    val value = content(TeamState.Upcoming(fixture.copy(start = start)))
    val frame = TeamWidgetText.frame(key, value, false, reference, zone, EnglishWidgetLocale(true))
    val hero = assertIs<TeamHeroText.Upcoming>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
    assertEquals("Plays FNATIC", hero.lead)
    assertEquals("Tomorrow", hero.day)
    assertEquals("01:30", hero.value)
  }

  @Test
  fun `idle hero shows the latest result and omits it from large history`() {
    for ((result, expectedLead) in listOf(ResultKind.Win to "Beat Paper Rex", ResultKind.Loss to "Lost to Paper Rex")) {
      val frame = frame(TeamState.Idle(history.first().copy(result = result)))
      for (presentation in listOf(frame.medium, frame.large)) {
        val hero = assertIs<TeamHeroText.Idle>(assertIs<TeamDisplayBody.Visible>(presentation.body).hero)
        assertEquals(expectedLead, hero.lead)
        assertEquals("2–1", hero.value)
        assertEquals("Oct 15 · No match scheduled", hero.context)
      }
      val detail = assertIs<TeamDetail.History>(assertIs<TeamDisplayBody.Visible>(frame.large.body).detail)
      assertEquals("Before that", detail.heading)
      assertEquals(listOf("DRX", "G2 Esports", "FNATIC", "LOUD"), detail.rows.map { it.name })
      assertEquals(WidgetTap.Team("2"), frame.tap)
    }
  }

  @Test
  fun `empty season and missing latest score or date have complete copy`() {
    for (hidden in listOf(false, true)) {
      val frame = frame(TeamState.Idle(null), hidden)
      for (presentation in listOf(frame.medium, frame.large)) {
        val text = when (val body = presentation.body) {
          is TeamDisplayBody.Visible -> assertIs<TeamHeroText.Idle>(body.hero).let {
            listOf(it.lead, it.value, it.context)
          }

          is TeamDisplayBody.Hidden -> listOf(body.hero.lead, body.hero.value, body.hero.context)
        }
        assertEquals(listOf("Season starts soon", "No matches", "VLR has not listed a match yet"), text)
      }
    }
    val value = frame(TeamState.Idle(history.first().copy(day = null, score = null)))
    val hero = assertIs<TeamHeroText.Idle>(assertIs<TeamDisplayBody.Visible>(value.medium.body).hero)
    assertEquals("–", hero.value)
    assertEquals("TBD · No match scheduled", hero.context)
  }

  @Test
  fun `live hero selects medium map pills and large context with a localized accent`() {
    val maps = listOf(
      WidgetMap("Haven", WidgetMapPick.Team("SEN"), WidgetMapStatus.Won, WidgetScore(13, 8)),
      WidgetMap("Lotus", WidgetMapPick.Opponent("FNC"), WidgetMapStatus.Lost, WidgetScore(9, 13)),
      WidgetMap("Ascent", WidgetMapPick.Decider, WidgetMapStatus.Live, WidgetScore(7, 5)),
    )
    val frame = frame(TeamState.Live(fixture, WidgetScore(1, 1), maps))
    for (presentation in listOf(frame.medium, frame.large)) {
      val hero = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(presentation.body).hero)
      assertEquals("Playing FNATIC", hero.lead)
      assertEquals("Live", hero.liveLabel)
      assertEquals("1–1", hero.value)
    }
    val medium = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
    assertEquals(
      listOf(
        PillText("Haven 13–8", PillTone.Won),
        PillText("Lotus 9–13", PillTone.Lost),
        PillText("Ascent 7–5", PillTone.Live),
      ),
      assertIs<TeamLiveBelow.Pills>(medium.below).items,
    )
    val large = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.large.body).hero)
    assertEquals(TeamLiveBelow.Context("Masters London · Upper final · Bo3"), large.below)
    assertEquals(WidgetTap.Match("100"), frame.heroTap())
    val detail = assertIs<TeamDetail.Maps>(assertIs<TeamDisplayBody.Visible>(frame.large.body).detail)
    assertEquals("Maps", detail.heading)
    assertEquals(false, detail.compact)
    assertEquals(listOf("SEN pick · Won", "FNC pick · Lost", "Decider · Playing now"), detail.rows.map { it.subtitle })
    assertEquals(listOf("13–8", "9–13", "7–5"), detail.rows.map { it.score })
  }

  @Test
  fun `live maps have localized next subtitle and score when progress is absent`() {
    val maps = listOf(
      WidgetMap("Ascent", WidgetMapPick.Team("SEN"), WidgetMapStatus.Won, WidgetScore(13, 10)),
      WidgetMap("Haven", WidgetMapPick.Opponent("FNC"), WidgetMapStatus.Next, null),
      WidgetMap("Lotus", WidgetMapPick.Decider, WidgetMapStatus.Next, null),
    )
    val frame = frame(TeamState.Live(fixture, WidgetScore(1, 0), maps))
    val medium = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
    assertEquals(
      listOf("Ascent 13–10", "Haven next"),
      assertIs<TeamLiveBelow.Pills>(medium.below).items.map {
        it.text
      },
    )
    val detail = assertIs<TeamDetail.Maps>(assertIs<TeamDisplayBody.Visible>(frame.large.body).detail)
    assertEquals(listOf("SEN pick · Won", "FNC pick · Next", "Decider · Next"), detail.rows.map { it.subtitle })
    assertEquals(listOf("13–10", "Next", "Next"), detail.rows.map { it.score })
  }

  @Test
  fun `bo5 keeps compact finished pills current map and only first next map`() {
    val maps = listOf(
      WidgetMap("Ascent", WidgetMapPick.Team("SEN"), WidgetMapStatus.Won, WidgetScore(13, 10)),
      WidgetMap("Icebox", WidgetMapPick.Opponent("FNC"), WidgetMapStatus.Lost, WidgetScore(8, 13)),
      WidgetMap("Split", WidgetMapPick.Team("SEN"), WidgetMapStatus.Live, WidgetScore(7, 5)),
      WidgetMap("Haven", WidgetMapPick.Opponent("FNC"), WidgetMapStatus.Next, null),
      WidgetMap("Lotus", WidgetMapPick.Decider, WidgetMapStatus.Next, null),
    )
    val frame = frame(TeamState.Live(fixture.copy(format = WidgetSeriesFormat.Bo5), WidgetScore(1, 1), maps))
    val medium = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
    assertEquals(
      listOf("13–10", "8–13", "Split 7–5", "Haven next"),
      assertIs<TeamLiveBelow.Pills>(medium.below).items.map {
        it.text
      },
    )
    val detail = assertIs<TeamDetail.Maps>(assertIs<TeamDisplayBody.Visible>(frame.large.body).detail)
    assertEquals(true, detail.compact)
    assertEquals(listOf("Ascent", "Icebox", "Split", "Haven", "Lotus"), detail.rows.map { it.name })
    assertEquals(listOf(null, null, null, null, null), detail.rows.map { it.subtitle })
  }

  @Test
  fun `live without scores or maps uses a dash and recent history`() {
    val frame = frame(TeamState.Live(fixture.copy(opponent = null, stage = "", format = null), null, emptyList()))
    val hero = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
    assertEquals("Playing TBD", hero.lead)
    assertEquals("Live", hero.liveLabel)
    assertEquals("–", hero.value)
    assertEquals(TeamLiveBelow.Context("Masters London"), hero.below)
    assertEquals(
      "Recent",
      assertIs<TeamDetail.History>(assertIs<TeamDisplayBody.Visible>(frame.large.body).detail).heading,
    )
  }

  @Test
  fun `hidden live uses neutral form and history without score or map types`() {
    val maps = listOf(WidgetMap("Ascent", WidgetMapPick.Team("SEN"), WidgetMapStatus.Live, WidgetScore(7, 5)))
    val frame = frame(TeamState.Live(fixture, WidgetScore(1, 1), maps), hidden = true)
    for (presentation in listOf(frame.medium, frame.large)) {
      val body = assertIs<TeamDisplayBody.Hidden>(presentation.body)
      val hero = assertIs<NeutralTeamHeroText.Live>(body.hero)
      assertEquals("Playing FNATIC", hero.lead)
      assertEquals("Live now", hero.value)
      assertEquals("Scores hidden · Bo3", hero.context)
      assertEquals("Live", hero.liveLabel)
      assertEquals(
        listOf(
          NeutralFormMark.Neutral,
          NeutralFormMark.Neutral,
          NeutralFormMark.Neutral,
          NeutralFormMark.Neutral,
          NeutralFormMark.Neutral,
          NeutralFormMark.Live,
        ),
        body.form.map {
          it.mark
        },
      )
    }
    assertNull(assertIs<TeamDisplayBody.Hidden>(frame.medium.body).history)
    val history = assertIs<TeamDisplayBody.Hidden>(frame.large.body).history!!
    assertEquals("Recent", history.heading)
    assertEquals(listOf("Paper Rex", "DRX", "G2 Esports", "FNATIC"), history.rows.map { it.name })
    assertEquals(listOf("Oct 15", "Oct 12", "Oct 9", "Oct 5"), history.rows.map { it.date })
    assertEquals(WidgetTap.Match("100"), frame.heroTap())
  }

  @Test
  fun `hidden idle keeps last played date and before that heading`() {
    val frame = frame(TeamState.Idle(history.first()), hidden = true)
    for (presentation in listOf(frame.medium, frame.large)) {
      val hero = assertIs<NeutralTeamHeroText.Idle>(assertIs<TeamDisplayBody.Hidden>(presentation.body).hero)
      assertEquals("Last played Paper Rex", hero.lead)
      assertEquals("Oct 15", hero.value)
      assertEquals("Scores hidden · No match scheduled", hero.context)
    }
    val history = assertIs<TeamDisplayBody.Hidden>(frame.large.body).history!!
    assertEquals("Before that", history.heading)
    assertEquals(listOf("DRX", "G2 Esports", "FNATIC", "LOUD"), history.rows.map { it.name })
    assertEquals(WidgetTap.Team("2"), frame.tap)
  }

  @Test
  fun `hidden upcoming keeps the public schedule and match tap`() {
    val frame = frame(TeamState.Upcoming(fixture), hidden = true)
    for (presentation in listOf(frame.medium, frame.large)) {
      val hero = assertIs<NeutralTeamHeroText.Upcoming>(assertIs<TeamDisplayBody.Hidden>(presentation.body).hero)
      assertEquals("Plays FNATIC", hero.lead)
      assertEquals("Today", hero.day)
      assertEquals("2:14\u202FPM", hero.value)
      assertEquals("Masters London · Upper final · Bo3", hero.context)
    }
    assertEquals("Recent", assertIs<TeamDisplayBody.Hidden>(frame.large.body).history!!.heading)
    assertEquals(WidgetTap.Match("100"), frame.heroTap())
  }

  @Test
  fun `result markers form labels and rank numbers use the supplied locale`() {
    val custom = object : WidgetLocale by locale {
      override fun text(key: WidgetTextKey, vararg arguments: String): String = when (key) {
        WidgetTextKey.WinMark -> "勝"
        WidgetTextKey.LossMark -> "敗"
        WidgetTextKey.Win -> "Victory"
        WidgetTextKey.Loss -> "Defeat"
        WidgetTextKey.Live -> "Playing"
        else -> locale.text(key, *arguments)
      }

      override fun integer(value: Long): String = "[$value]"
    }
    val value = TeamWidgetText.frame(key, content(TeamState.Upcoming(fixture)), false, now, TimeZone.UTC, custom)
    val rows = assertIs<TeamDetail.History>(assertIs<TeamDisplayBody.Visible>(value.large.body).detail).rows
    assertEquals(listOf("勝", "勝", "敗", "勝"), rows.map { it.mark })
    assertEquals(
      listOf("Victory", "Victory", "Defeat", "Victory", "Victory"),
      assertIs<TeamDisplayBody.Visible>(value.medium.body).form.map {
        it.label
      },
    )
    assertEquals("#[4] worldwide · #[1] Americas", value.large.header.ranking)
    val live = TeamWidgetText.frame(
      key,
      content(TeamState.Live(fixture, null, emptyList())),
      false,
      now,
      TimeZone.UTC,
      custom,
    )
    assertEquals("Playing", assertIs<TeamDisplayBody.Visible>(live.medium.body).form.last().label)
    assertEquals(
      "Playing",
      assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(live.medium.body).hero).liveLabel,
    )
    val hidden = TeamWidgetText.frame(
      key,
      content(TeamState.Live(fixture, null, emptyList())),
      true,
      now,
      TimeZone.UTC,
      custom,
    )
    assertEquals(
      "Playing",
      assertIs<NeutralTeamHeroText.Live>(assertIs<TeamDisplayBody.Hidden>(hidden.medium.body).hero).liveLabel,
    )
  }

  @Test
  fun `setup and placeholder provide Android instructions and typed tap targets`() {
    val choose = TeamWidgetText.setup(SetupReason.Choose, locale)
    assertEquals(
      SetupText(
        "Choose a team",
        "Val Esports",
        "To use this widget,",
        "Tap to choose",
        "Pick a team from your favorites",
        "Recent results show here",
      ),
      choose.text,
    )
    assertEquals(WidgetTap.Chooser, choose.tap)
    val empty = TeamWidgetText.setup(SetupReason.NoFavorites, locale)
    assertEquals(
      SetupText(
        "No favorite teams",
        "Val Esports",
        "To use this widget,",
        "Open Val Esports",
        "Favorite a team, then pick it here",
        "Recent results show here",
      ),
      empty.text,
    )
    assertEquals(WidgetTap.App, empty.tap)
    val placeholder = TeamWidgetText.placeholder(key, locale, key.entityId)
    assertEquals(PlaceholderText("Team", "Loading team…", "Recent results show here"), placeholder.text)
    assertEquals(WidgetTap.Team(key.entityId), placeholder.tap)
  }

  @Test
  fun `frame lists only logos used by either size and prepares monogram fallbacks`() {
    val upcoming = frame(TeamState.Upcoming(fixture))
    assertEquals(listOf("own", "opponent", "history-1", "history-2", "history-3", "history-4"), upcoming.logoSources)
    assertEquals("SEN", upcoming.medium.header.team.monogram)
    val live =
      frame(
        TeamState.Live(fixture, null, listOf(WidgetMap("Ascent", WidgetMapPick.Decider, WidgetMapStatus.Live, null))),
      )
    assertEquals(listOf("own", "opponent"), live.logoSources)
    val hidden = frame(TeamState.Live(fixture, null, emptyList()), hidden = true)
    assertEquals(listOf("own", "opponent", "history-1", "history-2", "history-3", "history-4"), hidden.logoSources)
    for ((name, expected) in listOf("Sentinels" to "SEN", "G2 Esports" to "G2", "𐐨abcd" to "𐐀AB")) {
      val value = content(
        TeamState.Idle(null),
      ).copy(header = header.copy(team = own.copy(name = name, tag = null, logo = null)))
      val monogram = TeamWidgetText.frame(key, value, false, now, TimeZone.UTC, locale).medium.header.team
      assertEquals(expected, monogram.monogram)
      assertNull(monogram.source)
    }
  }

  private fun frame(state: TeamState, hidden: Boolean = false): FavoriteWidgetFrame.Team =
    TeamWidgetText.frame(key, content(state), hidden, now, TimeZone.UTC, locale)

  private fun content(state: TeamState): TeamContent = TeamContent(
    header,
    listOf(FormMark.Win, FormMark.Win, FormMark.Loss, FormMark.Win, FormMark.Win) +
      if (state is TeamState.Live) listOf(FormMark.Live) else emptyList(),
    history,
    state,
  )

  private fun result(id: String, day: String, name: String, result: ResultKind, score: WidgetScore): TeamResult =
    TeamResult(id, LocalDate.parse(day), WidgetTeam(id, name, logo = "history-$id"), result, score)
}

internal fun FavoriteWidgetFrame.heroTap(): WidgetTap.Entity? = when (val body = assertIs<FavoriteWidgetFrame.Team>(this).medium.body) {
  is TeamDisplayBody.Visible -> body.hero.tap
  is TeamDisplayBody.Hidden -> body.hero.tap
}
