/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.slider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.roundToInt

/**
 * Selects a value by dragging, tapping, keyboard, or accessibility adjustment. [steps] counts
 * interior stops, so two steps gives four selectable values including the range endpoints.
 * Optional [stopLabels] are centered beneath those stops and can be tapped to select them.
 * Add a label through [modifier] semantics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun PrismSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  steps: Int = 0,
  stopLabels: List<String> = emptyList(),
  style: PrismSliderStyle = PrismSliderStyle.Rail,
  onValueChangeFinished: (() -> Unit)? = null,
) {
  require(stopLabels.isEmpty() || stopLabels.size == steps + 2) { "Provide one label for every selectable stop" }
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val dragged by interactionSource.collectIsDraggedAsState()
  val focused by interactionSource.collectIsFocusedAsState()
  val thumbColor = style.thumbColor(enabled, pressed || dragged || focused)
  val trackColor = style.trackColor(enabled)
  val activeColor = style.activeColor(enabled)
  val trackThickness = style.inactiveThickness
  val fillThickness = style.activeThickness
  val tickHeight = style.tickHeight

  val rangeLength = valueRange.endInclusive - valueRange.start
  val selectedIndex = if (rangeLength > 0f) {
    (((value - valueRange.start) / rangeLength).coerceIn(0f, 1f) * (steps + 1)).roundToInt()
  } else {
    0
  }
  PrismSliderLayout(
    labels = stopLabels,
    selectedIndex = selectedIndex,
    onLabelSelected = { index ->
      onValueChange(valueRange.start + rangeLength * index / (stopLabels.size - 1))
      onValueChangeFinished?.invoke()
    },
    enabled = enabled,
    style = style,
    modifier = modifier,
  ) { sliderModifier ->
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides style.layoutHeight) {
      Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = sliderModifier.heightIn(min = style.layoutHeight),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = { Box(Modifier.size(style.thumbSize).background(thumbColor)) },
        track = { state ->
          Canvas(Modifier.fillMaxWidth().height(style.trackHeight)) {
            val rangeLength = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (rangeLength > 0f) {
              ((state.value - state.valueRange.start) / rangeLength).coerceIn(0f, 1f)
            } else {
              0f
            }
            val railHeight = trackThickness.toPx()
            val fillHeight = fillThickness.toPx()
            drawRect(trackColor, Offset(0f, (size.height - railHeight) / 2f), Size(size.width, railHeight))
            val fillWidth = fraction * size.width
            val fillX = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
            drawRect(activeColor, Offset(fillX, (size.height - fillHeight) / 2f), Size(fillWidth, fillHeight))
            if (state.steps > 0) {
              val tickWidth = railHeight.coerceAtMost(size.width)
              val tickHeightPx = tickHeight.toPx()
              repeat(state.steps + 2) { index ->
                val tickFraction = index.toFloat() / (state.steps + 1)
                val logicalX = (size.width - tickWidth) * tickFraction
                val tickX = if (layoutDirection == LayoutDirection.Rtl) size.width - logicalX - tickWidth else logicalX
                drawRect(
                  color = if (tickFraction <= fraction) activeColor else trackColor,
                  topLeft = Offset(tickX, (size.height - tickHeightPx) / 2f),
                  size = Size(tickWidth, tickHeightPx),
                )
              }
            }
          }
        },
      )
    }
  }
}

@Composable
private fun PrismSliderLayout(
  labels: List<String>,
  selectedIndex: Int,
  onLabelSelected: (Int) -> Unit,
  enabled: Boolean,
  style: PrismSliderStyle,
  modifier: Modifier,
  slider: @Composable (Modifier) -> Unit,
) {
  if (labels.isEmpty()) {
    slider(modifier)
    return
  }
  val thumbWidth = style.thumbSize.width
  Layout(
    modifier = modifier,
    content = {
      slider(Modifier)
      labels.forEachIndexed { index, label ->
        Text(
          text = label,
          modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { onLabelSelected(index) }
            .padding(vertical = style.labelVerticalPadding),
          style = style.labelTextStyle,
          color = style.labelColor(enabled, index == selectedIndex),
          textAlign = TextAlign.Center,
        )
      }
    },
  ) { measurables, constraints ->
    val labelMeasurables = measurables.drop(1)
    val width = if (constraints.hasBoundedWidth) {
      constraints.maxWidth
    } else {
      constraints.constrainWidth(labelMeasurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) } * labels.size)
    }
    val labelWidth = width / labels.size
    val thumbWidthPx = thumbWidth.roundToPx().coerceAtMost(width)
    val sliderWidth = (width - labelWidth + thumbWidthPx).coerceAtMost(width)
    val rail = measurables.first().measure(constraints.copy(minWidth = sliderWidth, maxWidth = sliderWidth, minHeight = 0))
    val labelPlaceables = labelMeasurables.map {
      it.measure(constraints.copy(minWidth = labelWidth, maxWidth = labelWidth, minHeight = 0))
    }
    val sliderX = (width - sliderWidth) / 2
    val trackWidth = sliderWidth - thumbWidthPx
    layout(width, constraints.constrainHeight(rail.height + labelPlaceables.maxOf { it.height })) {
      rail.placeRelative(sliderX, 0)
      labelPlaceables.forEachIndexed { index, label ->
        val stopCenter = sliderX + thumbWidthPx / 2f + trackWidth * index.toFloat() / (labels.size - 1)
        label.placeRelative((stopCenter - label.width / 2f).roundToInt(), rail.height)
      }
    }
  }
}
