/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.frame

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset

/** Border override and a solid shadow extending down and right from the face. */
@Immutable
public data class PrismFrameTokens(
  val border: BorderStroke? = null,
  val shadowColor: Color = Color.Transparent,
  val shadowOffset: DpOffset = DpOffset.Zero,
) {
  init {
    require(shadowOffset.x.value.isFinite() && shadowOffset.x.value >= 0f)
    require(shadowOffset.y.value.isFinite() && shadowOffset.y.value >= 0f)
  }
}

/** Frames are opt-in: nested surfaces stay flat unless their style selects a role. */
@Immutable
public data class PrismFrames(
  val panel: PrismFrameTokens = PrismFrameTokens(),
  val control: PrismFrameTokens = PrismFrameTokens(),
  val compact: PrismFrameTokens = PrismFrameTokens(),
)

internal val LocalPrismFrames = staticCompositionLocalOf { PrismFrames() }
