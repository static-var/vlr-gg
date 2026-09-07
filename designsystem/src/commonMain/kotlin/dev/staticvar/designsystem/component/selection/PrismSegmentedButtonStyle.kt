/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/** Visual tokens for [PrismSegmentedButtons], including selected and disabled treatments. */
@Immutable
public sealed interface PrismSegmentedButtonStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val frame: PrismFrameTokens
    get() = Prism.frames.control

  @get:Composable
  @get:ReadOnlyComposable
  public val colors: SegmentedButtonColors

  @get:Composable
  @get:ReadOnlyComposable
  public val shape: CornerBasedShape

  @get:Composable
  @get:ReadOnlyComposable
  public val borderWidth: Dp

  @get:Composable
  @get:ReadOnlyComposable
  public val minHeight: Dp

  @get:Composable
  @get:ReadOnlyComposable
  public val labelStyle: TextStyle

  @Composable
  @ReadOnlyComposable
  public fun borderColor(enabled: Boolean): Color

  /** Outlined choices with a subtle accent fill and the active theme's small corners. */
  public data object Outlined : PrismSegmentedButtonStyle {
    override val colors: SegmentedButtonColors
      @Composable
      @ReadOnlyComposable
      get() = SegmentedButtonColors(
        activeContainerColor = Prism.color.accentSubtle,
        activeContentColor = Prism.color.accent,
        activeBorderColor = Prism.color.stroke,
        inactiveContainerColor = Prism.color.surface,
        inactiveContentColor = Prism.color.bodyColor,
        inactiveBorderColor = Prism.color.stroke,
        disabledActiveContainerColor = Prism.color.surfaceDim,
        disabledActiveContentColor = Prism.color.captionColor,
        disabledActiveBorderColor = Prism.color.strokeVariant,
        disabledInactiveContainerColor = Prism.color.surfaceDim,
        disabledInactiveContentColor = Prism.color.captionColor,
        disabledInactiveBorderColor = Prism.color.strokeVariant,
      )

    override val shape: CornerBasedShape
      @Composable
      @ReadOnlyComposable
      get() = Prism.shapes.small

    override val borderWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = frame.border?.width ?: Prism.dimens.strokeDefault

    override val minHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.controlHeight

    override val labelStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = Prism.typography.button

    @Composable
    @ReadOnlyComposable
    override fun borderColor(enabled: Boolean): Color = if (enabled) Prism.color.stroke else Prism.color.strokeVariant
  }
}
