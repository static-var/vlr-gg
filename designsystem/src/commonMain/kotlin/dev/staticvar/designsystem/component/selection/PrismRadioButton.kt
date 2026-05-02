/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Flat brutalist radio button with square framing and center indicator.
 */
@Composable
public fun PrismRadioButton(
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val visualState = rememberPrismRadioVisualState(selected = selected, enabled = enabled)

  PrismRadioTouchTarget(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
  ) {
    PrismRadioBox(visualState = visualState)
  }
}

@Composable
private fun rememberPrismRadioVisualState(selected: Boolean, enabled: Boolean): PrismRadioVisualState {
  val animation = Prism.anim.standard
  val animatedContainerColor by
    animateColorAsState(
      targetValue = radioContainerColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "radio_container",
    )
  val animatedBorderColor by
    animateColorAsState(
      targetValue = radioBorderColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "radio_border",
    )
  val animatedIndicatorColor by
    animateColorAsState(
      targetValue = radioIndicatorColor(enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "radio_indicator",
    )
  val animatedBorderWidth by
    animateDpAsState(
      targetValue = if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
      animationSpec = animation.dpSpec(),
      label = "radio_border_width",
    )
  val indicatorScale by
    animateFloatAsState(
      targetValue = if (selected) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "radio_indicator_scale",
    )
  val indicatorAlpha by
    animateFloatAsState(
      targetValue = if (selected) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "radio_indicator_alpha",
    )

  return PrismRadioVisualState(
    containerColor = animatedContainerColor,
    borderColor = animatedBorderColor,
    borderWidth = animatedBorderWidth,
    indicatorColor = animatedIndicatorColor,
    indicatorScale = indicatorScale,
    indicatorAlpha = indicatorAlpha,
  )
}

@Composable
private fun radioContainerColor(selected: Boolean, enabled: Boolean): Color = when {
  !enabled -> Prism.color.surfaceDim
  selected -> Prism.color.accentSubtle
  else -> Prism.color.surface
}

@Composable
private fun radioBorderColor(selected: Boolean, enabled: Boolean): Color = when {
  !enabled -> Prism.color.stroke
  selected -> Prism.color.accent
  else -> Prism.color.stroke
}

@Composable
private fun radioIndicatorColor(enabled: Boolean): Color = if (enabled) Prism.color.accent else Prism.color.labelColor

@Composable
private fun PrismRadioTouchTarget(
  selected: Boolean,
  onClick: () -> Unit,
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
      .selectable(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        role = Role.RadioButton,
        interactionSource = interactionSource,
        indication = ripple(),
      ),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun PrismRadioBox(visualState: PrismRadioVisualState) {
  PrismSurface(
    modifier = Modifier.size(Prism.dimens.iconM),
    color = visualState.containerColor,
    shape = Prism.shapes.small,
    border = BorderStroke(width = visualState.borderWidth, color = visualState.borderColor),
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
        Modifier.size(Prism.dimens.spacingS)
          .graphicsLayer {
            scaleX = visualState.indicatorScale
            scaleY = visualState.indicatorScale
            alpha = visualState.indicatorAlpha
          }
          .background(color = visualState.indicatorColor, shape = Prism.shapes.small),
      )
    }
  }
}

private data class PrismRadioVisualState(
  val containerColor: Color,
  val borderColor: Color,
  val borderWidth: Dp,
  val indicatorColor: Color,
  val indicatorScale: Float,
  val indicatorAlpha: Float,
)
