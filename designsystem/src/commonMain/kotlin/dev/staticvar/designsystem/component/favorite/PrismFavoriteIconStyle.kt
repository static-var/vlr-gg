/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.favorite

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismFavoriteIcon].
 */
@Immutable
public sealed interface PrismFavoriteIconStyle {
  @Composable
  @ReadOnlyComposable
  public fun containerColor(selected: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun border(selected: Boolean): BorderStroke?

  /** Default favorite marker with a visible surface container. */
  public data object Boxed : PrismFavoriteIconStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(selected: Boolean): Color =
      if (selected) Prism.color.surfaceDim else Prism.color.surface

    @Composable
    @ReadOnlyComposable
    override fun border(selected: Boolean): BorderStroke =
      BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke)
  }

  /** Favorite marker without a surface container, for host-owned ticket placement. */
  public data object Bare : PrismFavoriteIconStyle {
    @Composable
    @ReadOnlyComposable
    override fun containerColor(selected: Boolean): Color = Color.Transparent

    @Composable
    @ReadOnlyComposable
    override fun border(selected: Boolean): BorderStroke? = null
  }
}
