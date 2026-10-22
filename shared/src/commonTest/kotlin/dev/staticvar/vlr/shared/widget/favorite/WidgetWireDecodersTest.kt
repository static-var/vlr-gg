/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

internal class WidgetWireDecodersTest {
  private val reference = Instant.parse("2026-10-20T12:00:00Z")

  @Test
  fun profileReadsWidgetFieldsAndIgnoresAppOnlyFacts() {
    val profile = WidgetWireDecoders.profile(
      json(
        """{
        "team":{"id":"2","name":"Sentinels","tag":"SEN","logo":"profile.png","img":"ignored.png"},
        "rank":4,"region":"Americas","region_rank":1,
        "recent":[{"match_id":"100","played_on":"2026-1-5","opponent":{"name":"FNATIC"},
          "team_score":1,"opponent_score":2,"won":false,"event":7}],
        "elo":{},"maps":false,"circuits":7,"form":null
      }""",
      ),
    )
    assertEquals(
      TeamProfile(
        RemoteTeam("2", "Sentinels", "SEN", "profile.png", null),
        4,
        "Americas",
        1,
        listOf(
          RemoteTeamResult(
            "100",
            WidgetDay.parse("2026-01-05"),
            RemoteTeam(null, "FNATIC", null, null, null),
            1,
            2,
            false,
          ),
        ),
      ),
      profile,
    )
  }

  @Test
  fun profileRequiresTeamAndRecentResultFields() {
    val invalid = listOf(
      """{}""",
      """{"team":{},"recent":[]}""",
      """{"team":{"name":"A"}}""",
      """{"team":{"name":"A"},"recent":null}""",
      """{"team":{"name":"A"},"recent":{}}""",
      """{"team":{"name":"A"},"recent":[null]}""",
      """{"team":{"name":"A"},"recent":[{"match_id":"m","played_on":"2026-01-05","opponent":{"name":"B"}}]}""",
    )
    for (body in invalid) assertFailsWith<SerializationException>(body) { WidgetWireDecoders.profile(json(body)) }
  }

  @Test
  fun profileOptionalFieldsRejectMalformedValues() {
    for ((key, value) in listOf("rank" to "\"4\"", "region" to "4", "region_rank" to "false")) {
      assertFailsWith<SerializationException>(key) {
        WidgetWireDecoders.profile(json("""{"team":{"name":"A"},"recent":[],"$key":$value}"""))
      }
    }
    assertEquals(
      TeamProfile(RemoteTeam(null, "A", null, null, null), null, null, null, emptyList()),
      WidgetWireDecoders.profile(json("""{"team":{"name":"A"},"rank":null,"recent":[]}""")),
    )
  }

  @Test
  fun previewsKeepOrderTeamFactsAndCurrentMapScores() {
    val previews = WidgetWireDecoders.previews(
      json(
        """[
        {"id":"later","event":"Masters","series":"Upper final","status":"live","time":"2026-10-23T20:00:00Z",
          "team1":{"id":"1","name":"FNATIC","tag":"FNC","img":"fnatic.png","score":0},
          "team2":{"id":"2","name":"Sentinels","tag":"SEN","score":1},
          "current_map":{"name":"Haven","number":2,"scores":[null,7]}},
        {"id":"earlier","event":"Masters","series":"Bo3","time":"TBD","team1":{"name":"A"},"team2":{"name":"TBD"}}
      ]""",
      ),
    )
    assertEquals(
      listOf(
        MatchPreview(
          "later",
          "Masters",
          "Upper final",
          RemoteMatchPhase.Live,
          Instant.parse("2026-10-23T20:00:00Z"),
          RemoteTeam("1", "FNATIC", "FNC", "fnatic.png", 0),
          RemoteTeam("2", "Sentinels", "SEN", null, 1),
          CurrentMap("Haven", 2, listOf(null, 7)),
        ),
        MatchPreview(
          "earlier",
          "Masters",
          "Bo3",
          RemoteMatchPhase.Unknown,
          null,
          RemoteTeam(null, "A", null, null, null),
          RemoteTeam(null, "TBD", null, null, null),
          null,
        ),
      ),
      previews,
    )
  }

