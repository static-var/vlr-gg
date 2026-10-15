/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.staticvar.designsystem.component.surface.prismAnimatedBackground
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Bottom navigation bar for compact screens.
 *
 * [style] owns the token-backed colors, dimensions, typography, and selected-item motion values.
 * This composable owns item selection and layout.
 */
@Composable
public fun PrismBottomNavBar(
  items: List<PrismBottomNavItem>,
  selectedItemId: String,
  onItemSelected: (PrismBottomNavItem) -> Unit,
  modifier: Modifier = Modifier,
  style: PrismBottomNavBarStyle = PrismBottomNavBarStyle.Default,
) {
  val systemBarHeight = with(LocalDensity.current) {
    WindowInsets.navigationBars.getBottom(this).toDp()
  }
  val visualStates = items.map { item ->
    rememberPrismBottomNavItemVisualState(selected = item.id == selectedItemId, style = style)
  }
  Layout(
    modifier = modifier
      .fillMaxWidth()
      .height(style.containerHeight + systemBarHeight)
      .background(style.containerColor),
    content = {
      items.forEachIndexed { index, item ->
        PrismBottomNavItem(
          item = item,
          selected = item.id == selectedItemId,
          style = style,
          visualState = visualStates[index],
          onClick = { onItemSelected(item) },
        )
      }
    },
    measurePolicy = remember(visualStates) { PrismBottomNavMeasurePolicy(visualStates) },
  )
}

private class PrismBottomNavMeasurePolicy(private val visualStates: List<PrismBottomNavItemVisualState>) :
  MeasurePolicy {
  override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
    val weights = visualStates.map { it.weight.value }
    val unitWidth = if (weights.isEmpty()) 0f else width / weights.sum()
    var remainder = width - weights.sumOf { (it * unitWidth).roundToInt() }
    val placeables = measurables.mapIndexed { index, measurable ->
      val correction = remainder.sign
      remainder -= correction
      val itemWidth = ((weights[index] * unitWidth).roundToInt() + correction).coerceAtLeast(0)
      measurable.measure(Constraints.fixed(itemWidth, constraints.maxHeight))
    }
    return layout(width, constraints.maxHeight) {
      var x = 0
      placeables.forEach { placeable ->
        placeable.placeRelative(x, 0)
        x += placeable.width
      }
    }
  }

  override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
    intrinsicWidth(measurables, height) { minIntrinsicWidth(it) }

  override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
    intrinsicWidth(measurables, height) { maxIntrinsicWidth(it) }

  override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
    intrinsicHeight(measurables, width) { minIntrinsicHeight(it) }

  override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
    intrinsicHeight(measurables, width) { maxIntrinsicHeight(it) }

  private inline fun intrinsicWidth(
    measurables: List<IntrinsicMeasurable>,
    height: Int,
    childWidth: IntrinsicMeasurable.(Int) -> Int,
  ): Int {
    var unitWidth = 0
    var totalWeight = 0f
    measurables.forEachIndexed { index, measurable ->
      val weight = visualStates[index].weight.value
      totalWeight += weight
      unitWidth = maxOf(unitWidth, (measurable.childWidth(height) / weight).roundToInt())
    }
    return (unitWidth * totalWeight).roundToInt()
  }

  private inline fun intrinsicHeight(
    measurables: List<IntrinsicMeasurable>,
    width: Int,
    childHeight: IntrinsicMeasurable.(Int) -> Int,
  ): Int {
    if (measurables.isEmpty()) return 0
    val weights = visualStates.map { it.weight.value }
    val unitWidth = if (width == Constraints.Infinity) Constraints.Infinity else (width / weights.sum()).roundToInt()
    var height = 0
    measurables.forEachIndexed { index, measurable ->
      val itemWidth = if (unitWidth == Constraints.Infinity) {
        Constraints.Infinity
      } else {
        (unitWidth * weights[index]).roundToInt()
      }
      height = maxOf(height, measurable.childHeight(itemWidth))
    }
    return height
  }
}

@Composable
private fun PrismBottomNavItem(
  item: PrismBottomNavItem,
  selected: Boolean,
  style: PrismBottomNavBarStyle,
  visualState: PrismBottomNavItemVisualState,
  onClick: () -> Unit,
) {
  val itemShape = style.itemShape

  Column(
    modifier = Modifier
      .fillMaxHeight()
      .clickable(enabled = true, onClick = onClick)
      .prismAnimatedBackground(RectangleShape) { visualState.backgroundColor.value }
      .zIndex(visualState.zIndex),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(style.itemHeight)
        .offset { IntOffset(0, visualState.offsetY.value.roundToPx()) }
        .prismAnimatedBackground(itemShape) { visualState.backgroundColor.value },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(modifier = Modifier.height(style.itemTopSpacing))
      PrismBottomNavItemContent(item, selected, visualState.contentColor, visualState.labelTextStyle)
    }
  }
}

@Composable
private fun PrismBottomNavItemContent(
  item: PrismBottomNavItem,
  selected: Boolean,
  contentColor: State<Color>,
  labelTextStyle: TextStyle,
) {
  Icon(
    imageVector = if (selected) item.selectedIcon else item.icon,
    contentDescription = item.label,
    tint = contentColor.value,
  )
  Text(
    text = item.label,
    style = labelTextStyle,
    color = contentColor.value,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

@Composable
private fun rememberPrismBottomNavItemVisualState(
  selected: Boolean,
  style: PrismBottomNavBarStyle,
): PrismBottomNavItemVisualState {
  val animation = style.selectionAnimation
  val transition = updateTransition(
    targetState = selected,
    label = "BottomNavItemSelection",
  )
  val offsetY = transition.animateDp(
    transitionSpec = { animation.dpSpec() },
    label = "BottomNavItemOffset",
  ) { isSelected ->
    style.itemOffsetY(selected = isSelected)
  }
  val weight = transition.animateFloat(
    transitionSpec = { animation.floatSpec() },
    label = "BottomNavItemWeight",
  ) { isSelected ->
    style.itemWeight(selected = isSelected)
  }
  val contentColor = transition.animateColor(
    transitionSpec = { animation.colorSpec() },
    label = "BottomNavItemContentColor",
  ) { isSelected ->
    style.contentColor(selected = isSelected)
  }
  val backgroundColor = transition.animateColor(
    transitionSpec = { animation.colorSpec() },
    label = "BottomNavItemBackgroundColor",
  ) { isSelected ->
    style.itemContainerColor(selected = isSelected)
  }

  return PrismBottomNavItemVisualState(
    offsetY = offsetY,
    weight = weight,
    contentColor = contentColor,
    backgroundColor = backgroundColor,
    zIndex = style.itemZIndex(selected = selected),
    labelTextStyle = style.labelTextStyle,
  )
}

private data class PrismBottomNavItemVisualState(
  val offsetY: State<Dp>,
  val weight: State<Float>,
  val contentColor: State<Color>,
  val backgroundColor: State<Color>,
  val zIndex: Float,
  val labelTextStyle: TextStyle,
)
