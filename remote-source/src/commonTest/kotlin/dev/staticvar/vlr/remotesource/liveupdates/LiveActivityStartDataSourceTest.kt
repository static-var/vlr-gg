/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveActivityStartDataSourceTest {
  @Test
  fun postsTheMatchPathAndAcceptsNoContent() = runTest {
    var path = ""
    var method = HttpMethod.Get
    val client = HttpClient(MockEngine) {
      expectSuccess = true
      engine {
        addHandler { request ->
          path = request.url.encodedPath
          method = request.method
          respond("", status = HttpStatusCode.NoContent)
        }
      }
    }

    assertEquals(LiveActivityStartResult.Started, LiveActivityStartDataSourceImpl(client).start(ClientId, "123"))
    assertEquals("/api/v1/live-updates/clients/$ClientId/matches/123/live-activity", path)
    assertEquals(HttpMethod.Post, method)
  }

  @Test
  fun distinguishesKnownRejectionFromAmbiguousServerFailure() = runTest {
    for ((status, expected) in listOf(
      HttpStatusCode.BadRequest to LiveActivityStartResult.Rejected,
      HttpStatusCode.NotFound to LiveActivityStartResult.Rejected,
      HttpStatusCode.ServiceUnavailable to LiveActivityStartResult.Unknown,
    )) {
      val client = HttpClient(MockEngine) {
        expectSuccess = true
        engine { addHandler { respond("", status = status) } }
      }
      assertEquals(expected, LiveActivityStartDataSourceImpl(client).start(ClientId, "123"))
    }
  }

  private companion object {
    const val ClientId: String = "01996ff9-3000-7000-8000-000000000001"
  }
}
