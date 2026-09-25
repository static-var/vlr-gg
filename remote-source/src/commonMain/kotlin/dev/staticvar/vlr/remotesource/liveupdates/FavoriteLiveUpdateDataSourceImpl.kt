/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Reads and updates a client's direct favorites through the live updates API. */
internal class FavoriteLiveUpdateDataSourceImpl(
  private val client: HttpClient,
) : FavoriteLiveUpdateDataSource {
  override suspend fun read(clientId: String): FavoriteReadResult = try {
    val response = client.get(path(clientId))
    when (response.status) {
      HttpStatusCode.NotFound -> FavoriteReadResult.NotRegistered
      else -> if (response.status.isSuccess()) {
        FavoriteReadResult.Found(response.body<FavoriteGroups>())
      } else {
        FavoriteReadResult.Failure
      }
    }
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (response: ResponseException) {
    if (response.response.status == HttpStatusCode.NotFound) FavoriteReadResult.NotRegistered else FavoriteReadResult.Failure
  } catch (_: Exception) {
    FavoriteReadResult.Failure
  }

  override suspend fun add(clientId: String, favorites: FavoriteGroups): Boolean = try {
    client.put(path(clientId)) {
      contentType(ContentType.Application.Json)
      setBody(favorites)
    }.status.isSuccess()
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (_: Exception) {
    false
  }

  override suspend fun remove(clientId: String, favorites: FavoriteGroups): Boolean = try {
    client.delete(path(clientId)) {
      contentType(ContentType.Application.Json)
      setBody(favorites)
    }.status.isSuccess()
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (_: Exception) {
    false
  }

  private fun path(clientId: String): String = "/api/v1/live-updates/clients/$clientId/favorites"
}
