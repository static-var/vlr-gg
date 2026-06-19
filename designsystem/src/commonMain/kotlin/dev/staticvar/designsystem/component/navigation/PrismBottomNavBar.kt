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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun PrismBottomNavBar(
  items: List<PrismBottomNavItem>,
  selectedItemId: String,
  onItemSelected: (PrismBottomNavItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val systemBarHeight = with(LocalDensity.current) {
    WindowInsets.navigationBars.getBottom(this).toDp()
  }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp + systemBarHeight)
      .background(Prism.color.background),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    items.forEach { item ->
      PrismBottomNavItem(
        item = item,
        selected = item.id == selectedItemId,
        onClick = { onItemSelected(item) },
      )
    }
  }
}

@Composable
private fun RowScope.PrismBottomNavItem(item: PrismBottomNavItem, selected: Boolean, onClick: () -> Unit) {
  val transition = updateTransition(
    targetState = selected,
    label = "BottomNavItemSelection",
  )
  val offsetY by transition.animateDp(
    transitionSpec = { tween(800) },
    label = "BottomNavItemOffset",
  ) { isSelected ->
    if (isSelected) (-12).dp else 0.dp
  }
  val weight by transition.animateFloat(
    transitionSpec = { tween(800) },
    label = "BottomNavItemWeight",
  ) { isSelected ->
    if (isSelected) 1.1f else 1f
  }
  val contentColor by transition.animateColor(
    transitionSpec = { tween(800) },
    label = "BottomNavItemContentColor",
  ) { isSelected ->
    if (isSelected) Prism.color.background else Prism.color.labelColor
  }
  val backgroundColor by transition.animateColor(
    transitionSpec = { tween(800) },
    label = "BottomNavItemBackgroundColor",
  ) { isSelected ->
    if (isSelected) Prism.color.accent else Prism.color.background
  }

  Column(
    modifier = Modifier
      .weight(weight)
      .fillMaxHeight()
      .clickable(enabled = true, onClick = onClick)
      .background(backgroundColor)
      .zIndex(if (selected) 1f else 0f),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .offset(y = offsetY)
        .background(backgroundColor),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(modifier = Modifier.height(8.dp))
      Icon(
        imageVector = if (selected) item.selectedIcon else item.icon,
        contentDescription = item.label,
        tint = contentColor,
      )
      Text(
        text = item.label,
        style = Prism.typography.caption,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
