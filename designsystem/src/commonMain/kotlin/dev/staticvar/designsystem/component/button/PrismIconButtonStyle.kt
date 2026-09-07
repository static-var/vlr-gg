/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/**
 * Visual treatment for a [PrismIconButton].
 *
 * Size remains a layout concern on [PrismIconButtonSize]; this style owns only the icon button
 * surface, icon color, and border for enabled and selected states.
 */
@Immutable
public sealed interface PrismIconButtonStyle {
  /** Theme frame applied outside the component face. */
  public val frame: PrismFrameTokens
    @Composable
    @ReadOnlyComposable
    get() = Prism.frames.compact

  /** Container color used for the current enabled and selected state. */
  @Composable
  @ReadOnlyComposable
  public fun containerColor(enabled: Boolean, selected: Boolean): Color

  /** Icon tint used for the current enabled and selected state. */
  @Composable
  @ReadOnlyComposable
  public fun iconColor(enabled: Boolean, selected: Boolean): Color

  /** Border drawn around the icon button surface. */
  @Composable
  @ReadOnlyComposable
  public fun border(enabled: Boolean, selected: Boolean): BorderStroke

  /** Default square bordered icon button style. */
  public data object Bordered : PrismIconButtonStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(enabled: Boolean, selected: Boolean): Color = when {
      !enabled -> Prism.color.surfaceDim
      selected -> Prism.color.accentSubtle
      else -> Prism.color.surface
    }

    @Composable
    @ReadOnlyComposable
    override fun iconColor(enabled: Boolean, selected: Boolean): Color =
      if (enabled) Prism.color.titleColor else Prism.color.labelColor

    @Composable
    @ReadOnlyComposable
    override fun border(enabled: Boolean, selected: Boolean): BorderStroke = BorderStroke(
      width = Prism.dimens.strokeDefault,
      color =
      when {
        !enabled -> Prism.color.stroke
        selected -> Prism.color.strokeVariant
        else -> Prism.color.stroke
      },
    )
  }
}
