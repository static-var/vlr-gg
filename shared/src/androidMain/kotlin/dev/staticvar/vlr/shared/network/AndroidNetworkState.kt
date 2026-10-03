/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.network

import dev.staticvar.vlr.core.network.NetworkStatus

internal class AndroidNetworkState(
  private val publish: (NetworkStatus) -> Unit,
) {
  private val lock = Any()
  private var network: Long? = null
  private var closed = false

  fun available(network: Long) = synchronized(lock) {
    if (this.network != network) update(network, false)
  }

  fun update(network: Long?, validated: Boolean) = synchronized(lock) {
    if (closed) return
    this.network = network
    when {
      network == null -> publish(NetworkStatus.Offline)
      validated -> publish(NetworkStatus.Online)
      else -> publish(NetworkStatus.Unknown)
    }
  }

  fun lost(network: Long) = synchronized(lock) {
    if (this.network == network) update(null, false)
  }

  fun close() = synchronized(lock) {
    closed = true
  }
}
