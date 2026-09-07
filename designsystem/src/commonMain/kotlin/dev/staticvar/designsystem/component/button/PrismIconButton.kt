/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList", "MatchingDeclarationName")

package dev.staticvar.designsystem.component.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import dev.staticvar.designsystem.component.frame.rememberPrismPressProgress
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

public enum class PrismIconButtonSize {
  Small,
  Medium,
  Large,
  XL,
}

/**
 * Icon button with a theme-provided frame and a stable minimum touch target.
 */
@Composable
public fun PrismIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: PrismIconButtonSize = PrismIconButtonSize.Medium,
  style: PrismIconButtonStyle = PrismIconButtonStyle.Bordered,
  enabled: Boolean = true,
  selected: Boolean = false,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val frame = style.frame
  val pressProgress by rememberPrismPressProgress(
    interactionSource,
    enabled = enabled && frame.shadowOffset != DpOffset.Zero,
  )
  val (containerSize, iconSize) =
    when (size) {
      PrismIconButtonSize.Small -> Prism.dimens.iconM + Prism.dimens.spacingXs to Prism.dimens.iconS
      PrismIconButtonSize.Medium -> Prism.dimens.iconM + Prism.dimens.spacingS to Prism.dimens.iconM
      PrismIconButtonSize.Large -> Prism.dimens.iconM + Prism.dimens.spacingM to Prism.dimens.iconL
      PrismIconButtonSize.XL -> Prism.dimens.iconL + Prism.dimens.spacingL to Prism.dimens.iconL
    }

  Box(
    modifier =
    modifier
      .sizeIn(
        minWidth = Prism.dimens.touchTargetMin,
        minHeight = Prism.dimens.touchTargetMin,
      )
      .clickable(
        onClick = onClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = if (frame.shadowOffset == DpOffset.Zero) ripple() else null,
      ),
    contentAlignment = Alignment.Center,
  ) {
    PrismSurface(
      modifier = Modifier.size(
        width = containerSize + frame.shadowOffset.x,
        height = containerSize + frame.shadowOffset.y,
      ),
      color = style.containerColor(enabled = enabled, selected = selected),
      shape = Prism.shapes.small,
      border = style.border(enabled = enabled, selected = selected),
      frame = frame,
      pressProgress = pressProgress,
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          modifier = Modifier.size(iconSize),
          tint = style.iconColor(enabled = enabled, selected = selected),
        )
      }
    }
  }
}
