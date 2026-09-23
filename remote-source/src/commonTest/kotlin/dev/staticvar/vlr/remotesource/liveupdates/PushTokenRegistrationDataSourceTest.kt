/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.testJson
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PushTokenRegistrationDataSourceTest {
  @Test
  fun iosRequestUsesClientPathAndBackendPlatformValue() = runTest {
    val requests = mutableListOf<Triple<String, HttpMethod, String>>()
    val client = mockClient { request ->
      requests += Triple(request.url.encodedPath, request.method, request.body.toByteArray().decodeToString())
      respond("", status = HttpStatusCode.NoContent, headers = jsonHeaders())
    }

    val success = PushTokenRegistrationDataSourceImpl(client).register(ClientId, PushPlatform.Ios, "00aaff")

    assertTrue(success)
    val (path, method, body) = requests.single()
    assertEquals("/api/v1/live-updates/clients/$ClientId/token", path)
    assertEquals(HttpMethod.Put, method)
    assertEquals("00aaff", testJson().parseToJsonElement(body).jsonObject.getValue("token").jsonPrimitive.content)
    assertEquals("iOS", testJson().parseToJsonElement(body).jsonObject.getValue("platform").jsonPrimitive.content)
  }

  @Test
  fun androidTokenIsOpaqueAndOnlyTwoHundredsCountAsSuccess() = runTest {
    val token = "fcm:opaque token/+=="
    var body = ""
    val client = mockClient { request ->
      body = request.body.toByteArray().decodeToString()
      respond("{}", status = HttpStatusCode.BadGateway, headers = jsonHeaders())
    }

    val success = PushTokenRegistrationDataSourceImpl(client).register(ClientId, PushPlatform.Android, token)

    assertFalse(success)
    val payload = testJson().parseToJsonElement(body).jsonObject
    assertEquals(token, payload.getValue("token").jsonPrimitive.content)
    assertEquals("android", payload.getValue("platform").jsonPrimitive.content)
  }

  private companion object {
    const val ClientId: String = "01996ff9-3000-7000-8000-000000000001"
  }
}
