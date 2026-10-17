/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.designsystem.component.card.PrismCardMascotWidth
import dev.staticvar.designsystem.component.card.onCardMascotVisibilityChanged

@Composable
internal fun CardMascot(
  state: CardMascotState,
  character: MascotCharacter,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val id = remember(state) { MascotOwnerToken() }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val lifecycleState by lifecycle.currentStateAsState()
  val active = enabled && lifecycleState == Lifecycle.State.RESUMED
  var inViewport by remember { mutableStateOf(false) }
  LaunchedEffect(state, id, active, inViewport, lifecycle) {
    if (active && inViewport && lifecycle.currentState == Lifecycle.State.RESUMED) {
      state.register(id)
    } else {
      state.release(id)
    }
  }
  DisposableEffect(state, id) {
    onDispose { state.release(id) }
  }
  Box(
    modifier.onCardMascotVisibilityChanged { visible ->
      inViewport = visible
      if (!visible) state.release(id)
    }.clipToBounds().clearAndSetSemantics {},
  ) {
    if (active && inViewport && state.ownerId === id) {
      val artwork = Modifier.mascotPeekArtwork(PrismCardMascotWidth)
      when (character) {
        MascotCharacter.Lynx -> LynxMascot(artwork)
        MascotCharacter.Rosie -> RosieMascot(artwork)
      }
    }
  }
}
