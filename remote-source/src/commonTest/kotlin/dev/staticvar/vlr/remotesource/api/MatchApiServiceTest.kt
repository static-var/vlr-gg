/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchApiServiceTest {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  @Test
  fun `getMatches returns success with valid JSON`() = runTest {
    val mockEngine = MockEngine {
      respond(
        content = """
          [
            {
              "id": "123",
              "event": "Champions 2025",
              "series": "Grand Final",
              "status": "live",
              "team1": {
                "name": "Team A",
                "country": "US",
                "img": "/img/team-a.png",
                "score": 1
              },
              "team2": {
                "name": "Team B",
                "country": "KR",
                "img": "/img/team-b.png",
                "score": 0
              },
              "time": "2025-10-02T20:00:00Z",
              "event_id": "456"
            }
          ]
        """.trimIndent(),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }

    val httpClient = HttpClient(mockEngine) {
      install(ContentNegotiation) {
        json(json)
      }
    }

    val service = MatchApiServiceImpl(httpClient)
    val result = service.getMatches()

    assertTrue(result.isSuccess)
    val matches = result.getOrThrow()
    assertEquals(1, matches.size)
    assertEquals("123", matches[0].id)
    assertEquals("Champions 2025", matches[0].event)
    assertEquals("live", matches[0].status)
    assertEquals("Team A", matches[0].team1.name)
    assertEquals("Team B", matches[0].team2.name)
  }

  @Test
  fun `getMatches returns failure on network error`() = runTest {
    val mockEngine = MockEngine { request ->
      respond(
        content = "Server Error",
        status = HttpStatusCode.InternalServerError,
      )
    }

    val httpClient = HttpClient(mockEngine) {
      install(ContentNegotiation) {
        json(json)
      }
    }

    val service = MatchApiServiceImpl(httpClient)
    val result = service.getMatches()

    assertTrue(result.isFailure)
  }

  @Test
  fun `getMatchDetails returns success with valid JSON`() = runTest {
    val mockEngine = MockEngine {
      respond(
        content = """
          {
            "id": "123",
            "event": {
              "id": "456",
              "name": "Champions 2025",
              "series": "Playoffs",
              "stage": "Grand Final",
              "img": "/img/event.png"
            },
            "previous_encounters": [],
            "note": "",
            "score": "2-1",
            "teams": [],
            "bans": [],
            "videos": {
              "streams": [],
              "vods": []
            },
            "data": [],
            "map_count": 3
          }
        """.trimIndent(),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }

    val httpClient = HttpClient(mockEngine) {
      install(ContentNegotiation) {
        json(json)
      }
    }

    val service = MatchApiServiceImpl(httpClient)
    val result = service.getMatchDetails("123")

    assertTrue(result.isSuccess)
    val details = result.getOrThrow()
    assertEquals("123", details.id)
    assertEquals("Champions 2025", details.event.name)
    assertEquals(3, details.mapCount)
  }
}