  @Test
  fun previewsRejectANonArrayAndSkipMalformedRows() {
    for (body in listOf("null", "{}")) {
      assertFailsWith<SerializationException>(body) { WidgetWireDecoders.previews(json(body)) }
    }
    val valid = WidgetWireDecoders.previews(json("[$preview]"))
    assertEquals(listOf("m"), valid.map(MatchPreview::id))
    for (body in listOf("[null,$preview]", "[{},$preview]", """[$preview,{"id":"incomplete"}]""")) {
      assertEquals(valid, WidgetWireDecoders.previews(json(body)), body)
    }
    for (current in listOf(
      """{"scores":[]}""",
      """{"name":"Haven","scores":{}}""",
      """{"name":"Haven","scores":[7,"5"]}""",
    )) {
      val value = json(preview).jsonObject
      assertEquals(
        valid,
        WidgetWireDecoders.previews(json("[$preview,${JsonObject(value + ("current_map" to json(current)))}]")),
        current,
      )
    }
  }

  @Test
  fun previewStatusesUseExactUntrimmedAliases() {
    val cases = listOf(
      "upcoming" to RemoteMatchPhase.Upcoming,
      "UPCOMING" to RemoteMatchPhase.Upcoming,
      "live" to RemoteMatchPhase.Live,
      "ongoing" to RemoteMatchPhase.Live,
      "completed" to RemoteMatchPhase.Completed,
      "final" to RemoteMatchPhase.Completed,
      "tbd" to RemoteMatchPhase.Unknown,
      " upcoming " to RemoteMatchPhase.Unknown,
      " LIVE " to RemoteMatchPhase.Unknown,
      " FINAL " to RemoteMatchPhase.Unknown,
      "paused" to RemoteMatchPhase.Unknown,
      "unknown" to RemoteMatchPhase.Unknown,
      "cancelled" to RemoteMatchPhase.Unknown,
    )
    for ((status, expected) in cases) {
      val value = json(preview).jsonObject
      val decoded = WidgetWireDecoders.previews(jsonArray(JsonObject(value + ("status" to JsonPrimitive(status)))))
      assertEquals(expected, decoded.single().phase, status)
    }
  }

  @Test
  fun eventStatusesKeepTolerantMatchAndEventAliases() {
    val cases = listOf(
      Triple(" upcoming ", RemoteMatchPhase.Upcoming, RemoteEventStatus.Upcoming),
      Triple("TBD", RemoteMatchPhase.Upcoming, RemoteEventStatus.Upcoming),
      Triple(" LIVE ", RemoteMatchPhase.Live, RemoteEventStatus.Ongoing),
      Triple("ongoing", RemoteMatchPhase.Live, RemoteEventStatus.Ongoing),
      Triple("completed", RemoteMatchPhase.Completed, RemoteEventStatus.Completed),
      Triple(" FINAL ", RemoteMatchPhase.Completed, RemoteEventStatus.Completed),
      Triple("paused", RemoteMatchPhase.Unknown, RemoteEventStatus.Paused),
      Triple("unknown", RemoteMatchPhase.Unknown, RemoteEventStatus.Unknown),
      Triple("cancelled", RemoteMatchPhase.Unknown, RemoteEventStatus.Unknown),
    )
    for ((status, matchPhase, eventStatus) in cases) {
      val event = WidgetWireDecoders.event(
        json("""{"id":"test","title":"Title","status":"$status","matches":[{"id":"m","status":"$status"}]}"""),
        "test",
        reference,
      )
      assertEquals(eventStatus, event.status)
      assertEquals(matchPhase, event.matches.single().phase)
    }
  }

