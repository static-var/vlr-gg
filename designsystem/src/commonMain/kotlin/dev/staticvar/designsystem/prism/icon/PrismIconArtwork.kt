/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal fun prismIconArtwork(
  name: String,
  outline: String = "",
  fill: String = "",
  rounded: Boolean = false,
  autoMirror: Boolean = false,
): ImageVector = ImageVector.Builder(
  name = name,
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f,
  autoMirror = autoMirror,
).apply {
  if (fill.isNotEmpty()) {
    addPath(pathData = PathParser().parsePathString(fill).toNodes(), fill = SolidColor(Color.Black))
  }
  if (outline.isNotEmpty()) {
    addPath(
      pathData = PathParser().parsePathString(outline).toNodes(),
      fill = null,
      stroke = SolidColor(Color.Black),
      strokeLineWidth = if (rounded) 1.8f else 2.2f,
      strokeLineCap = if (rounded) StrokeCap.Round else StrokeCap.Square,
      strokeLineJoin = if (rounded) StrokeJoin.Round else StrokeJoin.Miter,
    )
  }
}.build()
