/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/** Token-backed action surface treatment for [PrismFabSheet]. */
@Immutable
public sealed interface PrismFabSheetStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val frame: PrismFrameTokens
    get() = Prism.frames.control

  @get:Composable
  @get:ReadOnlyComposable
  public val actionSize: Dp

  @get:Composable
  @get:ReadOnlyComposable
  public val containerColor: Color

  @get:Composable
  @get:ReadOnlyComposable
  public val contentColor: Color

  @get:Composable
  @get:ReadOnlyComposable
  public val border: BorderStroke

  /** Prominent action using the active Prism accent and the standard sheet surface. */
  public data object Accent : PrismFabSheetStyle {
    override val actionSize: Dp
      @Composable
      @ReadOnlyComposable
      get() = Prism.dimens.touchTargetMin + Prism.dimens.spacingS
    override val containerColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.accentSubtle
    override val contentColor: Color
      @Composable
      @ReadOnlyComposable
      get() = Prism.color.titleColor
    override val border: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(Prism.dimens.strokeDefault, Prism.color.strokeVariant)
  }
}
