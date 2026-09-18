/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import coil3.BitmapImage
import coil3.Image
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val AnalysisSize = 32
private const val OutlineSamples = 16

internal fun analyzeLogoImage(image: Image): LogoEdgeAnalysis {
  val source = image.logoImageBitmapOrNull() ?: return LogoEdgeAnalysis.Empty
  val scale = AnalysisSize.toFloat() / max(source.width, source.height)
  val sampleSize = IntSize(
    width = (source.width * scale).roundToInt().coerceAtLeast(1),
    height = (source.height * scale).roundToInt().coerceAtLeast(1),
  )
  val sample = ImageBitmap(sampleSize.width, sampleSize.height)
  CanvasDrawScope().draw(
    density = Density(1f),
    layoutDirection = LayoutDirection.Ltr,
    canvas = Canvas(sample),
    size = Size(sampleSize.width.toFloat(), sampleSize.height.toFloat()),
  ) {
    drawImage(
      image = source,
      dstSize = sampleSize,
    )
  }
  return analyzeLogoEdges(sample)
}

internal fun renderOutlinedLogo(
  image: Image,
  pixelSize: IntSize,
  radiusPx: Float,
  outlineColor: Color,
): ImageBitmap? {
  val source = image.logoImageBitmapOrNull() ?: return null
  require(pixelSize.width > 0 && pixelSize.height > 0) { "pixelSize must be positive: $pixelSize" }
  require(radiusPx.isFinite() && radiusPx >= 0f) { "radiusPx must be finite and non-negative: $radiusPx" }

  val padding = ceil(radiusPx).toInt()
  val outputWidth = pixelSize.width.toLong() + padding.toLong() * 2
  val outputHeight = pixelSize.height.toLong() + padding.toLong() * 2
  require(outputWidth <= Int.MAX_VALUE && outputHeight <= Int.MAX_VALUE) {
    "Outlined logo dimensions are too large: ${outputWidth}x$outputHeight"
  }

  val output = ImageBitmap(outputWidth.toInt(), outputHeight.toInt())
  val fitted = source.fitInside(pixelSize)
  val outlineFilter = ColorFilter.tint(outlineColor)
  CanvasDrawScope().draw(
    density = Density(1f),
    layoutDirection = LayoutDirection.Ltr,
    canvas = Canvas(output),
    size = Size(output.width.toFloat(), output.height.toFloat()),
  ) {
    repeat(OutlineSamples) { index ->
      val angle = 2 * PI * index / OutlineSamples
      drawFittedImage(
        source = source,
        fitted = fitted,
        offset = Offset(
          x = padding + cos(angle).toFloat() * radiusPx,
          y = padding + sin(angle).toFloat() * radiusPx,
        ),
        colorFilter = outlineFilter,
      )
    }
    drawFittedImage(
      source = source,
      fitted = fitted,
      offset = Offset(padding.toFloat(), padding.toFloat()),
    )
  }
  return output
}

private fun DrawScope.drawFittedImage(
  source: ImageBitmap,
  fitted: FittedImage,
  offset: Offset,
  colorFilter: ColorFilter? = null,
) {
  translate(
    left = offset.x + fitted.left,
    top = offset.y + fitted.top,
  ) {
    scale(
      scaleX = fitted.scale,
      scaleY = fitted.scale,
      pivot = Offset.Zero,
    ) {
      drawImage(source, colorFilter = colorFilter)
    }
  }
}

private fun ImageBitmap.fitInside(pixelSize: IntSize): FittedImage {
  val scale = min(
    pixelSize.width / width.toFloat(),
    pixelSize.height / height.toFloat(),
  )
  return FittedImage(
    scale = scale,
    left = (pixelSize.width - width * scale) / 2f,
    top = (pixelSize.height - height * scale) / 2f,
  )
}

private data class FittedImage(
  val scale: Float,
  val left: Float,
  val top: Float,
)

private fun Image.logoImageBitmapOrNull(): ImageBitmap? =
  if (this is BitmapImage && shareable && width > 0 && height > 0) bitmap.toLogoImageBitmap() else null

internal expect fun coil3.Bitmap.toLogoImageBitmap(): ImageBitmap
