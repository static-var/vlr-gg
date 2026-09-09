/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.refresh

import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.core.telemetry.TelemetryLevel
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import dev.staticvar.vlr.core.telemetry.captureExceptionSafely
import dev.staticvar.vlr.core.telemetry.logSafely
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RefreshController(
  scope: CoroutineScope,
  networkMonitor: NetworkMonitor,
  telemetry: TelemetryReporter = AppTelemetry,
  operation: String = "refresh",
  action: suspend () -> Result<Unit>,
) {
  private val requests = Channel<Unit>(Channel.CONFLATED)
  val state: StateFlow<RefreshState>
    field = MutableStateFlow(RefreshState())

  init {
    scope.launch {
      for (request in requests) {
        networkMonitor.isOnline.first { it }
        state.update { it.copy(isRefreshing = true, errorMessage = null, errorDetails = null) }
        try {
          action().getOrThrow()
          currentCoroutineContext().ensureActive()
          state.update { it.copy(hasCompleted = true) }
        } catch (cancellation: CancellationException) {
          throw cancellation
        } catch (error: Exception) {
          currentCoroutineContext().ensureActive()
          telemetry.captureExceptionSafely(error, operation)
          telemetry.logSafely(TelemetryLevel.Warning, "$operation failed")
          state.update {
            it.copy(
              hasCompleted = true,
              errorMessage = error.message ?: "Could not refresh data",
              errorDetails = error.stackTraceToString(),
            )
          }
        } finally {
          state.update { it.copy(isRefreshing = false) }
        }
      }
    }.invokeOnCompletion { requests.close() }
  }

  fun refresh() {
    requests.trySend(Unit)
  }
}
