/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.refresh

import dev.staticvar.vlr.core.network.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RefreshController(
  scope: CoroutineScope,
  networkMonitor: NetworkMonitor,
  action: suspend () -> Result<Unit>,
) {
  private val requests = Channel<Unit>(Channel.CONFLATED)
  private val mutableState = MutableStateFlow(RefreshState())
  val state: StateFlow<RefreshState> = mutableState.asStateFlow()

  init {
    scope.launch {
      for (request in requests) {
        networkMonitor.isOnline.first { it }
        mutableState.value = RefreshState(isRefreshing = true)
        try {
          action().getOrThrow()
          currentCoroutineContext().ensureActive()
        } catch (cancellation: CancellationException) {
          throw cancellation
        } catch (error: Exception) {
          currentCoroutineContext().ensureActive()
          mutableState.update { it.copy(errorMessage = error.message ?: "Could not refresh data") }
        } finally {
          mutableState.update { it.copy(isRefreshing = false) }
        }
      }
    }.invokeOnCompletion { requests.close() }
  }

  fun refresh() {
    requests.trySend(Unit)
  }
}
