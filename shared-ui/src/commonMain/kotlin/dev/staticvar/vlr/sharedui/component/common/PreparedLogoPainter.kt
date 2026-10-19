/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal class PreparedLogoPainter(private val bitmap: ImageBitmap) : Painter() {
  override val intrinsicSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())

  override fun DrawScope.onDraw() {
    drawImage(image = bitmap, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
  }
}
