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

public val StairStepNewsFilled: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepNewsFilled",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Solid Outline Silhouette (CW)
      moveTo(8f, 4f); horizontalLineTo(30f); verticalLineTo(8f); horizontalLineTo(34f); verticalLineTo(12f); horizontalLineTo(40f); verticalLineTo(44f); horizontalLineTo(8f); close()
      // Cutouts (CCW)
      // Fold cutout
      moveTo(30f, 4f); verticalLineTo(16f); horizontalLineTo(34f); verticalLineTo(12f); horizontalLineTo(40f); verticalLineTo(8f); horizontalLineTo(34f); verticalLineTo(4f); close()
      // Image cutout
      moveTo(16f, 12f); verticalLineTo(20f); horizontalLineTo(24f); verticalLineTo(12f); close()
      // Title cutout
      moveTo(26f, 12f); verticalLineTo(16f); horizontalLineTo(34f); verticalLineTo(12f); close()
      // Subtitle cutout
      moveTo(26f, 18f); verticalLineTo(20f); horizontalLineTo(32f); verticalLineTo(18f); close()
      // Text line 1
      moveTo(16f, 24f); verticalLineTo(26f); horizontalLineTo(34f); verticalLineTo(24f); close()
      // Text line 2
      moveTo(16f, 28f); verticalLineTo(30f); horizontalLineTo(32f); verticalLineTo(28f); close()
      // Text line 3
      moveTo(16f, 32f); verticalLineTo(34f); horizontalLineTo(34f); verticalLineTo(32f); close()
      // Text line 4
      moveTo(16f, 36f); verticalLineTo(38f); horizontalLineTo(28f); verticalLineTo(36f); close()
    }
  }.build()
