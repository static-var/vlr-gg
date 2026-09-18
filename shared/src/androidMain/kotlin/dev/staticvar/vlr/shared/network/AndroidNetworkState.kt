/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.network

import dev.staticvar.vlr.core.network.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AndroidNetworkState(
  private val scope: CoroutineScope,
  private val publish: (NetworkStatus) -> Unit,
) {
  private val lock = Any()
  private var network: Long? = null
  private var validated = false
  private var initialized = false
  private var closed = false
  private var generation = 0L
  private var validation: Job? = null

  fun update(network: Long?, validated: Boolean) = synchronized(lock) {
    if (closed || initialized && this.network == network && this.validated == validated) return
    initialized = true
    this.network = network
    this.validated = validated
    validation?.cancel()
    val currentGeneration = ++generation
    when {
      network == null -> publish(NetworkStatus.Offline)
      validated -> publish(NetworkStatus.Online)
      else -> {
        publish(NetworkStatus.Unknown)
        validation = scope.launch {
          delay(5_000)
          synchronized(lock) {
            if (!closed && generation == currentGeneration) publish(NetworkStatus.Offline)
          }
        }
      }
    }
  }

  fun lost(network: Long) = synchronized(lock) {
    if (this.network == network) update(null, false)
  }

  fun close() = synchronized(lock) {
    closed = true
    validation?.cancel()
  }
}
