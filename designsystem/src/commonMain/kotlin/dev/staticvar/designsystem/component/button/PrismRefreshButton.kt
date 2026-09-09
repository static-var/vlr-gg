/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.button

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.icon.refresh.RefreshArrows

/** Refresh action whose vector rotates while busy. The button frame stays still and repeat taps are disabled. */
@Composable
public fun PrismRefreshButton(
  isRefreshing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismIconButtonStyle = PrismIconButtonStyle.Bordered,
) {
  val rotation: State<Float>? = if (isRefreshing) {
    rememberInfiniteTransition(label = "refresh_rotation").animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(durationMillis = 1_800, easing = LinearEasing)),
      label = "refresh_angle",
    )
  } else {
    null
  }
  PrismIconButton(
    icon = RefreshArrows,
    contentDescription = "Refresh",
    onClick = onClick,
    modifier = modifier,
    enabled = enabled && !isRefreshing,
    style = style,
    size = PrismIconButtonSize.Toolbar,
    iconModifier = Modifier.size(28.dp).graphicsLayer { rotationZ = rotation?.value ?: 0f },
  )
}
