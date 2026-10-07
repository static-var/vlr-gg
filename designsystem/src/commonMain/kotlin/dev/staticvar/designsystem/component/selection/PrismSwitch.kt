/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Flat brutalist switch with rectangular track and square thumb.
 */
@Composable
public fun PrismSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val trackWidth = Prism.dimens.controlHeight
  val horizontalPadding = Prism.dimens.spacingXs
  val thumbSize = Prism.dimens.iconS
  val thumbTravel = trackWidth - (horizontalPadding * 2) - thumbSize
  val visualState =
    rememberPrismSwitchVisualState(
      checked = checked,
      enabled = enabled,
      thumbTravel = thumbTravel,
    )

  PrismSwitchTouchTarget(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
  ) {
    PrismSwitchTrack(
      trackWidth = trackWidth,
      horizontalPadding = horizontalPadding,
      thumbSize = thumbSize,
      visualState = visualState,
    )
  }
}

@Composable
private fun rememberPrismSwitchVisualState(
  checked: Boolean,
  enabled: Boolean,
  thumbTravel: Dp,
): PrismSwitchVisualState {
  val trackColor =
    when {
      !enabled -> Prism.color.surfaceDim
      checked -> Prism.color.accentSubtle
      else -> Prism.color.surface
    }
  val borderColor =
    when {
      !enabled -> Prism.color.stroke
      checked -> Prism.color.accent
      else -> Prism.color.stroke
    }
  val thumbColor =
    when {
      !enabled -> Prism.color.labelColor
      checked -> Prism.color.accent
      else -> Prism.color.titleColor
    }
  val standardAnimation = Prism.anim.selection
  val slowAnimation = Prism.anim.slowFade
  val animatedTrackColor by
    animateColorAsState(
      targetValue = trackColor,
      animationSpec = standardAnimation.colorSpec(),
      label = "switch_track_color",
    )
  val animatedBorderColor by
    animateColorAsState(
      targetValue = borderColor,
      animationSpec = standardAnimation.colorSpec(),
      label = "switch_border_color",
    )
  val animatedThumbColor by
    animateColorAsState(
      targetValue = thumbColor,
      animationSpec = standardAnimation.colorSpec(),
      label = "switch_thumb_color",
    )
  val animatedBorderWidth by
    animateDpAsState(
      targetValue = if (checked) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
      animationSpec = standardAnimation.dpSpec(),
      label = "switch_border_width",
    )
  val animatedThumbOffset by
    animateDpAsState(
      targetValue = if (checked) thumbTravel else 0.dp,
      animationSpec = slowAnimation.dpSpec(),
      label = "switch_thumb_offset",
    )

  return PrismSwitchVisualState(
    trackColor = animatedTrackColor,
    borderColor = animatedBorderColor,
    borderWidth = animatedBorderWidth,
    thumbColor = animatedThumbColor,
    thumbOffset = animatedThumbOffset,
  )
}

@Composable
private fun PrismSwitchTouchTarget(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier,
  enabled: Boolean,
  interactionSource: MutableInteractionSource,
  content: @Composable () -> Unit,
) {
  Box(
    modifier =
    modifier
      .sizeIn(
        minWidth = Prism.dimens.touchTargetMin,
        minHeight = Prism.dimens.touchTargetMin,
      )
      .toggleable(
        value = checked,
        onValueChange = onCheckedChange,
        enabled = enabled,
        role = Role.Switch,
        interactionSource = interactionSource,
        indication = ripple(),
      ),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun PrismSwitchTrack(
  trackWidth: Dp,
  horizontalPadding: Dp,
  thumbSize: Dp,
  visualState: PrismSwitchVisualState,
) {
  PrismSurface(
    modifier = Modifier.width(trackWidth).height(Prism.dimens.iconM),
    color = visualState.trackColor,
    shape = Prism.shapes.small,
    border = BorderStroke(width = visualState.borderWidth, color = visualState.borderColor),
  ) {
    Box(
      modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding),
      contentAlignment = Alignment.CenterStart,
    ) {
      Box(
        modifier =
        Modifier.offset(x = visualState.thumbOffset)
          .size(thumbSize)
          .background(color = visualState.thumbColor, shape = Prism.shapes.small),
      )
    }
  }
}

private data class PrismSwitchVisualState(
  val trackColor: Color,
  val borderColor: Color,
  val borderWidth: Dp,
  val thumbColor: Color,
  val thumbOffset: Dp,
)
