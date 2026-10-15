/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.surface

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.backgroundColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.unit.Dp
import kotlin.math.ceil
import kotlin.math.min

internal fun Modifier.prismAnimatedBackground(shape: Shape, color: () -> Color): Modifier =
  surfaceSemantics(shape, color).drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind { drawOutline(outline, color()) }
  }

internal fun Modifier.prismAnimatedSurface(
  shape: Shape,
  containerColor: () -> Color,
  borderColor: () -> Color,
  borderWidth: () -> Dp,
): Modifier = surfaceSemantics(shape, containerColor).drawWithCache {
  val outline = shape.createOutline(size, layoutDirection, this)
  val width = borderWidth()
  val hasBorder = width.toPx() >= 0f && size.minDimension > 0f
  val strokeWidth = min(if (width == Dp.Hairline) 1f else ceil(width.toPx()), ceil(size.minDimension / 2))
  val fillBorder = strokeWidth * 2 > size.minDimension
  val stroke = Stroke(strokeWidth)
  val borderPath = if (hasBorder && outline is Outline.Rounded && !outline.roundRect.isSimple) {
    roundedBorderPath(outline.roundRect, strokeWidth, fillBorder)
  } else {
    null
  }
  val genericBorder = if (hasBorder && !fillBorder && outline is Outline.Generic) {
    genericBorderMask(outline.path, strokeWidth)
  } else {
    null
  }
  onDrawWithContent {
    drawOutline(outline, containerColor())
    drawContent()
    if (hasBorder) {
      val color = borderColor()
      when {
        fillBorder -> drawOutline(outline, color)

        borderPath != null -> drawPath(borderPath, color)

        outline is Outline.Rectangle -> drawRect(
          color = color,
          topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
          size = Size(size.width - strokeWidth, size.height - strokeWidth),
          style = stroke,
        )

        outline is Outline.Rounded -> drawRoundedBorder(outline, color, stroke)

        outline is Outline.Generic && genericBorder != null -> {
          val bounds = outline.path.getBounds()
          translate(bounds.left, bounds.top) {
            drawImage(genericBorder, colorFilter = ColorFilter.tint(color))
          }
        }
      }
    }
  }
}

private fun DrawScope.drawRoundedBorder(outline: Outline.Rounded, color: Color, stroke: Stroke) {
  val width = stroke.width
  val halfWidth = width / 2
  val radius = outline.roundRect.topLeftCornerRadius
  if (radius.x < halfWidth) {
    clipRect(width, width, size.width - width, size.height - width, clipOp = ClipOp.Difference) {
      drawRoundRect(color, cornerRadius = radius)
    }
  } else {
    drawRoundRect(
      color = color,
      topLeft = Offset(halfWidth, halfWidth),
      size = Size(size.width - width, size.height - width),
      cornerRadius = radius.inset(halfWidth),
      style = stroke,
    )
  }
}

private fun roundedBorderPath(roundedRect: RoundRect, width: Float, fillBorder: Boolean): Path = Path().apply {
  addRoundRect(roundedRect)
  if (!fillBorder) {
    val inner = Path().apply {
      addRoundRect(
        RoundRect(
          left = width,
          top = width,
          right = roundedRect.width - width,
          bottom = roundedRect.height - width,
          topLeftCornerRadius = roundedRect.topLeftCornerRadius.inset(width),
          topRightCornerRadius = roundedRect.topRightCornerRadius.inset(width),
          bottomLeftCornerRadius = roundedRect.bottomLeftCornerRadius.inset(width),
          bottomRightCornerRadius = roundedRect.bottomRightCornerRadius.inset(width),
        ),
      )
    }
    op(this, inner, PathOperation.Difference)
  }
}

private fun CornerRadius.inset(width: Float): CornerRadius =
  CornerRadius((x - width).coerceAtLeast(0f), (y - width).coerceAtLeast(0f))

private fun Modifier.surfaceSemantics(shape: Shape, color: () -> Color): Modifier = semantics {
  this.shape = shape
  backgroundColor = ColorProducer { color() }
}

private fun CacheDrawScope.genericBorderMask(path: Path, width: Float): ImageBitmap {
  val bounds = path.getBounds()
  val bitmap = ImageBitmap(
    width = ceil(bounds.width).toInt().coerceAtLeast(1),
    height = ceil(bounds.height).toInt().coerceAtLeast(1),
    config = ImageBitmapConfig.Alpha8,
  )
  val outside = Path().apply {
    addRect(bounds)
    op(this, path, PathOperation.Difference)
  }
  CanvasDrawScope().draw(
    density = this,
    layoutDirection = layoutDirection,
    canvas = Canvas(bitmap),
    size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
  ) {
    translate(-bounds.left, -bounds.top) {
      drawPath(path, Color.Black, style = Stroke(width * 2))
      scale((size.width + 1) / size.width, (size.height + 1) / size.height) {
        drawPath(outside, Color.Black, blendMode = BlendMode.Clear)
      }
    }
  }
  bitmap.prepareToDraw()
  return bitmap
}
