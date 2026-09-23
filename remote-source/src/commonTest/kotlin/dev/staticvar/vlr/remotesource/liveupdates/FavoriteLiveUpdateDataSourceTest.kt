/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.testJson
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteLiveUpdateDataSourceTest {
  @Test
  fun replaceSendsEveryFavoriteGroupToTheClientPath() = runTest {
    var path = ""
    var method = HttpMethod.Get
    var body = ""
    val client = mockClient { request ->
      path = request.url.encodedPath
      method = request.method
      body = request.body.toByteArray().decodeToString()
      respond("", status = HttpStatusCode.NoContent, headers = jsonHeaders())
    }

    val success = FavoriteLiveUpdateDataSourceImpl(client).replace(
      clientId = ClientId,
      teams = listOf("11"),
      matches = listOf("22"),
      players = listOf("33"),
      events = listOf("44"),
    )

    assertTrue(success)
    assertEquals("/api/v1/live-updates/clients/$ClientId/favorites", path)
    assertEquals(HttpMethod.Put, method)
    val payload = testJson().parseToJsonElement(body).jsonObject
    assertEquals(listOf("11"), payload.getValue("teams").stringValues())
    assertEquals(listOf("22"), payload.getValue("matches").stringValues())
    assertEquals(listOf("33"), payload.getValue("players").stringValues())
    assertEquals(listOf("44"), payload.getValue("events").stringValues())
  }

  @Test
  fun nonSuccessfulResponseReturnsFalse() = runTest {
    val client = mockClient {
      respond("{}", status = HttpStatusCode.BadGateway, headers = jsonHeaders())
    }

    val success = FavoriteLiveUpdateDataSourceImpl(client).replace(
      clientId = ClientId,
      teams = emptyList(),
      matches = emptyList(),
      players = emptyList(),
      events = emptyList(),
    )

    assertFalse(success)
  }

  private fun kotlinx.serialization.json.JsonElement.stringValues(): List<String> =
    (this as JsonArray).map { it.jsonPrimitive.content }

  private companion object {
    const val ClientId: String = "01996ff9-3000-7000-8000-000000000001"
  }
}
