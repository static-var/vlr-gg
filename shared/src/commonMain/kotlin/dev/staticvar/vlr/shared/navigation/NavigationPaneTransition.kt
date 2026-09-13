/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress

internal data class GroupedPaneState(val groupKey: Any, val detailEntry: NavEntry<*>?)

internal val LocalGroupedPaneTransition = compositionLocalOf<Transition<GroupedPaneState?>?> { null }

@Composable
internal fun rememberGroupedPaneTransition(
  currentPane: GroupedPaneState?,
  previousPane: GroupedPaneState?,
  gestureTransition: NavigationEventTransitionState,
): Transition<GroupedPaneState?> {
  val state = remember { SeekableTransitionState(currentPane) }
  val transition = rememberTransition(state, label = "navigation detail pane")
  LaunchedEffect(currentPane, previousPane, gestureTransition) {
    when (gestureTransition) {
      is InProgress -> state.seekTo(gestureTransition.latestEvent.progress, previousPane)
      else -> state.animateTo(currentPane)
    }
  }
  return transition
}
