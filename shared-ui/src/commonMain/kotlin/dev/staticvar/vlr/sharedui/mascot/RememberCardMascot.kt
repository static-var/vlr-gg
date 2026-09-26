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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.random.Random

public class MascotOwnerToken

@Stable
public class CardMascotState internal constructor(private val allowed: Boolean) {
  public var ownerId: MascotOwnerToken? by mutableStateOf(null)
    private set

  private var hasAppeared: Boolean = false
  private val candidates = mutableStateListOf<MascotOwnerToken>()

  internal val visibleCandidates: List<MascotOwnerToken> get() = candidates.toList()

  internal fun register(id: MascotOwnerToken) {
    if (allowed && !hasAppeared && id !in candidates) candidates.add(id)
  }

  internal fun selectOwner(random: Random) {
    if (!allowed || hasAppeared || candidates.isEmpty()) return
    ownerId = candidates.random(random)
    hasAppeared = true
  }

  public fun release(id: MascotOwnerToken) {
    candidates.remove(id)
    if (ownerId === id) ownerId = null
  }

  internal fun dispose() {
    ownerId = null
    candidates.clear()
    hasAppeared = true
  }
}

/** Rolls once per screen visit, then chooses among cards that remain visible after layout settles. */
@Composable
public fun rememberCardMascot(
  screenKey: String,
  probabilityPercent: Int,
  random: Random = Random.Default,
): CardMascotState {
  val state = remember(screenKey) { CardMascotState(allowed = random.nextInt(100) < probabilityPercent) }

  LaunchedEffect(state, state.visibleCandidates) {
    if (state.visibleCandidates.isNotEmpty()) {
      delay(250)
      state.selectOwner(random)
    }
  }

  DisposableEffect(state) {
    onDispose { state.dispose() }
  }
  return state
}
