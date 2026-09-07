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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter

@Composable
internal fun RefreshWhenResumed(isOnline: StateFlow<Boolean>, onRefresh: () -> Unit) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val refresh by rememberUpdatedState(onRefresh)

  LaunchedEffect(lifecycle, isOnline) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      isOnline.filter { it }.collect { refresh() }
    }
  }
}
