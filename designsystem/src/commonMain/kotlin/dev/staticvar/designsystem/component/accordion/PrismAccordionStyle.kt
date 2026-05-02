/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.accordion

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for a [PrismAccordion].
 *
 * A style owns the accordion surface, content, border, divider, and indicator colors. Built-in
 * styles read from [Prism] tokens so they adapt to the active light or dark theme.
 */
@Immutable
public sealed interface PrismAccordionStyle {
  /** Container color for the accordion surface. */
  @get:Composable
  @get:ReadOnlyComposable
  public val containerColor: Color

  /** Content color provided to the accordion body. */
  @get:Composable
  @get:ReadOnlyComposable
  public val contentColor: Color

  /** Optional border drawn around the accordion surface. */
  @get:Composable
  @get:ReadOnlyComposable
  public val border: BorderStroke?

  /** Divider color between the header and expanded content. */
  @get:Composable
  @get:ReadOnlyComposable
  public val dividerColor: Color

  /** Indicator icon color. */
  @get:Composable
  @get:ReadOnlyComposable
  public val indicatorColor: Color

  /** Default accordion style with a base surface and visible outline. */
  public data object Outlined : PrismAccordionStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.surface

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.contentPrimary

    override val border: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke)

    override val dividerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.stroke

    override val indicatorColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.labelColor
  }

  /** Filled accordion style for stronger separation from the page background. */
  public data object Filled : PrismAccordionStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.surfaceVariant

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.contentPrimary

    override val border: BorderStroke?
      @Composable
      @ReadOnlyComposable
      get() = null

    override val dividerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.stroke

    override val indicatorColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.labelColor
  }

  /** Minimal accordion style with a transparent surface and no outline. */
  public data object Minimal : PrismAccordionStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Color.Transparent

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.contentPrimary

    override val border: BorderStroke?
      @Composable
      @ReadOnlyComposable
      get() = null

    override val dividerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.stroke

    override val indicatorColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.labelColor
  }
}
