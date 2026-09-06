/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismSegmentedFilterTabs].
 *
 * A style owns the group surface, tab surface, content, and border treatment for selected,
 * unselected, and disabled states. Built-in styles read from [Prism] tokens so they adapt to the
 * active theme.
 */
@Immutable
public sealed interface PrismSegmentedFilterTabStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val groupContainerColor: Color

  @get:Composable
  @get:ReadOnlyComposable
  public val groupBorder: BorderStroke

  @Composable
  @ReadOnlyComposable
  public fun containerColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun contentColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun borderColor(selected: Boolean, enabled: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun borderWidth(selected: Boolean): Dp

  @Composable
  @ReadOnlyComposable
  public fun tabShape(first: Boolean, last: Boolean): Shape

  /** Default flat segmented filter style. */
  public data object Flat : PrismSegmentedFilterTabStyle {
    @Composable
    @ReadOnlyComposable
    override fun tabShape(first: Boolean, last: Boolean): Shape = Prism.shapes.small.let { shape ->
      shape.copy(
        topStart = if (first) shape.topStart else CornerSize(0.dp),
        bottomStart = if (first) shape.bottomStart else CornerSize(0.dp),
        topEnd = if (last) shape.topEnd else CornerSize(0.dp),
        bottomEnd = if (last) shape.bottomEnd else CornerSize(0.dp),
      )
    }

    override val groupContainerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.background

    override val groupBorder: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(Prism.dimens.strokeDefault, Prism.color.strokeVariant)

    @Composable
    @ReadOnlyComposable
    override fun containerColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.surfaceDim
      selected -> Prism.color.accentSubtle
      else -> Prism.color.surface
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
    override fun borderColor(selected: Boolean, enabled: Boolean): Color = when {
      !enabled -> Prism.color.stroke
      selected -> Prism.color.strokeVariant
      else -> Prism.color.stroke
    }

    @Composable
    @ReadOnlyComposable
    override fun borderWidth(selected: Boolean): Dp =
      if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault
  }
}