  @Test
  fun detailsUseMissingListAndNestedDefaults() {
    assertEquals(
      MatchDetails(emptyList(), emptyList(), emptyList(), "Final", null, 3, null),
      WidgetWireDecoders.details(
        json("""{"event":{"stage":"Final"},"total_maps":3,"teams":null,"data":null,"veto":null}"""),
      ),
    )
    assertEquals(
      MatchDetails(
        emptyList(),
        listOf(RemoteMap("", null, emptyList())),
        listOf(RemoteVeto(null, "unknown", "")),
        null,
        CurrentMap("Haven", null, emptyList()),
        null,
        null,
      ),
      WidgetWireDecoders.details(json("""{"data":[{}],"veto":[{}],"current_map":{"name":"Haven"}}""")),
    )
  }

  @Test
  fun detailsRejectMalformedOptionalArraysAndFields() {
    for (key in listOf("teams", "data", "veto")) {
      for (bad in listOf("{}", "[null]", "[7]")) {
        assertFailsWith<SerializationException>("$key $bad") { WidgetWireDecoders.details(json("""{"$key":$bad}""")) }
      }
    }
    for (body in listOf(
      """{"data":[{"teams":{}}]}""",
      """{"data":[{"teams":[{}]}]}""",
      """{"data":[{"live":"true"}]}""",
      """{"veto":[{"action":3}]}""",
      """{"event":7}""",
      """{"event":{"stage":7}}""",
      """{"total_maps":"5"}""",
      """{"map_count":2.5}""",
      """{"current_map":{"name":"Haven","scores":[7,false]}}""",
    )) {
      assertFailsWith<SerializationException>(body) { WidgetWireDecoders.details(json(body)) }
    }
  }

  @Test
  fun detailsRetainInvalidTotalMapsWithoutFallingBackToMapCount() {
    val details = WidgetWireDecoders.details(json("""{"total_maps":2,"map_count":3}"""))
    assertEquals(2, details.totalMaps)
    assertEquals(3, details.mapCount)
  }

  @Test
  fun realMatchFixtureRetainsSeriesOrderAndScores() {
    assertEquals(
      MatchDetails(
        teams = listOf(
          RemoteTeam("6961", "LOUD", "LOUD", "https://owcdn.net/img/62bbec8dc1b9f.png", 3),
          RemoteTeam("2059", "Team Vitality", "VIT", "https://owcdn.net/img/6466d79e1ed40.png", 1),
        ),
        maps = listOf(
          RemoteMap(
            "Summit",
            false,
            listOf(RemoteTeam(null, "LOUD", null, null, 13), RemoteTeam(null, "Team Vitality", null, null, 11)),
          ),
          RemoteMap(
            "Lotus",
            false,
            listOf(RemoteTeam(null, "LOUD", null, null, 8), RemoteTeam(null, "Team Vitality", null, null, 13)),
          ),
          RemoteMap(
            "Sunset",
            false,
            listOf(RemoteTeam(null, "LOUD", null, null, 13), RemoteTeam(null, "Team Vitality", null, null, 5)),
          ),
          RemoteMap(
            "Haven",
            false,
            listOf(RemoteTeam(null, "LOUD", null, null, 13), RemoteTeam(null, "Team Vitality", null, null, 10)),
          ),
        ),
        veto = listOf(
          RemoteVeto("LOUD", "ban", "Ascent"),
          RemoteVeto("LOUD", "ban", "Split"),
          RemoteVeto("LOUD", "pick", "Summit"),
          RemoteVeto("VIT", "pick", "Lotus"),
          RemoteVeto("LOUD", "pick", "Sunset"),
          RemoteVeto("VIT", "pick", "Haven"),
          RemoteVeto(null, "remains", "Abyss"),
        ),
        eventStage = "Playoffs: Grand Final",
        currentMap = null,
        totalMaps = 5,
        mapCount = 4,
      ),
      WidgetWireDecoders.details(json(readWidgetFixture("match-754737.json"))),
    )
  }

