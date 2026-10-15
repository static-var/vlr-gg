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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
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
 * Flat brutalist checkbox with a square, bordered control.
 */
@Composable
public fun PrismCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val visualState = rememberPrismCheckboxVisualState(checked = checked, enabled = enabled)

  PrismCheckboxTouchTarget(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
  ) {
    PrismCheckboxBox(visualState = visualState)
  }
}

@Composable
private fun rememberPrismCheckboxVisualState(checked: Boolean, enabled: Boolean): PrismCheckboxVisualState {
  val animation = Prism.anim.selection
  val animatedContainerColor =
    animateColorAsState(
      targetValue = checkboxContainerColor(checked = checked, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "checkbox_container",
    )
  val animatedBorderColor =
    animateColorAsState(
      targetValue = checkboxBorderColor(checked = checked, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "checkbox_border",
    )
  val animatedIndicatorColor =
    animateColorAsState(
      targetValue = checkboxIndicatorColor(enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "checkbox_indicator",
    )
  val animatedBorderWidth =
    animateDpAsState(
      targetValue = if (checked) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
      animationSpec = animation.dpSpec(),
      label = "checkbox_border_width",
    )
  val indicatorScale =
    animateFloatAsState(
      targetValue = if (checked) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "checkbox_indicator_scale",
    )
  val indicatorAlpha =
    animateFloatAsState(
      targetValue = if (checked) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "checkbox_indicator_alpha",
    )

  return PrismCheckboxVisualState(
    containerColor = animatedContainerColor,
    borderColor = animatedBorderColor,
    borderWidth = animatedBorderWidth,
    indicatorColor = animatedIndicatorColor,
    indicatorScale = indicatorScale,
    indicatorAlpha = indicatorAlpha,
  )
}

@Composable
private fun checkboxContainerColor(checked: Boolean, enabled: Boolean): Color = when {
  !enabled -> Prism.color.surfaceDim
  checked -> Prism.color.accentSubtle
  else -> Prism.color.surface
}

@Composable
private fun checkboxBorderColor(checked: Boolean, enabled: Boolean): Color = when {
  !enabled -> Prism.color.stroke
  checked -> Prism.color.accent
  else -> Prism.color.stroke
}

@Composable
private fun checkboxIndicatorColor(enabled: Boolean): Color =
  if (enabled) Prism.color.accent else Prism.color.labelColor

@Composable
private fun PrismCheckboxTouchTarget(
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
        role = Role.Checkbox,
        interactionSource = interactionSource,
        indication = ripple(),
      ),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun PrismCheckboxBox(visualState: PrismCheckboxVisualState) {
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
      modifier = Modifier.fillMaxSize().padding(Prism.dimens.spacingXs),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
        Modifier.fillMaxSize()
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

private data class PrismCheckboxVisualState(
  val containerColor: State<Color>,
  val borderColor: State<Color>,
  val borderWidth: State<Dp>,
  val indicatorColor: State<Color>,
  val indicatorScale: State<Float>,
  val indicatorAlpha: State<Float>,
)
