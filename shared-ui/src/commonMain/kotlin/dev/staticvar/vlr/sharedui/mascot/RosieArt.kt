/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// Generated from rosie.svg by art/mascot/svg2compose.py; edit the SVG in art/mascot, not this file.
internal object RosieArt : MascotArt {
  override val tailPivot = Offset(108f, 128f)
  override val eyesPivot = Offset(80f, 62f)
  override val pawsBounds = Rect(53f, 128.5f, 107f, 143.5f)
  private val shadow0 = Path().apply {
    addOval(Rect(34f, 139.5f, 126f, 148.5f))
  }
  private val tail0 = Path().apply {
    moveTo(108f, 134f)
    cubicTo(124f, 138f, 142f, 132f, 146f, 118f)
    cubicTo(150f, 106f, 142f, 96f, 132f, 97f)
    cubicTo(124f, 98f, 120f, 106f, 125f, 112f)
    cubicTo(128f, 116f, 134f, 114f, 134f, 110f)
    cubicTo(136f, 118f, 124f, 124f, 108f, 120f)
    close()
  }
  private val tail1 = Path().apply {
    moveTo(136f, 101f)
    cubicTo(142f, 103f, 144f, 110f, 141f, 117f)
    cubicTo(140f, 111f, 137f, 107f, 132f, 105f)
    cubicTo(132f, 102f, 134f, 101f, 136f, 101f)
    close()
  }
  private val body0 = Path().apply {
    moveTo(58f, 90f)
    cubicTo(50f, 98f, 47f, 106f, 47f, 114f)
    cubicTo(38f, 118f, 35f, 132f, 42f, 139f)
    cubicTo(47f, 144f, 113f, 144f, 118f, 139f)
    cubicTo(125f, 132f, 122f, 118f, 113f, 114f)
    cubicTo(113f, 106f, 110f, 98f, 102f, 90f)
    close()
  }
  private val body1 = Path().apply {
    moveTo(62f, 96f)
    cubicTo(70f, 100f, 90f, 100f, 98f, 96f)
    cubicTo(100f, 108f, 95f, 118f, 88f, 122f)
    cubicTo(86f, 126f, 82f, 126f, 80f, 123f)
    cubicTo(78f, 126f, 74f, 126f, 72f, 122f)
    cubicTo(65f, 118f, 60f, 108f, 62f, 96f)
    close()
  }
  private val paws0 = Path().apply {
    addOval(Rect(53f, 128.5f, 79f, 143.5f))
  }
  private val paws1 = Path().apply {
    addOval(Rect(81f, 128.5f, 107f, 143.5f))
  }
  private val paws2 = Path().apply {
    moveTo(63f, 137f)
    lineTo(63f, 141f)
    moveTo(69f, 137f)
    lineTo(69f, 141f)
    moveTo(91f, 137f)
    lineTo(91f, 141f)
    moveTo(97f, 137f)
    lineTo(97f, 141f)
  }
  private val head0 = Path().apply {
    moveTo(51f, 28f)
    cubicTo(42f, 21f, 33f, 31f, 32f, 45f)
    cubicTo(30f, 58f, 29f, 77f, 33f, 87f)
    cubicTo(37f, 98f, 47f, 96f, 52f, 87f)
    lineTo(61f, 42f)
    close()
    moveTo(109f, 27f)
    cubicTo(119f, 20f, 128f, 29f, 128.5f, 43f)
    cubicTo(130f, 57f, 131f, 75f, 127f, 85f)
    cubicTo(122f, 95f, 114f, 94f, 108f, 84f)
    lineTo(99f, 41f)
    close()
  }
  private val head1 = Path().apply {
    moveTo(80f, 22f)
    cubicTo(54f, 22f, 40f, 37f, 39f, 59f)
    cubicTo(38f, 82f, 53f, 99f, 80f, 99f)
    cubicTo(107f, 99f, 122f, 82f, 121f, 59f)
    cubicTo(120f, 37f, 106f, 22f, 80f, 22f)
    close()
  }
  private val head2 = Path().apply {
    moveTo(74f, 27f)
    cubicTo(73f, 33f, 75f, 38f, 80f, 41f)
    cubicTo(85f, 38f, 87f, 33f, 86f, 27f)
    close()
  }
  private val head3 = Path().apply {
    addOval(Rect(53f, 63f, 107f, 97f))
  }
  private val head4 = Path().apply {
    addOval(Rect(45.2f, 78f, 54.8f, 84f))
  }
  private val head5 = Path().apply {
    addOval(Rect(105.2f, 78f, 114.8f, 84f))
  }
  private val eyes0 = Path().apply {
    addOval(Rect(53.5f, 52f, 68.5f, 72f))
  }
  private val eyes1 = Path().apply {
    addOval(Rect(91.5f, 52f, 106.5f, 72f))
  }
  private val eyes2 = Path().apply {
    addOval(Rect(56.3f, 55.8f, 61.7f, 61.2f))
  }
  private val eyes3 = Path().apply {
    addOval(Rect(94.3f, 55.8f, 99.7f, 61.2f))
  }
  private val face0 = Path().apply {
    moveTo(65f, 78f)
    quadraticTo(80f, 84f, 95f, 78f)
    cubicTo(94f, 88f, 87f, 94f, 80f, 94f)
    cubicTo(73f, 94f, 66f, 88f, 65f, 78f)
    close()
  }
  private val face1 = Path().apply {
    moveTo(72f, 87f)
    quadraticTo(80f, 83f, 88f, 87f)
    cubicTo(88f, 96f, 72f, 96f, 72f, 87f)
    close()
  }
  private val face2 = Path().apply {
    moveTo(61f, 78f)
    cubicTo(64f, 72f, 74f, 72f, 80f, 76f)
    cubicTo(86f, 72f, 96f, 72f, 99f, 78f)
    cubicTo(101f, 82f, 93f, 85f, 87f, 83f)
    quadraticTo(82f, 82f, 80f, 78f)
    quadraticTo(78f, 82f, 73f, 83f)
    cubicTo(67f, 85f, 59f, 82f, 61f, 78f)
    close()
  }
  private val face3 = Path().apply {
    moveTo(70f, 70f)
    quadraticTo(80f, 66f, 90f, 70f)
    cubicTo(94f, 76f, 85f, 82f, 80f, 82f)
    cubicTo(75f, 82f, 66f, 76f, 70f, 70f)
    close()
  }