  @Test
  fun realEventFixturesDecodeMetadataAndAllEndpointLists() {
    data class Case(
      val id: String,
      val title: String,
      val status: RemoteEventStatus,
      val start: String,
      val end: String,
      val counts: List<Int>,
    )
    val cases = listOf(
      Case(
        "2766",
        "Valorant Champions 2026",
        RemoteEventStatus.Completed,
        "2026-09-24",
        "2026-10-18",
        listOf(8, 34, 0, 8),
      ),
      Case(
        "2977",
        "VCT 2026: Americas Stage 2",
        RemoteEventStatus.Completed,
        "2026-07-17",
        "2026-09-07",
        listOf(8, 60, 12, 8),
      ),
      Case("3122", "Project Blender 2026", RemoteEventStatus.Upcoming, "2026-10-03", "2026-12-21", listOf(8, 78, 8, 8)),
      Case(
        "3161",
        "Valorant Masters Seoul 2027",
        RemoteEventStatus.Unknown,
        "2027-02-20",
        "2027-03-03",
        listOf(0, 0, 0, 0),
      ),
    )
    for (case in cases) {
      val event = fixture(case.id)
      assertEquals(case.id, event.id)
      assertEquals(case.title, event.title)
      assertEquals(case.status, event.status)
      assertEquals(EventDates(WidgetDay.parse(case.start), WidgetDay.parse(case.end)), event.dates)
      assertEquals(case.counts, listOf(event.teams.size, event.matches.size, event.standings.size, event.prizes.size))
    }
  }

  @Test
  fun eventFixtureRetainsFinalAndPrizeFactsWithoutResolvingRoster() {
    val event = fixture("2766")
    assertEquals(
      RemoteEventMatch(
        "754737",
        WidgetDay.parse("2026-10-18"),
        Instant.parse("2026-10-18T06:00:00Z"),
        RemoteMatchPhase.Completed,
        listOf(RemoteEventParticipant("LOUD", 3), RemoteEventParticipant("Team Vitality", 1)),
        "Grand Final",
        "Playoffs",
      ),
      event.matches.last(),
    )
    assertEquals(
      RemoteEventPrize("1st", "$1,000,000", RemoteEventTeam("6961", "LOUD", "https://owcdn.net/img/62bbec8dc1b9f.png")),
      event.prizes.first(),
    )
  }

  @Test
  fun eventFixtureRetainsStandingOrderAndOptionalNumbers() {
    val event = fixture("2977")
    assertEquals(
      RemoteEventStanding("https://owcdn.net/img/62875027c8e06.png", "Sentinels", 2, 3, 0, 0, "Group Omega"),
      event.standings[9],
    )
    assertEquals(
      listOf("NRG", "LEVIATÁN", "MIBR", "Evil Geniuses", "ENVY", "FURIA"),
      event.standings.take(6).map {
        it.team
      },
    )
  }

  @Test
  fun eventFixtureKeepsUnknownTimesAndUnassignedPrizes() {
    val event = fixture("3122")
    assertEquals(
      RemoteEventMatch(
        "769163",
        WidgetDay.parse("2026-10-24"),
        null,
        RemoteMatchPhase.Upcoming,
        listOf(RemoteEventParticipant("TBD", null), RemoteEventParticipant("TBD", null)),
        "Grand Final",
        "Phase 2",
      ),
      event.matches.last(),
    )
    assertEquals(RemoteEventPrize("1st", "€2,000", null), event.prizes.first())
    assertEquals(null, fixture("3161").prize)
  }

