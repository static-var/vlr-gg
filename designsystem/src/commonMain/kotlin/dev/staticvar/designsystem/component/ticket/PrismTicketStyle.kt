/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.ticket

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/** Ticket colors, spacing, outline, and perforation, resolved from the active Prism theme. */
@Immutable
public sealed interface PrismTicketStyle {
  public val containerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = Prism.color.surface
  public val contentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = Prism.color.contentPrimary
  public val headerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = Prism.color.accentSubtle
  public val headerContentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = Prism.color.contentPrimary
  public val stubColor: Color
    @Composable
    @ReadOnlyComposable
    get() = Prism.color.surfaceVariant
  public val perforationColor: Color
    @Composable
    @ReadOnlyComposable
    get() = Prism.color.strokeVariant
  public val shape: Shape
    @Composable
    @ReadOnlyComposable
    get() = Prism.shapes.medium
  public val frame: PrismFrameTokens
    @Composable
    @ReadOnlyComposable
    get() = Prism.frames.panel
  public val border: BorderStroke
    @Composable
    @ReadOnlyComposable
    get() = BorderStroke(Prism.dimens.strokeDefault, Prism.color.strokeVariant)
  public val padding: Dp
    @Composable
    @ReadOnlyComposable
    get() = Prism.dimens.spacingL
  public val headerVerticalPadding: Dp
    @Composable
    @ReadOnlyComposable
    get() = Prism.dimens.spacingM
  public val notchRadius: Dp
    @Composable
    @ReadOnlyComposable
    get() = Prism.dimens.spacingS
  public val dashLength: Dp
    @Composable
    @ReadOnlyComposable
    get() = Prism.dimens.spacingXs
  public val perforationWidth: Dp
    @Composable
    @ReadOnlyComposable
    get() = Prism.dimens.strokeDefault

  public val tearDepth: Dp
    @Composable
    @ReadOnlyComposable
    get() = Prism.dimens.spacingS
  public val tearWidth: Dp
    @Composable
    @ReadOnlyComposable
    get() = Prism.dimens.spacingM

  /** Accent header, base surface body, and a contrasting detachable stub. */
  public data object Standard : PrismTicketStyle
}