  override fun DrawScope.drawShadow(palette: MascotPalette) {
    drawPath(shadow0, palette.shadow)
  }

  override fun DrawScope.drawTail(palette: MascotPalette) {
    drawPath(tail0, palette.fur)
    drawPath(tail0, palette.outline, style = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(tail1, palette.furLight)
  }

  override fun DrawScope.drawBody(palette: MascotPalette) {
    drawPath(body0, palette.fur)
    drawPath(body0, palette.outline, style = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(body1, palette.furLight)
    drawPaws(palette)
  }

  override fun DrawScope.drawPaws(palette: MascotPalette) {
    drawPath(paws0, palette.fur)
    drawPath(paws0, palette.outline, style = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(paws1, palette.fur)
    drawPath(paws1, palette.outline, style = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(
      paws2,
      palette.outline.copy(alpha = palette.outline.alpha * 0.5f),
      style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }

  override fun DrawScope.drawHead(palette: MascotPalette) {
    drawPath(head0, palette.furDark)
    drawPath(head0, palette.outline, style = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(head1, palette.fur)
    drawPath(head1, palette.outline, style = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(head2, palette.furLight)
    drawPath(head3, palette.furLight)
    drawPath(head4, palette.pink.copy(alpha = palette.pink.alpha * 0.75f))
    drawPath(head5, palette.pink.copy(alpha = palette.pink.alpha * 0.75f))
  }

  override fun DrawScope.drawEyes(palette: MascotPalette) {
    drawPath(eyes0, palette.ink)
    drawPath(eyes1, palette.ink)
    drawPath(eyes2, palette.white)
    drawPath(eyes3, palette.white)
  }

  override fun DrawScope.drawFace(palette: MascotPalette) {
    drawPath(face0, palette.ink)
    drawPath(face1, palette.pink)
    drawPath(face2, palette.furLight)
    drawPath(face3, palette.ink)
  }
}
