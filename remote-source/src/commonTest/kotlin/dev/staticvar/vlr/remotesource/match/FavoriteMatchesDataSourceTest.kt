/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.testJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FavoriteMatchesDataSourceTest {
  @Test
  fun listRequestsClientMatchesWithExplicitResultsFlag() = runTest {
    val paths = mutableListOf<String>()
    val queries = mutableListOf<String>()
    val client = mockClient { request ->
      paths += request.url.encodedPath
      queries += request.url.parameters["include_results"].orEmpty()
      respond(
        """[{"id":"42","event":"Champions","series":"Bo3","status":"live","team1":{"id":"1","name":"Alpha"},"team2":{"id":"2","name":"Beta"}}]""",
        HttpStatusCode.OK,
        jsonHeaders(),
      )
    }
    val source = FavoriteMatchesDataSourceImpl(client)

    assertEquals("42", source.list(ClientId, includeResults = true).getOrThrow().single().id)
    assertEquals("42", source.list(ClientId, includeResults = false).getOrThrow().single().id)
    assertEquals(listOf("/api/v1/favorites/$ClientId/matches", "/api/v1/favorites/$ClientId/matches"), paths)
    assertEquals(listOf("true", "false"), queries)
  }

  @Test
  fun unregisteredAndUnavailableResponsesCanBeRetriedLater() = runTest {
    for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.ServiceUnavailable)) {
      val client = HttpClient(MockEngine) {
        expectSuccess = true
        install(ContentNegotiation) { json(testJson()) }
        engine { addHandler { respond("", status, jsonHeaders()) } }
      }
      val error = FavoriteMatchesDataSourceImpl(client).list(ClientId, includeResults = true).exceptionOrNull()
      assertEquals(status.value, assertIs<FavoriteMatchesUnavailableException>(error).statusCode)
    }
  }

  private companion object {
    const val ClientId = "01996ff9-3000-7000-8000-000000000001"
  }
}
