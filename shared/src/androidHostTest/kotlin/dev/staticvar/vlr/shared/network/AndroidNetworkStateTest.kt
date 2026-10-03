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
  fun unvalidatedNetworkStaysUnknownAfterFiveSeconds() = runTest {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState { status = it }

    state.update(network = 1L, validated = false)
    runCurrent()
    advanceTimeBy(60_000)
    runCurrent()
    assertEquals(NetworkStatus.Unknown, status)

    state.update(network = 1L, validated = true)
    assertEquals(NetworkStatus.Online, status)
  }

  @Test
  fun availabilityPreservesValidationFromTheInitialProbe() {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState { status = it }

    state.update(network = 1L, validated = true)
    state.available(network = 1L)

    assertEquals(NetworkStatus.Online, status)
  }

  @Test
  fun replacementNetworkWaitsForItsOwnCapabilities() {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState { status = it }

    state.update(network = 1L, validated = true)
    state.available(network = 2L)
    assertEquals(NetworkStatus.Unknown, status)

    state.update(network = 2L, validated = true)
    assertEquals(NetworkStatus.Online, status)

    state.update(network = 2L, validated = false)
    assertEquals(NetworkStatus.Unknown, status)
  }

  @Test
  fun losingAnOldNetworkDoesNotDisconnectTheReplacement() {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState { status = it }

    state.update(network = 1L, validated = true)
    state.update(network = 2L, validated = true)
    state.lost(network = 1L)
    assertEquals(NetworkStatus.Online, status)

    state.lost(network = 2L)
    assertEquals(NetworkStatus.Offline, status)
  }

  @Test
  fun validatedCallbackRecoversFromAnAbsentInitialNetwork() {
    var status = NetworkStatus.Unknown
    val state = AndroidNetworkState { status = it }

    state.update(network = null, validated = false)
    assertEquals(NetworkStatus.Offline, status)

    state.available(network = 1L)
    state.update(network = 1L, validated = true)
    assertEquals(NetworkStatus.Online, status)
  }

  @Test
  fun closingIgnoresFurtherCallbacks() {
    val statuses = mutableListOf<NetworkStatus>()
    val state = AndroidNetworkState { statuses += it }

    state.update(network = 1L, validated = true)
    state.close()
    state.lost(network = 1L)
    state.available(network = 2L)
    state.update(network = 2L, validated = false)

    assertEquals(listOf(NetworkStatus.Online), statuses)
  }
}
