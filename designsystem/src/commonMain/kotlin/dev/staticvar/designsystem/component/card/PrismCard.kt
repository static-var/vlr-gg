/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList", "MatchingDeclarationName")

package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import dev.staticvar.designsystem.component.frame.rememberPrismPressProgress
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * A Prism content container with token-backed styling and optional click handling.
 *
 * Use this for grouped content that needs a consistent surface, border, spacing, and content
 * color. The visual treatment is supplied by [PrismCardStyle], which keeps color and border
 * decisions outside of the composable call site.
 *
 * Example:
 * ```
 * PrismCard(
 *   style = PrismCardStyle.Outlined,
 *   onClick = { }
 * ) {
 *   Text("Card content")
 * }
 *
 * PrismCard(
 *   style = PrismCardStyle.Gradient(Brush.linearGradient(...))
 * ) {
 *   Text("Gradient card")
 * }
 * ```
 *
 * @param modifier Modifier applied to the card container.
 * @param style Visual treatment for the card surface.
 * @param onClick Optional click handler. When provided, the card uses button semantics and ripple
 *   feedback.
 * @param enabled Whether the clickable card can receive input.
 * @param shape Shape used for the card surface and ripple clipping.
 * @param interactionSource Interaction source used by the clickable state.
 * @param onLongClick Optional long-press action on an interactive card.
 * @param content Content placed inside the card with Prism spacing.
 */
@Composable
public fun PrismCard(
  modifier: Modifier = Modifier,
  style: PrismCardStyle = PrismCardStyle.Outlined,
  onClick: (() -> Unit)? = null,
  enabled: Boolean = true,
  shape: Shape = Prism.shapes.medium,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  onLongClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val frame = style.frame
  val pressProgress by rememberPrismPressProgress(
    interactionSource,
    enabled = enabled && onClick != null && frame.shadowOffset != DpOffset.Zero,
  )
  val finalModifier =
    if (onClick != null) {
      modifier.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = if (frame.shadowOffset == DpOffset.Zero) ripple() else null,
      )
    } else {
      modifier
    }

  PrismCardHost(modifier = finalModifier) {
    PrismSurface(
      color = style.containerColor,
      brush = style.brush,
      contentColor = style.contentColor,
      shape = shape,
      border = style.border,
      frame = frame,
      pressProgress = pressProgress,
    ) {
      Column(modifier = Modifier.padding(Prism.dimens.spacingM)) { content() }
    }
  }
}
