/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.singleResponseClient
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

/** Checks direct favorite reads and selective writes against the API contract. */
class FavoriteLiveUpdateDataSourceTest {
  @Test
  fun readReturnsDirectFavoriteGroups() = runTest {
    var path = ""
    var method = HttpMethod.Put
    val client = mockClient { request ->
      path = request.url.encodedPath
      method = request.method
      respond("""{"teams":["11"],"matches":["22"],"players":["33"],"events":["44"]}""", headers = jsonHeaders())
    }

    assertEquals(FavoriteReadResult.Found(Favorites), FavoriteLiveUpdateDataSourceImpl(client).read(ClientId))
    assertEquals(Path, path)
    assertEquals(HttpMethod.Get, method)
  }

  @Test
  fun unregisteredClientReturnsNotRegistered() = runTest {
    val client = singleResponseClient("{}", status = HttpStatusCode.NotFound).config {
      expectSuccess = true
    }

    assertEquals(FavoriteReadResult.NotRegistered, FavoriteLiveUpdateDataSourceImpl(client).read(ClientId))
  }

  @Test
  fun unreadableFavoritesReturnFailure() = runTest {
    val unavailable = singleResponseClient("{}", status = HttpStatusCode.BadGateway)
    val malformed = singleResponseClient("not json")

    assertEquals(FavoriteReadResult.Failure, FavoriteLiveUpdateDataSourceImpl(unavailable).read(ClientId))
    assertEquals(FavoriteReadResult.Failure, FavoriteLiveUpdateDataSourceImpl(malformed).read(ClientId))
  }

  @Test
  fun addAndRemoveSendOnlyTheProvidedFavoriteIds() = runTest {
    val requests = mutableListOf<Triple<String, HttpMethod, String>>()
    val client = mockClient { request ->
      requests += Triple(request.url.encodedPath, request.method, request.body.toByteArray().decodeToString())
      respond("", status = HttpStatusCode.NoContent, headers = jsonHeaders())
    }
    val source = FavoriteLiveUpdateDataSourceImpl(client)

    assertTrue(source.add(ClientId, Favorites))
    assertTrue(source.remove(ClientId, Favorites))

    assertEquals(listOf(HttpMethod.Put, HttpMethod.Delete), requests.map { it.second })
    assertEquals(listOf(Path, Path), requests.map { it.first })
    requests.forEach { (_, _, body) ->
      val payload = testJson().parseToJsonElement(body).jsonObject
      assertEquals(listOf("11"), payload.getValue("teams").stringValues())
      assertEquals(listOf("22"), payload.getValue("matches").stringValues())
      assertEquals(listOf("33"), payload.getValue("players").stringValues())
      assertEquals(listOf("44"), payload.getValue("events").stringValues())
    }
  }

  @Test
  fun rejectedWritesReturnFalse() = runTest {
    val client = singleResponseClient("{}", status = HttpStatusCode.BadGateway)
    val source = FavoriteLiveUpdateDataSourceImpl(client)

    assertFalse(source.add(ClientId, Favorites))
    assertFalse(source.remove(ClientId, Favorites))
  }

  private fun kotlinx.serialization.json.JsonElement.stringValues(): List<String> =
    (this as JsonArray).map { it.jsonPrimitive.content }

  /** Holds the client ID used in API request tests. */
  private companion object {
    const val ClientId: String = "01996ff9-3000-7000-8000-000000000001"
    const val Path: String = "/api/v1/live-updates/clients/$ClientId/favorites"
    val Favorites = FavoriteGroups(
      teams = listOf("11"),
      matches = listOf("22"),
      players = listOf("33"),
      events = listOf("44"),
    )
  }
}
