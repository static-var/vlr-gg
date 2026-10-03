/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.core.telemetry.NoOpTelemetryReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshWhenResumedTest {
  @Test
  fun offlineResumeAndManualRetryBothAttemptRequests() = runTest {
    val status = MutableStateFlow(NetworkStatus.Offline)
    var requests = 0
    val monitor = object : NetworkMonitor { override val status = status }
    val controller = RefreshController(backgroundScope, monitor, NoOpTelemetryReporter) {
      requests++
      Result.success(Unit)
    }
    backgroundScope.launch { refreshOnConnectivity(status, controller::refresh) }
    runCurrent()
    assertEquals(1, requests)

    controller.refresh()
    runCurrent()
    assertEquals(2, requests)

    status.value = NetworkStatus.Unknown
    runCurrent()
    assertEquals(3, requests)

    status.value = NetworkStatus.Online
    runCurrent()
    assertEquals(4, requests)

    status.value = NetworkStatus.Offline
    runCurrent()
    assertEquals(4, requests)

    status.value = NetworkStatus.Online
    runCurrent()
    assertEquals(5, requests)
  }

  @Test
  fun validationRetriesAnAttemptThatFailedBeforeTheNetworkWasReady() = runTest {
    val status = MutableStateFlow(NetworkStatus.Unknown)
    val monitor = object : NetworkMonitor { override val status = status }
    var requests = 0
    val controller = RefreshController(backgroundScope, monitor, NoOpTelemetryReporter) {
      requests++
      if (requests == 1) Result.failure(IllegalStateException("Not ready")) else Result.success(Unit)
    }
    backgroundScope.launch { refreshOnConnectivity(status, controller::refresh) }
    runCurrent()
    assertEquals(1, requests)
    assertEquals("Not ready", controller.state.value.errorMessage)

    status.value = NetworkStatus.Online
    runCurrent()
    assertEquals(2, requests)
    assertEquals(null, controller.state.value.errorMessage)
  }
}
