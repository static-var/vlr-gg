/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.refresh

import dev.staticvar.vlr.core.network.NetworkMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
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
    assertEquals(RefreshState(), controller.state.value)
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
    val controller = RefreshController(backgroundScope, TestNetworkMonitor()) {
      calls++
      if (calls == 1) Result.failure(IllegalStateException("Unavailable")) else Result.success(Unit)
    }

    controller.refresh()
    runCurrent()
    assertEquals(RefreshState(errorMessage = "Unavailable"), controller.state.value)

    controller.refresh()
    runCurrent()
    assertEquals(2, calls)
    assertEquals(RefreshState(), controller.state.value)
  }

  @Test
  fun cancellingTheOwnerStopsActiveAndQueuedWorkWithoutAnError() = runTest {
    var calls = 0
    var cancelled = false
    val controller = RefreshController(backgroundScope, TestNetworkMonitor()) {
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
  }

  private class TestNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(online)
  }
}
