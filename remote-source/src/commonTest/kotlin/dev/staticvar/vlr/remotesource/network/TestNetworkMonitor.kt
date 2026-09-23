/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Mutable connectivity snapshot for transport policy tests. */
internal class TestNetworkMonitor(initial: NetworkStatus = NetworkStatus.Online) : NetworkMonitor {
  private val mutableStatus = MutableStateFlow(initial)
  override val status: StateFlow<NetworkStatus> = mutableStatus

  fun setStatus(value: NetworkStatus) {
    mutableStatus.value = value
  }
}
