/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import dev.staticvar.vlr.core.notifications.PushPlatform
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/** Uploads a client's push token to the live updates API. */
internal class PushTokenRegistrationDataSourceImpl(
  private val client: HttpClient,
) : PushTokenRegistrationDataSource {
  override suspend fun register(clientId: String, platform: PushPlatform, token: String): Boolean = try {
    client.put("/api/v1/live-updates/clients/$clientId/token") {
      contentType(ContentType.Application.Json)
      setBody(PushTokenRegistrationRequest(token = token, platform = platform.wireValue))
    }.status.isSuccess()
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (_: Exception) {
    false
  }
}

/** Push token and platform name sent to the registration API. */
@Serializable
private data class PushTokenRegistrationRequest(
  val token: String,
  val platform: String,
)

private val PushPlatform.wireValue: String
  get() = when (this) {
    PushPlatform.Ios -> "iOS"
    PushPlatform.Android -> "android"
  }
