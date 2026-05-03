/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.divider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.Prism

/**
 * Horizontal Prism divider for separating dense app content.
 *
 * The [style] controls the supported thickness and color treatment. This component intentionally
 * supports horizontal dividers only.
 */
@Composable
public fun PrismDivider(modifier: Modifier = Modifier, style: PrismDividerStyle = PrismDividerStyle.Default) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = style.endCapHeight),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    DividerEndCap(width = style.endCapWidth, height = style.endCapHeight, color = style.endCapColor)
    Box(
      modifier = Modifier
        .weight(1f)
        .height(style.thickness)
        .background(style.color),
    )
    DividerEndCap(width = style.endCapWidth, height = style.endCapHeight, color = style.endCapColor)
  }
}

@Composable
private fun DividerEndCap(width: Dp, height: Dp, color: Color) {
  Box(
    modifier = Modifier
      .width(width)
      .height(height)
      .clip(Prism.shapes.small)
      .background(color),
  )
}
