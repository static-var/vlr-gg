/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.events

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val StairStepEventsFilled: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepEventsFilled",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Solid Trophy (CW)
      // Base
      moveTo(12f, 40f); horizontalLineTo(36f); verticalLineTo(44f); horizontalLineTo(12f); close()
      moveTo(16f, 36f); horizontalLineTo(32f); verticalLineTo(40f); horizontalLineTo(16f); close()
      // Stem
      moveTo(22f, 28f); horizontalLineTo(26f); verticalLineTo(36f); horizontalLineTo(22f); close()
      // Cup
      moveTo(16f, 26f); horizontalLineTo(32f); verticalLineTo(28f); horizontalLineTo(16f); close()
      moveTo(14f, 20f); horizontalLineTo(34f); verticalLineTo(26f); horizontalLineTo(14f); close()
      moveTo(12f, 12f); horizontalLineTo(36f); verticalLineTo(20f); horizontalLineTo(12f); close()
      moveTo(10f, 8f); horizontalLineTo(38f); verticalLineTo(12f); horizontalLineTo(10f); close()
      // Left handle
      moveTo(6f, 12f); horizontalLineTo(12f); verticalLineTo(14f); horizontalLineTo(6f); close()
      moveTo(4f, 14f); horizontalLineTo(6f); verticalLineTo(22f); horizontalLineTo(4f); close()
      moveTo(6f, 22f); horizontalLineTo(14f); verticalLineTo(24f); horizontalLineTo(6f); close()
      // Right handle
      moveTo(36f, 12f); horizontalLineTo(42f); verticalLineTo(14f); horizontalLineTo(36f); close()
      moveTo(42f, 14f); horizontalLineTo(44f); verticalLineTo(22f); horizontalLineTo(42f); close()
      moveTo(36f, 22f); horizontalLineTo(42f); verticalLineTo(24f); horizontalLineTo(36f); close()
      
      // Star Cutout (CCW)
      moveTo(23f, 14f); verticalLineTo(16f); horizontalLineTo(25f); verticalLineTo(14f); close()
      moveTo(21f, 16f); verticalLineTo(18f); horizontalLineTo(27f); verticalLineTo(16f); close()
      moveTo(23f, 18f); verticalLineTo(20f); horizontalLineTo(25f); verticalLineTo(18f); close()
    }
  }.build()
