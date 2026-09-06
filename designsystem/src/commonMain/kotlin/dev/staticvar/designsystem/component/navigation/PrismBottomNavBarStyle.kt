/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismBottomNavBar].
 *
 * A style owns the bottom bar colors, dimensions, selected-item emphasis, and label typography.
 * Built-in styles read from [Prism] tokens so the bar adapts to the active light or dark theme.
 */
@Immutable
public sealed interface PrismBottomNavBarStyle {
  /** Bar height before navigation-bar inset is added. */
  @get:Composable
  @get:ReadOnlyComposable
  public val containerHeight: Dp

  /** Main bar background color. */
  @get:Composable
  @get:ReadOnlyComposable
  public val containerColor: Color

  @get:Composable
  @get:ReadOnlyComposable
  public val itemShape: Shape

  /** Height of each item content lane. */
  @get:Composable
  @get:ReadOnlyComposable
  public val itemHeight: Dp

  /** Top spacer inside each item lane. */
  @get:Composable
  @get:ReadOnlyComposable
  public val itemTopSpacing: Dp

  /** Label text style used for destination names. */
  @get:Composable
  @get:ReadOnlyComposable
  public val labelTextStyle: TextStyle

  /** Animation duration for selected-item color, offset, and weight changes. */
  public val selectionAnimationDurationMillis: Int

  @Composable
  @ReadOnlyComposable
  public fun itemContainerColor(selected: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun contentColor(selected: Boolean): Color

  @Composable
  @ReadOnlyComposable
  public fun itemOffsetY(selected: Boolean): Dp

  public fun itemWeight(selected: Boolean): Float

  public fun itemZIndex(selected: Boolean): Float

  /** Default compact bottom navigation treatment. */
  public data object Default : PrismBottomNavBarStyle {
    override val containerHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.touchTargetMin

    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.background

    override val itemShape: Shape
      @Composable
      @ReadOnlyComposable
      get() = Prism.shapes.small.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

    override val itemHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.touchTargetMin

    override val itemTopSpacing: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingS

    override val labelTextStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = Prism.typography.sectionTitle.copy(fontSize = PrismBottomNavBarDefaults.labelFontSize)

    override val selectionAnimationDurationMillis: Int = PrismBottomNavBarDefaults.selectionAnimationDurationMillis

    @Composable
    @ReadOnlyComposable
    override fun itemContainerColor(selected: Boolean): Color =
      if (selected) Prism.color.accent else Prism.color.background

    @Composable
    @ReadOnlyComposable
    override fun contentColor(selected: Boolean): Color = if (selected) Prism.color.onAccent else Prism.color.labelColor

    @Composable
    @ReadOnlyComposable
    override fun itemOffsetY(selected: Boolean): Dp =
      if (selected) -PrismBottomNavBarDefaults.selectedItemOffset else PrismBottomNavBarDefaults.unselectedItemOffset

    override fun itemWeight(selected: Boolean): Float = if (selected) {
      PrismBottomNavBarDefaults.selectedItemWeight
    } else {
      PrismBottomNavBarDefaults.unselectedItemWeight
    }

    override fun itemZIndex(selected: Boolean): Float =
      if (selected) PrismBottomNavBarDefaults.selectedItemZIndex else PrismBottomNavBarDefaults.unselectedItemZIndex
  }
}

private object PrismBottomNavBarDefaults {
  const val selectionAnimationDurationMillis: Int = 800
  const val unselectedItemWeight: Float = 1f
  const val selectedItemWeight: Float = 1.1f
  const val unselectedItemZIndex: Float = 0f
  const val selectedItemZIndex: Float = 1f
  val unselectedItemOffset: Dp = 0.dp
  val selectedItemOffset: Dp = 12.dp
  val labelFontSize = 12.sp
}
