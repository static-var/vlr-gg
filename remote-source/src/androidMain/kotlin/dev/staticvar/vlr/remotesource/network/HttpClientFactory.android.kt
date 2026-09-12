/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

actual class HttpClientFactory {
  actual fun create(json: Json, configuration: NetworkConfiguration): HttpClient = HttpClient(OkHttp) {
    configureHttpClient(json, configuration)
  }
}
