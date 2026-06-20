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

public val StairStepRankingsFilled: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepRankingsFilled",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Star Sparkle
      moveTo(23f, 2f); horizontalLineTo(25f); verticalLineTo(4f); horizontalLineTo(27f); verticalLineTo(6f); horizontalLineTo(25f); verticalLineTo(8f); horizontalLineTo(23f); verticalLineTo(6f); horizontalLineTo(21f); verticalLineTo(4f); horizontalLineTo(23f); close()

      // Center Podium Solid (CW)
      moveTo(23f, 12f); horizontalLineTo(25f)
      verticalLineTo(14f); horizontalLineTo(27f); verticalLineTo(16f); horizontalLineTo(29f)
      verticalLineTo(44f); horizontalLineTo(19f); verticalLineTo(16f)
      horizontalLineTo(21f); verticalLineTo(14f); horizontalLineTo(23f); close()

      // Left Podium Solid (CW)
      moveTo(11f, 24f); horizontalLineTo(13f)
      verticalLineTo(26f); horizontalLineTo(15f); verticalLineTo(28f); horizontalLineTo(17f)
      verticalLineTo(44f); horizontalLineTo(7f); verticalLineTo(28f)
      horizontalLineTo(9f); verticalLineTo(26f); horizontalLineTo(11f); close()

      // Right Podium Solid (CW)
      moveTo(35f, 32f); horizontalLineTo(37f)
      verticalLineTo(34f); horizontalLineTo(39f); verticalLineTo(36f); horizontalLineTo(41f)
      verticalLineTo(44f); horizontalLineTo(31f); verticalLineTo(36f)
      horizontalLineTo(33f); verticalLineTo(34f); horizontalLineTo(35f); close()
      
      // Ground Axis (CW)
      moveTo(4f, 44f); horizontalLineTo(44f); verticalLineTo(48f); horizontalLineTo(4f); close()
    }
  }.build()
