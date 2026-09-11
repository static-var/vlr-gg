/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.share

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val ShareArrow: ImageVector by lazy {
  ImageVector.Builder(
    name = "PrismShareArrow",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Square, strokeLineJoin = StrokeJoin.Miter) {
      moveTo(12f, 15f)
      verticalLineTo(3f)
      moveTo(8f, 7f)
      lineTo(12f, 3f)
      lineTo(16f, 7f)
      moveTo(6f, 11f)
      horizontalLineTo(4f)
      verticalLineTo(21f)
      horizontalLineTo(20f)
      verticalLineTo(11f)
      horizontalLineTo(18f)
    }
  }.build()
}
