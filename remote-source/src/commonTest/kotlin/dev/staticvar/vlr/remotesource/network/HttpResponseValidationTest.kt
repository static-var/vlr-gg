/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import dev.staticvar.vlr.remotesource.events.EventDataSourceImpl
import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.team.TeamDataSourceImpl
import dev.staticvar.vlr.remotesource.testJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HttpResponseValidationTest {
  @Test
  fun errorJsonCannotBecomeSuccessfulDetails() = runTest {
    for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.TooManyRequests, HttpStatusCode.ServiceUnavailable)) {
      val client = client(status, """{"detail":"unavailable"}""")
      try {
        val eventError = assertIs<ResponseException>(EventDataSourceImpl(client).details("event1").exceptionOrNull())
        assertEquals(status, eventError.response.status)
        val teamError = assertIs<ResponseException>(TeamDataSourceImpl(client).details("team1").exceptionOrNull())
        assertEquals(status, teamError.response.status)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun successfulDetailsStillDecode() = runTest {
    val client = client(HttpStatusCode.OK, """{"id":"event1","title":"Champions"}""")
    try {
      assertEquals("Champions", EventDataSourceImpl(client).details("event1").getOrThrow().title)
    } finally {
      client.close()
    }
  }

  private fun client(status: HttpStatusCode, body: String) = HttpClient(MockEngine) {
    configureHttpClient(testJson(), NetworkConfiguration(host = "api.example"))
    engine { addHandler { respond(body, status, jsonHeaders()) } }
  }
}
