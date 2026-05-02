package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.localsource.database.Events
import dev.staticvar.vlr.remotesource.common.EventStatus
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.events.EventDetailsDto
import dev.staticvar.vlr.remotesource.events.EventMatchDto
import dev.staticvar.vlr.remotesource.events.EventMatchTeamDto
import dev.staticvar.vlr.remotesource.events.EventPrizeDto
import dev.staticvar.vlr.remotesource.events.EventPrizeTeamDto
import dev.staticvar.vlr.remotesource.events.EventStandingsEntryDto
import dev.staticvar.vlr.remotesource.events.EventTeamDto
import dev.staticvar.vlr.remotesource.team.CompletedMatchDto
import dev.staticvar.vlr.remotesource.team.TeamDetailsDto
import dev.staticvar.vlr.remotesource.team.TeamPlayerDto
import dev.staticvar.vlr.remotesource.team.UpcomingMatchDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventTeamMappersTest {

  @Test
  fun `event mapping positive`() {
    val dto = EventDetailsDto(
      id = "E1",
      title = "Championship",
      subtitle = "Final Stage",
      dates = "Jan 1-10",
      prize = "$1000",
      location = "na",
      status = EventStatus.ONGOING,
      img = "logo.png",
      prizes = listOf(
        EventPrizeDto("1st", "$500", EventPrizeTeamDto("T1","Team 1","t1.png","us")),
        EventPrizeDto("2nd", "$300", EventPrizeTeamDto("T2","Team 2","t2.png","br")),
      ),
      teams = listOf(EventTeamDto("Team 1","T1","t1.png","1"), EventTeamDto("Team 2","T2","t2.png",null)),
      matches = listOf(
        EventMatchDto(
          id = "M1",
          time = "12:00",
          date = "2024-01-02",
          eta = "1h",
          status = MatchStatus.LIVE,
          teams = listOf(
            EventMatchTeamDto("Team 1","na",13),
            EventMatchTeamDto("Team 2","na",7)
          ),
          round = "R1",
          stage = "StageA"
        )
      ),
      standings = listOf(
        EventStandingsEntryDto("t1.png","Team 1","us",3,1,0,20,10,10,"A"),
        EventStandingsEntryDto("t2.png","Team 2","br",1,3,0,-10,-20,-10,"A"),
      )
    )

    val eventEntity = dto.toEventEntity(now = 111L)
    assertEquals("E1", eventEntity.id)
    assertEquals("Championship", eventEntity.name)
    assertEquals("ONGOING", eventEntity.status)
    assertEquals(111L, eventEntity.last_updated)

    val prizeEntities = dto.toPrizeEntities()
    assertEquals(2, prizeEntities.size)
    assertEquals("1st", prizeEntities.first().position)
    assertEquals("T1", prizeEntities.first().team_id)

    val teamEntities = dto.toTeamEntities()
    assertEquals(2, teamEntities.size)
    assertEquals("T2", teamEntities[1].team_id)

    val standingEntities = dto.toStandingEntities()
    assertEquals(2, standingEntities.size)
    assertEquals(3L, standingEntities.first().wins)

    val matchLinks = dto.toEventMatchLinkEntities()
    assertEquals(1, matchLinks.size)
    assertEquals("M1", matchLinks.first().match_id)
    val matchLink = matchLinks.first()
    assertEquals("12:00", matchLink.time)
    assertEquals("2024-01-02", matchLink.date)
    assertEquals("1h", matchLink.eta)
    assertEquals("live", matchLink.status)
    assertEquals("Team 1", matchLink.team1_name)
    assertEquals("na", matchLink.team1_region)
    assertEquals(13L, matchLink.team1_score)
    assertEquals("Team 2", matchLink.team2_name)
    assertEquals(7L, matchLink.team2_score)
  }

  @Test
  fun `event mapping negative edge cases`() {
    val dto = EventDetailsDto(
      id = "E2",
      title = "EdgeCase",
      subtitle = "",
      dates = "",
      prize = "",
      location = "",
      status = null,
      img = "",
      prizes = emptyList(),
      teams = listOf(EventTeamDto("NoIdTeam","","",null)),
      matches = listOf(
        EventMatchDto(
          id = "", // blank -> should be ignored
          time = "",
          date = "",
          eta = null,
          status = null,
          teams = emptyList(),
          round = "",
          stage = ""
        )
      ),
      standings = emptyList()
    )

    val eventEntity = dto.toEventEntity(now = 5L)
  assertEquals(EventStatus.UPCOMING.name, eventEntity.status) // null status normalized to UPCOMING
    assertTrue(dto.toPrizeEntities().isEmpty())
    assertEquals(1, dto.toTeamEntities().size)
    assertEquals(null, dto.toTeamEntities().first().team_id) // blank id -> null
    assertTrue(dto.toStandingEntities().isEmpty())
    assertTrue(dto.toEventMatchLinkEntities().isEmpty())
  // blank match id should NOT produce link entity
  }

  @Test
  fun `team mapping positive`() {
    val dto = TeamDetailsDto(
      name = "Static Warriors",
      tag = "SW",
      img = "sw.png",
      website = "https://sw.gg",
      twitter = "@sw",
      country = "us",
      rank = 5,
      region = "na",
      roster = listOf(
        TeamPlayerDto("P1","Player One","P1","Duelist","p1.png"),
        TeamPlayerDto("P2",null,"AliasTwo",null,"p2.png"),
      ),
      upcoming = listOf(
        UpcomingMatchDto("UM1","Championship","StageA","Rivals","2024-01-03","2h")
      ),
      completed = listOf(
        CompletedMatchDto("CM1","Qualifier","StageZ","OtherTeam","2024-01-01","13:7")
      )
    )

    val core = dto.toTeamEntity(id = "TEAM1", now = 333L)
    assertEquals("TEAM1", core.id)
    assertEquals(5L, core.rank)
    assertEquals(333L, core.last_updated)

    val roster = dto.toRosterEntities("TEAM1")
    assertEquals(2, roster.size)
    assertEquals("P1", roster.first().player_id)
    assertEquals("AliasTwo", roster[1].player_name)

    val upcoming = dto.toUpcomingMatchEntities("TEAM1")
    assertEquals(1, upcoming.size)
    assertEquals("UM1", upcoming.first().match_id)

    val completed = dto.toCompletedMatchEntities("TEAM1")
    assertEquals(1, completed.size)
    assertEquals("CM1", completed.first().match_id)
    assertEquals("13:7", completed.first().result)
  }

  @Test
  fun `team mapping negative edge cases`() {
    val dto = TeamDetailsDto(
      name = "Ghosts",
      tag = "",
      img = "",
      website = null,
      twitter = null,
      country = "",
      rank = 0,
      region = "",
      roster = listOf(TeamPlayerDto("","Unnamed","","Coach","")),
      upcoming = listOf(UpcomingMatchDto("","","","","","")),
      completed = listOf(CompletedMatchDto("","","","","","")),
    )

    val core = dto.toTeamEntity(id = "GHOST", now = 1L)
    assertEquals("GHOST", core.id)
    assertEquals(null, core.region)

    val roster = dto.toRosterEntities("GHOST")
    assertEquals(1, roster.size)
    assertTrue(roster.first().player_id.startsWith("GHOST")) // fallback composite id

    assertTrue(dto.toUpcomingMatchEntities("GHOST").isEmpty())
    assertTrue(dto.toCompletedMatchEntities("GHOST").isEmpty())
  }
}
