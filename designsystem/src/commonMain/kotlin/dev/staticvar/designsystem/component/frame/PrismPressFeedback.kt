/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.frame

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import dev.staticvar.designsystem.prism.Prism
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance

@Composable
internal fun rememberPrismPressProgress(interactionSource: InteractionSource, enabled: Boolean = true): State<Float> {
  val progress = remember { Animatable(0f) }
  val animation = Prism.anim.press
  LaunchedEffect(interactionSource, enabled, animation) {
    progress.snapTo(0f)
    if (!enabled) return@LaunchedEffect
    val activePresses = mutableSetOf<PressInteraction.Press>()
    val spec = animation.floatSpec()
    interactionSource.interactions
      .filterIsInstance<PressInteraction>()
      .filter { interaction ->
        when (interaction) {
          is PressInteraction.Press -> activePresses.add(interaction) && activePresses.size == 1
          is PressInteraction.Release -> activePresses.remove(interaction.press) && activePresses.isEmpty()
          is PressInteraction.Cancel -> activePresses.remove(interaction.press) && activePresses.isEmpty()
          else -> false
        }
      }
      .collectLatest { interaction ->
        when (interaction) {
          is PressInteraction.Press -> progress.animateTo(1f, spec)
          is PressInteraction.Release -> {
            // Complete quick taps even when press and release arrive in the same frame.
            if (progress.value < 1f) progress.animateTo(1f, spec)
            progress.animateTo(0f, spec)
          }
          is PressInteraction.Cancel -> progress.animateTo(0f, spec)
        }
      }
  }
  return progress.asState()
}
