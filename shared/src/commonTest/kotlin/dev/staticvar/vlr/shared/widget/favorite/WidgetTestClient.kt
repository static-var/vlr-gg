/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode

internal fun widgetTestClient(response: suspend (String, String?, String?) -> WidgetTestResponse): HttpClient =
  HttpClient(
    MockEngine { request ->
      val result = response(
        request.url.encodedPath + request.url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty(),
        request.headers["Authorization"],
        request.headers["app-name"],
      )
      respond(result.body, result.status)
    },
  ) {
    expectSuccess = true
    defaultRequest {
      url("https://example.test")
      header("Authorization", "test-token")
      header("app-name", "dev.staticvar.vlr")
    }
  }

internal data class WidgetTestResponse(val body: String, val status: HttpStatusCode = HttpStatusCode.OK)
