/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
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
  val id = remember(state) { Any() }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val lifecycleState by lifecycle.currentStateAsState()
  val active = enabled && lifecycleState == Lifecycle.State.RESUMED
  var inViewport by remember { mutableStateOf(false) }
  LaunchedEffect(state, id, active, inViewport, lifecycle) {
    if (active && inViewport && lifecycle.currentState == Lifecycle.State.RESUMED) {
      state.tryAcquire(id)
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
      // Crop the artwork's transparent margin so its paws meet the host's bottom edge.
      val artwork = Modifier.offset(y = (-2).dp)
        .wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(PrismCardMascotWidth)
      when (character) {
        MascotCharacter.Lynx -> LynxMascot(artwork)
        MascotCharacter.Rosie -> RosieMascot(artwork)
      }
    }
  }
}
