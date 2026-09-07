/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.frame

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.offset
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/**
 * Reserves shadow space outside the foreground. Pressing moves only the foreground; measured
 * bounds, hit targets, semantics, and the shadow stay fixed. Offsets are physical down/right
 * in either layout direction.
 * Apply before foreground background, border, and clipping modifiers.
 */
public fun Modifier.prismFrame(frame: PrismFrameTokens, shape: Shape, pressProgress: Float = 0f): Modifier =
  if (frame.shadowOffset == DpOffset.Zero) {
    this
  } else {
    drawWithCache {
      val dx = frame.shadowOffset.x.roundToPx().coerceAtMost(size.width.toInt())
      val dy = frame.shadowOffset.y.roundToPx().coerceAtMost(size.height.toInt())
      val outline = shape.createOutline(
        Size((size.width - dx).coerceAtLeast(0f), (size.height - dy).coerceAtLeast(0f)),
        layoutDirection,
        this,
      )
      onDrawWithContent {
        translate(dx.toFloat(), dy.toFloat()) { drawOutline(outline, frame.shadowColor) }
        val progress = pressProgress.coerceIn(0f, 1f)
        translate(dx * progress, dy * progress) { this@onDrawWithContent.drawContent() }
      }
    }.layout { measurable, constraints ->
      val dx = frame.shadowOffset.x.roundToPx().coerceAtMost(constraints.maxWidth)
      val dy = frame.shadowOffset.y.roundToPx().coerceAtMost(constraints.maxHeight)
      val placeable = measurable.measure(constraints.offset(horizontal = -dx, vertical = -dy))
      layout(placeable.width + dx, placeable.height + dy) {
        placeable.place(0, 0)
      }
    }
  }
