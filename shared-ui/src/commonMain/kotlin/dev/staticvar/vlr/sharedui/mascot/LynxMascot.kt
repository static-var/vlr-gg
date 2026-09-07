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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
public fun LynxMascot(
  modifier: Modifier = Modifier,
  animated: Boolean = true,
): Unit {
  val colors = Prism.color
  val paths = remember { LynxPaths() }
  val phase =
    if (animated) {
      rememberInfiniteTransition(label = "Lynx idle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
        label = "Lynx motion",
      )
    } else {
      null
    }
  val fur = lerp(colors.accent, colors.surface, 0.28f)
  val outline = lerp(colors.accent, colors.contentPrimary, 0.55f)
  val lightFur = lerp(colors.surface, colors.onAccent, 0.2f)
  val stroke = remember { Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round) }

  Canvas(modifier.size(160.dp).semantics { contentDescription = "Lynx cat mascot" }) {
    val progress = phase?.value ?: 0f
    val wave = sin(progress * 2f * PI.toFloat())
    val blink = if (animated) (1f - abs(progress - 0.79f) / 0.025f).coerceIn(0f, 1f) else 0f
    val factor = size.minDimension / 160f
    translate((size.width - 160f * factor) / 2f, (size.height - 160f * factor) / 2f) {
      scale(factor, factor, pivot = Offset.Zero) {
        drawOval(colors.contentPrimary.copy(alpha = 0.08f), Offset(37f, 139f), Size(92f, 10f))
        rotate(wave * 9f, pivot = Offset(107f, 124f)) {
          drawPath(paths.tail, fur)
          drawPath(paths.tail, outline, style = stroke)
        }
        scale(1f + wave * 0.009f, 1f + wave * 0.014f, pivot = Offset(80f, 140f)) {
          drawPath(paths.body, fur)
          drawPath(paths.body, outline, style = stroke)
          drawPath(paths.bib, lightFur)
          drawPath(paths.paws, fur)
          drawPath(paths.paws, outline, style = stroke)
          drawLine(outline, Offset(57f, 134f), Offset(57f, 138f), 1.6f, StrokeCap.Round)
          drawLine(outline, Offset(63f, 134f), Offset(63f, 139f), 1.6f, StrokeCap.Round)
          drawLine(outline, Offset(97f, 134f), Offset(97f, 139f), 1.6f, StrokeCap.Round)
          drawLine(outline, Offset(103f, 134f), Offset(103f, 138f), 1.6f, StrokeCap.Round)
          translate(top = -wave * 0.8f) {
            drawPath(paths.ears, fur)
            drawPath(paths.ears, outline, style = stroke)
            drawPath(paths.innerEars, colors.accentSubtle)
            drawPath(paths.head, fur)
            drawPath(paths.head, outline, style = stroke)
            drawPath(paths.muzzle, lightFur)
            drawOval(colors.accentSubtle, Offset(45f, 78f), Size(14f, 7f))
            drawOval(colors.accentSubtle, Offset(101f, 78f), Size(14f, 7f))
            scale(1f, 1f - blink * 0.93f, pivot = Offset(80f, 73f)) {
              drawPath(paths.eyes, colors.contentPrimary)
              drawPath(paths.eyeGlints, colors.surface)
            }
            drawPath(paths.nose, outline)
            drawPath(paths.mouth, outline, style = stroke)
            drawPath(paths.whiskers, outline.copy(alpha = 0.7f), style = stroke)
          }
        }
      }
    }
  }
}

