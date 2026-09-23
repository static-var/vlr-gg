/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.testJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.io.IOException
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Exercises the production client configuration against a controllable transport. */
class LiveUpdateRetryTest {
  @Test
  fun scoped_writes_recover_after_transient_responses() = runTest {
    for (status in listOf(HttpStatusCode.RequestTimeout, HttpStatusCode.TooManyRequests, HttpStatusCode.InternalServerError)) {
      var sends = 0
      val client = client { _ ->
        sends++
        respond("{}", if (sends == 1) status else HttpStatusCode.OK, jsonHeaders())
      }
      try {
        assertEquals(HttpStatusCode.OK, client.put(TokenPath).status)
        assertEquals(2, sends)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun retries_stop_after_three_replays() = runTest {
    var sends = 0
    val client = client {
      sends++
      respond("{}", HttpStatusCode.ServiceUnavailable, jsonHeaders())
    }
    try {
      assertFailsWith<ResponseException> { client.delete(TokenPath) }
      assertEquals(4, sends)
    } finally {
      client.close()
    }
  }

  @Test
  fun transport_io_and_timeouts_retry_but_validation_errors_do_not() = runTest {
    for (failure in listOf<Throwable>(IOException("disconnected"), HttpRequestTimeoutException(TokenPath, 100))) {
      var sends = 0
      val client = client {
        sends++
        if (sends == 1) throw failure
        respond("{}", HttpStatusCode.OK, jsonHeaders())
      }
      try {
        assertEquals(HttpStatusCode.OK, client.put(FavoritesPath).status)
        assertEquals(2, sends)
      } finally {
        client.close()
      }
    }
    for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized)) {
      var sends = 0
      val client = client {
        sends++
        respond("{}", status, jsonHeaders())
      }
      try {
        assertFailsWith<ResponseException> { client.put(TokenPath) }
        assertEquals(1, sends)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun ktor_request_timeout_retries_after_its_cancellation_wrapper() = runTest {
    var sends = 0
    val client = client(timeoutMillis = 50) {
      sends++
      if (sends == 1) delay(1_000)
      respond("{}", HttpStatusCode.OK, jsonHeaders())
    }
    try {
      assertEquals(HttpStatusCode.OK, client.put(TokenPath).status)
      assertEquals(2, sends)
    } finally {
      client.close()
    }
  }

  @Test
  fun live_activity_posts_and_other_routes_are_never_replayed() = runTest {
    for (requestPath in listOf(LiveActivityPath, "/api/v1/matches", "/api/v1/live-updates/clients/$ClientId/other")) {
      var sends = 0
      val client = client {
        sends++
        respond("{}", HttpStatusCode.ServiceUnavailable, jsonHeaders())
      }
      try {
        assertFailsWith<ResponseException> {
          if (requestPath == LiveActivityPath) client.post(requestPath) else client.put(requestPath)
        }
        assertEquals(1, sends)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun known_offline_skips_scoped_request_and_unknown_still_attempts() = runTest {
    val monitor = TestNetworkMonitor(NetworkStatus.Offline)
    var sends = 0
    val client = client(monitor) {
      sends++
      respond("{}", HttpStatusCode.OK, jsonHeaders())
    }
    try {
      assertFailsWith<IOException> { client.put(TokenPath) }
      assertEquals(0, sends)
      monitor.setStatus(NetworkStatus.Unknown)
      assertEquals(HttpStatusCode.OK, client.put(TokenPath).status)
      assertEquals(1, sends)
      monitor.setStatus(NetworkStatus.Offline)
      assertEquals(HttpStatusCode.OK, client.get("/api/v1/matches").status)
      assertEquals(2, sends)
    } finally {
      client.close()
    }
  }

  @Test
  fun going_offline_during_backoff_prevents_the_next_send() = runTest {
    val monitor = TestNetworkMonitor()
    var sends = 0
    val client = client(monitor) {
      sends++
      respond("{}", HttpStatusCode.ServiceUnavailable, jsonHeaders())
    }
    try {
      val request = async { runCatching { client.put(TokenPath) } }
      runCurrent()
      assertEquals(1, sends)
      advanceTimeBy(100)
      monitor.setStatus(NetworkStatus.Offline)
      advanceUntilIdle()
      assertTrue(request.await().isFailure)
      assertEquals(1, sends)
    } finally {
      client.close()
    }
  }

  @Test
  fun retry_after_sets_the_minimum_backoff() = runTest {
    var sends = 0
    val sendTimes = mutableListOf<Long>()
    val client = client {
      sends++
      sendTimes += testScheduler.currentTime
      if (sends == 1) {
        respond("{}", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "3"))
      } else {
        respond("{}", HttpStatusCode.OK, jsonHeaders())
      }
    }
    try {
      assertEquals(HttpStatusCode.OK, client.put(FavoritesPath).status)
      assertEquals(2, sends)
      assertTrue(sendTimes[1] - sendTimes[0] >= 3_000)
    } finally {
      client.close()
    }
  }

  @Test
  fun cancellation_does_not_start_another_attempt() = runTest {
    var sends = 0
    val client = client {
      sends++
      respond("{}", HttpStatusCode.ServiceUnavailable, jsonHeaders())
    }
    try {
      val request = async { client.put(TokenPath) }
      runCurrent()
      assertEquals(1, sends)
      request.cancelAndJoin()
      advanceUntilIdle()
      assertEquals(1, sends)
    } finally {
      client.close()
    }
  }

  private fun TestScope.client(
    monitor: TestNetworkMonitor = TestNetworkMonitor(),
    timeoutMillis: Long = 20_000,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
  ): HttpClient = HttpClient(MockEngine) {
    configureHttpClient(
      testJson(),
      NetworkConfiguration(host = "api.example", timeoutMillis = timeoutMillis),
      object : AcceptLanguageProvider {
        override fun preferredLanguageTags(): List<String> = listOf("en-US")
      },
      monitor,
    )
    engine {
      dispatcher = StandardTestDispatcher(testScheduler)
      addHandler(handler)
    }
  }
}

private const val ClientId = "123e4567-e89b-12d3-a456-426614174000"
private const val TokenPath = "/api/v1/live-updates/clients/$ClientId/token"
private const val FavoritesPath = "/api/v1/live-updates/clients/$ClientId/favorites"
private const val LiveActivityPath = "/api/v1/live-updates/clients/$ClientId/matches/123/live-activity"
