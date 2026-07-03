/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex

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
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(style.containerHeight + systemBarHeight)
      .background(style.containerColor),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    items.forEach { item ->
      PrismBottomNavItem(
        item = item,
        selected = item.id == selectedItemId,
        style = style,
        onClick = { onItemSelected(item) },
      )
    }
  }
}

@Composable
private fun RowScope.PrismBottomNavItem(
  item: PrismBottomNavItem,
  selected: Boolean,
  style: PrismBottomNavBarStyle,
  onClick: () -> Unit,
) {
  val visualState = rememberPrismBottomNavItemVisualState(selected = selected, style = style)

  Column(
    modifier = Modifier
      .weight(visualState.weight)
      .fillMaxHeight()
      .clickable(enabled = true, onClick = onClick)
      .background(visualState.backgroundColor)
      .zIndex(visualState.zIndex),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(style.itemHeight)
        .offset(y = visualState.offsetY)
        .background(visualState.backgroundColor),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(modifier = Modifier.height(style.itemTopSpacing))
      Icon(
        imageVector = if (selected) item.selectedIcon else item.icon,
        contentDescription = item.label,
        tint = visualState.contentColor,
      )
      Text(
        text = item.label,
        style = visualState.labelTextStyle,
        color = visualState.contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun rememberPrismBottomNavItemVisualState(
  selected: Boolean,
  style: PrismBottomNavBarStyle,
): PrismBottomNavItemVisualState {
  val transition = updateTransition(
    targetState = selected,
    label = "BottomNavItemSelection",
  )
  val offsetY by transition.animateDp(
    transitionSpec = { tween(style.selectionAnimationDurationMillis) },
    label = "BottomNavItemOffset",
  ) { isSelected ->
    style.itemOffsetY(selected = isSelected)
  }
  val weight by transition.animateFloat(
    transitionSpec = { tween(style.selectionAnimationDurationMillis) },
    label = "BottomNavItemWeight",
  ) { isSelected ->
    style.itemWeight(selected = isSelected)
  }
  val contentColor by transition.animateColor(
    transitionSpec = { tween(style.selectionAnimationDurationMillis) },
    label = "BottomNavItemContentColor",
  ) { isSelected ->
    style.contentColor(selected = isSelected)
  }
  val backgroundColor by transition.animateColor(
    transitionSpec = { tween(style.selectionAnimationDurationMillis) },
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
  val offsetY: Dp,
  val weight: Float,
  val contentColor: Color,
  val backgroundColor: Color,
  val zIndex: Float,
  val labelTextStyle: TextStyle,
)
