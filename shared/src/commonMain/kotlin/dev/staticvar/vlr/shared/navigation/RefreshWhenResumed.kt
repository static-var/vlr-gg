/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.staticvar.vlr.core.network.NetworkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun RefreshWhenResumed(networkStatus: StateFlow<NetworkStatus>, onRefresh: () -> Unit) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val refresh by rememberUpdatedState(onRefresh)

  LaunchedEffect(lifecycle, networkStatus) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      refreshOnConnectivity(networkStatus) { refresh() }
    }
  }
}

internal suspend fun refreshOnConnectivity(networkStatus: Flow<NetworkStatus>, onRefresh: () -> Unit) {
  var initial = true
  networkStatus.distinctUntilChanged().collect { status ->
    if (initial || status != NetworkStatus.Offline) onRefresh()
    initial = false
  }
}
