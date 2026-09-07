/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.animation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

@Immutable
public data class PrismAnimationPreset(val durationMillis: Int, val easing: Easing) {
  public fun floatSpec(): FiniteAnimationSpec<Float> = tween(durationMillis = durationMillis, easing = easing)

  public fun intOffsetSpec(): FiniteAnimationSpec<IntOffset> = tween(durationMillis = durationMillis, easing = easing)

  public fun dpSpec(): FiniteAnimationSpec<Dp> = tween(durationMillis = durationMillis, easing = easing)

  public fun colorSpec(): FiniteAnimationSpec<Color> = tween(durationMillis = durationMillis, easing = easing)
}

@Immutable
public data class PrismAnimationTokens(
  val standard: PrismAnimationPreset = PrismAnimationPreset(durationMillis = 320, easing = FastOutSlowInEasing),
  val press: PrismAnimationPreset = PrismAnimationPreset(durationMillis = 100, easing = FastOutSlowInEasing),
  val selection: PrismAnimationPreset = standard,
  val navigationSelection: PrismAnimationPreset =
    PrismAnimationPreset(durationMillis = 800, easing = FastOutSlowInEasing),
  val sheetEnter: PrismAnimationPreset = PrismAnimationPreset(durationMillis = 280, easing = FastOutSlowInEasing),
  val sheetExit: PrismAnimationPreset = sheetEnter,
  val scrimEnter: PrismAnimationPreset = sheetEnter,
  val scrimExit: PrismAnimationPreset = sheetEnter,
  val slowFade: PrismAnimationPreset = PrismAnimationPreset(durationMillis = 460, easing = LinearOutSlowInEasing),
)

internal val DefaultPrismAnimations: PrismAnimationTokens = PrismAnimationTokens()

internal val LocalPrismAnimations = staticCompositionLocalOf { DefaultPrismAnimations }
