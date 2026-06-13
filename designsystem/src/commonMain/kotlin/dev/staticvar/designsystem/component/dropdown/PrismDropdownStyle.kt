/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.dropdown

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismDropdown].
 */
@Immutable
public sealed interface PrismDropdownStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val triggerWidth: Dp

  @get:Composable
  @get:ReadOnlyComposable
  public val menuWidth: Dp

  @get:Composable
  @get:ReadOnlyComposable
  public val chevronWidth: Dp

  @get:Composable
  @get:ReadOnlyComposable
  public val optionMinHeight: Dp

  @Composable
  @ReadOnlyComposable
  public fun triggerContainerColor(enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun triggerContentColor(enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun triggerBorder(expanded: Boolean, enabled: Boolean): BorderStroke

  @Composable
  @ReadOnlyComposable
  public fun optionContainerColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun optionContentColor(selected: Boolean, enabled: Boolean): Color

  @get:Composable
  @get:ReadOnlyComposable
  public val menuBorder: BorderStroke

  @get:Composable
  @get:ReadOnlyComposable
  public val selectedRailColor: Color

  public data object Brutalist : PrismDropdownStyle {
    override val triggerWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = 196.dp

    override val menuWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = 212.dp

    override val chevronWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = 40.dp

    override val optionMinHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = 44.dp

    @Composable
    @ReadOnlyComposable
    override fun triggerContainerColor(enabled: Boolean): Color =
      if (enabled) Prism.color.surface else Prism.color.surfaceDim

    @Composable
    @ReadOnlyComposable
    override fun triggerContentColor(enabled: Boolean): Color =
      if (enabled) Prism.color.titleColor else Prism.color.captionColor

    @Composable
    @ReadOnlyComposable
    override fun triggerBorder(expanded: Boolean, enabled: Boolean): BorderStroke {
      val width = if (expanded && enabled) Prism.dimens.strokeThick else Prism.dimens.strokeDefault
      val color = if (enabled) Prism.color.stroke else Prism.color.strokeVariant
      return BorderStroke(width = width, color = color)
    }

    @Composable
    @ReadOnlyComposable
    override fun optionContainerColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.surfaceDim
      selected -> Prism.color.accentSubtle
      else -> Prism.color.surface
    }

    @Composable
    @ReadOnlyComposable
    override fun optionContentColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.captionColor
      selected -> Prism.color.accent
      else -> Prism.color.bodyColor
    }

    override val menuBorder: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(width = Prism.dimens.strokeThick, color = Prism.color.stroke)

    override val selectedRailColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.accent
  }
}
