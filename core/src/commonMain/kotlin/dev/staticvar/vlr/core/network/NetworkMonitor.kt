/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.network

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
  val status: StateFlow<NetworkStatus>
}

enum class NetworkStatus {
  Unknown,
  Online,
  Offline,
}
