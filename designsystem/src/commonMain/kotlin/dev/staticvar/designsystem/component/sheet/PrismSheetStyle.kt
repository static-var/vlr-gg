/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/** Border and frame treatment for persistent and modal sheets. */
@Immutable
public sealed interface PrismSheetStyle {
  @get:Composable
  @get:ReadOnlyComposable
  public val frame: PrismFrameTokens

  @get:Composable
  @get:ReadOnlyComposable
  public val border: BorderStroke

  public data object Standard : PrismSheetStyle {
    override val frame: PrismFrameTokens
      @Composable
      @ReadOnlyComposable
      get() = Prism.frames.panel

    override val border: BorderStroke
      @Composable
      @ReadOnlyComposable
      get() = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke)
  }
}
