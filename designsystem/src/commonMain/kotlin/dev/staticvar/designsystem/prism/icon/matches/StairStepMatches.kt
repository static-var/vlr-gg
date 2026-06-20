/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.matches

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val StairStepMatches: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepMatches",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Top bar
      moveTo(10f, 8f); horizontalLineTo(38f); verticalLineTo(14f); horizontalLineTo(10f); close()
      // Left top bumper area
      moveTo(6f, 12f); horizontalLineTo(10f); verticalLineTo(18f); horizontalLineTo(6f); close()
      // Right top bumper area
      moveTo(38f, 12f); horizontalLineTo(42f); verticalLineTo(18f); horizontalLineTo(38f); close()
      // Left wall
      moveTo(4f, 18f); horizontalLineTo(10f); verticalLineTo(30f); horizontalLineTo(4f); close()
      // Right wall
      moveTo(38f, 18f); horizontalLineTo(44f); verticalLineTo(30f); horizontalLineTo(38f); close()
      // Left Grip
      moveTo(8f, 30f); horizontalLineTo(18f); verticalLineTo(36f); horizontalLineTo(8f); close()
      // Right Grip
      moveTo(30f, 30f); horizontalLineTo(40f); verticalLineTo(36f); horizontalLineTo(30f); close()
      // Bottom bridge
      moveTo(18f, 26f); horizontalLineTo(30f); verticalLineTo(30f); horizontalLineTo(18f); close()
      
      // D-Pad
      moveTo(14f, 18f); horizontalLineTo(18f); verticalLineTo(26f); horizontalLineTo(14f); close()
      moveTo(12f, 20f); horizontalLineTo(20f); verticalLineTo(24f); horizontalLineTo(12f); close()
      
      // Action Buttons
      moveTo(30f, 18f); horizontalLineTo(32f); verticalLineTo(20f); horizontalLineTo(30f); close()
      moveTo(30f, 24f); horizontalLineTo(32f); verticalLineTo(26f); horizontalLineTo(30f); close()
      moveTo(27f, 21f); horizontalLineTo(29f); verticalLineTo(23f); horizontalLineTo(27f); close()
      moveTo(33f, 21f); horizontalLineTo(35f); verticalLineTo(23f); horizontalLineTo(33f); close()
      
      // Select/Start
      moveTo(22f, 20f); horizontalLineTo(24f); verticalLineTo(22f); horizontalLineTo(22f); close()
      moveTo(26f, 20f); horizontalLineTo(28f); verticalLineTo(22f); horizontalLineTo(26f); close()
    }
  }.build()
