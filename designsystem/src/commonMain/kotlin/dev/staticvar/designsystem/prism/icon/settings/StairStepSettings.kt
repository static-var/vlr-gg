/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
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
  path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
    moveTo(9f, 2f); horizontalLineTo(15f); verticalLineTo(5f)
    horizontalLineTo(19f); verticalLineTo(9f); horizontalLineTo(22f)
    verticalLineTo(15f); horizontalLineTo(19f); verticalLineTo(19f)
    horizontalLineTo(15f); verticalLineTo(22f); horizontalLineTo(9f)
    verticalLineTo(19f); horizontalLineTo(5f); verticalLineTo(15f)
    horizontalLineTo(2f); verticalLineTo(9f); horizontalLineTo(5f)
    verticalLineTo(5f); horizontalLineTo(9f); close()
    moveTo(9f, 9f); horizontalLineTo(15f); verticalLineTo(15f); horizontalLineTo(9f); close()
  }
}.build()
