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
 * Visual treatment for a [PrismButton].
 *
 * A style owns the button colors and border used by the built-in Prism button variants. Built-in
 * styles read from [Prism] tokens so they adapt to the active light or dark theme.
 */
@Immutable
public sealed interface PrismButtonStyle {
  /** Theme frame applied outside the component face. */
  public val frame: PrismFrameTokens
    @Composable
    @ReadOnlyComposable
    get() = Prism.frames.control

  /** Container color used for the current enabled state. */
  @Composable
  @ReadOnlyComposable
  public fun containerColor(enabled: Boolean): Color

  /** Content color used for text and icons in the current enabled state. */
  @Composable
  @ReadOnlyComposable
  public fun contentColor(enabled: Boolean): Color

  /** Border drawn around the button in the current enabled state. */
  @Composable
  @ReadOnlyComposable
  public fun border(enabled: Boolean): BorderStroke

  /** Accent-filled call-to-action style. */
  public data object Primary : PrismButtonStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(enabled: Boolean): Color =
      if (enabled) Prism.color.primaryAction else Prism.color.surfaceDim

    @Composable
    @ReadOnlyComposable
    override fun contentColor(enabled: Boolean): Color =
      if (enabled) Prism.color.onPrimaryAction else Prism.color.labelColor

    @Composable
    @ReadOnlyComposable
    override fun border(enabled: Boolean): BorderStroke = BorderStroke(
      width = Prism.dimens.strokeThick,
      color = if (enabled) Prism.color.primaryAction else Prism.color.stroke,
    )
  }

  /** Flat surface action with a standard outline. */
  public data object Secondary : PrismButtonStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(enabled: Boolean): Color = if (enabled) Prism.color.surface else Prism.color.surfaceDim

    @Composable
    @ReadOnlyComposable
    override fun contentColor(enabled: Boolean): Color = if (enabled) Prism.color.titleColor else Prism.color.labelColor

    @Composable
    @ReadOnlyComposable
    override fun border(enabled: Boolean): BorderStroke = BorderStroke(
      width = Prism.dimens.strokeDefault,
      color = if (enabled) Prism.color.stroke else Prism.color.strokeVariant,
    )
  }

  /** Ghost-style flat action with a softer outline. */
  public data object Tertiary : PrismButtonStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(enabled: Boolean): Color =
      if (enabled) Prism.color.background else Prism.color.surfaceDim

    @Composable
    @ReadOnlyComposable
    override fun contentColor(enabled: Boolean): Color = if (enabled) Prism.color.labelColor else Prism.color.labelColor

    @Composable
    @ReadOnlyComposable
    override fun border(enabled: Boolean): BorderStroke = BorderStroke(
      width = Prism.dimens.strokeDefault,
      color = if (enabled) Prism.color.strokeVariant else Prism.color.stroke,
    )
  }
}
