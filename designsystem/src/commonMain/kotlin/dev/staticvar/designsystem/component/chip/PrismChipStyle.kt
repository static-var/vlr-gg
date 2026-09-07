/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.chip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/** Theme-backed frame and selection colors for [PrismChipGroup]. */
@Immutable
public sealed interface PrismChipStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val frame: PrismFrameTokens
    get() = Prism.frames.compact

  @get:Composable
  @get:ReadOnlyComposable
  public val borderWidth: Dp
    get() = frame.border?.width ?: Prism.dimens.strokeDefault

  @Composable
  @ReadOnlyComposable
  public fun containerColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun contentColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun borderColor(selected: Boolean, enabled: Boolean): Color

  public data object Default : PrismChipStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.surfaceDim
      selected -> Prism.color.accentSubtle
      else -> Prism.color.background
    }

    @Composable
    @ReadOnlyComposable
    override fun contentColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.captionColor
      selected -> Prism.color.accent
      else -> Prism.color.bodyColor
    }

    @Composable
    @ReadOnlyComposable
    override fun borderColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.stroke
      selected -> Prism.color.accent
      else -> Prism.color.stroke
    }
  }
}
