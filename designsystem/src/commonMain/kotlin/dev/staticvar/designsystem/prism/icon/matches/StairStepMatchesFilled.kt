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

public val StairStepMatchesFilled: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepMatchesFilled",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Gamepad Silhouette (CW)
      moveTo(10f, 8f); horizontalLineTo(38f); verticalLineTo(12f); horizontalLineTo(42f); verticalLineTo(18f); horizontalLineTo(44f); verticalLineTo(30f); horizontalLineTo(40f); verticalLineTo(36f); horizontalLineTo(30f); verticalLineTo(30f); horizontalLineTo(18f); verticalLineTo(36f); horizontalLineTo(8f); verticalLineTo(30f); horizontalLineTo(4f); verticalLineTo(18f); horizontalLineTo(6f); verticalLineTo(12f); horizontalLineTo(10f); close()
      // D-Pad Cutout (CCW)
      moveTo(14f, 18f); verticalLineTo(20f); horizontalLineTo(12f); verticalLineTo(24f); horizontalLineTo(14f); verticalLineTo(26f); horizontalLineTo(18f); verticalLineTo(24f); horizontalLineTo(20f); verticalLineTo(20f); horizontalLineTo(18f); verticalLineTo(18f); close()
      // Action Buttons (CCW)
      moveTo(30f, 18f); verticalLineTo(20f); horizontalLineTo(32f); verticalLineTo(18f); close()
      moveTo(30f, 24f); verticalLineTo(26f); horizontalLineTo(32f); verticalLineTo(24f); close()
      moveTo(27f, 21f); verticalLineTo(23f); horizontalLineTo(29f); verticalLineTo(21f); close()
      moveTo(33f, 21f); verticalLineTo(23f); horizontalLineTo(35f); verticalLineTo(21f); close()
      // Select/Start (CCW)
      moveTo(22f, 20f); verticalLineTo(22f); horizontalLineTo(24f); verticalLineTo(20f); close()
      moveTo(26f, 20f); verticalLineTo(22f); horizontalLineTo(28f); verticalLineTo(20f); close()
    }
  }.build()
