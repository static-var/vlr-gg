/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.favorite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

@Immutable
public enum class PrismFavoriteIconSize(internal val containerSize: Dp, internal val strokeWidth: Dp) {
  Small(containerSize = 16.dp, strokeWidth = 1.5.dp),
  Medium(containerSize = 32.dp, strokeWidth = 2.dp),
  Large(containerSize = 48.dp, strokeWidth = 2.5.dp),
}

@Composable
public fun PrismFavoriteIcon(
  selected: Boolean,
  modifier: Modifier = Modifier,
  size: PrismFavoriteIconSize = PrismFavoriteIconSize.Medium,
  style: PrismFavoriteIconStyle = PrismFavoriteIconStyle.Boxed,
  contentDescription: String? = if (selected) "Favorite" else "Not favorite",
) {
  val markColor = if (selected) Prism.color.accent else Prism.color.labelColor
  val borderColor = Prism.color.stroke

  PrismSurface(
    modifier = modifier
      .size(size.containerSize)
      .then(
        if (contentDescription == null) {
          Modifier
        } else {
          Modifier.semantics { this.contentDescription = contentDescription }
        },
      ),
    color = style.containerColor(selected = selected),
    shape = Prism.shapes.small,
    border = style.border(selected = selected),
  ) {
    Box(modifier = Modifier.size(size.containerSize), contentAlignment = Alignment.Center) {
      Canvas(modifier = Modifier.size(size.containerSize)) {
        val strokePx = size.strokeWidth.toPx()
        val markStroke = Stroke(width = strokePx)
        val fillColor = if (selected) markColor else Color.Transparent
        val inset = size.containerSize.toPx() * 0.22f
        val markSize = this.size.width - (inset * 2f)

        translate(left = inset, top = inset) {
          val path = bookmarkTagPath(size = Size(markSize, markSize))
          drawPath(path = path, color = fillColor)
          drawPath(path = path, color = markColor, style = markStroke)
          if (selected) {
            drawLine(
              color = borderColor,
              start = Offset(markSize * 0.28f, markSize * 0.2f),
              end = Offset(markSize * 0.72f, markSize * 0.2f),
              strokeWidth = strokePx,
            )
          }
        }
      }
    }
  }
}

private fun bookmarkTagPath(size: Size): Path = Path().apply {
  moveTo(size.width * 0.18f, 0f)
  lineTo(size.width * 0.82f, 0f)
  lineTo(size.width * 0.82f, size.height)
  lineTo(size.width * 0.5f, size.height * 0.72f)
  lineTo(size.width * 0.18f, size.height)
  close()
}
