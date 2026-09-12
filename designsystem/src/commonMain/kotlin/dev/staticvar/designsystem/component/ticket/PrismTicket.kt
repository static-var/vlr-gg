/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.ticket

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import dev.staticvar.designsystem.prism.Prism
import kotlin.math.roundToInt

/**
 * A ticket-shaped container with an optional header, a body, and a perforated bottom [stub] with a zigzag torn edge.
 * Slots size to their content; the notches follow the body/stub boundary. Cutouts reveal the actual
 * background. [style] owns the visual treatment. Place actions inside the slots; the ticket itself
 * has no click behavior and does not merge its children's accessibility semantics.
 */
@Composable
public fun PrismTicket(
  modifier: Modifier = Modifier,
  style: PrismTicketStyle = PrismTicketStyle.Standard,
  header: (@Composable ColumnScope.() -> Unit)? = null,
  stub: @Composable ColumnScope.() -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  val seam = remember { mutableFloatStateOf(0f) }
  val frame = style.frame
  val animation = Prism.anim.standard
  CompositionLocalProvider(LocalContentColor provides style.contentColor) {
    Column(
      modifier = modifier
        .absolutePadding(right = frame.shadowOffset.x, bottom = frame.shadowOffset.y)
        .ticketSurface(style, seam)
        .animateContentSize(animationSpec = tween(durationMillis = animation.durationMillis, easing = animation.easing)),
    ) {
      Column(Modifier.fillMaxWidth().onSizeChanged { seam.floatValue = it.height.toFloat() }) {
        if (header != null) {
          CompositionLocalProvider(LocalContentColor provides style.headerContentColor) {
            Column(
              Modifier.fillMaxWidth().background(style.headerColor)
                .padding(horizontal = style.padding, vertical = style.headerVerticalPadding),
              content = header,
            )
          }
        }
        Column(Modifier.fillMaxWidth().padding(style.padding), content = content)
      }
      Column(Modifier.fillMaxWidth().background(style.stubColor).padding(style.padding), content = stub)
    }
  }
}

@Composable
private fun Modifier.ticketSurface(style: PrismTicketStyle, seam: FloatState): Modifier {
  val shape = style.shape
  val frame = style.frame
  val border = frame.border ?: style.border
  val background = style.containerColor
  val notchRadius = style.notchRadius
  val dashLength = style.dashLength
  val perforationWidth = style.perforationWidth
  val perforationColor = style.perforationColor
  val tearDepth = style.tearDepth
  val tearWidth = style.tearWidth
  return drawWithCache {
    val radius = minOf(notchRadius.toPx(), size.width / 2f, seam.floatValue, size.height - seam.floatValue)
      .coerceAtLeast(0f)
    val outline = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    val cutouts = Path().apply {
      addOval(Rect(-radius, seam.floatValue - radius, radius, seam.floatValue + radius))
      addOval(Rect(size.width - radius, seam.floatValue - radius, size.width + radius, seam.floatValue + radius))
    }
    val notched = Path.combine(PathOperation.Difference, outline, cutouts)
    val ticket = Path.combine(
      PathOperation.Intersect,
      notched,
      tornEdge(size, tearWidth.toPx(), minOf(tearDepth.toPx(), size.height - seam.floatValue)),
    )
    onDrawWithContent {
      translate(frame.shadowOffset.x.toPx(), frame.shadowOffset.y.toPx()) { drawPath(ticket, frame.shadowColor) }
      clipPath(ticket) {
        drawPath(ticket, background)
        this@onDrawWithContent.drawContent()
        drawPath(ticket, border.brush, style = Stroke(border.width.toPx() * 2f))
        drawLine(
          color = perforationColor,
          start = Offset(radius, seam.floatValue),
          end = Offset(size.width - radius, seam.floatValue),
          strokeWidth = perforationWidth.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), dashLength.toPx())),
        )
      }
    }
  }
}

private fun tornEdge(size: Size, toothWidth: Float, depth: Float): Path {
  val teeth = (size.width / toothWidth).roundToInt().coerceAtLeast(1)
  val pitch = size.width / teeth
  return Path().apply {
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - depth)
    for (tooth in teeth downTo 1) {
      lineTo((tooth - 0.5f) * pitch, size.height)
      lineTo((tooth - 1) * pitch, size.height - depth)
    }
    close()
  }
}
