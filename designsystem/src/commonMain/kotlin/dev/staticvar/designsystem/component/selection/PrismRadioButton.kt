/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.component.surface.prismAnimatedBackground
import dev.staticvar.designsystem.component.surface.prismAnimatedSurface
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
  val animation = Prism.anim.selection
  val animatedContainerColor =
    animateColorAsState(
      targetValue = radioContainerColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "radio_container",
    )
  val animatedBorderColor =
    animateColorAsState(
      targetValue = radioBorderColor(selected = selected, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "radio_border",
    )
  val animatedIndicatorColor =
    animateColorAsState(
      targetValue = radioIndicatorColor(enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "radio_indicator",
    )
  val animatedBorderWidth =
    animateDpAsState(
      targetValue = if (selected) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
      animationSpec = animation.dpSpec(),
      label = "radio_border_width",
    )
  val indicatorScale =
    animateFloatAsState(
      targetValue = if (selected) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "radio_indicator_scale",
    )
  val indicatorAlpha =
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
  val shape = Prism.shapes.small
  Box(
    modifier = Modifier.size(Prism.dimens.iconM)
      .prismAnimatedSurface(
        shape = shape,
        containerColor = { visualState.containerColor.value },
        borderColor = { visualState.borderColor.value },
        borderWidth = { visualState.borderWidth.value },
      )
      .clip(shape),
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
        Modifier.size(Prism.dimens.spacingS)
          .graphicsLayer {
            scaleX = visualState.indicatorScale.value
            scaleY = visualState.indicatorScale.value
            alpha = visualState.indicatorAlpha.value
          }
          .prismAnimatedBackground(shape) { visualState.indicatorColor.value },
      )
    }
  }
}

private data class PrismRadioVisualState(
  val containerColor: State<Color>,
  val borderColor: State<Color>,
  val borderWidth: State<Dp>,
  val indicatorColor: State<Color>,
  val indicatorScale: State<Float>,
  val indicatorAlpha: State<Float>,
)
