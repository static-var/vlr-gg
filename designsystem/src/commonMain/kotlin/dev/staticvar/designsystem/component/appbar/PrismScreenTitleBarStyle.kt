/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.appbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismScreenTitleBar].
 *
 * A style owns the compact bar dimensions, slot spacing, and title typography. Built-in styles
 * read from [Prism] tokens so spacing follows the active design system.
 */
@Immutable
public sealed interface PrismScreenTitleBarStyle {
  /** Minimum height of the content row before status-bar inset is added. */
  @get:Composable
  @get:ReadOnlyComposable
  public val containerHeight: Dp

  /** Horizontal padding around the whole title bar. */
  @get:Composable
  @get:ReadOnlyComposable
  public val horizontalPadding: Dp

  /** Vertical padding around the whole title bar. */
  @get:Composable
  @get:ReadOnlyComposable
  public val verticalPadding: Dp

  /** Spacing between trailing action entries. */
  @get:Composable
  @get:ReadOnlyComposable
  public val actionSpacing: Dp

  /** Title text style for the main screen label. */
  @get:Composable
  @get:ReadOnlyComposable
  public val titleTextStyle: TextStyle

  @Composable
  @ReadOnlyComposable
  public fun titleHorizontalPadding(hasNavigation: Boolean): Dp

  @Composable
  @ReadOnlyComposable
  public fun subtitleHorizontalPadding(hasNavigation: Boolean): Dp

  /** Default compact title bar style. */
  public data object Default : PrismScreenTitleBarStyle {
    override val containerHeight: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.touchTargetMin + Prism.dimens.spacingS

    override val horizontalPadding: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingM

    override val verticalPadding: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingS

    override val actionSpacing: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.spacingXs

    override val titleTextStyle: TextStyle
      @Composable
      @ReadOnlyComposable
      get() = Prism.typography.sectionTitle.copy(fontSize = PrismScreenTitleBarDefaults.titleFontSize)

    @Composable
    @ReadOnlyComposable
    override fun titleHorizontalPadding(hasNavigation: Boolean): Dp =
      if (hasNavigation) Prism.dimens.spacingS else Prism.dimens.spacingXs

    @Composable
    @ReadOnlyComposable
    override fun subtitleHorizontalPadding(hasNavigation: Boolean): Dp =
      if (hasNavigation) Prism.dimens.spacingXs else PrismScreenTitleBarDefaults.noPadding
  }
}

private object PrismScreenTitleBarDefaults {
  val noPadding: Dp = 0.dp
  val titleFontSize = 16.sp
}
