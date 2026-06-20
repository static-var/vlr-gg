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

public val StairStepBackFilled: ImageVector
  get() = ImageVector.Builder(
    name = "PrismStairStepBackFilled",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      // Solid Outline Silhouette (CW)
      moveTo(2f, 26f); verticalLineTo(22f); horizontalLineTo(6f); verticalLineTo(18f); horizontalLineTo(10f); verticalLineTo(14f); horizontalLineTo(14f); verticalLineTo(10f); horizontalLineTo(18f); verticalLineTo(16f); horizontalLineTo(42f); verticalLineTo(32f); horizontalLineTo(18f); verticalLineTo(38f); horizontalLineTo(14f); verticalLineTo(34f); horizontalLineTo(10f); verticalLineTo(30f); horizontalLineTo(6f); verticalLineTo(26f); close()
    }
  }.build()
