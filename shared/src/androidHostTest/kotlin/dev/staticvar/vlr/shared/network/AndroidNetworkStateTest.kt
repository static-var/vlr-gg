/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.network

import dev.staticvar.vlr.core.network.NetworkStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidNetworkStateTest {
  @Test
  fun unvalidatedNetworkBecomesOfflineAfterFiveSeconds() = runTest {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState(this) { status = it }

    state.update(network = 1L, validated = false)
    runCurrent()
    advanceTimeBy(4_999)
    runCurrent()
    assertEquals(NetworkStatus.Unknown, status)

    advanceTimeBy(1)
    runCurrent()
    assertEquals(NetworkStatus.Offline, status)
  }

  @Test
  fun validationPublishesOnlineAndCancelsPendingTimeout() = runTest {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState(this) { status = it }

    state.update(network = 1L, validated = false)
    runCurrent()
    advanceTimeBy(2_000)
    state.update(network = 1L, validated = true)
    assertEquals(NetworkStatus.Online, status)

    advanceTimeBy(5_000)
    runCurrent()
    assertEquals(NetworkStatus.Online, status)
  }

  @Test
  fun repeatedUnvalidatedUpdatesDoNotExtendTheDeadline() = runTest {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState(this) { status = it }

    state.update(network = 1L, validated = false)
    runCurrent()
    advanceTimeBy(4_000)
    state.update(network = 1L, validated = false)
    runCurrent()
    advanceTimeBy(1_000)
    runCurrent()

    assertEquals(NetworkStatus.Offline, status)
  }

  @Test
  fun replacementNetworkGetsItsOwnValidationDeadline() = runTest {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState(this) { status = it }

    state.update(network = 1L, validated = false)
    runCurrent()
    advanceTimeBy(4_000)
    state.update(network = 2L, validated = false)
    runCurrent()
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(NetworkStatus.Unknown, status)

    advanceTimeBy(4_000)
    runCurrent()
    assertEquals(NetworkStatus.Offline, status)
  }

  @Test
  fun losingAnOldNetworkDoesNotDisconnectTheReplacement() = runTest {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState(this) { status = it }

    state.update(network = 1L, validated = true)
    state.update(network = 2L, validated = true)
    state.lost(network = 1L)
    assertEquals(NetworkStatus.Online, status)

    state.lost(network = 2L)
    assertEquals(NetworkStatus.Offline, status)
  }

  @Test
  fun absentNetworkPublishesOfflineImmediately() = runTest {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState(this) { status = it }

    state.update(network = null, validated = false)

    assertEquals(NetworkStatus.Offline, status)
  }

  @Test
  fun closingCancelsPendingValidation() = runTest {
    val statuses = mutableListOf<NetworkStatus>()
    val state = AndroidNetworkState(this) { statuses += it }

    state.update(network = 1L, validated = false)
    runCurrent()
    state.close()
    val beforeTimeout = statuses.toList()
    advanceTimeBy(5_000)
    runCurrent()

    assertEquals(beforeTimeout, statuses)
  }
}
