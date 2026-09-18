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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter

@Composable
internal fun RefreshWhenResumed(networkStatus: StateFlow<NetworkStatus>, onRefresh: () -> Unit) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val refresh by rememberUpdatedState(onRefresh)

  LaunchedEffect(lifecycle, networkStatus) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      networkStatus.filter { it == NetworkStatus.Online }.collect { refresh() }
    }
  }
}
