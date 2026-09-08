/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

public data class MascotCue(
  val id: String,
  val message: String,
  val priority: Int,
  val secondaryMessage: String? = null,
)

/** Shows once per screen visit. Finishing or dismissing does not allow another appearance. */
@Stable
public class MascotState internal constructor() {
  public var cue: MascotCue? by mutableStateOf(null)
    private set

  internal var hasShown: Boolean = false
    private set

  public fun onFinished() {
    cue = null
  }

  internal fun dismissIfUnavailable(candidates: List<MascotCue>, canShow: Boolean) {
    if (!canShow || candidates.none { it.id == cue?.id }) cue = null
  }

  internal fun showOnce(candidates: List<MascotCue>) {
    if (hasShown) return
    if (candidates.isEmpty()) return
    hasShown = true
    cue = candidates.sortedWith(compareByDescending<MascotCue> { it.priority }.thenBy { it.id }).first()
  }
}

/**
 * Call outside lazy items and pass only candidates derived from the screen's current content.
 * [screenKey] identifies the visit; ordinary data updates must not change it. The caller supplies
 * visibility, loading, scrolling, and modal state so this helper needs no observers or requests.
 */
@Composable
public fun rememberMascot(
  screenKey: String,
  candidates: List<MascotCue>,
  isScreenActive: Boolean,
  isContentReady: Boolean,
  isInteracting: Boolean,
  settleDelayMillis: Long = 1_500L,
): MascotState {
  require(settleDelayMillis >= 0L) { "Settle delay must not be negative" }
  val state = remember(screenKey) { MascotState() }
  val canShow = isScreenActive && isContentReady && !isInteracting

  DisposableEffect(state) {
    onDispose { state.onFinished() }
  }

  LaunchedEffect(state, candidates, canShow, settleDelayMillis) {
    state.dismissIfUnavailable(candidates, canShow)
    if (!canShow) return@LaunchedEffect
    if (candidates.isEmpty()) return@LaunchedEffect
    if (state.hasShown) return@LaunchedEffect
    delay(settleDelayMillis)
    state.showOnce(candidates)
  }
  return state
}
