/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect fun readFixture(fileName: String): String

internal fun testJson(): Json = Json {
  ignoreUnknownKeys = true
  isLenient = true
  coerceInputValues = true
}

internal fun mockClient(
  handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): HttpClient = HttpClient(MockEngine) {
  install(ContentNegotiation) { json(testJson()) }
  engine { this.addHandler(handler) }
}

internal fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

internal fun singleResponseClient(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient =
  mockClient { respond(content = body, status = status, headers = jsonHeaders()) }
