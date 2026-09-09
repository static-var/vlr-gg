/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.staticvar.designsystem.component.button.PrismRefreshButton

/** Cached refreshes animate here; initial loads keep the screen's centered loading indicator. */
@Composable
public fun SharedRefreshButton(
  isLoading: Boolean,
  isRefreshing: Boolean,
  hasContent: Boolean,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isOnline = LocalIsOnline.current
  val busy = isLoading || isRefreshing
  PrismRefreshButton(
    isRefreshing = isOnline && hasContent && isRefreshing,
    enabled = isOnline && !busy,
    onClick = onRefresh,
    modifier = modifier.semantics {
      stateDescription = when {
        !isOnline -> "Offline"
        isRefreshing -> "Refreshing"
        isLoading -> "Loading"
        else -> "Ready to refresh"
      }
    },
  )
}
