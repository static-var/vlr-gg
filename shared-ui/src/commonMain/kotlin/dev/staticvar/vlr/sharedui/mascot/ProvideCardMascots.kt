/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import dev.staticvar.designsystem.component.card.LocalPrismCardMascot

private val LocalMascotPauses = compositionLocalOf<MutableMap<Any, Unit>?> { null }

@Composable
public fun PauseCardMascots(paused: Boolean) {
  val pauses = LocalMascotPauses.current
  DisposableEffect(pauses, paused) {
    val token = Any()
    if (paused) pauses?.put(token, Unit)
    onDispose { pauses?.remove(token) }
  }
}

/** Shares visit ownership and preferences; each card owns its mascot's placement. */
@Composable
public fun ProvideCardMascots(
  screenKey: String,
  isActive: Boolean,
  probabilityPercent: Int,
  content: @Composable () -> Unit,
) {
  val state = rememberCardMascot(screenKey, probabilityPercent)
  val character = LocalMascotCharacter.current
  val pauses = remember(screenKey) { mutableStateMapOf<Any, Unit>() }
  val enabled = isActive && pauses.isEmpty() && probabilityPercent > 0
  CompositionLocalProvider(
    LocalMascotPauses provides pauses,
    LocalPrismCardMascot provides character?.let { selectedCharacter ->
      { modifier -> CardMascot(state, selectedCharacter, enabled, modifier) }
    },
  ) { content() }
}
