/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.random.Random

@Stable
public class CardMascotState internal constructor(private val allowed: Boolean) {
  public var ownerId: Any? by mutableStateOf(null)
    private set

  private var hasAppeared: Boolean = false

  public fun tryAcquire(id: Any): Boolean {
    if (ownerId == id) return true
    if (!allowed || hasAppeared) return false
    if (ownerId != null) return false

    ownerId = id
    hasAppeared = true
    return true
  }

  public fun release(id: Any) {
    if (ownerId == id) ownerId = null
  }

  internal fun dispose() {
    ownerId = null
    hasAppeared = true
  }
}

/** Rolls once per screen visit; a visible card can claim the single allowed appearance. */
@Composable
public fun rememberCardMascot(
  screenKey: String,
  probabilityPercent: Int,
  random: Random = Random.Default,
): CardMascotState {
  val state = remember(screenKey) { CardMascotState(allowed = random.nextInt(100) < probabilityPercent) }

  DisposableEffect(state) {
    onDispose { state.dispose() }
  }
  return state
}
