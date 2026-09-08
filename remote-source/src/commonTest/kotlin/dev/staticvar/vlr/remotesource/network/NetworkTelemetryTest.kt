/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import dev.staticvar.vlr.core.telemetry.NoOpTelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetrySpan
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class NetworkTelemetryTest {
  @Test
  fun requestPreservesAuthenticationButTelemetryOmitsItsCredentialsAndSearchText() = runTest {
    val recorder = RecordingTelemetry()
    val client = HttpClient(MockEngine) {
      install(NetworkTelemetry) { reporter = recorder }
      engine {
        addHandler { request ->
          assertEquals("Bearer private-token", request.headers[HttpHeaders.Authorization])
          assertEquals("private search", request.url.parameters["q"])
          assertNull(request.headers["sentry-trace"])
          assertNull(request.headers["baggage"])
          respond("[]")
        }
      }
    }
    try {
      client.get("https://api.example/api/v1/search/?q=private%20search") {
        headers { append(HttpHeaders.Authorization, "Bearer private-token") }
      }
      assertEquals(listOf("http.client GET /api/v1/search"), recorder.spans)
      assertEquals(listOf("http GET /api/v1/search status=200"), recorder.breadcrumbs)
      assertEquals(listOf(TelemetrySpanStatus.Ok), recorder.finishes)
      assertFalse((recorder.spans + recorder.breadcrumbs).any { it.contains("private") })
    } finally {
      client.close()
    }
  }

  @Test
  fun httpFailureClosesSpanWithoutCapturingADuplicateError() = runTest {
    val recorder = RecordingTelemetry()
    val client = HttpClient(MockEngine) {
      install(NetworkTelemetry) { reporter = recorder }
      engine { addHandler { respond("private response", HttpStatusCode.ServiceUnavailable) } }
    }
    try {
      client.get("https://api.example/api/v1/player/private-id?token=secret")
      assertEquals(listOf("http GET /api/v1/player/{id} status=503"), recorder.breadcrumbs)
      assertEquals(listOf(TelemetrySpanStatus.Error), recorder.finishes)
      assertEquals(0, recorder.capturedErrors)
    } finally {
      client.close()
    }
  }

  @Test
  fun transportFailureAndCancellationAreRethrownAndCloseExactlyOneSpan() = runTest {
    for (failure in listOf(IllegalStateException("secret host"), CancellationException("private request"))) {
      val recorder = RecordingTelemetry()
      val client = HttpClient(MockEngine) {
        install(NetworkTelemetry) { reporter = recorder }
        engine { addHandler { throw failure } }
      }
      try {
        assertFailsWith<Exception> { client.get("https://api.example/api/v1/matches/") }
        assertEquals(
          listOf(if (failure is CancellationException) TelemetrySpanStatus.Cancelled else TelemetrySpanStatus.Error),
          recorder.finishes,
        )
        assertEquals(0, recorder.capturedErrors)
        assertFalse(recorder.breadcrumbs.any { it.contains("secret") || it.contains("private") })
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun arbitraryPathsAndDynamicSegmentsNeverBecomeTelemetry() {
    assertEquals("/api/v1/standings/{year}", telemetryEndpoint("/api/v1/standings/2026"))
    assertEquals("/api/v1/events/{id}", telemetryEndpoint("/api/v1/events/secret%2Fpath"))
    assertEquals("<other>", telemetryEndpoint("/private/email@example.com"))
    assertEquals("<other>", telemetryEndpoint("/api/v1/secret"))
    assertEquals("<other>", telemetryEndpoint("/api/v1/team/id/secret"))
  }
}

private class RecordingTelemetry : TelemetryReporter by NoOpTelemetryReporter {
  val spans = mutableListOf<String>()
  val breadcrumbs = mutableListOf<String>()
  val finishes = mutableListOf<TelemetrySpanStatus>()
  var capturedErrors = 0

  override fun startSpan(operation: String, description: String): TelemetrySpan {
    spans += "$operation $description"
    return object : TelemetrySpan {
      override fun finish(status: TelemetrySpanStatus) {
        finishes += status
      }
    }
  }

  override fun breadcrumb(category: String, message: String) {
    breadcrumbs += "$category $message"
  }

  override fun captureException(error: Throwable, operation: String) {
    capturedErrors++
  }
}
