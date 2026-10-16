/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.divider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismDivider].
 *
 * Built-in styles map to the supported divider weights: hairline, default, and strong.
 * Each style owns the divider thickness, color, and end treatment.
 */
@Immutable
public sealed interface PrismDividerStyle {
  /** Thickness of the divider line. */
  public val thickness: Dp

  /** Color used to draw the divider line. */
  @get:Composable
  @get:ReadOnlyComposable
  public val color: Color

  /** Length of each end cap along the divider. */
  @get:Composable
  @get:ReadOnlyComposable
  public val endCapWidth: Dp

  /** Thickness of the end cap marks across the divider. */
  @get:Composable
  @get:ReadOnlyComposable
  public val endCapHeight: Dp

  /** Color used for the divider end cap marks. */
  @get:Composable
  @get:ReadOnlyComposable
  public val endCapColor: Color

  /** Fine 0.5dp separator for compact grouped content. */
  public data object Hairline : PrismDividerStyle {
    override val thickness: Dp = 0.5.dp

    override val color: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.divider

    override val endCapWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingXs

    override val endCapHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingXs

    override val endCapColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.stroke
  }

  /** Standard 1dp separator for most content boundaries. */
  public data object Default : PrismDividerStyle {
    override val thickness: Dp = 1.dp

    override val color: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.stroke

    override val endCapWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingS

    override val endCapHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingXs

    override val endCapColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.strokeVariant
  }

  /** Strong 2dp separator for high-emphasis structural breaks. */
  public data object Strong : PrismDividerStyle {
    override val thickness: Dp = 2.dp

    override val color: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.strokeVariant

    override val endCapWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingM

    override val endCapHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingS

    override val endCapColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.accent
  }
}
