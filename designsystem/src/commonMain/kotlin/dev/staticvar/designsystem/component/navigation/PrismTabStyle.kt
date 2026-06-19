/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismTabs].
 *
 * A style owns the ticket container, content color, indicator, and border states. Built-in styles
 * read from [Prism] tokens so they adapt to the active theme.
 */
@Immutable
public sealed interface PrismTabStyle {
  @Composable
  @ReadOnlyComposable
  public fun containerColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun contentColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun indicatorColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun indicatorHeight(selected: Boolean): Dp

  @Composable
  @ReadOnlyComposable
  public fun indicatorHorizontalInset(selected: Boolean): Dp

  @Composable
  @ReadOnlyComposable
  public fun indicatorOffsetY(selected: Boolean): Dp

  @Composable
  @ReadOnlyComposable
  public fun border(selected: Boolean, enabled: Boolean): BorderStroke

  @get:Composable
  @get:ReadOnlyComposable
  public val minWidth: Dp

  /** Default ticket tab style for content section switching. */
  public data object Ticket : PrismTabStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.surfaceDim
      selected -> Prism.color.surface
      else -> Prism.color.surfaceVariant
    }

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
      selected -> Prism.color.accent
      else -> Color.Transparent
    }

    @Composable
    @ReadOnlyComposable
    override fun indicatorHeight(selected: Boolean): Dp = if (selected) Prism.dimens.spacingXs * 1.5f else 0.dp

    @Composable
    @ReadOnlyComposable
    override fun indicatorHorizontalInset(selected: Boolean): Dp = if (selected) Prism.dimens.spacingS else 0.dp

    @Composable
    @ReadOnlyComposable
    override fun indicatorOffsetY(selected: Boolean): Dp = if (selected) -Prism.dimens.strokeThick else 0.dp

    @Composable
    @ReadOnlyComposable
    override fun border(selected: Boolean, enabled: Boolean): BorderStroke {
      val width = if (selected && enabled) Prism.dimens.strokeThick else Prism.dimens.strokeDefault
      val color = if (enabled) Prism.color.stroke else Prism.color.strokeVariant
      return BorderStroke(width = width, color = color)
    }

    override val minWidth: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.touchTargetMin
  }

  public companion object {
    public val Underlined: PrismTabStyle = Ticket
  }
}
