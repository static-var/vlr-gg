/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal class PreparedLogoPainter(
  private val treatment: LogoTreatment.Outlined,
  private val pixelSize: IntSize,
) : Painter() {
  override val intrinsicSize = Size(pixelSize.width.toFloat(), pixelSize.height.toFloat())

  override fun DrawScope.onDraw() {
    val xPadding = treatment.padding * size.width / pixelSize.width
    val yPadding = treatment.padding * size.height / pixelSize.height
    drawImage(
      image = treatment.bitmap,
      dstOffset = IntOffset(-xPadding.roundToInt(), -yPadding.roundToInt()),
      dstSize = IntSize((size.width + 2 * xPadding).roundToInt(), (size.height + 2 * yPadding).roundToInt()),
    )
  }
}
