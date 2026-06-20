/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.back

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val StairStepBack: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepBack",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Main Arrow Outline (4px thick)
      // Tip
      moveTo(2f, 22f); horizontalLineTo(6f); verticalLineTo(26f); horizontalLineTo(2f); close()
      // Top edge stairs
      moveTo(6f, 18f); horizontalLineTo(10f); verticalLineTo(22f); horizontalLineTo(6f); close()
      moveTo(10f, 14f); horizontalLineTo(14f); verticalLineTo(18f); horizontalLineTo(10f); close()
      moveTo(14f, 10f); horizontalLineTo(18f); verticalLineTo(14f); horizontalLineTo(14f); close()
      // Vertical connector top
      moveTo(14f, 14f); horizontalLineTo(18f); verticalLineTo(16f); horizontalLineTo(14f); close()
      // Top shaft
      moveTo(18f, 16f); horizontalLineTo(42f); verticalLineTo(20f); horizontalLineTo(18f); close()
      // Right shaft
      moveTo(38f, 20f); horizontalLineTo(42f); verticalLineTo(28f); horizontalLineTo(38f); close()
      // Bottom shaft
      moveTo(18f, 28f); horizontalLineTo(42f); verticalLineTo(32f); horizontalLineTo(18f); close()
      // Vertical connector bottom
      moveTo(14f, 32f); horizontalLineTo(18f); verticalLineTo(34f); horizontalLineTo(14f); close()
      // Bottom edge stairs
      moveTo(14f, 34f); horizontalLineTo(18f); verticalLineTo(38f); horizontalLineTo(14f); close()
      moveTo(10f, 30f); horizontalLineTo(14f); verticalLineTo(34f); horizontalLineTo(10f); close()
      moveTo(6f, 26f); horizontalLineTo(10f); verticalLineTo(30f); horizontalLineTo(6f); close()
    }
  }.build()
