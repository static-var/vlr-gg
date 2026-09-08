/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.refresh

import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.telemetry.NoOpTelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetryLevel
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshControllerTest {
  @Test
  fun requestsDuringAnActiveRefreshBecomeOneFollowup() = runTest {
    val network = TestNetworkMonitor()
    val firstRequest = CompletableDeferred<Unit>()
    var calls = 0
    val controller = RefreshController(backgroundScope, network) {
      calls++
      if (calls == 1) firstRequest.await()
      Result.success(Unit)
    }

    controller.refresh()
    runCurrent()
    assertTrue(controller.state.value.isRefreshing)

    repeat(10) { controller.refresh() }
    runCurrent()
    assertEquals(1, calls)

    firstRequest.complete(Unit)
    runCurrent()
    assertEquals(2, calls)
    assertEquals(RefreshState(hasCompleted = true), controller.state.value)
  }

  @Test
  fun queuedFollowupWaitsForConnectivity() = runTest {
    val network = TestNetworkMonitor()
    val firstRequest = CompletableDeferred<Unit>()
    var calls = 0
    val controller = RefreshController(backgroundScope, network) {
      calls++
      if (calls == 1) firstRequest.await()
      Result.success(Unit)
    }

    controller.refresh()
    runCurrent()
    network.isOnline.value = false
    controller.refresh()
    firstRequest.complete(Unit)
    runCurrent()
    assertEquals(1, calls)
    assertFalse(controller.state.value.isRefreshing)

    network.isOnline.value = true
    runCurrent()
    assertEquals(2, calls)
  }

  @Test
  fun offlineRequestWaitsWithoutShowingRefreshProgress() = runTest {
    val network = TestNetworkMonitor(online = false)
    var calls = 0
    val controller = RefreshController(backgroundScope, network) {
      calls++
      Result.success(Unit)
    }

    controller.refresh()
    runCurrent()
    assertEquals(0, calls)
    assertEquals(RefreshState(), controller.state.value)

    network.isOnline.value = true
    runCurrent()
    assertEquals(1, calls)
  }

  @Test
  fun failedRefreshStopsProgressAndCanBeRetried() = runTest {
    var calls = 0
    val telemetry = RecordingTelemetry()
    val failure = IllegalStateException("Unavailable")
    val controller = RefreshController(
      backgroundScope,
      TestNetworkMonitor(),
      telemetry = telemetry,
      operation = "matches.refresh",
    ) {
      calls++
      if (calls == 1) Result.failure(failure) else Result.success(Unit)
    }

    controller.refresh()
    runCurrent()
    assertTrue(controller.state.value.hasCompleted)
    assertFalse(controller.state.value.isLoading(hasContent = false))
    assertEquals("Unavailable", controller.state.value.errorMessage)
    assertTrue(controller.state.value.errorDetails.orEmpty().contains("IllegalStateException: Unavailable"))
    assertEquals(listOf<Pair<Throwable, String>>(failure to "matches.refresh"), telemetry.failures)
    assertEquals(listOf(TelemetryLevel.Warning to "matches.refresh failed"), telemetry.logs)

    controller.refresh()
    runCurrent()
    assertEquals(2, calls)
    assertEquals(RefreshState(hasCompleted = true), controller.state.value)
    assertEquals(1, telemetry.failures.size)
    assertEquals(1, telemetry.logs.size)
  }

  @Test
  fun cancellingTheOwnerStopsActiveAndQueuedWorkWithoutAnError() = runTest {
    var calls = 0
    var cancelled = false
    val telemetry = RecordingTelemetry()
    val controller = RefreshController(backgroundScope, TestNetworkMonitor(), telemetry = telemetry) {
      calls++
      try {
        awaitCancellation()
      } finally {
        cancelled = true
      }
    }

    controller.refresh()
    runCurrent()
    controller.refresh()
    backgroundScope.cancel()
    runCurrent()

    assertTrue(cancelled)
    assertEquals(1, calls)
    assertEquals(RefreshState(), controller.state.value)
    controller.refresh()
    runCurrent()
    assertEquals(1, calls)
    assertTrue(telemetry.failures.isEmpty())
    assertTrue(telemetry.logs.isEmpty())
  }

  @Test
  fun cancellationReturnedAsFailureIsNotReported() = runTest {
    val telemetry = RecordingTelemetry()
    val controller = RefreshController(backgroundScope, TestNetworkMonitor(), telemetry = telemetry) {
      Result.failure(CancellationException("Owner left the screen"))
    }

    controller.refresh()
    runCurrent()

    assertTrue(telemetry.failures.isEmpty())
    assertTrue(telemetry.logs.isEmpty())
    assertEquals(RefreshState(), controller.state.value)
  }

  @Test
  fun reportingFailureDoesNotInterruptRefreshRecovery() = runTest {
    val telemetry = object : TelemetryReporter by NoOpTelemetryReporter {
      override fun captureException(error: Throwable, operation: String) {
        throw IllegalStateException("Telemetry unavailable")
      }

      override fun log(level: TelemetryLevel, message: String) {
        throw IllegalStateException("Logging unavailable")
      }
    }
    var calls = 0
    val controller = RefreshController(backgroundScope, TestNetworkMonitor(), telemetry = telemetry) {
      calls++
      if (calls == 1) Result.failure(IllegalStateException("Refresh unavailable")) else Result.success(Unit)
    }

    controller.refresh()
    runCurrent()
    assertEquals("Refresh unavailable", controller.state.value.errorMessage)
    assertFalse(controller.state.value.isRefreshing)

    controller.refresh()
    runCurrent()
    assertEquals(2, calls)
    assertEquals(RefreshState(hasCompleted = true), controller.state.value)
  }

  private class RecordingTelemetry : TelemetryReporter by NoOpTelemetryReporter {
    val failures = mutableListOf<Pair<Throwable, String>>()
    val logs = mutableListOf<Pair<TelemetryLevel, String>>()

    override fun captureException(error: Throwable, operation: String) {
      failures += error to operation
    }

    override fun log(level: TelemetryLevel, message: String) {
      logs += level to message
    }
  }

  private class TestNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(online)
  }
}
