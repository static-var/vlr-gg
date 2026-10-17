/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Idle motion of one character: how far and how often the tail swings, and when in the loop it blinks. */
internal data class MascotMotion(val tailDegrees: Float, val tailSwingsPerLoop: Int, val blinkAt: Float)

@Composable
internal fun MascotFigure(
  art: MascotArt,
  motion: MascotMotion,
  description: String,
  modifier: Modifier = Modifier,
  animated: Boolean = true,
) {
  val palette = mascotPalette(Prism.color)
  val phase =
    if (animated) {
      rememberInfiniteTransition(label = "$description idle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
        label = "$description motion",
      )
    } else {
      null
    }
  Canvas(modifier.size(160.dp).semantics { contentDescription = description }) {
    val progress = phase?.value ?: 0f
    val breath = sin(progress * 2f * PI.toFloat())
    val tailSwing = sin(progress * 2f * PI.toFloat() * motion.tailSwingsPerLoop)
    val blink = if (animated) (1f - abs(progress - motion.blinkAt) / 0.026f).coerceIn(0f, 1f) else 0f
    val factor = size.minDimension / 160f
    translate((size.width - 160f * factor) / 2f, (size.height - 160f * factor) / 2f) {
      scale(factor, factor, pivot = Offset.Zero) {
        with(art) {
          drawShadow(palette)
          rotate(tailSwing * motion.tailDegrees, pivot = tailPivot) { drawTail(palette) }
          scale(1f + breath * 0.009f, 1f + breath * 0.014f, pivot = Offset(80f, 142f)) {
            drawBody(palette)
            translate(top = -breath * 0.8f) {
              drawHead(palette)
              scale(1f, 1f - blink * 0.93f, pivot = eyesPivot) { drawEyes(palette) }
              drawFace(palette)
            }
          }
        }
      }
    }
  }
}

/** The character's front paws at figure scale, one art unit per dp, for resting on the celebration banner. */
@Composable
internal fun MascotPaws(art: MascotArt, modifier: Modifier = Modifier) {
  val palette = mascotPalette(Prism.color)
  val bounds = art.pawsBounds.inflate(OutlineMargin)
  Canvas(modifier.size(bounds.width.dp, bounds.height.dp)) {
    scale(density, density, pivot = Offset.Zero) {
      translate(-bounds.left, -bounds.top) { with(art) { drawPaws(palette) } }
    }
  }
}

/** Half of the 3.5 unit outline, so strokes on the edge of a layer's bounds are not clipped. */
private const val OutlineMargin = 1.75f
