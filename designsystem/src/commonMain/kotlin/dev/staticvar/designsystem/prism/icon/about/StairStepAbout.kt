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

public val StairStepAbout: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepAbout",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Top and Bottom flat edges
      moveTo(16f, 4f); horizontalLineTo(32f); verticalLineTo(8f); horizontalLineTo(16f); close()
      moveTo(16f, 40f); horizontalLineTo(32f); verticalLineTo(44f); horizontalLineTo(16f); close()
      
      // Left and Right flat edges
      moveTo(4f, 16f); horizontalLineTo(8f); verticalLineTo(32f); horizontalLineTo(4f); close()
      moveTo(40f, 16f); horizontalLineTo(44f); verticalLineTo(32f); horizontalLineTo(40f); close()
      
      // Diagonals (stairs)
      moveTo(12f, 8f); horizontalLineTo(16f); verticalLineTo(12f); horizontalLineTo(12f); close()
      moveTo(8f, 12f); horizontalLineTo(12f); verticalLineTo(16f); horizontalLineTo(8f); close()
      
      moveTo(32f, 8f); horizontalLineTo(36f); verticalLineTo(12f); horizontalLineTo(32f); close()
      moveTo(36f, 12f); horizontalLineTo(40f); verticalLineTo(16f); horizontalLineTo(36f); close()
      
      moveTo(8f, 32f); horizontalLineTo(12f); verticalLineTo(36f); horizontalLineTo(8f); close()
      moveTo(12f, 36f); horizontalLineTo(16f); verticalLineTo(40f); horizontalLineTo(12f); close()
      
      moveTo(36f, 32f); horizontalLineTo(40f); verticalLineTo(36f); horizontalLineTo(36f); close()
      moveTo(32f, 36f); horizontalLineTo(36f); verticalLineTo(40f); horizontalLineTo(32f); close()
      
      // Information 'i' inside
      moveTo(22f, 14f); horizontalLineTo(26f); verticalLineTo(18f); horizontalLineTo(22f); close()
      
      moveTo(22f, 22f); horizontalLineTo(26f); verticalLineTo(34f); horizontalLineTo(22f); close()
      moveTo(20f, 34f); horizontalLineTo(28f); verticalLineTo(36f); horizontalLineTo(20f); close()
      moveTo(20f, 22f); horizontalLineTo(22f); verticalLineTo(24f); horizontalLineTo(20f); close()
    }
  }.build()
