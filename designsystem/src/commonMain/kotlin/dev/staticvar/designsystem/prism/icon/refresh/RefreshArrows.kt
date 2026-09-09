/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon.refresh

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val RefreshArrows: ImageVector by lazy {
  ImageVector.Builder(
    name = "PrismRefreshArrows",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Square, strokeLineJoin = StrokeJoin.Miter) {
      moveTo(4.5f, 9f)
      curveTo(5.7f, 5f, 9.7f, 2.8f, 13.7f, 3.7f)
      curveTo(16.3f, 4.2f, 18.4f, 5.7f, 20f, 8f)
      moveTo(15f, 8f)
      horizontalLineTo(20f)
      verticalLineTo(3f)
      moveTo(19.5f, 15f)
      curveTo(18.3f, 19f, 14.3f, 21.2f, 10.3f, 20.3f)
      curveTo(7.7f, 19.8f, 5.6f, 18.3f, 4f, 16f)
      moveTo(9f, 16f)
      horizontalLineTo(4f)
      verticalLineTo(21f)
    }
  }.build()
}
