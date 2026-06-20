/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.news

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val StairStepNews: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepNews",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Top border
      moveTo(8f, 4f); horizontalLineTo(30f); verticalLineTo(8f); horizontalLineTo(8f); close()
      // Left border
      moveTo(8f, 8f); horizontalLineTo(12f); verticalLineTo(44f); horizontalLineTo(8f); close()
      // Bottom border
      moveTo(12f, 40f); horizontalLineTo(40f); verticalLineTo(44f); horizontalLineTo(12f); close()
      // Right border
      moveTo(36f, 16f); horizontalLineTo(40f); verticalLineTo(40f); horizontalLineTo(36f); close()
      // Dog ear (Fold)
      moveTo(30f, 4f); horizontalLineTo(34f); verticalLineTo(8f); horizontalLineTo(40f); verticalLineTo(12f); horizontalLineTo(34f); verticalLineTo(16f); horizontalLineTo(30f); close()
      // Image block (square on left)
      moveTo(16f, 12f); horizontalLineTo(24f); verticalLineTo(20f); horizontalLineTo(16f); close()
      // Title line (thick)
      moveTo(26f, 12f); horizontalLineTo(34f); verticalLineTo(16f); horizontalLineTo(26f); close()
      // Subtitle line (thinner)
      moveTo(26f, 18f); horizontalLineTo(32f); verticalLineTo(20f); horizontalLineTo(26f); close()
      // Text line 1
      moveTo(16f, 24f); horizontalLineTo(34f); verticalLineTo(26f); horizontalLineTo(16f); close()
      // Text line 2
      moveTo(16f, 28f); horizontalLineTo(32f); verticalLineTo(30f); horizontalLineTo(16f); close()
      // Text line 3
      moveTo(16f, 32f); horizontalLineTo(34f); verticalLineTo(34f); horizontalLineTo(16f); close()
      // Text line 4
      moveTo(16f, 36f); horizontalLineTo(28f); verticalLineTo(38f); horizontalLineTo(16f); close()
    }
  }.build()
