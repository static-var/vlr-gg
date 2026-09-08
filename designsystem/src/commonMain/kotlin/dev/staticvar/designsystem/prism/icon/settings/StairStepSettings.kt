/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val StairStepSettings: ImageVector = ImageVector.Builder(
  name = "PrismStairStepSettings",
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f,
).apply {
  path(fill = SolidColor(Color.Black)) {
    moveTo(3f, 6f); horizontalLineTo(21f); verticalLineTo(8f); horizontalLineTo(3f); close()
    moveTo(7f, 3f); horizontalLineTo(10f); verticalLineTo(11f); horizontalLineTo(7f); close()
    moveTo(3f, 16f); horizontalLineTo(21f); verticalLineTo(18f); horizontalLineTo(3f); close()
    moveTo(14f, 13f); horizontalLineTo(17f); verticalLineTo(21f); horizontalLineTo(14f); close()
  }
}.build()
