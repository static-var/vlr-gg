/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

/** Requests a Live Activity through the API and translates its response. */
internal class LiveActivityStartDataSourceImpl(
  private val client: HttpClient,
) : LiveActivityStartDataSource {
  /**
   * Treats a successful start response as accepted and invalid or missing targets as rejected.
   * Returns unknown for other failures while preserving coroutine cancellation.
   */
  override suspend fun start(clientId: String, matchId: String): LiveActivityStartResult = try {
    when (client.post("/api/v1/live-updates/clients/$clientId/matches/$matchId/live-activity").status) {
      HttpStatusCode.NoContent -> LiveActivityStartResult.Started
      HttpStatusCode.BadRequest, HttpStatusCode.NotFound -> LiveActivityStartResult.Rejected
      else -> LiveActivityStartResult.Unknown
    }
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (response: ResponseException) {
    when (response.response.status) {
      HttpStatusCode.BadRequest, HttpStatusCode.NotFound -> LiveActivityStartResult.Rejected
      else -> LiveActivityStartResult.Unknown
    }
  } catch (_: Exception) {
    LiveActivityStartResult.Unknown
  }
}
