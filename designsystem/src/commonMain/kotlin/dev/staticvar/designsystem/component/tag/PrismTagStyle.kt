/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.tag

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for a [PrismTag].
 *
 * A style owns the tag container, content, and border colors. Built-in styles read from [Prism]
 * tokens so they adapt to the active light or dark theme.
 */
@Immutable
public sealed interface PrismTagStyle {
  /** Container color for the enabled tag surface. */
  @get:Composable
  @get:ReadOnlyComposable
  public val containerColor: Color

  /** Content color for text and leading content in the enabled tag. */
  @get:Composable
  @get:ReadOnlyComposable
  public val contentColor: Color

  /** Border drawn around the enabled tag surface. */
  @get:Composable
  @get:ReadOnlyComposable
  public val border: BorderStroke
    get() = BorderStroke(width = Prism.dimens.strokeDefault, color = borderColor)

  /** Border color for the enabled tag surface. */
  @get:Composable
  @get:ReadOnlyComposable
  public val borderColor: Color

  /** Container color used when the tag is disabled. */
  @get:Composable
  @get:ReadOnlyComposable
  public val disabledContainerColor: Color
    get() = Prism.color.surfaceDim

  /** Content color used when the tag is disabled. */
  @get:Composable
  @get:ReadOnlyComposable
  public val disabledContentColor: Color
    get() = Prism.color.captionColor

  /** Border drawn when the tag is disabled. */
  @get:Composable
  @get:ReadOnlyComposable
  public val disabledBorder: BorderStroke
    get() = BorderStroke(width = Prism.dimens.strokeDefault, color = disabledBorderColor)

  /** Border color used when the tag is disabled. */
  @get:Composable
  @get:ReadOnlyComposable
  public val disabledBorderColor: Color
    get() = Prism.color.stroke

  /** Default metadata tag using base surface and stroke tokens. */
  public data object Neutral : PrismTagStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.surface

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.labelColor

    override val borderColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.stroke
  }

  /** Accent tag for highlighted metadata. */
  public data object Accent : PrismTagStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.accentSubtle

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.accent

    override val borderColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.accent
  }

  /** Success tag for completed or positive statuses. */
  public data object Success : PrismTagStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.successContainer

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.success

    override val borderColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.success
  }

  /** Warning tag for cautionary statuses. */
  public data object Warning : PrismTagStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.warningContainer

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.warning

    override val borderColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.warning
  }

  /** Danger tag for live, urgent, or error statuses. */
  public data object Danger : PrismTagStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.dangerContainer

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.danger

    override val borderColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.danger
  }

  /** Informational tag for neutral-progress statuses. */
  public data object Info : PrismTagStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.infoContainer

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.info

    override val borderColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.info
  }
}
