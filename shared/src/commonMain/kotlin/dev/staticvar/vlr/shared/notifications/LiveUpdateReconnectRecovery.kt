/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Retries pending live-update writes after a confirmed return from offline status. */
internal class LiveUpdateReconnectRecovery(
  networkMonitor: NetworkMonitor,
  private val tokenUploader: PushTokenRegistrationUploader,
  private val favoriteSync: FavoriteLiveUpdateCoordinator,
  appScope: CoroutineScope,
) {
  init {
    appScope.launch {
      var wasOffline = false
      networkMonitor.status.collect { status ->
        when (status) {
          NetworkStatus.Offline -> wasOffline = true
          NetworkStatus.Online -> if (wasOffline) {
            wasOffline = false
            tokenUploader.retry()
            favoriteSync.retry()
          }
          NetworkStatus.Unknown -> Unit
        }
      }
    }
  }
}
