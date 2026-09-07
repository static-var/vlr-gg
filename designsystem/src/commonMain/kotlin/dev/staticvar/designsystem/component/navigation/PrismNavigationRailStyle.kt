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
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/** Theme-backed destination treatment within [PrismBottomNavBarLarge]. */
@Immutable
public sealed interface PrismNavigationRailStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val frame: PrismFrameTokens
    get() = Prism.frames.compact

  @Composable
  @ReadOnlyComposable
  public fun containerColor(selected: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun contentColor(selected: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun iconColor(selected: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun border(selected: Boolean): BorderStroke

  public data object Default : PrismNavigationRailStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(selected: Boolean): Color =
      if (selected) Prism.color.accentSubtle else Prism.color.background

    @Composable
    @ReadOnlyComposable
    override fun contentColor(selected: Boolean): Color =
      if (selected) Prism.color.titleColor else Prism.color.bodyColor

    @Composable
    @ReadOnlyComposable
    override fun iconColor(selected: Boolean): Color = if (selected) Prism.color.titleColor else Prism.color.labelColor

    @Composable
    @ReadOnlyComposable
    override fun border(selected: Boolean): BorderStroke = BorderStroke(
      width = frame.border?.width ?: Prism.dimens.strokeDefault,
      color = if (selected) Prism.color.accent else Prism.color.stroke,
    )
  }
}
