/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Platform-specific HttpClient factory.
 * Each platform provides its own engine and configuration.
 */
expect class HttpClientFactory() {
  fun create(json: Json, configuration: NetworkConfiguration): HttpClient
}
