/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/**
 * Visual treatment for a [PrismCard].
 *
 * A style owns the surface color, content color, border, and optional background brush used by the
 * card. Built-in styles read from [Prism] tokens so they adapt to the active light or dark theme.
 */
@Immutable
public sealed interface PrismCardStyle {
  /** Theme frame applied outside the component face. */
  public val frame: PrismFrameTokens
    @Composable
    @ReadOnlyComposable
    get() = Prism.frames.panel

  /**
   * Solid fallback color for the card surface.
   *
   * When [brush] is not null, the brush is drawn by the surface and this color should be treated as
   * the non-brush fallback.
   */
  @get:Composable
  @get:ReadOnlyComposable
  public val containerColor: Color

  /** Preferred content color for text and icons placed inside the card. */
  @get:Composable
  @get:ReadOnlyComposable
  public val contentColor: Color

  /** Border drawn around the card surface. */
  @get:Composable
  @get:ReadOnlyComposable
  public val border: BorderStroke

  /** Optional brush used for gradient or image-like card backgrounds. */
  public val brush: Brush?
    get() = null

  /** Default card style with the base surface color and a more visible outline. */
  public data object Outlined : PrismCardStyle {
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
      get() = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.strokeVariant)
  }

  /** Filled card style for cards that need stronger separation from the page background. */
  public data object Filled : PrismCardStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.surfaceVariant

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.contentPrimary

    override val border: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke)
  }

  /**
   * Brush-backed card style.
   *
   * @param brush Brush drawn as the card background.
   * @param foregroundColor Content color to use over the brush.
   */
  public data class Gradient(override val brush: Brush, private val foregroundColor: Color = Color.White) :
    PrismCardStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Color.Transparent

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = foregroundColor

    override val border: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke)
  }
}
