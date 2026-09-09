/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.slider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import dev.staticvar.designsystem.prism.Prism

/** Owns the rail, stop marks, and thumb appearance without changing slider interactions. */
@Immutable
public sealed interface PrismSliderStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val layoutHeight: Dp
    get() = Prism.dimens.spacingL

  @get:Composable
  @get:ReadOnlyComposable
  public val labelTextStyle: TextStyle
    get() = Prism.typography.bodySmall

  @get:Composable
  @get:ReadOnlyComposable
  public val labelVerticalPadding: Dp
    get() = Prism.dimens.spacingXs

  @get:Composable
  @get:ReadOnlyComposable
  public val thumbSize: DpSize
    get() = DpSize(Prism.dimens.spacingXs, Prism.dimens.iconS)

  @get:Composable
  @get:ReadOnlyComposable
  public val trackHeight: Dp
    get() = Prism.dimens.iconS

  @get:Composable
  @get:ReadOnlyComposable
  public val inactiveThickness: Dp
    get() = Prism.dimens.strokeDefault

  @get:Composable
  @get:ReadOnlyComposable
  public val activeThickness: Dp
    get() = Prism.dimens.strokeDefault + Prism.dimens.strokeThick

  @get:Composable
  @get:ReadOnlyComposable
  public val tickHeight: Dp
    get() = Prism.dimens.spacingXs + Prism.dimens.strokeThick

  @Composable
  @ReadOnlyComposable
  public fun trackColor(enabled: Boolean): Color =
    if (enabled) Prism.color.strokeVariant else Prism.color.surfaceDim

  @Composable
  @ReadOnlyComposable
  public fun activeColor(enabled: Boolean): Color =
    if (enabled) Prism.color.accent else Prism.color.labelColor

  @Composable
  @ReadOnlyComposable
  public fun labelColor(enabled: Boolean, selected: Boolean): Color = when {
    !enabled -> Prism.color.labelColor
    selected -> Prism.color.accent
    else -> Prism.color.bodyColor
  }

  @Composable
  @ReadOnlyComposable
  public fun thumbColor(enabled: Boolean, interacting: Boolean): Color = when {
    !enabled -> Prism.color.labelColor
    interacting -> Prism.color.titleColor
    else -> Prism.color.accent
  }

  /** A slim accent rail with square stop marks and a narrow rectangular thumb. */
  public data object Rail : PrismSliderStyle
}
