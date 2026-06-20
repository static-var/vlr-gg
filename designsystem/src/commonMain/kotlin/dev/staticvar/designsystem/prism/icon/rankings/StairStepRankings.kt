/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.rankings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val StairStepRankings: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepRankings",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Star Sparkle
      moveTo(23f, 2f); horizontalLineTo(25f); verticalLineTo(4f); horizontalLineTo(27f); verticalLineTo(6f); horizontalLineTo(25f); verticalLineTo(8f); horizontalLineTo(23f); verticalLineTo(6f); horizontalLineTo(21f); verticalLineTo(4f); horizontalLineTo(23f); close()

      // Center Podium (1st)
      // Outer (CW)
      moveTo(23f, 12f); horizontalLineTo(25f)
      verticalLineTo(14f); horizontalLineTo(27f); verticalLineTo(16f); horizontalLineTo(29f)
      verticalLineTo(44f); horizontalLineTo(19f); verticalLineTo(16f)
      horizontalLineTo(21f); verticalLineTo(14f); horizontalLineTo(23f); close()
      // Inner Hole (CCW)
      moveTo(23f, 16f); verticalLineTo(40f); horizontalLineTo(25f); verticalLineTo(16f); close()

      // Left Podium (2nd)
      // Outer (CW)
      moveTo(11f, 24f); horizontalLineTo(13f)
      verticalLineTo(26f); horizontalLineTo(15f); verticalLineTo(28f); horizontalLineTo(17f)
      verticalLineTo(44f); horizontalLineTo(7f); verticalLineTo(28f)
      horizontalLineTo(9f); verticalLineTo(26f); horizontalLineTo(11f); close()
      // Inner Hole (CCW)
      moveTo(11f, 28f); verticalLineTo(40f); horizontalLineTo(13f); verticalLineTo(28f); close()

      // Right Podium (3rd)
      // Outer (CW)
      moveTo(35f, 32f); horizontalLineTo(37f)
      verticalLineTo(34f); horizontalLineTo(39f); verticalLineTo(36f); horizontalLineTo(41f)
      verticalLineTo(44f); horizontalLineTo(31f); verticalLineTo(36f)
      horizontalLineTo(33f); verticalLineTo(34f); horizontalLineTo(35f); close()
      // Inner Hole (CCW)
      moveTo(35f, 36f); verticalLineTo(40f); horizontalLineTo(37f); verticalLineTo(36f); close()
      
      // Ground Axis (CW)
      moveTo(4f, 44f); horizontalLineTo(44f); verticalLineTo(48f); horizontalLineTo(4f); close()
    }
  }.build()
