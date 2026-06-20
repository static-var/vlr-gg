/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.about

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val StairStepAboutFilled: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepAboutFilled",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Solid Octagon Silhouette (CW)
      moveTo(16f, 4f); horizontalLineTo(32f); verticalLineTo(8f); horizontalLineTo(36f); verticalLineTo(12f); horizontalLineTo(40f); verticalLineTo(16f); horizontalLineTo(44f); verticalLineTo(32f); horizontalLineTo(40f); verticalLineTo(36f); horizontalLineTo(36f); verticalLineTo(40f); horizontalLineTo(32f); verticalLineTo(44f); horizontalLineTo(16f); verticalLineTo(40f); horizontalLineTo(12f); verticalLineTo(36f); horizontalLineTo(8f); verticalLineTo(32f); horizontalLineTo(4f); verticalLineTo(16f); horizontalLineTo(8f); verticalLineTo(12f); horizontalLineTo(12f); verticalLineTo(8f); horizontalLineTo(16f); close()
      
      // Cutouts (CCW)
      // Dot
      moveTo(22f, 14f); verticalLineTo(18f); horizontalLineTo(26f); verticalLineTo(14f); close()
      // Stem
      moveTo(20f, 22f); verticalLineTo(24f); horizontalLineTo(22f); verticalLineTo(34f); horizontalLineTo(20f); verticalLineTo(36f); horizontalLineTo(28f); verticalLineTo(34f); horizontalLineTo(26f); verticalLineTo(22f); close()
    }
  }.build()
