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
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_loading
import vlr.shared_ui.generated.resources.shared_offline
import vlr.shared_ui.generated.resources.shared_ready_to_refresh
import vlr.shared_ui.generated.resources.shared_refreshing

/** Refresh control with an optional initial-loading animation. */
@Composable
public fun SharedRefreshButton(
  isLoading: Boolean,
  isRefreshing: Boolean,
  hasContent: Boolean,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  animateWhileLoading: Boolean = false,
) {
  val isOnline = LocalIsOnline.current
  val busy = isLoading || isRefreshing
  val refreshState = when {
    !isOnline -> stringResource(Res.string.shared_offline)
    isRefreshing -> stringResource(Res.string.shared_refreshing)
    isLoading -> stringResource(Res.string.shared_loading)
    else -> stringResource(Res.string.shared_ready_to_refresh)
  }
  PrismRefreshButton(
    isRefreshing = isOnline && ((hasContent && isRefreshing) || (animateWhileLoading && busy)),
    enabled = isOnline && !busy,
    onClick = onRefresh,
    modifier = modifier.semantics {
      stateDescription = refreshState
    },
  )
}
