/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismTabs].
 *
 * A style owns the tab content color, indicator color, indicator height, and baseline color for
 * the selected, unselected, and disabled states. Built-in styles read from [Prism] tokens so they
 * adapt to the active theme.
 */
@Immutable
public sealed interface PrismTabStyle {
  @Composable
  @ReadOnlyComposable
  public fun contentColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun indicatorColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun indicatorHeight(selected: Boolean): Dp

  @get:Composable
  @get:ReadOnlyComposable
  public val baselineColor: Color

  /** Default underline tab style for content section switching. */
  public data object Underlined : PrismTabStyle {
    @Composable
    @ReadOnlyComposable
    override fun contentColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.captionColor
      selected -> Prism.color.titleColor
      else -> Prism.color.labelColor
    }

    @Composable
    @ReadOnlyComposable
    override fun indicatorColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.stroke
      selected -> Prism.color.accent
      else -> Prism.color.strokeVariant
    }

    @Composable
    @ReadOnlyComposable
    override fun indicatorHeight(selected: Boolean): Dp =
      if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault

    override val baselineColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.strokeVariant
  }
}