private class LynxPaths {
  val tail = Path().apply {
    moveTo(103f, 120f)
    cubicTo(123f, 126f, 138f, 112f, 125f, 95f)
    cubicTo(118f, 86f, 116f, 78f, 122f, 76f)
    lineTo(125f, 81f)
    quadraticTo(124f, 73f, 129f, 72f)
    cubicTo(128f, 79f, 140f, 81f, 142f, 90f)
    lineTo(140f, 89f)
    cubicTo(149f, 103f, 146f, 117f, 137f, 127f)
    lineTo(140f, 126f)
    quadraticTo(135f, 136f, 126f, 136f)
    lineTo(128f, 139f)
    cubicTo(120f, 141f, 111f, 139f, 105f, 133f)
    close()
  }
  val body = Path().apply {
    moveTo(58f, 94f)
    cubicTo(51f, 98f, 48f, 103f, 45f, 109f)
    lineTo(49f, 107f)
    quadraticTo(42f, 117f, 42f, 123f)
    lineTo(46f, 120f)
    cubicTo(42f, 133f, 49f, 138f, 61f, 141f)
    cubicTo(79f, 146f, 107f, 143f, 113f, 134f)
    quadraticTo(117f, 128f, 113f, 119f)
    lineTo(117f, 122f)
    quadraticTo(116f, 110f, 109f, 103f)
    lineTo(113f, 104f)
    quadraticTo(109f, 96f, 102f, 94f)
    close()
  }
  val paws = Path().apply {
    addOval(Rect(47f, 126f, 73f, 142f))
    addOval(Rect(87f, 126f, 113f, 142f))
  }
  val ears = Path().apply {
    moveTo(42f, 63f)
    quadraticTo(36f, 45f, 35f, 24f)
    lineTo(41f, 29f)
    quadraticTo(38f, 16f, 43f, 20f)
    quadraticTo(49f, 29f, 52f, 31f)
    quadraticTo(60f, 34f, 68f, 47f)
    close()
    moveTo(92f, 47f)
    quadraticTo(100f, 34f, 108f, 31f)
    quadraticTo(111f, 29f, 117f, 20f)
    quadraticTo(122f, 16f, 119f, 29f)
    lineTo(125f, 24f)
    quadraticTo(124f, 45f, 118f, 63f)
    close()
  }
  val innerEars = Path().apply {
    moveTo(43f, 35f)
    quadraticTo(44f, 48f, 48f, 57f)
    lineTo(60f, 48f)
    close()
    moveTo(117f, 35f)
    quadraticTo(116f, 48f, 112f, 57f)
    lineTo(100f, 48f)
    close()
  }
  val head = Path().apply {
    moveTo(60f, 44f)
    quadraticTo(70f, 42f, 74f, 38f)
    quadraticTo(74f, 42f, 71f, 44f)
    quadraticTo(80f, 43f, 84f, 36f)
    quadraticTo(87f, 39f, 86f, 43f)
    cubicTo(106f, 41f, 120f, 51f, 121f, 67f)
    quadraticTo(122f, 72f, 128f, 75f)
    lineTo(124f, 76f)
    quadraticTo(127f, 84f, 134f, 87f)
    quadraticTo(128f, 90f, 123f, 87f)
    quadraticTo(124f, 95f, 129f, 99f)
    quadraticTo(120f, 100f, 117f, 96f)
    cubicTo(111f, 105f, 99f, 109f, 86f, 109f)
    lineTo(81f, 113f)
    lineTo(79f, 109f)
    cubicTo(63f, 110f, 51f, 105f, 45f, 99f)
    quadraticTo(40f, 103f, 34f, 100f)
    quadraticTo(40f, 96f, 39f, 91f)
    quadraticTo(32f, 93f, 28f, 89f)
    quadraticTo(35f, 85f, 36f, 79f)
    lineTo(31f, 78f)
    quadraticTo(38f, 72f, 38f, 65f)
    cubicTo(40f, 53f, 49f, 46f, 60f, 44f)
    close()
  }
  val eyes = Path().apply {
    addOval(Rect(54f, 66f, 67f, 82f))
    addOval(Rect(93f, 66f, 106f, 82f))
  }
  val nose = Path().apply {
    moveTo(76f, 81f)
    quadraticTo(80f, 79f, 84f, 81f)
    quadraticTo(84f, 83f, 80f, 86f)
    quadraticTo(76f, 83f, 76f, 81f)
    close()
  }
  val mouth = Path().apply {
    moveTo(80f, 86f)
    cubicTo(78f, 96f, 70f, 95f, 68f, 89f)
    moveTo(80f, 86f)
    cubicTo(82f, 96f, 90f, 95f, 92f, 89f)
  }
  val whiskers = Path().apply {
    moveTo(48f, 85f)
    lineTo(29f, 82f)
    moveTo(49f, 90f)
    lineTo(32f, 94f)
    moveTo(112f, 85f)
    lineTo(131f, 82f)
    moveTo(111f, 90f)
    lineTo(128f, 94f)
  }
  val muzzle = Path().apply {
    moveTo(80f, 80f)
    cubicTo(66f, 73f, 57f, 83f, 62f, 92f)
    quadraticTo(70f, 104f, 80f, 100f)
    quadraticTo(90f, 104f, 98f, 92f)
    cubicTo(103f, 83f, 94f, 73f, 80f, 80f)
    close()
  }
  val bib = Path().apply {
    moveTo(59f, 103f)
    quadraticTo(81f, 110f, 102f, 103f)
    cubicTo(100f, 112f, 95f, 116f, 97f, 121f)
    lineTo(91f, 118f)
    quadraticTo(92f, 127f, 83f, 134f)
    lineTo(81f, 128f)
    lineTo(76f, 132f)
    quadraticTo(70f, 125f, 71f, 119f)
    lineTo(65f, 122f)
    quadraticTo(65f, 112f, 59f, 103f)
    close()
  }
  val eyeGlints = Path().apply {
    addOval(Rect(56f, 67f, 61f, 72f))
    addOval(Rect(95f, 67f, 100f, 72f))
  }
}
