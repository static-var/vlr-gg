/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.icon

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.Prism

/**
 * Visual treatment for [PrismIcon].
 *
 * A style owns the icon container, foreground tint, border, and shape treatment. Size remains a
 * separate layout choice so callers can use the same visual style at different preset dimensions.
 */
@Immutable
public sealed interface PrismIconStyle {
  /** Container color behind the icon. */
  @get:Composable
  @get:ReadOnlyComposable
  public val containerColor: Color

  /** Tint applied to the icon or painter content. */
  @get:Composable
  @get:ReadOnlyComposable
  public val contentColor: Color

  /** Optional border around the icon container. */
  @get:Composable
  @get:ReadOnlyComposable
  public val border: BorderStroke?

  /** Bordered icon treatment for app metadata, team marks, and compact visual anchors. */
  public data object Bordered : PrismIconStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.surface

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.titleColor

    override val border: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke)
  }

  /** Bordered icon treatment for app metadata, team marks, and compact visual anchors. */
  public data object Borderless : PrismIconStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.surface

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.titleColor

    override val border: BorderStroke?
      @Composable
      @ReadOnlyComposable
      get() = null
  }

  /** Borderless icon treatment for icons already placed in a strong parent container. */
  public data object Plain : PrismIconStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Color.Transparent

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.titleColor

    override val border: BorderStroke?
      @Composable
      @ReadOnlyComposable
      get() = null
  }

  /** Muted bordered treatment for secondary metadata or low-emphasis visual anchors. */
  public data object Muted : PrismIconStyle {
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.surfaceDim

    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.labelColor

    override val border: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke)
  }
}
