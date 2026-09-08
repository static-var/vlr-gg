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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
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
public fun RosieMascot(modifier: Modifier = Modifier, animated: Boolean = true): Unit {
  val colors = Prism.color
  val paths = remember { RosiePaths() }
  val phase =
    if (animated) {
      rememberInfiniteTransition(label = "Rosie idle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
        label = "Rosie motion",
      )
    } else {
      null
    }
  val fur = lerp(colors.accent, colors.surface, 0.28f)
  val outline = lerp(colors.accent, colors.contentPrimary, 0.55f)
  val lightFur = lerp(colors.surface, colors.onAccent, 0.2f)
  val expressionInk = Color(0xFF302936)
  val stroke = remember { Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round) }

  Canvas(modifier.size(160.dp).semantics { contentDescription = "Rosie dog mascot" }) {
    val progress = phase?.value ?: 0f
    val wave = sin(progress * 2f * PI.toFloat())
    val wag = sin(progress * 8f * PI.toFloat())
    val blink = if (animated) (1f - abs(progress - 0.73f) / 0.026f).coerceIn(0f, 1f) else 0f
    val factor = size.minDimension / 160f
    translate((size.width - 160f * factor) / 2f, (size.height - 160f * factor) / 2f) {
      scale(factor, factor, pivot = Offset.Zero) {
        drawOval(colors.contentPrimary.copy(alpha = 0.08f), Offset(33f, 139f), Size(96f, 10f))
        rotate(wag * 7f, pivot = Offset(109f, 122f)) {
          drawPath(paths.tail, fur)
          drawPath(paths.tail, outline, style = stroke)
          drawPath(paths.tailPlume, lightFur)
          drawPath(paths.tailCurl, outline.copy(alpha = 0.45f), style = stroke)
        }
        scale(1f + wave * 0.009f, 1f + wave * 0.014f, pivot = Offset(78f, 141f)) {
          drawPath(paths.body, fur)
          drawPath(paths.body, outline, style = stroke)
          drawPath(paths.bib, lightFur)
          drawRosiePaws(paths.paws, fur, outline, stroke)
          translate(top = -wave * 0.8f) {
            drawPath(paths.ears, fur)
            drawPath(paths.ears, outline, style = stroke)
            drawPath(paths.innerEars, colors.accentSubtle)
            drawPath(paths.head, fur)
            drawPath(paths.head, outline, style = stroke)
            drawPath(paths.face, fur)
            drawPath(paths.brows, expressionInk.copy(alpha = 0.25f), style = stroke)
            scale(1f, 1f - blink * 0.93f, pivot = Offset(77f, 69f)) {
              drawPath(paths.eyes, expressionInk)
              drawPath(paths.eyeGlints, Color.White)
            }
            drawOval(colors.accentSubtle, Offset(43f, 77f), Size(13f, 6f))
            drawOval(colors.accentSubtle, Offset(100f, 77f), Size(13f, 6f))
            drawPath(paths.mouth, expressionInk)
            drawPath(paths.tongue, Color(0xFFE9A3AD))
            drawPath(paths.tongue, expressionInk, style = stroke)
            drawLine(outline.copy(alpha = 0.5f), Offset(77f, 93f), Offset(77f, 97f), 1.4f, StrokeCap.Round)
            drawPath(paths.muzzle, fur)
            drawPath(paths.smile, expressionInk, style = stroke)
            drawPath(paths.nose, expressionInk)
            drawOval(Color.White.copy(alpha = 0.65f), Offset(72f, 75f), Size(6f, 2.5f))
          }
        }
      }
    }
  }
}

