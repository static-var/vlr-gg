/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/** Uploads a client's favorite IDs to the live updates API. */
internal class FavoriteLiveUpdateDataSourceImpl(
  private val client: HttpClient,
) : FavoriteLiveUpdateDataSource {
  override suspend fun replace(
    clientId: String,
    teams: List<String>,
    matches: List<String>,
    players: List<String>,
    events: List<String>,
  ): Boolean = try {
    client.put("/api/v1/live-updates/clients/$clientId/favorites") {
      contentType(ContentType.Application.Json)
      setBody(
        FavoriteLiveUpdateRequest(
          teams = teams,
          matches = matches,
          players = players,
          events = events,
        ),
      )
    }.status.isSuccess()
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (_: Exception) {
    false
  }
}

/** Favorite IDs sent in a replacement request. */
@Serializable
private data class FavoriteLiveUpdateRequest(
  val teams: List<String>,
  val matches: List<String>,
  val players: List<String>,
  val events: List<String>,
)
