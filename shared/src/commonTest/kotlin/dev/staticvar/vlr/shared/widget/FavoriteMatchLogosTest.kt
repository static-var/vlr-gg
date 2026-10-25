/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.shared.widget.favorite.WidgetTestResponse
import dev.staticvar.vlr.shared.widget.favorite.widgetTestClient
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FavoriteMatchLogosTest {
  private val now = Instant.parse("2026-10-25T12:00:00Z").toEpochMilliseconds()
  private val hour = 60 * 60 * 1000L
  private val requests = mutableListOf<String>()
  private val details = mutableMapOf<String, WidgetTestResponse>()
  private val logos = FavoriteMatchLogos(
    widgetTestClient { path, _, _ ->
      requests += path
      details[path] ?: WidgetTestResponse("", HttpStatusCode.NotFound)
    },
    Json { ignoreUnknownKeys = true },
  )

  @Test
  fun `live match gets logos by team id and is looked up once`() = runTest {
    val live = match("773438", "LIVE", now - hour).copy(team1Id = "21858", team2Id = "9516")
    details["/api/v1/matches/773438"] = teams("""{"id":"9516","name":"FENNEL GC","img":"https://owcdn.net/img/fnl.png"}""",
      """{"id":"21858","name":"Gen.G GC","img":"https://owcdn.net/img/gen.png"}""")

    val filled = logos.fill(snapshot(matches = listOf(live, match("next", "UPCOMING", now + hour))), now)

    assertEquals("https://owcdn.net/img/gen.png", filled.matches[0].team1Logo)
    assertEquals("https://owcdn.net/img/fnl.png", filled.matches[0].team2Logo)
    assertEquals(null, filled.matches[1].team1Logo)
    assertEquals(listOf("/api/v1/matches/773438"), requests)

    logos.fill(snapshot(matches = listOf(live)), now)
    assertEquals(listOf("/api/v1/matches/773438"), requests)
  }

  @Test
  fun `recent result and its next match are both filled by team name`() = runTest {
    val final = match("final", "COMPLETED", now - hour).copy(score1 = 2, score2 = 1)
    val next = match("next", "UPCOMING", now + hour).copy(team1 = "LOUD", team2 = "Sentinels")
    details["/api/v1/matches/final"] = teams("""{"name":"Sentinels","img":"https://owcdn.net/img/sen.png"}""",
      """{"name":"paper rex ","img":"https://owcdn.net/img/prx.png"}""")
    details["/api/v1/matches/next"] = teams("""{"name":"LOUD","img":"https://owcdn.net/img/loud.png"}""")

    val filled = logos.fill(snapshot(matches = listOf(next), results = listOf(final)), now)

    assertEquals("https://owcdn.net/img/sen.png" to "https://owcdn.net/img/prx.png", filled.results.single().logos)
    assertEquals("https://owcdn.net/img/loud.png" to null, filled.matches.single().logos)
    assertEquals(listOf("/api/v1/matches/final", "/api/v1/matches/next"), requests.sorted())
  }

  @Test
  fun `failed or empty details leave the match as it was and retry next time`() = runTest {
    val live = match("down", "LIVE", now - hour)
    val snapshot = snapshot(matches = listOf(live))

    assertEquals(snapshot, logos.fill(snapshot, now))
    details["/api/v1/matches/down"] = teams("""{"name":"Sentinels","img":""}""")
    assertEquals(snapshot, logos.fill(snapshot, now))
    assertEquals(listOf("/api/v1/matches/down", "/api/v1/matches/down"), requests)
  }

  @Test
  fun `remembered logos skip the lookup`() = runTest {
    val live = match("seen", "LIVE", now - hour)
    logos.remember(listOf(live.copy(team1Logo = "https://owcdn.net/img/sen.png", team2Logo = "https://owcdn.net/img/prx.png")))

    val filled = logos.fill(snapshot(matches = listOf(live)), now)

    assertEquals("https://owcdn.net/img/sen.png" to "https://owcdn.net/img/prx.png", filled.matches.single().logos)
    assertEquals(emptyList(), requests)
  }

  @Test
  fun `remembered logos follow the team when the sides are swapped`() = runTest {
    val scheduled = match("swap", "LIVE", now - hour)
      .copy(team1Logo = "https://owcdn.net/img/sen.png", team2Logo = "https://owcdn.net/img/prx.png")
    logos.remember(listOf(scheduled))
    val fromFeed = match("swap", "LIVE", now - hour).copy(team1 = "Paper Rex", team2 = "Sentinels", team1Id = "624")

    val filled = logos.fill(snapshot(matches = listOf(fromFeed)), now)

    assertEquals("https://owcdn.net/img/prx.png" to "https://owcdn.net/img/sen.png", filled.matches.single().logos)
    assertEquals(emptyList(), requests)
  }

  private val UpcomingWidgetMatch.logos get() = team1Logo to team2Logo

  private fun teams(vararg teams: String) = WidgetTestResponse("""{"teams":[${teams.joinToString(",")}]}""")

  private fun match(id: String, status: String, start: Long) = UpcomingWidgetMatch(
    id = id,
    event = "Champions",
    team1 = "Sentinels",
    team2 = "Paper Rex",
    startTimeEpochMillis = start,
    status = status,
  )

  private fun snapshot(matches: List<UpcomingWidgetMatch> = emptyList(), results: List<UpcomingWidgetMatch> = emptyList()) =
    UpcomingMatchesSnapshot(
      savedAtEpochMillis = now,
      hasFavorites = true,
      matches = matches,
      theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
      results = results,
    )
}