  @Test
  fun eventOptionalArraysDiscardWholeArrayOnMalformedElement() {
    for (key in listOf("teams", "matches", "standings", "prizes")) {
      for (bad in listOf("null", "{}", "[{},7]", "[null]")) {
        val event = WidgetWireDecoders.event(json("""{"id":"test","title":"Title","$key":$bad}"""), "test", reference)
        assertEquals(
          listOf(0, 0, 0, 0),
          listOf(event.teams.size, event.matches.size, event.standings.size, event.prizes.size),
          "$key $bad",
        )
        assertEquals("Title", event.title)
      }
    }
  }

  @Test
  fun eventMalformedOptionalFactsUseDefaultsWithinValidObjects() {
    val event = WidgetWireDecoders.event(
      json(
        """{"id":"test","title":"Title","prize":3,"img":false,"location":{},"status":7,
        "teams":[{"id":2,"name":true,"img":[]}],
        "matches":[{"id":"m","time":4,"date":false,"status":{},"round":7,"stage":false,"teams":[{"name":"A"},7]}],
        "standings":[{"team":"A","wins":"1","losses":false,"ties":null,"map_difference":{},"group":3}],
        "prizes":[{"position":7,"prize":false,"team":3}]}""",
      ),
      "test",
      reference,
    )
    assertEquals(listOf(RemoteEventTeam(null, "", null)), event.teams)
    assertEquals(
      listOf(RemoteEventMatch("m", null, null, RemoteMatchPhase.Unknown, emptyList(), "", "")),
      event.matches,
    )
    assertEquals(listOf(RemoteEventStanding(null, "A", null, null, null, null, null)), event.standings)
    assertEquals(listOf(RemoteEventPrize("", "", null)), event.prizes)
    assertEquals(RemoteEventStatus.Unknown, event.status)
    assertEquals("", event.location)
    assertEquals(null, event.prize)
    assertEquals(null, event.logo)
  }

  @Test
  fun eventParticipantBadScoreDoesNotDiscardItsMatch() {
    val event = WidgetWireDecoders.event(
      json(
        """{"id":"test","title":"Title","matches":[{"id":"m","teams":[{"name":"A","score":"2"},{"name":"B","score":-1}]}]}""",
      ),
      "test",
      reference,
    )
    assertEquals(
      listOf(RemoteEventParticipant("A", null), RemoteEventParticipant("B", -1)),
      event.matches.single().teams,
    )
  }

  @Test
  fun eventDateFallbackUsesOnlyEligibleUniqueMatchDays() {
    val event = WidgetWireDecoders.event(
      json(
        """{"id":"test","title":"Title","matches":[
        {"id":"m","date":"2026-10-21"},
        {"id":"m","date":"2028-12-31"},
        {"id":"","date":"2024-01-01"},
        {"id":"overfull","date":"2023-01-01","teams":[{},{},{}]}
      ]}""",
      ),
      "test",
      reference,
    )
    assertEquals(EventDates(WidgetDay.parse("2026-10-21"), WidgetDay.parse("2026-10-21")), event.dates)
  }

  @Test
  fun eventRequiresMatchingIdentityAndNonblankTitle() {
    for (body in listOf(
      "null",
      "{}",
      """{"id":1,"title":"Title"}""",
      """{"id":"wrong","title":"Title"}""",
      """{"id":"test","title":false}""",
      """{"id":"test","title":" TBD "}""",
    )) {
      assertFailsWith<SerializationException>(body) { WidgetWireDecoders.event(json(body), "test", reference) }
    }
    val event = WidgetWireDecoders.event(json("""{"id":" test ","title":" Title "}"""), "test", reference)
    assertEquals("test", event.id)
    assertEquals("Title", event.title)
  }

  private fun fixture(id: String): EventProfile =
    WidgetWireDecoders.event(json(readWidgetFixture("event-$id.json")), id, reference)

  private fun json(body: String): JsonElement = Json.parseToJsonElement(body)

  private fun jsonArray(value: JsonElement): JsonElement = JsonArray(listOf(value))

  private val preview = """{"id":"m","event":"Masters","series":"Bo3","team1":{"name":"A"},"team2":{"name":"B"}}"""
}