private class RosiePaths {
  val tail = Path().apply {
    moveTo(103f, 126f)
    cubicTo(130f, 129f, 146f, 111f, 147f, 94f)
    lineTo(151f, 97f)
    quadraticTo(152f, 87f, 146f, 82f)
    lineTo(150f, 81f)
    quadraticTo(147f, 72f, 139f, 71f)
    lineTo(139f, 66f)
    quadraticTo(131f, 66f, 127f, 70f)
    lineTo(126f, 66f)
    cubicTo(110f, 69f, 104f, 82f, 110f, 92f)
    lineTo(108f, 96f)
    quadraticTo(115f, 95f, 119f, 90f)
    cubicTo(126f, 87f, 130f, 93f, 126f, 101f)
    quadraticTo(121f, 110f, 103f, 111f)
    close()
  }
  val tailPlume = Path().apply {
    moveTo(127f, 74f)
    cubicTo(141f, 68f, 149f, 89f, 139f, 106f)
    lineTo(137f, 101f)
    lineTo(132f, 111f)
    cubicTo(137f, 95f, 133f, 83f, 119f, 85f)
    quadraticTo(119f, 78f, 127f, 74f)
    close()
  }
  val tailCurl = Path().apply {
    moveTo(117f, 86f)
    cubicTo(127f, 80f, 138f, 92f, 129f, 105f)
  }
  val body = Path().apply {
    moveTo(50f, 93f)
    quadraticTo(38f, 103f, 36f, 116f)
    lineTo(40f, 113f)
    quadraticTo(36f, 125f, 39f, 131f)
    lineTo(43f, 127f)
    cubicTo(43f, 143f, 58f, 142f, 77f, 142f)
    cubicTo(103f, 145f, 114f, 135f, 112f, 124f)
    lineTo(116f, 127f)
    quadraticTo(117f, 116f, 111f, 109f)
    lineTo(115f, 110f)
    quadraticTo(111f, 98f, 101f, 94f)
    close()
  }
  val bib = Path().apply {
    moveTo(49f, 98f)
    quadraticTo(77f, 106f, 103f, 98f)
    quadraticTo(103f, 110f, 95f, 117f)
    lineTo(94f, 111f)
    quadraticTo(91f, 125f, 82f, 130f)
    lineTo(82f, 125f)
    lineTo(75f, 134f)
    quadraticTo(68f, 129f, 65f, 120f)
    lineTo(60f, 123f)
    quadraticTo(61f, 113f, 56f, 112f)
    lineTo(54f, 115f)
    quadraticTo(48f, 107f, 49f, 98f)
    close()
  }
  val paws = rosiePawPath()
  val ears = Path().apply {
    moveTo(39f, 58f)
    cubicTo(33f, 45f, 36f, 26f, 41f, 17f)
    lineTo(45f, 20f)
    lineTo(47f, 16f)
    cubicTo(55f, 22f, 62f, 37f, 64f, 45f)
    close()
    moveTo(90f, 45f)
    cubicTo(97f, 32f, 104f, 21f, 113f, 18f)
    lineTo(115f, 23f)
    lineTo(118f, 21f)
    cubicTo(123f, 32f, 123f, 47f, 118f, 58f)
    close()
  }
  val innerEars = Path().apply {
    moveTo(44f, 27f)
    quadraticTo(40f, 39f, 44f, 49f)
    lineTo(56f, 44f)
    quadraticTo(50f, 31f, 44f, 27f)
    close()
    moveTo(112f, 29f)
    quadraticTo(104f, 35f, 100f, 44f)
    lineTo(113f, 50f)
    quadraticTo(117f, 40f, 112f, 29f)
    close()
  }
  val head = Path().apply {
    moveTo(53f, 42f)
    quadraticTo(64f, 40f, 70f, 36f)
    lineTo(68f, 41f)
    quadraticTo(80f, 39f, 85f, 34f)
    quadraticTo(88f, 37f, 86f, 40f)
    cubicTo(104f, 39f, 117f, 49f, 120f, 62f)
    lineTo(125f, 66f)
    lineTo(121f, 67f)
    quadraticTo(125f, 77f, 131f, 81f)
    quadraticTo(127f, 84f, 122f, 81f)
    quadraticTo(122f, 92f, 126f, 96f)
    quadraticTo(121f, 97f, 117f, 94f)
    cubicTo(111f, 105f, 95f, 111f, 83f, 110f)
    lineTo(78f, 115f)
    lineTo(75f, 111f)
    quadraticTo(65f, 111f, 56f, 106f)
    lineTo(53f, 109f)
    quadraticTo(46f, 105f, 42f, 97f)
    lineTo(36f, 100f)
    quadraticTo(34f, 93f, 36f, 88f)
    lineTo(29f, 88f)
    quadraticTo(34f, 81f, 33f, 74f)
    lineTo(29f, 73f)
    quadraticTo(36f, 65f, 37f, 57f)
    quadraticTo(42f, 46f, 53f, 42f)
    close()
  }
  val face = Path().apply {
    moveTo(78f, 48f)
    cubicTo(67f, 44f, 43f, 54f, 43f, 72f)
    quadraticTo(43f, 88f, 53f, 92f)
    lineTo(50f, 94f)
    quadraticTo(59f, 101f, 65f, 101f)
    lineTo(64f, 105f)
    quadraticTo(78f, 111f, 89f, 104f)
    lineTo(90f, 108f)
    quadraticTo(102f, 104f, 108f, 96f)
    lineTo(104f, 97f)
    cubicTo(121f, 77f, 107f, 47f, 90f, 47f)
    quadraticTo(83f, 46f, 78f, 48f)
    close()
  }
  val brows = Path().apply {
    moveTo(52f, 58f)
    quadraticTo(56f, 54f, 61f, 57f)
    moveTo(94f, 57f)
    quadraticTo(99f, 54f, 103f, 58f)
  }
  val eyes = Path().apply {
    addOval(Rect(53f, 65f, 63f, 77f))
    addOval(Rect(93f, 65f, 103f, 77f))
  }
  val eyeGlints = Path().apply {
    addOval(Rect(55f, 66f, 58f, 69f))
    addOval(Rect(95f, 66f, 98f, 69f))
  }
  val mouth = Path().apply {
    moveTo(61f, 84f)
    quadraticTo(78f, 91f, 95f, 83f)
    cubicTo(92f, 95f, 85f, 101f, 78f, 102f)
    cubicTo(70f, 102f, 64f, 94f, 61f, 84f)
    close()
  }
  val tongue = Path().apply {
    moveTo(69f, 93f)
    quadraticTo(77f, 90f, 86f, 93f)
    cubicTo(86f, 105f, 70f, 106f, 69f, 93f)
    close()
  }
  val muzzle = Path().apply {
    moveTo(77f, 78f)
    cubicTo(69f, 71f, 58f, 77f, 59f, 84f)
    quadraticTo(60f, 91f, 70f, 89f)
    quadraticTo(75f, 89f, 77f, 84f)
    quadraticTo(79f, 89f, 85f, 89f)
    cubicTo(96f, 89f, 99f, 80f, 92f, 77f)
    quadraticTo(85f, 73f, 77f, 78f)
    close()
  }
  val smile = Path().apply {
    moveTo(59f, 84f)
    cubicTo(62f, 92f, 74f, 91f, 77f, 84f)
    cubicTo(81f, 91f, 92f, 91f, 96f, 83f)
  }
  val nose = Path().apply {
    moveTo(70f, 74f)
    quadraticTo(77f, 72f, 85f, 74f)
    cubicTo(89f, 77f, 83f, 83f, 77f, 84f)
    cubicTo(71f, 83f, 66f, 77f, 70f, 74f)
    close()
  }
}

