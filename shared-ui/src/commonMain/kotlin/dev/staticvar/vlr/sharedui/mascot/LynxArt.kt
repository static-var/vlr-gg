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

// Generated from lynx.svg by art/mascot/svg2compose.py; edit the SVG in art/mascot, not this file.
internal object LynxArt : MascotArt {
  override val tailPivot = Offset(108f, 125f)
  override val eyesPivot = Offset(80f, 65f)
  override val pawsBounds = Rect(52f, 121f, 110f, 143f)
  private val shadow0 = Path().apply {
    addOval(Rect(43f, 140f, 119f, 148f))
  }
  private val tail0 = Path().apply {
    moveTo(105f, 119f)
    cubicTo(112f, 120f, 114f, 111f, 124f, 109f)
    cubicTo(136f, 106f, 140f, 121f, 131f, 128f)
    cubicTo(123f, 135f, 113f, 133f, 106f, 130f)
    close()
  }
  private val tail1 = Path().apply {
    moveTo(126f, 109f)
    cubicTo(136f, 109f, 139f, 121f, 131f, 128f)
    lineTo(123f, 131f)
    quadraticTo(129f, 121f, 122f, 111f)
    close()
  }
  private val tail2 = Path().apply {
    moveTo(105f, 119f)
    cubicTo(112f, 120f, 114f, 111f, 124f, 109f)
    cubicTo(136f, 106f, 140f, 121f, 131f, 128f)
    cubicTo(123f, 135f, 113f, 133f, 106f, 130f)
    close()
  }
  private val body0 = Path().apply {
    moveTo(58f, 91f)
    cubicTo(50f, 103f, 46f, 118f, 48f, 131f)
    cubicTo(50f, 145f, 109f, 146f, 113f, 132f)
    cubicTo(116f, 118f, 110f, 102f, 102f, 91f)
    close()
  }
  private val body1 = Path().apply {
    addOval(Rect(61f, 96f, 99f, 138f))
  }
  private val paws0 = Path().apply {
    moveTo(52f, 133f)
    cubicTo(52f, 121f, 78f, 121f, 78f, 133f)
    lineTo(78f, 137f)
    quadraticTo(78f, 143f, 65f, 143f)
    quadraticTo(52f, 143f, 52f, 137f)
    close()
    moveTo(84f, 133f)
    cubicTo(84f, 121f, 110f, 121f, 110f, 133f)
    lineTo(110f, 137f)
    quadraticTo(110f, 143f, 97f, 143f)
    quadraticTo(84f, 143f, 84f, 137f)
    close()
  }
  private val paws1 = Path().apply {
    moveTo(61f, 135f)
    lineTo(61f, 139f)
    moveTo(69f, 135f)
    lineTo(69f, 139f)
    moveTo(93f, 135f)
    lineTo(93f, 139f)
    moveTo(101f, 135f)
    lineTo(101f, 139f)
  }
  private val head0 = Path().apply {
    moveTo(39f, 56f)
    lineTo(36f, 32f)
    lineTo(33f, 13f)
    quadraticTo(35f, 12f, 37f, 15f)
    lineTo(43f, 26f)
    lineTo(48f, 30f)
    lineTo(67f, 42f)
    close()
    moveTo(93f, 42f)
    lineTo(112f, 30f)
    lineTo(117f, 26f)
    lineTo(123f, 15f)
    quadraticTo(125f, 12f, 127f, 13f)
    lineTo(124f, 32f)
    lineTo(121f, 56f)
    close()
  }
  private val head1 = Path().apply {
    moveTo(33f, 13f)
    quadraticTo(35f, 12f, 37f, 15f)
    lineTo(43f, 26f)
    lineTo(48f, 30f)
    lineTo(43f, 31f)
    lineTo(40f, 35f)
    lineTo(36f, 32f)
    close()
    moveTo(127f, 13f)
    quadraticTo(125f, 12f, 123f, 15f)
    lineTo(117f, 26f)
    lineTo(112f, 30f)
    lineTo(117f, 31f)
    lineTo(120f, 35f)
    lineTo(124f, 32f)
    close()
  }
  private val head2 = Path().apply {
    moveTo(43f, 34f)
    lineTo(57f, 44f)
    lineTo(44f, 50f)
    close()
    moveTo(103f, 44f)
    lineTo(117f, 34f)
    lineTo(116f, 50f)
    close()
  }
  private val head3 = Path().apply {
    moveTo(39f, 56f)
    lineTo(36f, 32f)
    lineTo(33f, 13f)
    quadraticTo(35f, 12f, 37f, 15f)
    lineTo(43f, 26f)
    lineTo(48f, 30f)
    lineTo(67f, 42f)
    close()
    moveTo(93f, 42f)
    lineTo(112f, 30f)
    lineTo(117f, 26f)
    lineTo(123f, 15f)
    quadraticTo(125f, 12f, 127f, 13f)
    lineTo(124f, 32f)
    lineTo(121f, 56f)
    close()
  }
  private val head4 = Path().apply {
    moveTo(80f, 35f)
    cubicTo(103f, 35f, 122f, 47f, 124f, 67f)
    quadraticTo(125f, 76f, 130f, 82f)
    quadraticTo(126f, 86f, 119f, 86f)
    cubicTo(111f, 100f, 95f, 105f, 80f, 105f)
    cubicTo(65f, 105f, 49f, 100f, 41f, 86f)
    quadraticTo(34f, 86f, 30f, 82f)
    quadraticTo(35f, 76f, 36f, 67f)
    cubicTo(38f, 47f, 57f, 35f, 80f, 35f)
    close()
  }
  private val head5 = Path().apply {
    moveTo(66f, 38f)
    quadraticTo(70f, 36f, 74f, 36f)
    lineTo(73f, 47f)
    quadraticTo(70f, 51f, 67f, 47f)
    close()
    moveTo(86f, 36f)
    quadraticTo(90f, 36f, 94f, 38f)
    lineTo(93f, 47f)
    quadraticTo(90f, 51f, 87f, 47f)
    close()
  }
  private val head6 = Path().apply {
    moveTo(80f, 75f)
    cubicTo(67f, 68f, 56f, 77f, 60f, 87f)
    cubicTo(63f, 97f, 75f, 97f, 80f, 94f)
    cubicTo(85f, 97f, 97f, 97f, 100f, 87f)
    cubicTo(104f, 77f, 93f, 68f, 80f, 75f)
    close()
  }
  private val head7 = Path().apply {
    addOval(Rect(41f, 75f, 53f, 81f))
  }
  private val head8 = Path().apply {
    addOval(Rect(107f, 75f, 119f, 81f))
  }
  private val head9 = Path().apply {
    moveTo(80f, 35f)
    cubicTo(103f, 35f, 122f, 47f, 124f, 67f)
    quadraticTo(125f, 76f, 130f, 82f)
    quadraticTo(126f, 86f, 119f, 86f)
    cubicTo(111f, 100f, 95f, 105f, 80f, 105f)
    cubicTo(65f, 105f, 49f, 100f, 41f, 86f)
    quadraticTo(34f, 86f, 30f, 82f)
    quadraticTo(35f, 76f, 36f, 67f)
    cubicTo(38f, 47f, 57f, 35f, 80f, 35f)
    close()
  }
  private val eyes0 = Path().apply {
    addOval(Rect(51f, 55.5f, 67f, 74.5f))
  }
  private val eyes1 = Path().apply {
    addOval(Rect(93f, 55.5f, 109f, 74.5f))
  }
  private val eyes2 = Path().apply {
    addOval(Rect(53.7f, 59.2f, 59.3f, 64.8f))
  }
  private val eyes3 = Path().apply {
    addOval(Rect(95.7f, 59.2f, 101.3f, 64.8f))
  }
  private val face0 = Path().apply {
    moveTo(75f, 76f)
    quadraticTo(80f, 73f, 85f, 76f)
    quadraticTo(84f, 80f, 80f, 82f)
    quadraticTo(76f, 80f, 75f, 76f)
    close()
  }
  private val face1 = Path().apply {
    moveTo(80f, 81f)
    lineTo(80f, 84f)
    cubicTo(78f, 91f, 70f, 89f, 69f, 84f)
    moveTo(80f, 84f)
    cubicTo(82f, 91f, 90f, 89f, 91f, 84f)
  }
  private val stroke3_5 = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  private val stroke2_2 = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  private val stroke2_8 = Stroke(2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

  override fun DrawScope.drawShadow(palette: MascotPalette) {
    drawPath(shadow0, palette.shadow)
  }

  override fun DrawScope.drawTail(palette: MascotPalette) {
    drawPath(tail0, palette.fur)
    drawPath(tail1, palette.furDark)
    drawPath(tail2, palette.outline, style = stroke3_5)
  }

  override fun DrawScope.drawBody(palette: MascotPalette) {
    drawPath(body0, palette.fur)
    drawPath(body0, palette.outline, style = stroke3_5)
    drawPath(body1, palette.furLight)
    drawPaws(palette)
  }

  override fun DrawScope.drawPaws(palette: MascotPalette) {
    drawPath(paws0, palette.furLight)
    drawPath(paws0, palette.outline, style = stroke3_5)
    drawPath(paws1, palette.outline.copy(alpha = palette.outline.alpha * 0.5f), style = stroke2_2)
  }

  override fun DrawScope.drawHead(palette: MascotPalette) {
    drawPath(head0, palette.fur)
    drawPath(head1, palette.furDark)
    drawPath(head2, palette.pink)
    drawPath(head3, palette.outline, style = stroke3_5)
    drawPath(head4, palette.fur)
    drawPath(head5, palette.furDark)
    drawPath(head6, palette.furLight)
    drawPath(head7, palette.pink)
    drawPath(head8, palette.pink)
    drawPath(head9, palette.outline, style = stroke3_5)
  }

  override fun DrawScope.drawEyes(palette: MascotPalette) {
    drawPath(eyes0, palette.ink)
    drawPath(eyes1, palette.ink)
    drawPath(eyes2, palette.white)
    drawPath(eyes3, palette.white)
  }

  override fun DrawScope.drawFace(palette: MascotPalette) {
    drawPath(face0, palette.ink)
    drawPath(face1, palette.ink, style = stroke2_8)
  }
}
