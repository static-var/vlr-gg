/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun HttpClientConfig<*>.configureHttpClient(json: Json, configuration: NetworkConfiguration) {
  expectSuccess = true

  defaultRequest {
    url {
      host = configuration.host
      protocol = configuration.defaultProtocol
    }
    headers { configuration.defaultHeaders.forEach { append(it.key, it.value) } }
  }

  install(ContentNegotiation) { json(json) }
  install(NetworkTelemetry)

  install(HttpTimeout) {
    requestTimeoutMillis = configuration.timeoutMillis
    connectTimeoutMillis = configuration.timeoutMillis
    socketTimeoutMillis = configuration.timeoutMillis
  }

  if (configuration.enableCompression) {
    install(ContentEncoding) { gzip() }
  }

  if (configuration.enableNetworkLogs) {
    install(Logging) {
      logger = object : Logger {
        override fun log(message: String) {
          println("Ktor: $message")
        }
      }
      level = LogLevel.INFO
    }
  }
}
