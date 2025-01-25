package dev.staticvar.vlr.ui.common.illustration

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.ui.common.Illustration
import dev.staticvar.vlr.ui.theme.VLRTheme

public val Illustration.Loading: ImageVector
  @Composable get() {
    if (_loading != null) {
      return _loading!!
    }
    _loading = Builder(
      name = "Loading", defaultWidth = 500.0.dp, defaultHeight = 500.0.dp,
      viewportWidth = 500.0f, viewportHeight = 500.0f
    ).apply {
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(374.76f, 165.75f)
        curveToRelative(-8.45f, -10.11f, -17.34f, -20.0f, -25.44f, -30.41f)
        curveTo(325.6f, 104.8f, 294.4f, 75.39f, 255.0f, 67.17f)
        curveTo(215.11f, 58.85f, 167.63f, 73.0f, 143.56f, 107.0f)
        curveToRelative(-16.0f, 22.59f, -22.25f, 50.66f, -22.81f, 78.0f)
        curveToRelative(-0.25f, 12.48f, 1.89f, 25.09f, -0.12f, 37.5f)
        curveToRelative(-2.22f, 13.65f, -9.88f, 25.62f, -16.58f, 37.45f)
        curveToRelative(-8.86f, 15.65f, -12.68f, 32.88f, -7.3f, 50.52f)
        curveToRelative(12.17f, 39.85f, 67.12f, 58.37f, 104.87f, 51.7f)
        curveToRelative(20.58f, -3.64f, 39.44f, -13.61f, 59.38f, -19.9f)
        curveToRelative(16.44f, -5.19f, 33.36f, -6.7f, 50.07f, -1.74f)
        curveToRelative(38.08f, 11.3f, 77.08f, -8.18f, 93.21f, -44.0f)
        curveToRelative(19.47f, -43.24f, 1.25f, -92.34f, -26.64f, -127.23f)
        curveTo(376.69f, 168.08f, 375.74f, 166.91f, 374.76f, 165.75f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, fillAlpha = 0.7f, strokeAlpha
        = 0.7f, strokeLineWidth = 0.0f, strokeLineCap = Butt, strokeLineJoin = Miter,
        strokeLineMiter = 4.0f, pathFillType = NonZero
      ) {
        moveTo(374.76f, 165.75f)
        curveToRelative(-8.45f, -10.11f, -17.34f, -20.0f, -25.44f, -30.41f)
        curveTo(325.6f, 104.8f, 294.4f, 75.39f, 255.0f, 67.17f)
        curveTo(215.11f, 58.85f, 167.63f, 73.0f, 143.56f, 107.0f)
        curveToRelative(-16.0f, 22.59f, -22.25f, 50.66f, -22.81f, 78.0f)
        curveToRelative(-0.25f, 12.48f, 1.89f, 25.09f, -0.12f, 37.5f)
        curveToRelative(-2.22f, 13.65f, -9.88f, 25.62f, -16.58f, 37.45f)
        curveToRelative(-8.86f, 15.65f, -12.68f, 32.88f, -7.3f, 50.52f)
        curveToRelative(12.17f, 39.85f, 67.12f, 58.37f, 104.87f, 51.7f)
        curveToRelative(20.58f, -3.64f, 39.44f, -13.61f, 59.38f, -19.9f)
        curveToRelative(16.44f, -5.19f, 33.36f, -6.7f, 50.07f, -1.74f)
        curveToRelative(38.08f, 11.3f, 77.08f, -8.18f, 93.21f, -44.0f)
        curveToRelative(19.47f, -43.24f, 1.25f, -92.34f, -26.64f, -127.23f)
        curveTo(376.69f, 168.08f, 375.74f, 166.91f, 374.76f, 165.75f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(105.41f, 401.48f)
        arcToRelative(142.83f, 27.6f, 0.0f, true, false, 285.66f, 0.0f)
        arcToRelative(142.83f, 27.6f, 0.0f, true, false, -285.66f, 0.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, fillAlpha = 0.5f, strokeAlpha
        = 0.5f, strokeLineWidth = 0.0f, strokeLineCap = Butt, strokeLineJoin = Miter,
        strokeLineMiter = 4.0f, pathFillType = NonZero
      ) {
        moveTo(105.41f, 401.48f)
        arcToRelative(142.83f, 27.6f, 0.0f, true, false, 285.66f, 0.0f)
        arcToRelative(142.83f, 27.6f, 0.0f, true, false, -285.66f, 0.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(203.0f, 283.33f)
        lineTo(310.33f, 283.33f)
        arcTo(4.67f, 4.67f, 0.0f, false, true, 315.0f, 288.0f)
        lineTo(315.0f, 296.66f)
        arcTo(4.67f, 4.67f, 0.0f, false, true, 310.33f, 301.33f)
        lineTo(203.0f, 301.33f)
        arcTo(4.67f, 4.67f, 0.0f, false, true, 198.33f, 296.66f)
        lineTo(198.33f, 288.0f)
        arcTo(4.67f, 4.67f, 0.0f, false, true, 203.0f, 283.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(310.33f, 301.83f)
        horizontalLineTo(203.0f)
        arcToRelative(5.17f, 5.17f, 0.0f, false, true, -5.17f, -5.16f)
        verticalLineTo(288.0f)
        arcToRelative(5.18f, 5.18f, 0.0f, false, true, 5.17f, -5.17f)
        horizontalLineTo(310.33f)
        arcTo(5.18f, 5.18f, 0.0f, false, true, 315.5f, 288.0f)
        verticalLineToRelative(8.67f)
        arcTo(5.17f, 5.17f, 0.0f, false, true, 310.33f, 301.83f)
        close()
        moveTo(203.0f, 283.83f)
        arcToRelative(4.17f, 4.17f, 0.0f, false, false, -4.17f, 4.17f)
        verticalLineToRelative(8.67f)
        arcToRelative(4.17f, 4.17f, 0.0f, false, false, 4.17f, 4.16f)
        horizontalLineTo(310.33f)
        arcToRelative(4.17f, 4.17f, 0.0f, false, false, 4.17f, -4.16f)
        verticalLineTo(288.0f)
        arcToRelative(4.17f, 4.17f, 0.0f, false, false, -4.17f, -4.17f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(203.0f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(212.14f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(221.28f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(230.42f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(239.56f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(248.7f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(257.84f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(266.98f, 285.21f)
        horizontalLineToRelative(7.33f)
        verticalLineToRelative(14.79f)
        horizontalLineToRelative(-7.33f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(272.71f, 161.0f)
        lineToRelative(9.61f, -4.38f)
        lineToRelative(-2.0f, -11.54f)
        lineToRelative(-10.6f, -0.82f)
        arcToRelative(46.1f, 46.1f, 0.0f, false, false, -5.31f, -10.0f)
        lineToRelative(5.47f, -9.23f)
        lineToRelative(-8.29f, -8.29f)
        lineToRelative(-9.3f, 5.53f)
        arcTo(46.13f, 46.13f, 0.0f, false, false, 234.49f, 115.0f)
        lineToRelative(-2.68f, -10.63f)
        lineTo(220.09f, 104.37f)
        lineToRelative(-2.7f, 10.69f)
        arcToRelative(46.36f, 46.36f, 0.0f, false, false, -10.53f, 3.35f)
        lineToRelative(-8.38f, -7.25f)
        lineToRelative(-9.59f, 6.72f)
        lineToRelative(4.0f, 10.37f)
        arcToRelative(46.58f, 46.58f, 0.0f, false, false, -6.73f, 8.73f)
        lineToRelative(-11.0f, -1.14f)
        lineToRelative(-4.0f, 11.0f)
        lineToRelative(9.13f, 6.19f)
        arcToRelative(46.41f, 46.41f, 0.0f, false, false, -0.66f, 7.78f)
        curveToRelative(0.0f, 1.11f, 0.0f, 2.2f, 0.13f, 3.29f)
        lineToRelative(-9.61f, 5.36f)
        lineToRelative(3.0f, 11.32f)
        lineToRelative(10.88f, -0.14f)
        arcToRelative(46.9f, 46.9f, 0.0f, false, false, 6.0f, 9.5f)
        lineToRelative(-4.7f, 9.75f)
        lineToRelative(9.0f, 7.53f)
        lineToRelative(8.7f, -6.23f)
        arcToRelative(46.21f, 46.21f, 0.0f, false, false, 10.47f, 4.43f)
        lineToRelative(1.75f, 10.49f)
        lineToRelative(11.67f, 1.0f)
        lineToRelative(3.52f, -9.93f)
        arcToRelative(46.07f, 46.07f, 0.0f, false, false, 11.23f, -2.44f)
        lineToRelative(7.36f, 7.49f)
        lineToRelative(10.15f, -5.86f)
        lineToRelative(-2.79f, -10.09f)
        arcToRelative(46.62f, 46.62f, 0.0f, false, false, 7.8f, -8.5f)
        lineToRelative(10.31f, 1.9f)
        lineToRelative(5.0f, -10.61f)
        lineToRelative(-8.09f, -6.68f)
        arcTo(46.13f, 46.13f, 0.0f, false, false, 272.71f, 161.0f)
        close()
        moveTo(255.38f, 160.8f)
        arcToRelative(29.25f, 29.25f, 0.0f, true, true, -29.25f, -29.25f)
        arcTo(29.25f, 29.25f, 0.0f, false, true, 255.38f, 160.81f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(226.82f, 217.67f)
        horizontalLineToRelative(0.0f)
        lineToRelative(-11.67f, -1.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.49f, -0.45f)
        lineTo(212.92f, 206.0f)
        arcToRelative(47.08f, 47.08f, 0.0f, false, true, -10.0f, -4.2f)
        lineToRelative(-8.42f, 6.0f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.66f, 0.0f)
        lineToRelative(-9.0f, -7.53f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.14f, -0.65f)
        lineToRelative(4.54f, -9.45f)
        arcToRelative(47.49f, 47.49f, 0.0f, false, true, -5.67f, -9.0f)
        lineToRelative(-10.53f, 0.13f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, -0.53f, -0.4f)
        lineToRelative(-3.0f, -11.32f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.25f, -0.61f)
        lineToRelative(9.31f, -5.2f)
        curveToRelative(-0.07f, -1.12f, -0.1f, -2.07f, -0.1f, -3.0f)
        arcToRelative(47.0f, 47.0f, 0.0f, false, true, 0.61f, -7.53f)
        lineToRelative(-8.85f, -6.0f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.2f, -0.64f)
        lineToRelative(4.0f, -11.0f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.57f, -0.35f)
        lineToRelative(10.69f, 1.1f)
        arcToRelative(46.85f, 46.85f, 0.0f, false, true, 6.38f, -8.28f)
        lineToRelative(-3.83f, -10.05f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.2f, -0.63f)
        lineToRelative(9.59f, -6.72f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.67f, 0.0f)
        lineToRelative(8.11f, 7.0f)
        arcToRelative(47.09f, 47.09f, 0.0f, false, true, 10.0f, -3.17f)
        lineToRelative(2.61f, -10.36f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.53f, -0.41f)
        horizontalLineToRelative(11.72f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.52f, 0.41f)
        lineToRelative(2.6f, 10.29f)
        arcToRelative(46.62f, 46.62f, 0.0f, false, true, 17.35f, 7.08f)
        lineToRelative(9.0f, -5.35f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.66f, 0.08f)
        lineToRelative(8.28f, 8.28f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.08f, 0.66f)
        lineTo(265.0f, 134.2f)
        arcToRelative(47.26f, 47.26f, 0.0f, false, true, 5.05f, 9.56f)
        lineToRelative(10.26f, 0.79f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.49f, 0.45f)
        lineToRelative(2.0f, 11.54f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, -0.31f, 0.59f)
        lineToRelative(-9.3f, 4.23f)
        arcToRelative(47.0f, 47.0f, 0.0f, false, true, -1.38f, 10.85f)
        lineToRelative(7.82f, 6.46f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.15f, 0.64f)
        lineToRelative(-4.95f, 10.62f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.59f, 0.31f)
        lineToRelative(-10.0f, -1.84f)
        arcToRelative(48.27f, 48.27f, 0.0f, false, true, -7.42f, 8.09f)
        lineToRelative(2.69f, 9.75f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.25f, 0.62f)
        lineToRelative(-10.15f, 5.86f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.65f, -0.09f)
        lineToRelative(-7.13f, -7.24f)
        arcToRelative(46.87f, 46.87f, 0.0f, false, true, -10.69f, 2.31f)
        lineToRelative(-3.4f, 9.61f)
        arcTo(0.55f, 0.55f, 0.0f, false, true, 226.82f, 217.67f)
        close()
        moveTo(215.61f, 215.61f)
        lineTo(226.45f, 216.55f)
        lineTo(229.83f, 207.01f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.46f, -0.35f)
        arcToRelative(45.65f, 45.65f, 0.0f, false, false, 11.1f, -2.41f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.57f, 0.13f)
        lineToRelative(7.07f, 7.19f)
        lineToRelative(9.42f, -5.44f)
        lineToRelative(-2.68f, -9.69f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.17f, -0.55f)
        arcToRelative(46.59f, 46.59f, 0.0f, false, false, 7.71f, -8.4f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.54f, -0.22f)
        lineToRelative(9.89f, 1.82f)
        lineToRelative(4.6f, -9.85f)
        lineToRelative(-7.76f, -6.41f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, -0.18f, -0.56f)
        arcTo(46.15f, 46.15f, 0.0f, false, false, 272.17f, 161.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.32f, -0.49f)
        lineToRelative(9.23f, -4.21f)
        lineToRelative(-1.89f, -10.71f)
        lineToRelative(-10.19f, -0.79f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.46f, -0.35f)
        arcToRelative(45.71f, 45.71f, 0.0f, false, false, -5.24f, -9.92f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.0f, -0.59f)
        lineToRelative(5.25f, -8.87f)
        lineToRelative(-7.69f, -7.69f)
        lineToRelative(-8.94f, 5.31f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.58f, 0.0f)
        arcToRelative(45.73f, 45.73f, 0.0f, false, false, -17.56f, -7.16f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.43f, -0.4f)
        lineToRelative(-2.59f, -10.22f)
        lineTo(220.51f, 104.91f)
        lineToRelative(-2.59f, 10.29f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.43f, 0.4f)
        arcToRelative(46.0f, 46.0f, 0.0f, false, false, -10.41f, 3.3f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.58f, -0.08f)
        lineToRelative(-8.05f, -7.0f)
        lineToRelative(-8.91f, 6.23f)
        lineToRelative(3.81f, 10.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.12f, 0.57f)
        arcToRelative(46.61f, 46.61f, 0.0f, false, false, -6.65f, 8.63f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.52f, 0.26f)
        lineToRelative(-10.61f, -1.09f)
        lineToRelative(-3.72f, 10.22f)
        lineToRelative(8.78f, 6.0f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.23f, 0.54f)
        arcToRelative(46.78f, 46.78f, 0.0f, false, false, -0.65f, 7.69f)
        curveToRelative(0.0f, 1.0f, 0.0f, 2.0f, 0.13f, 3.25f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, -0.28f, 0.51f)
        lineToRelative(-9.24f, 5.16f)
        lineToRelative(2.82f, 10.5f)
        lineTo(184.0f, 180.1f)
        horizontalLineToRelative(0.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.49f, 0.31f)
        arcToRelative(46.0f, 46.0f, 0.0f, false, false, 5.9f, 9.39f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.07f, 0.57f)
        lineToRelative(-4.52f, 9.38f)
        lineToRelative(8.33f, 7.0f)
        lineToRelative(8.36f, -6.0f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.59f, 0.0f)
        arcToRelative(46.21f, 46.21f, 0.0f, false, false, 10.35f, 4.37f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.39f, 0.43f)
        close()
        moveTo(226.13f, 190.61f)
        arcToRelative(29.79f, 29.79f, 0.0f, true, true, 29.8f, -29.79f)
        arcTo(29.83f, 29.83f, 0.0f, false, true, 226.13f, 190.6f)
        close()
        moveTo(226.13f, 132.11f)
        arcToRelative(28.71f, 28.71f, 0.0f, true, false, 28.71f, 28.71f)
        arcTo(28.74f, 28.74f, 0.0f, false, false, 226.13f, 132.1f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(162.06f, 183.84f)
        arcToRelative(1.62f, 1.62f, 0.0f, false, true, -1.54f, -1.13f)
        arcToRelative(68.85f, 68.85f, 0.0f, false, true, -2.46f, -10.32f)
        arcToRelative(1.62f, 1.62f, 0.0f, true, true, 3.21f, -0.5f)
        arcToRelative(66.29f, 66.29f, 0.0f, false, false, 2.34f, 9.83f)
        arcToRelative(1.63f, 1.63f, 0.0f, false, true, -1.05f, 2.0f)
        arcTo(1.45f, 1.45f, 0.0f, false, true, 162.06f, 183.84f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(158.87f, 165.0f)
        arcToRelative(1.63f, 1.63f, 0.0f, false, true, -1.63f, -1.58f)
        curveToRelative(0.0f, -0.63f, 0.0f, -1.26f, 0.0f, -1.89f)
        arcTo(69.25f, 69.25f, 0.0f, false, true, 279.0f, 116.4f)
        arcToRelative(1.62f, 1.62f, 0.0f, true, true, -2.46f, 2.12f)
        arcToRelative(66.0f, 66.0f, 0.0f, false, false, -116.07f, 43.0f)
        curveToRelative(0.0f, 0.6f, 0.0f, 1.2f, 0.0f, 1.8f)
        arcToRelative(1.62f, 1.62f, 0.0f, false, true, -1.58f, 1.67f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(282.31f, 111.32f)
        lineToRelative(1.79f, 14.8f)
        lineToRelative(-13.71f, -5.84f)
        lineToRelative(11.92f, -8.96f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(352.22f, 213.69f)
        lineToRelative(8.21f, -2.45f)
        lineToRelative(-2.12f, -12.0f)
        lineToRelative(-8.61f, 0.52f)
        arcToRelative(38.8f, 38.8f, 0.0f, false, false, -4.41f, -8.36f)
        lineToRelative(5.41f, -6.82f)
        lineTo(342.08f, 176.0f)
        lineToRelative(-6.88f, 5.46f)
        arcToRelative(38.49f, 38.49f, 0.0f, false, false, -14.78f, -6.0f)
        lineToRelative(-1.0f, -8.84f)
        lineTo(307.22f, 166.62f)
        lineToRelative(-1.0f, 8.9f)
        arcToRelative(38.66f, 38.66f, 0.0f, false, false, -8.77f, 2.78f)
        lineToRelative(-6.0f, -6.73f)
        lineToRelative(-10.0f, 7.0f)
        lineToRelative(4.29f, 7.93f)
        arcToRelative(38.82f, 38.82f, 0.0f, false, false, -5.6f, 7.26f)
        lineToRelative(-8.76f, -2.09f)
        lineTo(267.24f, 203.0f)
        lineToRelative(8.0f, 4.0f)
        arcToRelative(38.25f, 38.25f, 0.0f, false, false, -0.55f, 6.47f)
        curveToRelative(0.0f, 0.92f, 0.0f, 1.83f, 0.11f, 2.74f)
        lineToRelative(-8.31f, 3.28f)
        lineToRelative(3.15f, 11.77f)
        lineToRelative(8.73f, -1.29f)
        arcToRelative(38.66f, 38.66f, 0.0f, false, false, 5.0f, 7.9f)
        lineToRelative(-4.84f, 7.33f)
        lineToRelative(9.34f, 7.83f)
        lineToRelative(6.3f, -6.0f)
        arcToRelative(38.64f, 38.64f, 0.0f, false, false, 8.72f, 3.68f)
        lineToRelative(0.23f, 8.62f)
        lineToRelative(12.14f, 1.06f)
        lineToRelative(1.72f, -8.37f)
        arcToRelative(38.67f, 38.67f, 0.0f, false, false, 9.34f, -2.0f)
        lineToRelative(5.07f, 6.83f)
        lineToRelative(10.55f, -6.09f)
        lineToRelative(-3.37f, -7.78f)
        arcToRelative(39.59f, 39.59f, 0.0f, false, false, 6.49f, -7.07f)
        lineToRelative(8.06f, 2.68f)
        lineToRelative(5.15f, -11.0f)
        lineTo(351.0f, 223.17f)
        arcTo(38.48f, 38.48f, 0.0f, false, false, 352.22f, 213.69f)
        close()
        moveTo(337.8f, 213.52f)
        arcToRelative(24.34f, 24.34f, 0.0f, true, true, -24.33f, -24.34f)
        arcTo(24.33f, 24.33f, 0.0f, false, true, 337.8f, 213.52f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(315.25f, 261.0f)
        horizontalLineToRelative(-0.05f)
        lineTo(303.07f, 260.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.5f, -0.52f)
        lineToRelative(-0.22f, -8.23f)
        arcToRelative(39.07f, 39.07f, 0.0f, false, true, -8.11f, -3.42f)
        lineToRelative(-6.0f, 5.69f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.72f, 0.0f)
        lineToRelative(-9.34f, -7.83f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.1f, -0.72f)
        lineToRelative(4.62f, -7.0f)
        arcToRelative(39.36f, 39.36f, 0.0f, false, true, -4.61f, -7.34f)
        lineToRelative(-8.34f, 1.24f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.6f, -0.4f)
        lineTo(266.0f, 219.68f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.33f, -0.64f)
        lineToRelative(7.94f, -3.14f)
        curveToRelative(-0.06f, -0.89f, -0.08f, -1.65f, -0.08f, -2.38f)
        arcToRelative(39.25f, 39.25f, 0.0f, false, true, 0.49f, -6.17f)
        lineTo(267.0f, 203.52f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.27f, -0.67f)
        lineToRelative(4.17f, -11.45f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.64f, -0.34f)
        lineToRelative(8.37f, 2.0f)
        arcToRelative(39.33f, 39.33f, 0.0f, false, true, 5.19f, -6.73f)
        lineToRelative(-4.1f, -7.58f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.17f, -0.7f)
        lineToRelative(10.0f, -7.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.72f, 0.09f)
        lineToRelative(5.7f, 6.43f)
        arcTo(38.86f, 38.86f, 0.0f, false, true, 305.7f, 175.0f)
        lineToRelative(1.0f, -8.5f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.54f, -0.48f)
        horizontalLineToRelative(12.19f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.53f, 0.48f)
        lineToRelative(1.0f, 8.45f)
        arcToRelative(38.87f, 38.87f, 0.0f, false, true, 14.26f, 5.81f)
        lineToRelative(6.57f, -5.21f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.72f, 0.0f)
        lineToRelative(8.61f, 8.62f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.0f, 0.72f)
        lineToRelative(-5.16f, 6.5f)
        arcToRelative(39.32f, 39.32f, 0.0f, false, true, 4.11f, 7.77f)
        lineToRelative(8.21f, -0.49f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.57f, 0.45f)
        lineToRelative(2.11f, 12.0f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.38f, 0.61f)
        lineToRelative(-7.82f, 2.33f)
        arcToRelative(39.55f, 39.55f, 0.0f, false, true, -1.12f, 8.83f)
        lineToRelative(6.9f, 4.24f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.21f, 0.69f)
        lineToRelative(-5.15f, 11.0f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.66f, 0.29f)
        lineToRelative(-7.69f, -2.56f)
        arcToRelative(39.6f, 39.6f, 0.0f, false, true, -6.0f, 6.58f)
        lineToRelative(3.21f, 7.42f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.22f, 0.68f)
        lineToRelative(-10.56f, 6.09f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.7f, -0.14f)
        lineToRelative(-4.84f, -6.52f)
        arcToRelative(39.06f, 39.06f, 0.0f, false, true, -8.69f, 1.88f)
        lineToRelative(-1.64f, 8.0f)
        arcTo(0.54f, 0.54f, 0.0f, false, true, 315.25f, 261.0f)
        close()
        moveTo(303.64f, 258.89f)
        lineTo(314.82f, 259.89f)
        lineTo(316.43f, 251.99f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.49f, -0.43f)
        arcToRelative(38.15f, 38.15f, 0.0f, false, false, 9.21f, -2.0f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.62f, 0.19f)
        lineToRelative(4.78f, 6.44f)
        lineToRelative(9.72f, -5.61f)
        lineToRelative(-3.19f, -7.34f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.15f, -0.63f)
        arcToRelative(37.76f, 37.76f, 0.0f, false, false, 6.39f, -7.0f)
        arcToRelative(0.57f, 0.57f, 0.0f, false, true, 0.62f, -0.2f)
        lineToRelative(7.6f, 2.54f)
        lineToRelative(4.74f, -10.17f)
        lineToRelative(-6.83f, -4.2f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.24f, -0.6f)
        arcToRelative(38.0f, 38.0f, 0.0f, false, false, 1.19f, -9.35f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.39f, -0.51f)
        lineToRelative(7.74f, -2.31f)
        lineToRelative(-1.95f, -11.05f)
        lineToRelative(-8.12f, 0.49f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.54f, -0.35f)
        arcToRelative(38.43f, 38.43f, 0.0f, false, false, -4.35f, -8.24f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.0f, -0.64f)
        lineToRelative(5.1f, -6.44f)
        lineTo(342.0f, 176.69f)
        lineToRelative(-6.5f, 5.16f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.64f, 0.0f)
        arcToRelative(37.76f, 37.76f, 0.0f, false, false, -14.57f, -5.94f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.44f, -0.47f)
        lineToRelative(-1.0f, -8.37f)
        lineTo(307.7f, 167.07f)
        lineToRelative(-1.0f, 8.42f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, -0.43f, 0.47f)
        arcToRelative(38.0f, 38.0f, 0.0f, false, false, -8.64f, 2.75f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.63f, -0.14f)
        lineToRelative(-5.65f, -6.36f)
        lineToRelative(-9.19f, 6.43f)
        lineToRelative(4.06f, 7.5f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, -0.09f, 0.64f)
        arcToRelative(37.88f, 37.88f, 0.0f, false, false, -5.52f, 7.16f)
        arcToRelative(0.57f, 0.57f, 0.0f, false, true, -0.6f, 0.25f)
        lineToRelative(-8.29f, -2.0f)
        lineToRelative(-3.83f, 10.54f)
        lineToRelative(7.58f, 3.79f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.29f, 0.58f)
        arcToRelative(38.43f, 38.43f, 0.0f, false, false, -0.54f, 6.38f)
        curveToRelative(0.0f, 0.81f, 0.0f, 1.67f, 0.11f, 2.7f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.34f, 0.54f)
        lineToRelative(-7.86f, 3.11f)
        lineToRelative(2.9f, 10.83f)
        lineToRelative(8.25f, -1.22f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.57f, 0.31f)
        arcToRelative(38.52f, 38.52f, 0.0f, false, false, 4.9f, 7.79f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.0f, 0.64f)
        lineToRelative(-4.57f, 6.93f)
        lineToRelative(8.59f, 7.21f)
        lineToRelative(6.0f, -5.63f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.64f, -0.08f)
        arcToRelative(37.75f, 37.75f, 0.0f, false, false, 8.59f, 3.63f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.4f, 0.5f)
        close()
        moveTo(313.47f, 238.37f)
        arcToRelative(24.88f, 24.88f, 0.0f, true, true, 24.88f, -24.88f)
        arcTo(24.91f, 24.91f, 0.0f, false, true, 313.47f, 238.4f)
        close()
        moveTo(313.47f, 189.69f)
        arcToRelative(23.8f, 23.8f, 0.0f, true, false, 23.79f, 23.8f)
        arcTo(23.83f, 23.83f, 0.0f, false, false, 313.47f, 189.72f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(313.08f, 272.65f)
        arcToRelative(58.17f, 58.17f, 0.0f, false, true, -36.84f, -13.27f)
        arcToRelative(1.62f, 1.62f, 0.0f, false, true, 2.07f, -2.5f)
        arcTo(54.68f, 54.68f, 0.0f, false, false, 362.41f, 191.0f)
        arcToRelative(1.63f, 1.63f, 0.0f, true, true, 2.93f, -1.41f)
        arcToRelative(57.93f, 57.93f, 0.0f, false, true, -52.26f, 83.06f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(358.38f, 196.02f)
        lineToRelative(-0.05f, -14.91f)
        lineToRelative(12.93f, 7.41f)
        lineToRelative(-12.88f, 7.5f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(124.0f, 401.33f)
        arcToRelative(51.67f, 13.0f, 0.0f, true, false, 103.34f, 0.0f)
        arcToRelative(51.67f, 13.0f, 0.0f, true, false, -103.34f, 0.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(284.52f, 390.51f)
        curveToRelative(0.0f, -0.58f, 0.07f, -1.16f, 0.07f, -1.76f)
        curveToRelative(0.0f, -14.0f, -13.15f, -25.32f, -29.38f, -25.32f)
        reflectiveCurveToRelative(-29.39f, 11.34f, -29.39f, 25.32f)
        curveToRelative(0.0f, 0.6f, 0.0f, 1.18f, 0.08f, 1.76f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(284.52f, 391.06f)
        horizontalLineTo(225.9f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.54f, -0.5f)
        curveToRelative(-0.05f, -0.68f, -0.08f, -1.26f, -0.08f, -1.81f)
        curveToRelative(0.0f, -14.26f, 13.43f, -25.86f, 29.93f, -25.86f)
        reflectiveCurveToRelative(29.93f, 11.6f, 29.93f, 25.86f)
        curveToRelative(0.0f, 0.55f, 0.0f, 1.12f, -0.08f, 1.81f)
        arcTo(0.55f, 0.55f, 0.0f, false, true, 284.52f, 391.06f)
        close()
        moveTo(226.4f, 390.0f)
        horizontalLineTo(284.0f)
        curveToRelative(0.0f, -0.44f, 0.0f, -0.84f, 0.0f, -1.22f)
        curveToRelative(0.0f, -13.66f, -12.94f, -24.78f, -28.84f, -24.78f)
        reflectiveCurveToRelative(-28.85f, 11.12f, -28.85f, 24.78f)
        curveTo(226.36f, 389.14f, 226.38f, 389.53f, 226.4f, 390.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(284.52f, 392.92f)
        curveToRelative(0.0f, -0.58f, 0.07f, -1.17f, 0.07f, -1.76f)
        curveToRelative(0.0f, -14.0f, -13.15f, -25.32f, -29.38f, -25.32f)
        reflectiveCurveToRelative(-29.39f, 11.34f, -29.39f, 25.32f)
        curveToRelative(0.0f, 0.59f, 0.0f, 1.18f, 0.08f, 1.76f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(284.52f, 393.46f)
        lineTo(225.9f, 393.46f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.54f, -0.49f)
        curveToRelative(-0.05f, -0.69f, -0.08f, -1.26f, -0.08f, -1.81f)
        curveToRelative(0.0f, -14.26f, 13.43f, -25.86f, 29.93f, -25.86f)
        reflectiveCurveToRelative(29.93f, 11.6f, 29.93f, 25.86f)
        curveToRelative(0.0f, 0.55f, 0.0f, 1.12f, -0.08f, 1.81f)
        arcTo(0.54f, 0.54f, 0.0f, false, true, 284.52f, 393.46f)
        close()
        moveTo(226.4f, 392.38f)
        lineTo(284.0f, 392.38f)
        curveToRelative(0.0f, -0.44f, 0.0f, -0.84f, 0.0f, -1.22f)
        curveToRelative(0.0f, -13.66f, -12.94f, -24.78f, -28.84f, -24.78f)
        reflectiveCurveToRelative(-28.85f, 11.12f, -28.85f, 24.78f)
        curveTo(226.36f, 391.54f, 226.38f, 391.94f, 226.4f, 392.38f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(192.07f, 229.55f)
        lineToRelative(20.21f, 31.95f)
        lineTo(234.49f, 249.0f)
        lineToRelative(1.35f, 10.84f)
        reflectiveCurveTo(214.45f, 276.67f, 211.47f, 278.0f)
        reflectiveCurveToRelative(-8.4f, 0.55f, -9.75f, -1.08f)
        reflectiveCurveTo(189.0f, 235.0f, 189.0f, 235.0f)
        reflectiveCurveTo(187.0f, 227.39f, 192.07f, 229.55f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(207.66f, 279.25f)
        curveToRelative(-2.62f, 0.0f, -5.33f, -0.72f, -6.36f, -2.0f)
        curveToRelative(-1.39f, -1.67f, -11.67f, -38.0f, -12.83f, -42.18f)
        curveToRelative(-0.13f, -0.47f, -1.12f, -4.53f, 0.57f, -6.0f)
        curveToRelative(0.75f, -0.64f, 1.84f, -0.67f, 3.24f, -0.07f)
        arcToRelative(0.49f, 0.49f, 0.0f, false, true, 0.24f, 0.21f)
        lineToRelative(19.94f, 31.52f)
        lineToRelative(21.76f, -12.21f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.51f, 0.0f)
        arcToRelative(0.57f, 0.57f, 0.0f, false, true, 0.3f, 0.42f)
        lineToRelative(1.35f, 10.83f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.2f, 0.49f)
        curveToRelative(-0.88f, 0.69f, -21.49f, 16.85f, -24.49f, 18.22f)
        arcTo(10.07f, 10.07f, 0.0f, false, true, 207.66f, 279.25f)
        close()
        moveTo(190.49f, 229.7f)
        arcToRelative(1.12f, 1.12f, 0.0f, false, false, -0.74f, 0.24f)
        curveToRelative(-0.89f, 0.77f, -0.62f, 3.41f, -0.24f, 4.89f)
        curveToRelative(4.53f, 16.06f, 11.63f, 40.46f, 12.63f, 41.77f)
        curveToRelative(1.18f, 1.41f, 6.36f, 2.18f, 9.1f, 0.93f)
        reflectiveCurveToRelative(21.66f, -16.0f, 24.0f, -17.89f)
        lineToRelative(-1.22f, -9.73f)
        lineTo(212.54f, 262.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.72f, -0.19f)
        lineTo(191.7f, 230.0f)
        arcTo(3.51f, 3.51f, 0.0f, false, false, 190.49f, 229.7f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(233.41f, 247.42f)
        lineToRelative(-15.44f, 9.75f)
        lineToRelative(8.21f, 15.98f)
        lineToRelative(12.91f, -14.08f)
        lineToRelative(-1.35f, -14.9f)
        lineToRelative(-4.33f, 3.25f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(226.18f, 273.69f)
        horizontalLineToRelative(-0.07f)
        arcToRelative(0.59f, 0.59f, 0.0f, false, true, -0.41f, -0.29f)
        lineToRelative(-8.21f, -16.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.19f, -0.71f)
        lineTo(233.1f, 247.0f)
        lineToRelative(4.31f, -3.23f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.87f, 0.38f)
        lineToRelative(1.35f, 14.9f)
        arcToRelative(0.49f, 0.49f, 0.0f, false, true, -0.14f, 0.41f)
        lineToRelative(-12.91f, 14.09f)
        arcTo(0.54f, 0.54f, 0.0f, false, true, 226.18f, 273.69f)
        close()
        moveTo(218.68f, 257.36f)
        lineTo(226.31f, 272.21f)
        lineTo(238.53f, 258.88f)
        lineTo(237.29f, 245.19f)
        lineTo(233.73f, 247.85f)
        lineTo(233.73f, 247.85f)
        close()
        moveTo(233.41f, 247.42f)
        horizontalLineToRelative(0.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(169.7f, 304.32f)
        lineToRelative(37.7f, 37.89f)
        reflectiveCurveToRelative(-9.48f, 8.94f, -16.25f, 16.53f)
        reflectiveCurveToRelative(-17.87f, 17.33f, -17.87f, 17.33f)
        lineToRelative(3.79f, 4.6f)
        reflectiveCurveToRelative(43.88f, -33.0f, 46.0f, -35.21f)
        reflectiveCurveToRelative(2.17f, -6.5f, 1.63f, -12.18f)
        reflectiveCurveToRelative(-16.52f, -50.38f, -16.52f, -50.38f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(177.07f, 381.22f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.42f, -0.2f)
        lineToRelative(-3.79f, -4.61f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.06f, -0.75f)
        curveToRelative(0.11f, -0.09f, 11.16f, -9.81f, 17.83f, -17.28f)
        curveToRelative(5.92f, -6.63f, 14.0f, -14.38f, 15.88f, -16.18f)
        lineToRelative(-37.31f, -37.5f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.16f, -0.45f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.28f, -0.4f)
        lineTo(208.0f, 282.43f)
        arcToRelative(0.57f, 0.57f, 0.0f, false, true, 0.46f, 0.0f)
        arcToRelative(0.58f, 0.58f, 0.0f, false, true, 0.32f, 0.33f)
        curveToRelative(0.65f, 1.82f, 16.0f, 44.82f, 16.55f, 50.5f)
        curveToRelative(0.58f, 6.12f, 0.48f, 10.36f, -1.78f, 12.63f)
        reflectiveCurveToRelative(-44.31f, 33.91f, -46.1f, 35.26f)
        arcTo(0.59f, 0.59f, 0.0f, false, true, 177.07f, 381.22f)
        close()
        moveTo(174.07f, 376.13f)
        lineTo(177.2f, 379.93f)
        curveToRelative(4.48f, -3.38f, 43.57f, -32.85f, 45.57f, -34.85f)
        reflectiveCurveToRelative(2.0f, -6.12f, 1.47f, -11.75f)
        curveToRelative(-0.5f, -5.2f, -14.37f, -44.3f, -16.28f, -49.65f)
        lineToRelative(-37.33f, 20.77f)
        lineToRelative(37.2f, 37.38f)
        arcToRelative(0.59f, 0.59f, 0.0f, false, true, 0.16f, 0.39f)
        arcToRelative(0.61f, 0.61f, 0.0f, false, true, -0.17f, 0.39f)
        curveToRelative(-0.1f, 0.09f, -9.54f, 9.0f, -16.22f, 16.49f)
        curveTo(185.56f, 365.81f, 176.07f, 374.32f, 174.0f, 376.13f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(189.86f, 353.56f)
        lineTo(199.0f, 369.51f)
        lineToRelative(-18.52f, 9.69f)
        arcToRelative(11.21f, 11.21f, 0.0f, false, false, 0.0f, 6.55f)
        curveToRelative(1.14f, 3.14f, 4.0f, 6.27f, 4.27f, 8.0f)
        reflectiveCurveToRelative(0.57f, 2.56f, -3.13f, 2.56f)
        reflectiveCurveToRelative(-7.69f, -3.13f, -9.69f, -5.41f)
        reflectiveCurveToRelative(-2.28f, -8.55f, -4.0f, -11.11f)
        reflectiveCurveToRelative(-4.56f, -6.27f, -2.28f, -9.69f)
        reflectiveCurveToRelative(6.83f, -4.0f, 6.83f, -4.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(181.6f, 396.83f)
        curveToRelative(-4.1f, 0.0f, -8.27f, -3.51f, -10.09f, -5.59f)
        curveToRelative(-1.16f, -1.32f, -1.76f, -3.77f, -2.34f, -6.14f)
        arcToRelative(18.08f, 18.08f, 0.0f, false, false, -1.69f, -5.0f)
        lineToRelative(-0.37f, -0.55f)
        curveToRelative(-1.79f, -2.64f, -4.24f, -6.25f, -1.91f, -9.74f)
        curveToRelative(2.18f, -3.28f, 6.25f, -4.07f, 7.07f, -4.2f)
        lineToRelative(17.27f, -12.46f)
        arcTo(0.58f, 0.58f, 0.0f, false, true, 190.0f, 353.0f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.35f, 0.26f)
        lineToRelative(9.12f, 16.0f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.05f, 0.42f)
        arcToRelative(0.51f, 0.51f, 0.0f, false, true, -0.27f, 0.32f)
        lineToRelative(-18.32f, 9.58f)
        arcToRelative(10.59f, 10.59f, 0.0f, false, false, 0.06f, 6.0f)
        arcToRelative(24.11f, 24.11f, 0.0f, false, false, 2.45f, 4.48f)
        arcToRelative(11.89f, 11.89f, 0.0f, false, true, 1.85f, 3.59f)
        lineToRelative(0.0f, 0.18f)
        curveToRelative(0.14f, 0.86f, 0.27f, 1.6f, -0.23f, 2.19f)
        reflectiveCurveTo(183.6f, 396.83f, 181.6f, 396.83f)
        close()
        moveTo(189.69f, 354.35f)
        lineTo(172.8f, 366.53f)
        arcToRelative(0.46f, 0.46f, 0.0f, false, true, -0.25f, 0.1f)
        reflectiveCurveToRelative(-4.34f, 0.59f, -6.45f, 3.75f)
        curveToRelative(-1.92f, 2.88f, 0.2f, 6.0f, 1.9f, 8.53f)
        lineToRelative(0.38f, 0.56f)
        arcToRelative(18.62f, 18.62f, 0.0f, false, true, 1.85f, 5.37f)
        curveToRelative(0.54f, 2.24f, 1.11f, 4.56f, 2.09f, 5.68f)
        curveToRelative(2.2f, 2.52f, 6.05f, 5.23f, 9.28f, 5.23f)
        curveToRelative(1.51f, 0.0f, 2.4f, -0.15f, 2.64f, -0.44f)
        reflectiveCurveToRelative(0.09f, -0.69f, 0.0f, -1.31f)
        lineToRelative(0.0f, -0.18f)
        arcToRelative(11.73f, 11.73f, 0.0f, false, false, -1.69f, -3.18f)
        arcToRelative(24.79f, 24.79f, 0.0f, false, true, -2.56f, -4.7f)
        arcToRelative(11.64f, 11.64f, 0.0f, false, true, 0.0f, -6.91f)
        arcToRelative(0.48f, 0.48f, 0.0f, false, true, 0.26f, -0.31f)
        lineToRelative(18.0f, -9.43f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(184.08f, 396.0f)
        curveToRelative(-4.0f, -1.15f, -7.71f, -4.35f, -10.25f, -7.84f)
        curveToRelative(-2.84f, -3.9f, -2.13f, -10.65f, -5.0f, -13.13f)
        curveToRelative(-1.74f, -1.53f, -1.88f, -4.53f, -1.73f, -6.61f)
        arcToRelative(7.82f, 7.82f, 0.0f, false, false, -1.48f, 1.64f)
        curveToRelative(-2.28f, 3.42f, 0.57f, 7.13f, 2.28f, 9.69f)
        reflectiveCurveToRelative(2.0f, 8.83f, 4.0f, 11.11f)
        reflectiveCurveToRelative(6.0f, 5.41f, 9.69f, 5.41f)
        arcTo(7.77f, 7.77f, 0.0f, false, false, 184.08f, 396.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(181.6f, 396.83f)
        curveToRelative(-4.1f, 0.0f, -8.27f, -3.51f, -10.09f, -5.59f)
        curveToRelative(-1.16f, -1.32f, -1.76f, -3.77f, -2.34f, -6.14f)
        arcToRelative(18.08f, 18.08f, 0.0f, false, false, -1.69f, -5.0f)
        lineToRelative(-0.37f, -0.55f)
        curveToRelative(-1.79f, -2.64f, -4.24f, -6.25f, -1.91f, -9.74f)
        arcToRelative(8.21f, 8.21f, 0.0f, false, true, 1.59f, -1.76f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.59f, -0.07f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.29f, 0.53f)
        curveToRelative(-0.12f, 1.69f, -0.09f, 4.73f, 1.55f, 6.16f)
        reflectiveCurveToRelative(2.14f, 3.92f, 2.71f, 6.59f)
        curveToRelative(0.51f, 2.38f, 1.0f, 4.84f, 2.34f, 6.63f)
        curveToRelative(2.8f, 3.85f, 6.43f, 6.63f, 10.0f, 7.64f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.4f, 0.5f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, -0.36f, 0.53f)
        arcTo(8.23f, 8.23f, 0.0f, false, true, 181.6f, 396.83f)
        close()
        moveTo(166.6f, 369.83f)
        arcToRelative(4.85f, 4.85f, 0.0f, false, false, -0.45f, 0.59f)
        curveToRelative(-1.92f, 2.88f, 0.2f, 6.0f, 1.9f, 8.53f)
        lineToRelative(0.38f, 0.56f)
        arcToRelative(18.62f, 18.62f, 0.0f, false, true, 1.85f, 5.37f)
        curveToRelative(0.54f, 2.24f, 1.11f, 4.56f, 2.09f, 5.68f)
        curveToRelative(2.2f, 2.52f, 6.05f, 5.23f, 9.28f, 5.23f)
        horizontalLineToRelative(0.22f)
        arcToRelative(22.5f, 22.5f, 0.0f, false, true, -8.43f, -7.25f)
        curveToRelative(-1.44f, -2.0f, -2.0f, -4.55f, -2.52f, -7.0f)
        reflectiveCurveToRelative(-1.0f, -4.84f, -2.37f, -6.0f)
        curveTo(167.0f, 374.12f, 166.56f, 371.83f, 166.55f, 369.79f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(166.6f, 299.87f)
        reflectiveCurveToRelative(7.31f, 13.46f, 13.0f, 21.31f)
        curveToRelative(7.58f, 10.47f, 21.3f, 26.45f, 21.3f, 26.45f)
        lineToRelative(-2.16f, 50.65f)
        lineToRelative(6.77f, 0.81f)
        reflectiveCurveToRelative(12.19f, -49.83f, 12.46f, -53.9f)
        reflectiveCurveToRelative(-17.61f, -42.25f, -17.61f, -42.25f)
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(205.51f, 399.63f)
        horizontalLineToRelative(-0.07f)
        lineToRelative(-6.77f, -0.81f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.47f, -0.57f)
        lineToRelative(2.15f, -50.43f)
        curveToRelative(-1.46f, -1.72f, -14.09f, -16.52f, -21.19f, -26.32f)
        curveToRelative(-5.64f, -7.79f, -13.0f, -21.23f, -13.0f, -21.37f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.22f, -0.73f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.73f, 0.21f)
        curveToRelative(0.08f, 0.14f, 7.37f, 13.52f, 13.0f, 21.25f)
        curveToRelative(7.48f, 10.34f, 21.14f, 26.26f, 21.28f, 26.42f)
        arcToRelative(0.62f, 0.62f, 0.0f, false, true, 0.13f, 0.37f)
        lineTo(199.3f, 397.8f)
        lineToRelative(5.8f, 0.7f)
        curveToRelative(1.23f, -5.08f, 12.08f, -49.58f, 12.33f, -53.34f)
        curveToRelative(0.21f, -3.22f, -12.6f, -31.4f, -17.56f, -42.0f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.26f, -0.72f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.72f, 0.26f)
        curveToRelative(1.84f, 3.92f, 17.93f, 38.44f, 17.66f, 42.52f)
        reflectiveCurveToRelative(-12.0f, 51.95f, -12.48f, 54.0f)
        arcTo(0.53f, 0.53f, 0.0f, false, true, 205.51f, 399.63f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(217.85f, 348.0f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.55f, -0.54f)
        curveToRelative(-0.06f, -14.28f, -6.0f, -25.66f, -12.32f, -37.71f)
        lineToRelative(-1.17f, -2.24f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.0f, -0.47f)
        lineTo(207.0f, 299.8f)
        arcToRelative(0.54f, 0.54f, 0.0f, true, true, 1.0f, 0.43f)
        lineToRelative(-3.0f, 7.0f)
        lineToRelative(1.0f, 2.0f)
        curveToRelative(6.37f, 12.17f, 12.38f, 23.66f, 12.45f, 38.2f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.54f, 0.55f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(204.75f, 371.0f)
        horizontalLineToRelative(-0.06f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.48f, -0.59f)
        reflectiveCurveToRelative(0.13f, -1.33f, 0.37f, -3.22f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.6f, -0.47f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.47f, 0.6f)
        curveToRelative(-0.23f, 1.88f, -0.36f, 3.19f, -0.36f, 3.2f)
        arcTo(0.55f, 0.55f, 0.0f, false, true, 204.75f, 371.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(205.59f, 364.13f)
        horizontalLineToRelative(-0.07f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.46f, -0.61f)
        curveToRelative(0.68f, -4.85f, 1.32f, -7.91f, 1.91f, -9.09f)
        arcToRelative(2.26f, 2.26f, 0.0f, false, false, 0.34f, -1.78f)
        curveToRelative(-0.27f, -0.57f, -1.27f, -0.82f, -2.24f, -1.06f)
        arcToRelative(1.0f, 1.0f, 0.0f, false, true, -0.81f, -0.7f)
        curveToRelative(-0.2f, -0.78f, 0.7f, -1.6f, 2.0f, -2.84f)
        arcToRelative(14.62f, 14.62f, 0.0f, false, false, 2.5f, -2.69f)
        curveToRelative(0.15f, -0.25f, 0.11f, -0.37f, 0.09f, -0.41f)
        curveToRelative(-0.18f, -0.55f, -1.62f, -1.15f, -3.0f, -1.72f)
        arcToRelative(19.86f, 19.86f, 0.0f, false, true, -5.54f, -3.05f)
        curveToRelative(-2.9f, -2.54f, -15.85f, -22.7f, -21.28f, -31.25f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.16f, -0.75f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.75f, 0.17f)
        curveToRelative(11.26f, 17.72f, 19.14f, 29.32f, 21.08f, 31.0f)
        arcToRelative(18.5f, 18.5f, 0.0f, false, false, 5.24f, 2.86f)
        curveToRelative(1.92f, 0.8f, 3.31f, 1.37f, 3.64f, 2.39f)
        arcToRelative(1.53f, 1.53f, 0.0f, false, true, -0.19f, 1.31f)
        arcToRelative(15.15f, 15.15f, 0.0f, false, true, -2.69f, 2.92f)
        arcToRelative(12.09f, 12.09f, 0.0f, false, false, -1.66f, 1.71f)
        curveToRelative(1.2f, 0.3f, 2.44f, 0.62f, 2.91f, 1.65f)
        arcToRelative(3.12f, 3.12f, 0.0f, false, true, -0.35f, 2.72f)
        curveToRelative(-0.37f, 0.73f, -1.0f, 2.85f, -1.81f, 8.75f)
        arcTo(0.55f, 0.55f, 0.0f, false, true, 205.59f, 364.13f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(178.44f, 307.49f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.46f, -0.26f)
        lineToRelative(-1.24f, -2.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, true, true, 0.91f, -0.58f)
        lineToRelative(1.24f, 2.0f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.16f, 0.75f)
        arcTo(0.52f, 0.52f, 0.0f, false, true, 178.44f, 307.49f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(197.77f, 371.62f)
        lineToRelative(17.76f, 1.5f)
        lineToRelative(-4.82f, 25.0f)
        arcToRelative(11.24f, 11.24f, 0.0f, false, false, 6.93f, 6.0f)
        curveToRelative(5.11f, 1.5f, 7.22f, 2.4f, 7.22f, 4.81f)
        reflectiveCurveToRelative(-2.11f, 3.0f, -10.53f, 3.91f)
        reflectiveCurveToRelative(-17.16f, -2.4f, -17.16f, -2.4f)
        reflectiveCurveToRelative(-3.91f, -0.61f, -4.21f, -3.31f)
        reflectiveCurveToRelative(3.91f, -10.24f, 3.91f, -10.24f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(211.26f, 413.54f)
        arcTo(45.0f, 45.0f, 0.0f, false, true, 197.0f, 411.0f)
        curveToRelative(-0.54f, -0.09f, -4.28f, -0.85f, -4.61f, -3.77f)
        curveToRelative(-0.3f, -2.7f, 3.25f, -9.25f, 3.91f, -10.44f)
        lineToRelative(0.9f, -25.15f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.19f, -0.39f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.4f, -0.13f)
        lineToRelative(17.76f, 1.5f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.38f, 0.21f)
        arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.1f, 0.43f)
        lineTo(211.28f, 398.0f)
        arcToRelative(10.78f, 10.78f, 0.0f, false, false, 6.51f, 5.57f)
        curveToRelative(4.86f, 1.43f, 7.61f, 2.38f, 7.61f, 5.33f)
        reflectiveCurveToRelative(-2.86f, 3.58f, -11.0f, 4.45f)
        arcTo(29.77f, 29.77f, 0.0f, false, true, 211.26f, 413.54f)
        close()
        moveTo(198.26f, 372.2f)
        lineTo(197.37f, 396.91f)
        arcToRelative(0.45f, 0.45f, 0.0f, false, true, -0.07f, 0.25f)
        curveToRelative(-1.14f, 2.0f, -4.07f, 7.83f, -3.84f, 9.91f)
        curveToRelative(0.25f, 2.26f, 3.72f, 2.83f, 3.76f, 2.83f)
        lineToRelative(0.1f, 0.0f)
        curveToRelative(0.09f, 0.0f, 8.71f, 3.25f, 16.91f, 2.37f)
        curveToRelative(8.36f, -0.89f, 10.0f, -1.46f, 10.0f, -3.37f)
        reflectiveCurveToRelative(-1.71f, -2.78f, -6.84f, -4.29f)
        arcToRelative(11.7f, 11.7f, 0.0f, false, true, -7.27f, -6.34f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.0f, -0.3f)
        lineToRelative(4.7f, -24.39f)
        close()
        moveTo(196.83f, 396.89f)
        horizontalLineToRelative(0.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(214.33f, 412.84f)
        curveToRelative(8.0f, -0.85f, 10.3f, -1.44f, 10.51f, -3.57f)
        arcToRelative(45.0f, 45.0f, 0.0f, false, true, -14.81f, 0.92f)
        arcToRelative(53.6f, 53.6f, 0.0f, false, true, -16.82f, -5.0f)
        arcToRelative(5.39f, 5.39f, 0.0f, false, false, -0.25f, 1.94f)
        curveToRelative(0.3f, 2.7f, 4.21f, 3.31f, 4.21f, 3.31f)
        reflectiveCurveTo(205.9f, 413.75f, 214.33f, 412.84f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(211.26f, 413.54f)
        arcTo(45.0f, 45.0f, 0.0f, false, true, 197.0f, 411.0f)
        curveToRelative(-0.54f, -0.09f, -4.28f, -0.85f, -4.61f, -3.77f)
        arcToRelative(6.22f, 6.22f, 0.0f, false, true, 0.26f, -2.14f)
        arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.3f, -0.35f)
        arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.47f, 0.0f)
        arcToRelative(53.29f, 53.29f, 0.0f, false, false, 16.65f, 4.94f)
        arcToRelative(43.5f, 43.5f, 0.0f, false, false, 14.62f, -0.91f)
        arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.48f, 0.13f)
        arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.18f, 0.45f)
        curveToRelative(-0.26f, 2.66f, -3.22f, 3.23f, -11.0f, 4.06f)
        arcTo(29.77f, 29.77f, 0.0f, false, true, 211.26f, 413.54f)
        close()
        moveTo(193.58f, 406.0f)
        arcToRelative(3.66f, 3.66f, 0.0f, false, false, -0.08f, 1.09f)
        curveToRelative(0.25f, 2.26f, 3.72f, 2.83f, 3.76f, 2.83f)
        lineToRelative(0.1f, 0.0f)
        curveToRelative(0.09f, 0.0f, 8.71f, 3.25f, 16.91f, 2.37f)
        horizontalLineToRelative(0.0f)
        curveToRelative(6.6f, -0.7f, 9.0f, -1.22f, 9.77f, -2.31f)
        arcToRelative(43.53f, 43.53f, 0.0f, false, true, -14.08f, 0.73f)
        arcTo(54.07f, 54.07f, 0.0f, false, true, 193.58f, 406.0f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(166.38f, 236.13f)
        reflectiveCurveToRelative(-20.55f, 10.48f, -25.28f, 15.62f)
        reflectiveCurveToRelative(-1.0f, 9.86f, 0.82f, 15.2f)
        reflectiveCurveToRelative(25.93f, 35.93f, 27.78f, 37.37f)
        reflectiveCurveToRelative(11.3f, -3.08f, 24.05f, -8.63f)
        reflectiveCurveToRelative(15.82f, -7.81f, 15.82f, -10.89f)
        reflectiveCurveToRelative(-16.48f, -53.0f, -17.5f, -55.25f)
        reflectiveCurveTo(171.72f, 233.0f, 166.38f, 236.13f)
        close()
      }
      path(
        fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
        pathFillType = NonZero
      ) {
        moveTo(170.77f, 305.13f)
        arcToRelative(2.24f, 2.24f, 0.0f, false, true, -1.4f, -0.38f)
        curveToRelative(-1.45f, -1.13f, -26.0f, -32.0f, -28.0f, -37.62f)
        curveToRelative(-0.36f, -1.0f, -0.8f, -2.06f, -1.22f, -3.06f)
        curveToRelative(-1.73f, -4.08f, -3.52f, -8.31f, 0.51f, -12.69f)
        curveToRelative(4.72f, -5.13f, 24.41f, -15.22f, 25.42f, -15.73f)
        curveToRelative(5.06f, -2.91f, 22.06f, -8.12f, 25.6f, -7.0f)
        arcToRelative(1.27f, 1.27f, 0.0f, false, true, 0.84f, 0.67f)
        curveToRelative(1.14f, 2.5f, 17.55f, 52.36f, 17.55f, 55.47f)
        curveToRelative(0.0f, 3.46f, -3.32f, 5.8f, -16.15f, 11.39f)
        curveToRelative(-1.76f, 0.76f, -3.46f, 1.51f, -5.08f, 2.23f)
        curveTo(179.14f, 302.7f, 173.48f, 305.12f, 170.77f, 305.13f)
        close()
        moveTo(190.45f, 229.52f)
        curveToRelative(-4.25f, 0.0f, -18.72f, 4.14f, -23.8f, 7.08f)
        horizontalLineToRelative(0.0f)
        curveToRelative(-0.2f, 0.1f, -20.52f, 10.49f, -25.12f, 15.5f)
        curveToRelative(-3.56f, 3.87f, -2.06f, 7.42f, -0.31f, 11.54f)
        curveToRelative(0.43f, 1.0f, 0.87f, 2.06f, 1.24f, 3.13f)
        curveToRelative(1.93f, 5.58f, 26.17f, 36.0f, 27.6f, 37.11f)
        reflectiveCurveToRelative(9.63f, -2.6f, 18.41f, -6.47f)
        lineToRelative(5.09f, -2.23f)
        curveToRelative(12.0f, -5.23f, 15.5f, -7.57f, 15.5f, -10.39f)
        curveToRelative(0.0f, -2.64f, -16.09f, -52.0f, -17.46f, -55.0f)
        curveTo(191.43f, 229.6f, 191.0f, 229.52f, 190.45f, 229.52f)
        close()
        moveTo(166.38f, 236.13f)
        horizontalLineToRelative(0.0f)
        close()
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(160.73f, 277.0f)
          lineToRelative(13.77f, 0.55f)
          lineTo(156.0f, 273.93f)
          reflectiveCurveToRelative(-12.95f, -5.79f, -12.95f, -12.68f)
          reflectiveCurveToRelative(8.0f, -9.36f, 8.0f, -9.36f)
          lineToRelative(19.57f, -5.24f)
          lineTo(172.0f, 234.53f)
          curveToRelative(-0.31f, 0.0f, -1.2f, 0.0f, -2.44f, 0.08f)
          arcToRelative(28.26f, 28.26f, 0.0f, false, false, -3.2f, 1.52f)
          reflectiveCurveToRelative(-20.55f, 10.48f, -25.28f, 15.62f)
          reflectiveCurveToRelative(-1.0f, 9.86f, 0.82f, 15.2f)
          reflectiveCurveToRelative(25.93f, 35.93f, 27.78f, 37.37f)
          reflectiveCurveToRelative(11.3f, -3.08f, 24.05f, -8.63f)
          lineToRelative(0.4f, -0.18f)
          curveToRelative(-3.66f, 0.74f, -15.41f, 3.62f, -19.1f, 0.46f)
          curveToRelative(-3.85f, -3.31f, -11.84f, -11.57f, -11.84f, -11.57f)
          lineToRelative(29.75f, 0.27f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, fillAlpha = 0.5f,
          strokeAlpha = 0.5f, strokeLineWidth = 0.0f, strokeLineCap = Butt,
          strokeLineJoin = Miter, strokeLineMiter = 4.0f, pathFillType = NonZero
        ) {
          moveTo(160.73f, 277.0f)
          lineToRelative(13.77f, 0.55f)
          lineTo(156.0f, 273.93f)
          reflectiveCurveToRelative(-12.95f, -5.79f, -12.95f, -12.68f)
          reflectiveCurveToRelative(8.0f, -9.36f, 8.0f, -9.36f)
          lineToRelative(19.57f, -5.24f)
          lineTo(172.0f, 234.53f)
          curveToRelative(-0.31f, 0.0f, -1.2f, 0.0f, -2.44f, 0.08f)
          arcToRelative(28.26f, 28.26f, 0.0f, false, false, -3.2f, 1.52f)
          reflectiveCurveToRelative(-20.55f, 10.48f, -25.28f, 15.62f)
          reflectiveCurveToRelative(-1.0f, 9.86f, 0.82f, 15.2f)
          reflectiveCurveToRelative(25.93f, 35.93f, 27.78f, 37.37f)
          reflectiveCurveToRelative(11.3f, -3.08f, 24.05f, -8.63f)
          lineToRelative(0.4f, -0.18f)
          curveToRelative(-3.66f, 0.74f, -15.41f, 3.62f, -19.1f, 0.46f)
          curveToRelative(-3.85f, -3.31f, -11.84f, -11.57f, -11.84f, -11.57f)
          lineToRelative(29.75f, 0.27f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(160.63f, 239.17f)
          arcToRelative(32.71f, 32.71f, 0.0f, false, false, 9.45f, 9.29f)
          curveToRelative(6.57f, 4.11f, 10.89f, 3.29f, 13.15f, -0.83f)
          curveToRelative(1.85f, -3.36f, -1.11f, -13.32f, -2.25f, -16.83f)
          curveToRelative(-5.67f, 1.54f, -11.91f, 3.77f, -14.6f, 5.33f)
          curveTo(166.38f, 236.13f, 164.0f, 237.35f, 160.63f, 239.17f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(177.61f, 251.7f)
          curveToRelative(-2.24f, 0.0f, -4.86f, -0.93f, -7.82f, -2.78f)
          arcToRelative(33.67f, 33.67f, 0.0f, false, true, -9.61f, -9.45f)
          arcToRelative(0.52f, 0.52f, 0.0f, false, true, -0.08f, -0.43f)
          arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.27f, -0.35f)
          curveToRelative(3.32f, -1.8f, 5.74f, -3.0f, 5.76f, -3.05f)
          curveToRelative(2.62f, -1.51f, 8.68f, -3.72f, 14.7f, -5.37f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.66f, 0.36f)
          curveToRelative(1.31f, 4.0f, 4.14f, 13.77f, 2.21f, 17.27f)
          arcToRelative(6.87f, 6.87f, 0.0f, false, true, -4.3f, 3.58f)
          arcTo(7.0f, 7.0f, 0.0f, false, true, 177.61f, 251.7f)
          close()
          moveTo(161.41f, 239.36f)
          arcToRelative(32.49f, 32.49f, 0.0f, false, false, 9.0f, 8.64f)
          curveToRelative(3.54f, 2.21f, 6.49f, 3.0f, 8.77f, 2.43f)
          arcToRelative(5.82f, 5.82f, 0.0f, false, false, 3.62f, -3.06f)
          curveToRelative(1.69f, -3.05f, -1.0f, -12.39f, -2.13f, -15.91f)
          curveToRelative(-5.7f, 1.58f, -11.51f, 3.71f, -14.0f, 5.14f)
          curveTo(166.6f, 236.62f, 164.44f, 237.72f, 161.41f, 239.36f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(179.32f, 228.32f)
          arcToRelative(5.14f, 5.14f, 0.0f, false, false, -1.23f, 3.9f)
          curveToRelative(0.41f, 2.06f, 2.47f, 10.07f, 2.26f, 12.74f)
          reflectiveCurveToRelative(-2.67f, 3.09f, -7.81f, 1.0f)
          reflectiveCurveToRelative(-8.42f, -8.0f, -9.25f, -9.45f)
          reflectiveCurveToRelative(-4.11f, -10.07f, -4.11f, -10.07f)
          lineToRelative(2.26f, -1.44f)
          reflectiveCurveToRelative(2.27f, 3.49f, 7.81f, 3.49f)
          reflectiveCurveToRelative(9.46f, -2.88f, 10.07f, -1.64f)
          reflectiveCurveTo(179.32f, 228.32f, 179.32f, 228.32f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(177.66f, 247.84f)
          arcToRelative(15.36f, 15.36f, 0.0f, false, true, -5.32f, -1.35f)
          curveToRelative(-3.44f, -1.37f, -6.61f, -4.59f, -9.45f, -9.56f)
          lineToRelative(-0.07f, -0.12f)
          curveToRelative(-0.83f, -1.46f, -4.0f, -9.8f, -4.14f, -10.15f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.21f, -0.65f)
          lineToRelative(2.26f, -1.44f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.75f, 0.16f)
          curveToRelative(0.09f, 0.13f, 2.19f, 3.25f, 7.35f, 3.25f)
          arcToRelative(22.17f, 22.17f, 0.0f, false, false, 7.0f, -1.35f)
          curveToRelative(1.76f, -0.58f, 3.0f, -1.0f, 3.54f, 0.0f)
          arcToRelative(2.0f, 2.0f, 0.0f, false, true, 0.24f, 1.64f)
          arcToRelative(1.0f, 1.0f, 0.0f, false, true, -0.37f, 0.46f)
          arcToRelative(4.51f, 4.51f, 0.0f, false, false, -1.06f, 3.37f)
          curveToRelative(0.09f, 0.44f, 0.25f, 1.14f, 0.45f, 2.0f)
          curveToRelative(0.74f, 3.26f, 2.0f, 8.71f, 1.82f, 10.89f)
          arcToRelative(2.81f, 2.81f, 0.0f, false, true, -1.26f, 2.35f)
          arcTo(3.55f, 3.55f, 0.0f, false, true, 177.66f, 247.84f)
          close()
          moveTo(159.85f, 226.69f)
          curveToRelative(0.66f, 1.73f, 3.22f, 8.37f, 3.91f, 9.58f)
          lineToRelative(0.07f, 0.12f)
          curveToRelative(2.71f, 4.76f, 5.71f, 7.82f, 8.91f, 9.1f)
          curveToRelative(3.95f, 1.57f, 5.61f, 1.38f, 6.32f, 0.94f)
          arcToRelative(1.77f, 1.77f, 0.0f, false, false, 0.75f, -1.51f)
          curveToRelative(0.16f, -2.0f, -1.12f, -7.58f, -1.8f, -10.57f)
          curveToRelative(-0.2f, -0.87f, -0.36f, -1.58f, -0.45f, -2.0f)
          arcToRelative(5.64f, 5.64f, 0.0f, false, true, 1.36f, -4.37f)
          arcToRelative(0.45f, 0.45f, 0.0f, false, true, 0.13f, -0.11f)
          arcToRelative(1.29f, 1.29f, 0.0f, false, false, -0.21f, -0.73f)
          arcToRelative(6.94f, 6.94f, 0.0f, false, false, -2.24f, 0.54f)
          arcToRelative(23.15f, 23.15f, 0.0f, false, true, -7.35f, 1.4f)
          arcToRelative(10.06f, 10.06f, 0.0f, false, true, -7.93f, -3.31f)
          close()
          moveTo(179.15f, 227.8f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(169.86f, 237.3f)
          lineToRelative(9.46f, -9.0f)
          lineToRelative(-0.3f, -1.69f)
          curveToRelative(-1.14f, -0.41f, -4.79f, 1.89f, -9.77f, 1.89f)
          curveToRelative(-5.54f, 0.0f, -7.81f, -3.49f, -7.81f, -3.49f)
          lineToRelative(-2.26f, 1.44f)
          reflectiveCurveToRelative(3.29f, 8.63f, 4.11f, 10.07f)
          reflectiveCurveToRelative(4.11f, 7.4f, 9.25f, 9.45f)
          curveToRelative(0.52f, 0.21f, 1.0f, 0.39f, 1.48f, 0.54f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, fillAlpha = 0.5f,
          strokeAlpha = 0.5f, strokeLineWidth = 0.0f, strokeLineCap = Butt,
          strokeLineJoin = Miter, strokeLineMiter = 4.0f, pathFillType = NonZero
        ) {
          moveTo(169.86f, 237.3f)
          lineToRelative(9.46f, -9.0f)
          lineToRelative(-0.3f, -1.69f)
          curveToRelative(-1.14f, -0.41f, -4.79f, 1.89f, -9.77f, 1.89f)
          curveToRelative(-5.54f, 0.0f, -7.81f, -3.49f, -7.81f, -3.49f)
          lineToRelative(-2.26f, 1.44f)
          reflectiveCurveToRelative(3.29f, 8.63f, 4.11f, 10.07f)
          reflectiveCurveToRelative(4.11f, 7.4f, 9.25f, 9.45f)
          curveToRelative(0.52f, 0.21f, 1.0f, 0.39f, 1.48f, 0.54f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(150.76f, 219.68f)
          lineToRelative(4.31f, 5.55f)
          lineToRelative(5.55f, -2.26f)
          lineToRelative(-4.11f, -7.6f)
          lineToRelative(-5.75f, 4.31f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(155.07f, 225.77f)
          arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.42f, -0.2f)
          lineTo(150.33f, 220.0f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.11f, -0.41f)
          arcToRelative(0.58f, 0.58f, 0.0f, false, true, 0.21f, -0.36f)
          lineToRelative(5.76f, -4.31f)
          arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.44f, -0.1f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.36f, 0.27f)
          lineToRelative(4.11f, 7.6f)
          arcToRelative(0.59f, 0.59f, 0.0f, false, true, 0.0f, 0.45f)
          arcToRelative(0.51f, 0.51f, 0.0f, false, true, -0.3f, 0.31f)
          lineToRelative(-5.55f, 2.26f)
          arcTo(0.52f, 0.52f, 0.0f, false, true, 155.07f, 225.77f)
          close()
          moveTo(151.52f, 219.77f)
          lineTo(155.25f, 224.56f)
          lineTo(159.86f, 222.68f)
          lineTo(156.33f, 216.16f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(158.16f, 222.0f)
          reflectiveCurveToRelative(-6.17f, -8.23f, -6.0f, -10.69f)
          reflectiveCurveToRelative(13.05f, -11.81f, 16.55f, -11.19f)
          reflectiveCurveToRelative(9.0f, 11.08f, 9.0f, 11.08f)
          lineToRelative(3.09f, 3.09f)
          lineToRelative(-1.0f, 2.88f)
          reflectiveCurveToRelative(5.27f, 7.29f, 4.65f, 8.32f)
          reflectiveCurveToRelative(-5.34f, 3.9f, -11.1f, 5.14f)
          reflectiveCurveToRelative(-11.3f, -2.47f, -11.3f, -2.47f)
          reflectiveCurveToRelative(-3.9f, 1.64f, -5.55f, 0.62f)
          reflectiveCurveToRelative(-3.9f, -4.73f, -3.49f, -5.55f)
          reflectiveCurveTo(158.16f, 222.0f, 158.16f, 222.0f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(170.91f, 231.37f)
          arcToRelative(18.27f, 18.27f, 0.0f, false, true, -8.9f, -2.65f)
          curveToRelative(-1.0f, 0.38f, -4.16f, 1.48f, -5.78f, 0.47f)
          reflectiveCurveToRelative(-4.34f, -5.0f, -3.7f, -6.25f)
          curveToRelative(0.39f, -0.79f, 2.56f, -1.21f, 4.63f, -1.44f)
          curveToRelative(-1.6f, -2.2f, -5.69f, -8.07f, -5.5f, -10.29f)
          curveToRelative(0.12f, -1.5f, 3.33f, -4.18f, 5.23f, -5.65f)
          curveToRelative(3.0f, -2.29f, 9.29f, -6.5f, 12.0f, -6.0f)
          curveToRelative(3.58f, 0.63f, 8.61f, 9.9f, 9.34f, 11.29f)
          lineToRelative(3.0f, 3.0f)
          arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.12f, 0.57f)
          lineToRelative(-0.93f, 2.61f)
          curveToRelative(3.52f, 4.89f, 5.0f, 7.81f, 4.52f, 8.68f)
          curveToRelative(-0.72f, 1.19f, -5.6f, 4.13f, -11.45f, 5.39f)
          arcTo(12.66f, 12.66f, 0.0f, false, true, 170.91f, 231.37f)
          close()
          moveTo(162.06f, 227.57f)
          arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.3f, 0.09f)
          curveToRelative(0.06f, 0.0f, 5.43f, 3.56f, 10.89f, 2.39f)
          curveToRelative(5.89f, -1.26f, 10.29f, -4.13f, 10.75f, -4.89f)
          curveToRelative(0.07f, -0.68f, -2.34f, -4.56f, -4.63f, -7.72f)
          arcToRelative(0.53f, 0.53f, 0.0f, false, true, -0.07f, -0.5f)
          lineToRelative(0.91f, -2.56f)
          lineToRelative(-2.85f, -2.84f)
          arcToRelative(0.71f, 0.71f, 0.0f, false, true, -0.1f, -0.14f)
          curveToRelative(-1.49f, -2.84f, -6.0f, -10.33f, -8.61f, -10.8f)
          curveToRelative(-1.62f, -0.28f, -6.5f, 2.27f, -11.1f, 5.82f)
          curveToRelative(-3.24f, 2.49f, -4.76f, 4.28f, -4.81f, 4.88f)
          curveToRelative(-0.14f, 1.65f, 3.51f, 7.21f, 5.85f, 10.32f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.06f, 0.55f)
          arcToRelative(0.52f, 0.52f, 0.0f, false, true, -0.45f, 0.31f)
          curveToRelative(-2.25f, 0.2f, -4.42f, 0.64f, -4.72f, 1.0f)
          curveToRelative(-0.1f, 0.58f, 1.75f, 3.84f, 3.32f, 4.82f)
          curveToRelative(1.15f, 0.72f, 4.11f, -0.26f, 5.0f, -0.66f)
          arcTo(0.52f, 0.52f, 0.0f, false, true, 162.06f, 227.57f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(173.36f, 230.58f)
          arcToRelative(31.89f, 31.89f, 0.0f, false, false, 6.54f, -2.2f)
          arcToRelative(4.93f, 4.93f, 0.0f, false, true, -0.58f, -0.06f)
          curveToRelative(-2.43f, -0.49f, -9.46f, -2.07f, -12.18f, -5.47f)
          arcToRelative(28.37f, 28.37f, 0.0f, false, false, -4.08f, -4.43f)
          lineToRelative(15.22f, -6.73f)
          lineToRelative(-0.54f, -0.54f)
          reflectiveCurveToRelative(-5.5f, -10.46f, -9.0f, -11.08f)
          reflectiveCurveToRelative(-16.35f, 8.72f, -16.55f, 11.19f)
          reflectiveCurveToRelative(6.0f, 10.68f, 6.0f, 10.68f)
          reflectiveCurveToRelative(-4.73f, 0.42f, -5.14f, 1.24f)
          reflectiveCurveToRelative(1.85f, 4.52f, 3.49f, 5.55f)
          reflectiveCurveToRelative(5.55f, -0.62f, 5.55f, -0.62f)
          reflectiveCurveTo(167.61f, 231.81f, 173.36f, 230.58f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, fillAlpha = 0.5f,
          strokeAlpha = 0.5f, strokeLineWidth = 0.0f, strokeLineCap = Butt,
          strokeLineJoin = Miter, strokeLineMiter = 4.0f, pathFillType = NonZero
        ) {
          moveTo(173.36f, 230.58f)
          arcToRelative(31.89f, 31.89f, 0.0f, false, false, 6.54f, -2.2f)
          arcToRelative(4.93f, 4.93f, 0.0f, false, true, -0.58f, -0.06f)
          curveToRelative(-2.43f, -0.49f, -9.46f, -2.07f, -12.18f, -5.47f)
          arcToRelative(28.37f, 28.37f, 0.0f, false, false, -4.08f, -4.43f)
          lineToRelative(15.22f, -6.73f)
          lineToRelative(-0.54f, -0.54f)
          reflectiveCurveToRelative(-5.5f, -10.46f, -9.0f, -11.08f)
          reflectiveCurveToRelative(-16.35f, 8.72f, -16.55f, 11.19f)
          reflectiveCurveToRelative(6.0f, 10.68f, 6.0f, 10.68f)
          reflectiveCurveToRelative(-4.73f, 0.42f, -5.14f, 1.24f)
          reflectiveCurveToRelative(1.85f, 4.52f, 3.49f, 5.55f)
          reflectiveCurveToRelative(5.55f, -0.62f, 5.55f, -0.62f)
          reflectiveCurveTo(167.61f, 231.81f, 173.36f, 230.58f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(228.85f, 223.59f)
          lineToRelative(22.82f, 149.82f)
          lineToRelative(2.87f, -0.41f)
          lineToRelative(-22.0f, -149.62f)
          arcTo(5.38f, 5.38f, 0.0f, false, false, 228.85f, 223.59f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(251.67f, 374.0f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.54f, -0.46f)
          lineTo(228.32f, 223.67f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.35f, -0.59f)
          arcToRelative(5.82f, 5.82f, 0.0f, false, true, 4.1f, -0.19f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.32f, 0.42f)
          lineToRelative(22.0f, 149.61f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.1f, 0.41f)
          arcToRelative(0.58f, 0.58f, 0.0f, false, true, -0.36f, 0.21f)
          lineToRelative(-2.88f, 0.41f)
          close()
          moveTo(229.46f, 224.0f)
          lineTo(252.12f, 372.8f)
          lineToRelative(1.81f, -0.26f)
          lineTo(232.07f, 223.8f)
          arcTo(5.09f, 5.09f, 0.0f, false, false, 229.46f, 224.0f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(229.47f, 242.09f)
          reflectiveCurveToRelative(-4.52f, -1.0f, -4.52f, 1.64f)
          reflectiveCurveToRelative(-0.41f, 7.19f, 1.23f, 8.22f)
          reflectiveCurveToRelative(3.7f, 0.62f, 3.91f, -1.85f)
          reflectiveCurveTo(230.09f, 241.47f, 229.47f, 242.09f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(227.88f, 253.0f)
          arcToRelative(3.78f, 3.78f, 0.0f, false, true, -2.0f, -0.61f)
          curveToRelative(-1.65f, -1.0f, -1.58f, -4.37f, -1.51f, -7.31f)
          curveToRelative(0.0f, -0.49f, 0.0f, -0.95f, 0.0f, -1.37f)
          arcToRelative(2.1f, 2.1f, 0.0f, false, true, 0.78f, -1.71f)
          curveToRelative(1.23f, -1.0f, 3.51f, -0.64f, 4.22f, -0.5f)
          arcToRelative(0.65f, 0.65f, 0.0f, false, true, 0.41f, 0.0f)
          curveToRelative(1.2f, 0.55f, 0.81f, 8.51f, 0.81f, 8.59f)
          arcToRelative(3.0f, 3.0f, 0.0f, false, true, -1.53f, 2.59f)
          arcTo(2.67f, 2.67f, 0.0f, false, true, 227.88f, 253.0f)
          close()
          moveTo(227.65f, 242.41f)
          arcToRelative(3.0f, 3.0f, 0.0f, false, false, -1.79f, 0.43f)
          arcToRelative(1.0f, 1.0f, 0.0f, false, false, -0.37f, 0.87f)
          curveToRelative(0.0f, 0.42f, 0.0f, 0.9f, 0.0f, 1.39f)
          curveToRelative(-0.05f, 2.26f, -0.12f, 5.67f, 1.0f, 6.37f)
          arcToRelative(2.26f, 2.26f, 0.0f, false, false, 2.15f, 0.28f)
          arcToRelative(1.94f, 1.94f, 0.0f, false, false, 0.93f, -1.71f)
          arcToRelative(35.82f, 35.82f, 0.0f, false, false, -0.27f, -7.46f)
          arcTo(8.14f, 8.14f, 0.0f, false, false, 227.65f, 242.43f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(232.12f, 253.27f)
          reflectiveCurveToRelative(-0.16f, 3.23f, 0.46f, 3.55f)
          reflectiveCurveToRelative(8.91f, -0.32f, 9.22f, -1.0f)
          reflectiveCurveToRelative(0.16f, -3.23f, -0.46f, -3.39f)
          reflectiveCurveTo(232.89f, 252.47f, 232.12f, 253.27f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(233.76f, 257.45f)
          arcToRelative(4.16f, 4.16f, 0.0f, false, true, -1.43f, -0.15f)
          curveToRelative(-0.61f, -0.32f, -0.87f, -1.68f, -0.75f, -4.05f)
          arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.14f, -0.35f)
          curveToRelative(1.06f, -1.11f, 9.33f, -1.07f, 9.76f, -1.0f)
          arcToRelative(1.06f, 1.06f, 0.0f, false, true, 0.66f, 0.64f)
          arcToRelative(5.75f, 5.75f, 0.0f, false, true, 0.15f, 3.51f)
          arcToRelative(1.11f, 1.11f, 0.0f, false, true, -0.7f, 0.49f)
          arcTo(38.74f, 38.74f, 0.0f, false, true, 233.76f, 257.45f)
          close()
          moveTo(232.87f, 256.34f)
          arcToRelative(38.23f, 38.23f, 0.0f, false, false, 8.48f, -0.81f)
          arcToRelative(5.22f, 5.22f, 0.0f, false, false, -0.22f, -2.55f)
          curveToRelative(-1.0f, -0.13f, -7.24f, 0.1f, -8.48f, 0.6f)
          arcTo(8.71f, 8.71f, 0.0f, false, false, 232.87f, 256.34f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(229.68f, 249.07f)
          reflectiveCurveToRelative(-0.2f, 4.11f, 0.58f, 4.52f)
          reflectiveCurveToRelative(11.37f, -0.41f, 11.76f, -1.23f)
          reflectiveCurveToRelative(0.19f, -4.11f, -0.59f, -4.31f)
          reflectiveCurveTo(230.66f, 248.05f, 229.68f, 249.07f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(231.7f, 254.24f)
          arcToRelative(4.92f, 4.92f, 0.0f, false, true, -1.69f, -0.17f)
          curveToRelative(-0.9f, -0.47f, -1.0f, -3.0f, -0.88f, -5.0f)
          arcToRelative(0.56f, 0.56f, 0.0f, false, true, 0.15f, -0.35f)
          curveToRelative(1.27f, -1.33f, 11.75f, -1.32f, 12.29f, -1.18f)
          arcToRelative(1.18f, 1.18f, 0.0f, false, true, 0.74f, 0.73f)
          arcToRelative(7.31f, 7.31f, 0.0f, false, true, 0.2f, 4.34f)
          arcToRelative(1.29f, 1.29f, 0.0f, false, true, -0.81f, 0.56f)
          arcTo(51.6f, 51.6f, 0.0f, false, true, 231.7f, 254.24f)
          close()
          moveTo(230.21f, 249.36f)
          arcToRelative(9.84f, 9.84f, 0.0f, false, false, 0.34f, 3.78f)
          curveToRelative(0.89f, 0.26f, 9.84f, -0.44f, 11.0f, -1.09f)
          curveToRelative(0.27f, -0.77f, 0.05f, -3.17f, -0.33f, -3.51f)
          curveTo(240.26f, 248.34f, 231.66f, 248.66f, 230.21f, 249.36f)
          close()
          moveTo(241.61f, 252.02f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(229.27f, 244.55f)
          reflectiveCurveToRelative(-0.21f, 4.11f, 0.61f, 4.52f)
          reflectiveCurveToRelative(11.92f, -0.41f, 12.33f, -1.23f)
          reflectiveCurveToRelative(0.21f, -4.11f, -0.61f, -4.32f)
          reflectiveCurveTo(230.29f, 243.52f, 229.27f, 244.55f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(231.37f, 249.67f)
          arcToRelative(7.48f, 7.48f, 0.0f, false, true, -1.73f, -0.11f)
          curveToRelative(-0.94f, -0.47f, -1.0f, -3.0f, -0.92f, -5.0f)
          arcToRelative(0.57f, 0.57f, 0.0f, false, true, 0.16f, -0.36f)
          curveToRelative(1.33f, -1.33f, 12.29f, -1.31f, 12.85f, -1.17f)
          arcToRelative(1.24f, 1.24f, 0.0f, false, true, 0.79f, 0.77f)
          arcToRelative(7.0f, 7.0f, 0.0f, false, true, 0.18f, 4.31f)
          curveTo(242.17f, 249.15f, 234.82f, 249.67f, 231.37f, 249.67f)
          close()
          moveTo(229.79f, 244.85f)
          curveToRelative(-0.06f, 1.77f, 0.1f, 3.49f, 0.37f, 3.76f)
          curveToRelative(0.89f, 0.27f, 10.41f, -0.45f, 11.6f, -1.09f)
          curveToRelative(0.27f, -0.75f, 0.05f, -3.18f, -0.35f, -3.49f)
          curveTo(240.56f, 243.85f, 231.33f, 244.13f, 229.79f, 244.85f)
          close()
          moveTo(241.79f, 247.49f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(228.65f, 240.0f)
          reflectiveCurveToRelative(-0.21f, 4.11f, 0.62f, 4.52f)
          reflectiveCurveToRelative(11.92f, -0.41f, 12.33f, -1.23f)
          reflectiveCurveToRelative(0.2f, -4.11f, -0.62f, -4.32f)
          reflectiveCurveTo(229.68f, 239.0f, 228.65f, 240.0f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(230.75f, 245.15f)
          arcTo(7.54f, 7.54f, 0.0f, false, true, 229.0f, 245.0f)
          curveToRelative(-0.94f, -0.47f, -1.0f, -3.0f, -0.91f, -5.0f)
          arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.16f, -0.35f)
          curveToRelative(1.32f, -1.33f, 12.28f, -1.31f, 12.84f, -1.17f)
          arcToRelative(1.24f, 1.24f, 0.0f, false, true, 0.79f, 0.77f)
          arcToRelative(7.0f, 7.0f, 0.0f, false, true, 0.18f, 4.31f)
          curveTo(241.55f, 244.62f, 234.2f, 245.15f, 230.75f, 245.15f)
          close()
          moveTo(229.18f, 240.33f)
          arcToRelative(9.37f, 9.37f, 0.0f, false, false, 0.36f, 3.76f)
          curveToRelative(0.9f, 0.27f, 10.42f, -0.46f, 11.61f, -1.09f)
          curveToRelative(0.27f, -0.75f, 0.0f, -3.18f, -0.35f, -3.49f)
          curveTo(240.0f, 239.33f, 230.72f, 239.6f, 229.18f, 240.33f)
          close()
          moveTo(241.18f, 242.97f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(139.15f, 259.07f)
          curveToRelative(0.28f, 6.2f, 5.65f, 10.61f, 9.0f, 12.76f)
          arcToRelative(9.18f, 9.18f, 0.0f, false, false, 5.11f, 1.46f)
          lineTo(213.0f, 272.5f)
          verticalLineTo(259.35f)
          lineToRelative(-64.53f, -9.0f)
          reflectiveCurveToRelative(-5.13f, 0.2f, -6.51f, 4.06f)
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(153.12f, 273.83f)
          arcToRelative(9.67f, 9.67f, 0.0f, false, true, -5.28f, -1.55f)
          curveToRelative(-3.32f, -2.14f, -8.93f, -6.7f, -9.23f, -13.19f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.52f, -0.57f)
          arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.56f, 0.52f)
          curveToRelative(0.28f, 6.0f, 5.59f, 10.31f, 8.74f, 12.33f)
          arcToRelative(8.39f, 8.39f, 0.0f, false, false, 4.81f, 1.38f)
          lineToRelative(59.25f, -0.78f)
          verticalLineTo(259.82f)
          lineToRelative(-64.07f, -9.0f)
          curveToRelative(0.05f, 0.0f, -4.68f, 0.25f, -5.92f, 3.71f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.69f, 0.33f)
          arcToRelative(0.56f, 0.56f, 0.0f, false, true, -0.33f, -0.7f)
          curveToRelative(1.48f, -4.15f, 6.77f, -4.41f, 7.0f, -4.41f)
          lineToRelative(64.62f, 9.0f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.47f, 0.54f)
          verticalLineTo(272.5f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.53f, 0.54f)
          lineToRelative(-59.79f, 0.79f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(148.5f, 250.31f)
          reflectiveCurveToRelative(-5.13f, 0.2f, -6.51f, 4.06f)
          lineToRelative(-2.84f, 4.7f)
          curveToRelative(0.28f, 6.2f, 5.65f, 10.61f, 9.0f, 12.76f)
          arcToRelative(9.18f, 9.18f, 0.0f, false, false, 5.11f, 1.46f)
          lineTo(213.0f, 272.5f)
          verticalLineToRelative(-10.0f)
          curveToRelative(-1.24f, -1.42f, -2.42f, -2.72f, -3.23f, -3.61f)
          lineToRelative(-1.37f, -0.2f)
          lineToRelative(-3.07f, 10.54f)
          reflectiveCurveToRelative(-35.82f, -0.82f, -46.56f, -2.75f)
          reflectiveCurveToRelative(-14.05f, -4.13f, -14.33f, -9.09f)
          arcTo(7.68f, 7.68f, 0.0f, false, true, 148.5f, 250.31f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, fillAlpha = 0.5f,
          strokeAlpha = 0.5f, strokeLineWidth = 0.0f, strokeLineCap = Butt,
          strokeLineJoin = Miter, strokeLineMiter = 4.0f, pathFillType = NonZero
        ) {
          moveTo(148.5f, 250.31f)
          reflectiveCurveToRelative(-5.13f, 0.2f, -6.51f, 4.06f)
          lineToRelative(-2.84f, 4.7f)
          curveToRelative(0.28f, 6.2f, 5.65f, 10.61f, 9.0f, 12.76f)
          arcToRelative(9.18f, 9.18f, 0.0f, false, false, 5.11f, 1.46f)
          lineTo(213.0f, 272.5f)
          verticalLineToRelative(-10.0f)
          curveToRelative(-1.24f, -1.42f, -2.42f, -2.72f, -3.23f, -3.61f)
          lineToRelative(-1.37f, -0.2f)
          lineToRelative(-3.07f, 10.54f)
          reflectiveCurveToRelative(-35.82f, -0.82f, -46.56f, -2.75f)
          reflectiveCurveToRelative(-14.05f, -4.13f, -14.33f, -9.09f)
          arcTo(7.68f, 7.68f, 0.0f, false, true, 148.5f, 250.31f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(208.71f, 257.71f)
          lineTo(208.3f, 277.0f)
          lineTo(228.0f, 272.92f)
          reflectiveCurveToRelative(10.07f, 0.82f, 12.33f, 0.62f)
          reflectiveCurveToRelative(4.11f, -3.5f, 4.11f, -5.14f)
          verticalLineToRelative(-3.7f)
          arcToRelative(3.56f, 3.56f, 0.0f, false, false, 1.44f, -2.88f)
          curveToRelative(-0.2f, -1.64f, -3.49f, -2.67f, -3.49f, -2.67f)
          reflectiveCurveToRelative(-9.45f, -2.06f, -12.13f, -1.44f)
          reflectiveCurveTo(208.71f, 257.71f, 208.71f, 257.71f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(208.3f, 277.57f)
          arcToRelative(0.52f, 0.52f, 0.0f, false, true, -0.34f, -0.12f)
          arcToRelative(0.57f, 0.57f, 0.0f, false, true, -0.2f, -0.43f)
          lineToRelative(0.41f, -19.32f)
          arcToRelative(0.58f, 0.58f, 0.0f, false, true, 0.17f, -0.38f)
          arcToRelative(0.46f, 0.46f, 0.0f, false, true, 0.39f, -0.15f)
          curveToRelative(6.52f, 0.21f, 19.38f, 0.49f, 21.44f, 0.0f)
          curveToRelative(2.76f, -0.64f, 12.0f, 1.36f, 12.36f, 1.44f)
          reflectiveCurveToRelative(3.68f, 1.2f, 3.92f, 3.13f)
          arcTo(3.91f, 3.91f, 0.0f, false, true, 245.0f, 265.0f)
          verticalLineToRelative(3.45f)
          curveToRelative(0.0f, 1.8f, -2.0f, 5.43f, -4.61f, 5.68f)
          curveToRelative(-2.19f, 0.2f, -11.33f, -0.53f, -12.34f, -0.61f)
          lineToRelative(-19.66f, 4.09f)
          close()
          moveTo(209.24f, 258.27f)
          lineTo(208.86f, 276.36f)
          lineTo(227.92f, 272.36f)
          horizontalLineToRelative(0.16f)
          curveToRelative(0.1f, 0.0f, 10.0f, 0.81f, 12.23f, 0.62f)
          curveToRelative(1.95f, -0.18f, 3.62f, -3.2f, 3.62f, -4.6f)
          verticalLineToRelative(-3.7f)
          arcToRelative(0.53f, 0.53f, 0.0f, false, true, 0.22f, -0.43f)
          reflectiveCurveToRelative(1.39f, -1.08f, 1.23f, -2.38f)
          curveToRelative(-0.13f, -1.0f, -2.16f, -1.92f, -3.12f, -2.22f)
          curveToRelative(-2.55f, -0.55f, -9.75f, -1.92f, -11.84f, -1.43f)
          curveTo(227.93f, 258.81f, 212.43f, 258.37f, 209.24f, 258.27f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(148.09f, 222.15f)
          lineToRelative(34.52f, -15.82f)
          reflectiveCurveToRelative(1.44f, -1.0f, -0.82f, -1.24f)
          arcToRelative(35.93f, 35.93f, 0.0f, false, false, -6.78f, 0.62f)
          reflectiveCurveToRelative(-7.29f, -10.37f, -8.73f, -10.78f)
          reflectiveCurveToRelative(-2.26f, 0.0f, -2.46f, 0.83f)
          lineToRelative(-0.21f, 0.82f)
          reflectiveCurveToRelative(-11.82f, 3.37f, -16.35f, 9.33f)
          reflectiveCurveTo(148.09f, 222.15f, 148.09f, 222.15f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(148.09f, 222.69f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.48f, -0.29f)
          curveToRelative(-0.23f, -0.43f, -5.45f, -10.65f, -0.78f, -16.81f)
          curveToRelative(4.23f, -5.57f, 14.4f, -8.86f, 16.33f, -9.44f)
          lineToRelative(0.13f, -0.53f)
          arcToRelative(1.73f, 1.73f, 0.0f, false, true, 0.82f, -1.11f)
          arcToRelative(3.0f, 3.0f, 0.0f, false, true, 2.32f, -0.1f)
          curveToRelative(1.45f, 0.42f, 6.88f, 8.0f, 8.82f, 10.71f)
          arcToRelative(32.81f, 32.81f, 0.0f, false, true, 6.59f, -0.57f)
          curveToRelative(0.66f, 0.06f, 1.55f, 0.22f, 1.74f, 0.91f)
          reflectiveCurveToRelative(-0.51f, 1.21f, -0.65f, 1.31f)
          lineToRelative(-0.09f, 0.0f)
          lineToRelative(-34.53f, 15.82f)
          arcTo(0.44f, 0.44f, 0.0f, false, true, 148.09f, 222.69f)
          close()
          moveTo(165.26f, 195.31f)
          arcToRelative(1.16f, 1.16f, 0.0f, false, false, -0.6f, 0.14f)
          arcToRelative(0.65f, 0.65f, 0.0f, false, false, -0.32f, 0.44f)
          lineToRelative(-0.2f, 0.82f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.38f, 0.39f)
          curveToRelative(-0.12f, 0.0f, -11.71f, 3.4f, -16.06f, 9.14f)
          curveToRelative(-3.74f, 4.93f, -0.3f, 13.16f, 0.64f, 15.2f)
          lineToRelative(34.0f, -15.58f)
          lineToRelative(0.11f, -0.1f)
          arcToRelative(3.46f, 3.46f, 0.0f, false, false, -0.7f, -0.13f)
          arcToRelative(35.56f, 35.56f, 0.0f, false, false, -6.63f, 0.61f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.54f, -0.22f)
          curveToRelative(-3.49f, -5.0f, -7.6f, -10.26f, -8.44f, -10.57f)
          arcTo(3.39f, 3.39f, 0.0f, false, false, 165.26f, 195.31f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(164.76f, 212.0f)
          arcToRelative(0.56f, 0.56f, 0.0f, false, true, -0.46f, -0.25f)
          curveToRelative(-0.32f, -0.5f, -3.14f, -5.0f, -3.14f, -7.1f)
          curveToRelative(0.0f, -1.91f, 2.59f, -3.53f, 3.73f, -4.15f)
          lineToRelative(-1.29f, -2.58f)
          arcToRelative(0.54f, 0.54f, 0.0f, true, true, 1.0f, -0.48f)
          lineToRelative(1.54f, 3.06f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.0f, 0.42f)
          arcToRelative(0.5f, 0.5f, 0.0f, false, true, -0.28f, 0.31f)
          curveToRelative(-0.93f, 0.45f, -3.6f, 2.0f, -3.6f, 3.42f)
          curveToRelative(0.0f, 1.54f, 2.15f, 5.22f, 3.0f, 6.51f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.17f, 0.75f)
          arcTo(0.62f, 0.62f, 0.0f, false, true, 164.76f, 212.0f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(153.54f, 216.76f)
          arcToRelative(0.57f, 0.57f, 0.0f, false, true, -0.48f, -0.28f)
          curveToRelative(-0.11f, -0.21f, -2.78f, -5.09f, -2.78f, -7.75f)
          arcToRelative(8.14f, 8.14f, 0.0f, false, true, 2.0f, -4.95f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.76f, 0.0f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, 0.0f, 0.77f)
          arcToRelative(7.12f, 7.12f, 0.0f, false, false, -1.73f, 4.22f)
          curveToRelative(0.0f, 2.38f, 2.63f, 7.17f, 2.65f, 7.22f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.21f, 0.74f)
          arcTo(0.59f, 0.59f, 0.0f, false, true, 153.54f, 216.76f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(148.44f, 219.65f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.23f, -1.0f)
          lineToRelative(19.89f, -9.18f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, 0.46f, 1.0f)
          lineToRelative(-19.9f, 9.19f)
          arcTo(0.59f, 0.59f, 0.0f, false, true, 148.44f, 219.65f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(284.52f, 395.0f)
          curveToRelative(0.0f, -0.58f, 0.07f, -1.17f, 0.07f, -1.76f)
          curveToRelative(0.0f, -14.0f, -13.15f, -25.33f, -29.38f, -25.33f)
          reflectiveCurveToRelative(-29.39f, 11.34f, -29.39f, 25.33f)
          curveToRelative(0.0f, 0.59f, 0.0f, 1.18f, 0.08f, 1.76f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(284.52f, 395.57f)
          lineTo(225.9f, 395.57f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.54f, -0.5f)
          curveToRelative(-0.05f, -0.68f, -0.08f, -1.25f, -0.08f, -1.8f)
          curveToRelative(0.0f, -14.26f, 13.43f, -25.87f, 29.93f, -25.87f)
          reflectiveCurveToRelative(29.93f, 11.61f, 29.93f, 25.87f)
          curveToRelative(0.0f, 0.54f, 0.0f, 1.12f, -0.08f, 1.8f)
          arcTo(0.55f, 0.55f, 0.0f, false, true, 284.52f, 395.57f)
          close()
          moveTo(226.4f, 394.49f)
          lineTo(284.0f, 394.49f)
          curveToRelative(0.0f, -0.45f, 0.0f, -0.84f, 0.0f, -1.22f)
          curveToRelative(0.0f, -13.67f, -12.94f, -24.78f, -28.84f, -24.78f)
          reflectiveCurveToRelative(-28.85f, 11.11f, -28.85f, 24.78f)
          curveTo(226.36f, 393.65f, 226.38f, 394.05f, 226.4f, 394.49f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(284.52f, 397.44f)
          curveToRelative(0.0f, -0.59f, 0.07f, -1.17f, 0.07f, -1.76f)
          curveToRelative(0.0f, -14.0f, -13.15f, -25.33f, -29.38f, -25.33f)
          reflectiveCurveToRelative(-29.39f, 11.34f, -29.39f, 25.33f)
          curveToRelative(0.0f, 0.59f, 0.0f, 1.17f, 0.08f, 1.76f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(284.52f, 398.0f)
          lineTo(225.9f, 398.0f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.54f, -0.5f)
          curveToRelative(-0.05f, -0.68f, -0.08f, -1.26f, -0.08f, -1.8f)
          curveToRelative(0.0f, -14.27f, 13.43f, -25.87f, 29.93f, -25.87f)
          reflectiveCurveToRelative(29.93f, 11.6f, 29.93f, 25.87f)
          curveToRelative(0.0f, 0.54f, 0.0f, 1.11f, -0.08f, 1.8f)
          arcTo(0.55f, 0.55f, 0.0f, false, true, 284.52f, 398.0f)
          close()
          moveTo(226.4f, 396.91f)
          lineTo(284.0f, 396.91f)
          curveToRelative(0.0f, -0.44f, 0.0f, -0.84f, 0.0f, -1.21f)
          curveToRelative(0.0f, -13.67f, -12.94f, -24.79f, -28.84f, -24.79f)
          reflectiveCurveTo(226.36f, 382.0f, 226.36f, 395.68f)
          curveTo(226.36f, 396.06f, 226.38f, 396.45f, 226.4f, 396.89f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primaryContainer), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(255.21f, 370.35f)
          curveToRelative(-10.0f, 0.0f, -18.83f, 4.31f, -24.14f, 10.89f)
          lineToRelative(6.89f, 5.44f)
          arcToRelative(21.05f, 21.05f, 0.0f, false, true, 32.78f, 0.0f)
          lineToRelative(9.0f, -5.0f)
          curveTo(274.44f, 374.85f, 265.43f, 370.35f, 255.21f, 370.35f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(270.74f, 387.23f)
          arcToRelative(0.51f, 0.51f, 0.0f, false, true, -0.42f, -0.21f)
          arcToRelative(20.51f, 20.51f, 0.0f, false, false, -31.94f, 0.0f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.75f, 0.09f)
          lineToRelative(-6.9f, -5.45f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.2f, -0.36f)
          arcToRelative(0.58f, 0.58f, 0.0f, false, true, 0.12f, -0.4f)
          curveToRelative(5.6f, -6.94f, 14.78f, -11.09f, 24.56f, -11.09f)
          curveToRelative(10.0f, 0.0f, 19.35f, 4.32f, 24.92f, 11.54f)
          arcToRelative(0.58f, 0.58f, 0.0f, false, true, 0.1f, 0.44f)
          arcToRelative(0.54f, 0.54f, 0.0f, false, true, -0.27f, 0.37f)
          lineToRelative(-9.0f, 5.0f)
          arcTo(0.59f, 0.59f, 0.0f, false, true, 270.74f, 387.23f)
          close()
          moveTo(254.35f, 378.3f)
          arcTo(21.49f, 21.49f, 0.0f, false, true, 270.87f, 386.0f)
          lineToRelative(8.0f, -4.47f)
          curveToRelative(-5.41f, -6.67f, -14.2f, -10.63f, -23.67f, -10.63f)
          curveToRelative(-9.25f, 0.0f, -17.94f, 3.83f, -23.37f, 10.27f)
          lineToRelative(6.0f, 4.77f)
          arcTo(21.53f, 21.53f, 0.0f, false, true, 254.35f, 378.3f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(263.23f, 396.69f)
          arcToRelative(8.93f, 8.93f, 0.0f, false, false, -17.85f, 0.0f)
          curveToRelative(0.0f, 0.25f, 0.0f, 0.5f, 0.0f, 0.75f)
          horizontalLineToRelative(17.77f)
          curveTo(263.21f, 397.19f, 263.23f, 396.94f, 263.23f, 396.69f)
          close()
        }
        path(
          fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
          strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
          pathFillType = NonZero
        ) {
          moveTo(263.19f, 398.0f)
          lineTo(245.42f, 398.0f)
          arcToRelative(0.55f, 0.55f, 0.0f, false, true, -0.54f, -0.5f)
          curveToRelative(0.0f, -0.26f, 0.0f, -0.52f, 0.0f, -0.79f)
          arcToRelative(9.47f, 9.47f, 0.0f, false, true, 18.93f, 0.0f)
          curveToRelative(0.0f, 0.27f, 0.0f, 0.53f, 0.0f, 0.79f)
          arcTo(0.54f, 0.54f, 0.0f, false, true, 263.19f, 398.0f)
          close()
          moveTo(245.93f, 396.91f)
          horizontalLineToRelative(16.76f)
          verticalLineToRelative(-0.2f)
          arcToRelative(8.38f, 8.38f, 0.0f, false, false, -16.76f, 0.0f)
          close()
        }
      }
    }
      .build()
    return _loading!!
  }

private var _loading: ImageVector? = null

@Preview
@Composable
private fun Preview(): Unit {
  Box(modifier = Modifier.padding(12.dp)) {
    Image(imageVector = Illustration.Loading, contentDescription = "")
  }
}
