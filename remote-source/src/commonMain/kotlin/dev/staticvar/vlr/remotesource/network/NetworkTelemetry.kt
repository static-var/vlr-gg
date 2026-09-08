/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException

internal class NetworkTelemetryConfiguration {
  var reporter: TelemetryReporter = AppTelemetry
}

internal val NetworkTelemetry = createClientPlugin("NetworkTelemetry", ::NetworkTelemetryConfiguration) {
  val reporter = pluginConfig.reporter
  on(Send) { request ->
    val description = "${telemetryMethod(request.method)} ${telemetryEndpoint(request.url.encodedPath)}"
    val span = reporter.startSpan(operation = "http.client", description = description)
    var status = TelemetrySpanStatus.Error
    try {
      val call = proceed(request)
      val responseStatus = call.response.status.value
      status = if (responseStatus < 400) TelemetrySpanStatus.Ok else TelemetrySpanStatus.Error
      reporter.breadcrumb(category = "http", message = "$description status=$responseStatus")
      call
    } catch (cancelled: CancellationException) {
      status = TelemetrySpanStatus.Cancelled
      reporter.breadcrumb(category = "http", message = "$description cancelled")
      throw cancelled
    } catch (error: Throwable) {
      reporter.breadcrumb(category = "http", message = "$description failed")
      throw error
    } finally {
      span.finish(status)
    }
  }
}

/** Only known route templates may leave the device; IDs and arbitrary paths are discarded. */
internal fun telemetryEndpoint(encodedPath: String): String {
  val segments = encodedPath.trim('/').split('/')
  if (segments.size !in 3..4 || segments[0] != "api" || segments[1] != "v1") return "<other>"
  val resource = segments[2]
  return when {
    segments.size == 3 && resource in listOf("events", "matches", "news", "rankings", "search", "version") ->
      "/api/v1/$resource"
    segments.size == 4 && resource in listOf("events", "matches", "news", "player", "team") ->
      "/api/v1/$resource/{id}"
    segments.size == 4 && resource == "standings" -> "/api/v1/standings/{year}"
    else -> "<other>"
  }
}

private fun telemetryMethod(method: HttpMethod): String = when (method) {
  HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch,
  HttpMethod.Delete, HttpMethod.Head, HttpMethod.Options -> method.value
  else -> "OTHER"
}