@Composable
internal fun RosiePaws(modifier: Modifier = Modifier) {
  val paths = remember { rosiePawPath() }
  val colors = Prism.color
  val fur = lerp(colors.accent, colors.surface, 0.28f)
  val outline = lerp(colors.accent, colors.contentPrimary, 0.55f)
  val stroke = remember { Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round) }
  Canvas(modifier) {
    val factor = minOf(size.width / 66f, size.height / 34f)
    scale(factor, factor, pivot = Offset.Zero) {
      translate(left = -44f, top = -114f) {
        drawRosiePaws(paths, fur, outline, stroke)
      }
    }
  }
}

private fun DrawScope.drawRosiePaws(paws: Path, fur: Color, outline: Color, stroke: Stroke) {
  drawPath(paws, fur)
  drawPath(paws, outline, style = stroke)
  drawLine(outline, Offset(55f, 135f), Offset(55f, 139f), 1.5f, StrokeCap.Round)
  drawLine(outline, Offset(61f, 136f), Offset(61f, 140f), 1.5f, StrokeCap.Round)
  drawLine(outline, Offset(92f, 136f), Offset(92f, 140f), 1.5f, StrokeCap.Round)
  drawLine(outline, Offset(98f, 135f), Offset(98f, 139f), 1.5f, StrokeCap.Round)
}

private fun rosiePawPath(): Path = Path().apply {
  moveTo(51f, 116f)
  cubicTo(47f, 123f, 44f, 134f, 48f, 140f)
  cubicTo(52f, 146f, 68f, 146f, 70f, 140f)
  quadraticTo(73f, 131f, 68f, 122f)
  moveTo(85f, 122f)
  quadraticTo(81f, 133f, 84f, 140f)
  cubicTo(87f, 147f, 103f, 144f, 106f, 140f)
  quadraticTo(109f, 131f, 103f, 117f)
}
