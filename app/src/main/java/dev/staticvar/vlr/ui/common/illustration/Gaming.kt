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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.ui.common.Illustration
import dev.staticvar.vlr.ui.theme.VLRTheme

public val Illustration.Gaming: ImageVector
    @Composable get() {
        if (_gaming != null) {
            return _gaming!!
        }
        _gaming = Builder(
            name = "Gaming", defaultWidth = 500.0.dp, defaultHeight = 500.0.dp,
            viewportWidth = 500.0f, viewportHeight = 500.0f
        ).apply {
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(420.57f, 323.71f)
                curveToRelative(-10.39f, 17.57f, -43.78f, 24.0f, -61.46f, 22.89f)
                curveToRelative(-20.34f, -1.29f, -36.73f, -13.75f, -50.93f, -26.0f)
                curveToRelative(-17.92f, -15.41f, -29.24f, -23.67f, -55.0f, -19.52f)
                curveToRelative(-22.69f, 3.65f, -44.22f, 11.59f, -67.25f, 13.0f)
                curveToRelative(-47.1f, 3.0f, -101.45f, -4.19f, -114.6f, -52.12f)
                curveToRelative(-12.06f, -43.95f, 14.44f, -88.0f, 66.65f, -87.82f)
                curveToRelative(44.09f, 0.17f, 44.0f, 55.0f, 92.33f, 37.42f)
                curveToRelative(45.0f, -16.41f, 52.0f, -79.75f, 107.87f, -74.91f)
                curveToRelative(55.54f, 4.83f, 19.48f, 70.21f, 21.0f, 105.71f)
                curveToRelative(1.0f, 22.78f, 27.39f, 12.51f, 44.85f, 21.87f)
                curveTo(425.13f, 275.6f, 433.72f, 301.48f, 420.57f, 323.71f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer.copy(0.9f)),
                stroke = null,
                fillAlpha = 0.9f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(420.57f, 323.71f)
                curveToRelative(-10.39f, 17.57f, -43.78f, 24.0f, -61.46f, 22.89f)
                curveToRelative(-20.34f, -1.29f, -36.73f, -13.75f, -50.93f, -26.0f)
                curveToRelative(-17.92f, -15.41f, -29.24f, -23.67f, -55.0f, -19.52f)
                curveToRelative(-22.69f, 3.65f, -44.22f, 11.59f, -67.25f, 13.0f)
                curveToRelative(-47.1f, 3.0f, -101.45f, -4.19f, -114.6f, -52.12f)
                curveToRelative(-12.06f, -43.95f, 14.44f, -88.0f, 66.65f, -87.82f)
                curveToRelative(44.09f, 0.17f, 44.0f, 55.0f, 92.33f, 37.42f)
                curveToRelative(45.0f, -16.41f, 52.0f, -79.75f, 107.87f, -74.91f)
                curveToRelative(55.54f, 4.83f, 19.48f, 70.21f, 21.0f, 105.71f)
                curveToRelative(1.0f, 22.78f, 27.39f, 12.51f, 44.85f, 21.87f)
                curveTo(425.13f, 275.6f, 433.72f, 301.48f, 420.57f, 323.71f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(56.11f, 416.24f)
                arcToRelative(193.89f, 11.32f, 0.0f, true, false, 387.78f, 0.0f)
                arcToRelative(193.89f, 11.32f, 0.0f, true, false, -387.78f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(101.83f, 286.51f)
                horizontalLineToRelative(231.54f)
                verticalLineToRelative(3.71f)
                horizontalLineToRelative(-231.54f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(383.61f, 290.22f)
                lineToRelative(-50.24f, -0.0f)
                lineToRelative(-0.0f, -3.71f)
                lineToRelative(50.24f, -0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.2f,
                strokeAlpha = 0.2f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(383.61f, 290.22f)
                lineToRelative(-50.24f, -0.0f)
                lineToRelative(-0.0f, -3.71f)
                lineToRelative(50.24f, -0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(366.83f, 294.19f)
                lineToRelative(-248.22f, 0.0f)
                lineToRelative(-8.3f, -3.97f)
                lineToRelative(264.82f, 0.0f)
                lineToRelative(-8.3f, 3.97f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.6f,
                strokeAlpha = 0.6f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(366.83f, 294.19f)
                lineToRelative(-248.22f, 0.0f)
                lineToRelative(-8.3f, -3.97f)
                lineToRelative(264.82f, 0.0f)
                lineToRelative(-8.3f, 3.97f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(126.11f, 294.19f)
                lineToRelative(0.0f, 118.34f)
                lineToRelative(3.18f, 0.0f)
                lineToRelative(4.0f, -118.34f)
                lineToRelative(-7.18f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(163.65f, 294.19f)
                lineToRelative(0.0f, 118.34f)
                lineToRelative(3.18f, 0.0f)
                lineToRelative(4.0f, -118.34f)
                lineToRelative(-7.18f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(366.83f, 294.19f)
                lineToRelative(0.0f, 118.34f)
                lineToRelative(-3.17f, 0.0f)
                lineToRelative(-4.01f, -118.34f)
                lineToRelative(7.18f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(329.3f, 294.19f)
                lineToRelative(0.0f, 118.34f)
                lineToRelative(-3.18f, 0.0f)
                lineToRelative(-4.0f, -118.34f)
                lineToRelative(7.18f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(253.74f, 241.59f)
                lineToRelative(-10.45f, 42.57f)
                lineToRelative(0.0f, 0.0f)
                lineToRelative(-0.29f, 2.35f)
                lineToRelative(72.08f, 0.0f)
                lineToRelative(0.3f, -2.35f)
                lineToRelative(10.44f, -42.57f)
                lineToRelative(-72.08f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.3f,
                strokeAlpha = 0.3f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(253.74f, 241.59f)
                lineToRelative(-10.45f, 42.57f)
                lineToRelative(0.0f, 0.0f)
                lineToRelative(-0.29f, 2.35f)
                lineToRelative(72.08f, 0.0f)
                lineToRelative(0.3f, -2.35f)
                lineToRelative(10.44f, -42.57f)
                lineToRelative(-72.08f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(323.61f, 241.59f)
                lineToRelative(-72.09f, 0.0f)
                lineToRelative(-10.44f, 42.57f)
                lineToRelative(72.08f, 0.0f)
                lineToRelative(10.45f, -42.57f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(311.63f, 282.21f)
                lineToRelative(9.5f, -38.67f)
                lineToRelative(-68.08f, 0.0f)
                lineToRelative(-9.49f, 38.67f)
                lineToRelative(68.07f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(294.38f, 284.16f)
                lineToRelative(-0.3f, 2.35f)
                lineToRelative(-79.37f, 0.0f)
                lineToRelative(0.29f, -2.35f)
                lineToRelative(79.38f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.6f,
                strokeAlpha = 0.6f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(294.38f, 284.16f)
                lineToRelative(-0.3f, 2.35f)
                lineToRelative(-79.37f, 0.0f)
                lineToRelative(0.29f, -2.35f)
                lineToRelative(79.38f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(144.44f, 278.77f)
                arcToRelative(6.35f, 6.35f, 0.0f, false, false, -5.63f, -6.1f)
                verticalLineToRelative(-1.15f)
                arcToRelative(0.5f, 0.5f, 0.0f, false, false, -0.52f, -0.49f)
                horizontalLineToRelative(-10.8f)
                arcToRelative(0.51f, 0.51f, 0.0f, false, false, -0.53f, 0.49f)
                verticalLineToRelative(15.0f)
                horizontalLineToRelative(11.85f)
                verticalLineToRelative(-1.64f)
                arcTo(6.34f, 6.34f, 0.0f, false, false, 144.44f, 278.77f)
                close()
                moveTo(138.81f, 282.94f)
                verticalLineToRelative(-8.33f)
                arcToRelative(4.21f, 4.21f, 0.0f, false, true, 0.0f, 8.33f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(346.56f, 286.51f)
                lineToRelative(10.04f, 0.0f)
                lineToRelative(-2.97f, -21.99f)
                lineToRelative(-7.07f, 0.0f)
                lineToRelative(0.0f, 21.99f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(331.84f, 286.51f)
                lineToRelative(19.02f, 0.0f)
                lineToRelative(3.53f, -25.74f)
                lineToRelative(-19.01f, 0.0f)
                lineToRelative(-3.54f, 25.74f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.3f,
                strokeAlpha
                = 0.3f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(331.84f, 286.51f)
                lineToRelative(19.02f, 0.0f)
                lineToRelative(3.53f, -25.74f)
                lineToRelative(-19.01f, 0.0f)
                lineToRelative(-3.54f, 25.74f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(329.3f, 286.51f)
                lineToRelative(19.79f, 0.0f)
                lineToRelative(3.54f, -25.74f)
                lineToRelative(-19.79f, 0.0f)
                lineToRelative(-3.54f, 25.74f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.4f,
                strokeAlpha = 0.4f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(347.34f, 284.48f)
                lineToRelative(2.98f, -21.68f)
                lineToRelative(-15.73f, 0.0f)
                lineToRelative(-2.98f, 21.68f)
                lineToRelative(15.73f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(197.54f, 248.51f)
                lineToRelative(-0.82f, 0.88f)
                lineToRelative(-0.88f, 1.0f)
                curveToRelative(-0.59f, 0.68f, -1.18f, 1.37f, -1.74f, 2.08f)
                curveToRelative(-1.12f, 1.42f, -2.2f, 2.87f, -3.21f, 4.36f)
                arcToRelative(46.61f, 46.61f, 0.0f, false, false, -5.0f, 9.24f)
                arcToRelative(28.59f, 28.59f, 0.0f, false, false, -1.42f, 4.78f)
                curveToRelative(-0.16f, 0.92f, -0.26f, 1.32f, -0.31f, 1.71f)
                arcToRelative(2.68f, 2.68f, 0.0f, false, false, 0.0f, 0.94f)
                arcToRelative(3.9f, 3.9f, 0.0f, false, false, 2.0f, 2.25f)
                arcToRelative(14.0f, 14.0f, 0.0f, false, false, 2.12f, 1.07f)
                quadToRelative(0.58f, 0.24f, 1.2f, 0.45f)
                curveToRelative(0.38f, 0.14f, 0.86f, 0.28f, 1.16f, 0.4f)
                verticalLineToRelative(5.51f)
                curveToRelative(-0.7f, 0.1f, -1.21f, 0.18f, -1.8f, 0.22f)
                reflectiveCurveToRelative(-1.16f, 0.05f, -1.75f, 0.0f)
                arcToRelative(17.0f, 17.0f, 0.0f, false, true, -3.6f, -0.46f)
                arcToRelative(13.49f, 13.49f, 0.0f, false, true, -3.78f, -1.52f)
                arcToRelative(10.94f, 10.94f, 0.0f, false, true, -3.48f, -3.23f)
                arcToRelative(11.67f, 11.67f, 0.0f, false, true, -1.82f, -4.66f)
                arcToRelative(9.89f, 9.89f, 0.0f, false, true, -0.13f, -1.18f)
                arcToRelative(10.94f, 10.94f, 0.0f, false, true, 0.0f, -1.14f)
                verticalLineToRelative(-0.56f)
                arcToRelative(3.0f, 3.0f, 0.0f, false, true, 0.0f, -0.45f)
                lineToRelative(0.08f, -0.88f)
                arcToRelative(34.7f, 34.7f, 0.0f, false, true, 1.44f, -6.73f)
                arcToRelative(49.94f, 49.94f, 0.0f, false, true, 5.67f, -11.92f)
                arcToRelative(57.63f, 57.63f, 0.0f, false, true, 3.77f, -5.25f)
                curveToRelative(0.67f, -0.84f, 1.36f, -1.66f, 2.08f, -2.47f)
                curveToRelative(0.35f, -0.41f, 0.72f, -0.81f, 1.1f, -1.21f)
                reflectiveCurveToRelative(0.73f, -0.75f, 1.25f, -1.25f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(188.65f, 277.55f)
                lineToRelative(4.66f, -3.42f)
                lineToRelative(2.65f, 9.58f)
                reflectiveCurveToRelative(-4.13f, 1.53f, -7.28f, -0.53f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(199.78f, 271.83f)
                lineToRelative(1.82f, 8.02f)
                lineToRelative(-5.64f, 3.86f)
                lineToRelative(-2.65f, -9.58f)
                lineToRelative(6.47f, -2.3f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(229.25f, 211.75f)
                curveToRelative(-0.21f, 0.59f, -0.06f, 1.18f, 0.33f, 1.31f)
                reflectiveCurveToRelative(0.87f, -0.23f, 1.07f, -0.82f)
                reflectiveCurveToRelative(0.06f, -1.19f, -0.32f, -1.32f)
                reflectiveCurveTo(229.46f, 211.15f, 229.25f, 211.75f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(228.29f, 207.73f)
                arcToRelative(0.36f, 0.36f, 0.0f, false, false, 0.23f, 0.0f)
                arcToRelative(2.92f, 2.92f, 0.0f, false, true, 2.63f, 0.4f)
                arcToRelative(0.37f, 0.37f, 0.0f, false, false, 0.48f, -0.56f)
                arcToRelative(3.67f, 3.67f, 0.0f, false, false, -3.32f, -0.55f)
                arcToRelative(0.37f, 0.37f, 0.0f, false, false, 0.0f, 0.7f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(207.32f, 213.93f)
                curveToRelative(1.12f, 5.69f, 2.23f, 16.12f, -1.75f, 19.92f)
                curveToRelative(0.0f, 0.0f, 1.56f, 5.77f, 12.14f, 5.77f)
                curveToRelative(11.63f, 0.0f, 5.56f, -5.77f, 5.56f, -5.77f)
                curveToRelative(-6.35f, -1.52f, -6.19f, -6.23f, -5.08f, -10.65f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(204.05f, 235.83f)
                curveToRelative(-0.84f, -1.83f, -1.75f, -3.92f, 0.14f, -4.57f)
                curveToRelative(2.09f, -0.72f, 13.53f, -1.46f, 17.47f, -0.11f)
                arcToRelative(4.06f, 4.06f, 0.0f, false, true, 2.83f, 5.06f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(244.0f, 245.59f)
                curveToRelative(2.45f, 4.41f, 5.08f, 8.95f, 7.74f, 13.31f)
                reflectiveCurveToRelative(5.45f, 8.7f, 8.43f, 12.66f)
                curveToRelative(0.74f, 1.0f, 1.51f, 1.95f, 2.27f, 2.9f)
                curveToRelative(0.38f, 0.49f, 0.78f, 0.89f, 1.17f, 1.34f)
                lineToRelative(0.58f, 0.66f)
                lineToRelative(0.15f, 0.17f)
                lineToRelative(0.07f, 0.08f)
                lineToRelative(0.0f, 0.0f)
                lineToRelative(0.07f, 0.09f)
                arcToRelative(4.38f, 4.38f, 0.0f, false, false, 1.64f, 1.0f)
                arcToRelative(15.7f, 15.7f, 0.0f, false, false, 2.91f, 0.71f)
                arcToRelative(65.22f, 65.22f, 0.0f, false, false, 14.61f, -0.17f)
                lineToRelative(1.52f, 5.3f)
                curveToRelative(-1.35f, 0.6f, -2.61f, 1.0f, -3.95f, 1.5f)
                reflectiveCurveToRelative(-2.66f, 0.83f, -4.0f, 1.17f)
                arcToRelative(38.86f, 38.86f, 0.0f, false, true, -8.68f, 1.16f)
                arcToRelative(21.23f, 21.23f, 0.0f, false, true, -4.89f, -0.49f)
                arcToRelative(13.49f, 13.49f, 0.0f, false, true, -5.44f, -2.42f)
                lineToRelative(-0.67f, -0.55f)
                lineToRelative(-0.16f, -0.14f)
                lineToRelative(-0.1f, -0.1f)
                lineToRelative(-0.19f, -0.19f)
                lineToRelative(-0.78f, -0.77f)
                curveToRelative(-0.51f, -0.51f, -1.07f, -1.0f, -1.53f, -1.54f)
                curveToRelative(-1.0f, -1.05f, -1.94f, -2.1f, -2.81f, -3.18f)
                arcToRelative(129.93f, 129.93f, 0.0f, false, true, -9.65f, -13.29f)
                quadToRelative(-2.19f, -3.42f, -4.22f, -6.91f)
                curveToRelative(-1.36f, -2.35f, -2.67f, -4.65f, -3.94f, -7.13f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(191.64f, 235.61f)
                curveToRelative(-7.3f, 1.67f, -15.66f, 21.56f, -15.66f, 21.56f)
                lineToRelative(14.65f, 10.0f)
                arcToRelative(123.23f, 123.23f, 0.0f, false, false, 9.67f, -16.45f)
                curveTo(204.63f, 241.66f, 199.1f, 233.9f, 191.64f, 235.61f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.2f,
                strokeAlpha = 0.2f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(196.86f, 248.29f)
                lineToRelative(-6.92f, 18.44f)
                lineToRelative(0.68f, 0.48f)
                reflectiveCurveToRelative(3.0f, -4.21f, 6.24f, -9.89f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(235.24f, 236.0f)
                reflectiveCurveToRelative(8.28f, 9.36f, 0.46f, 67.26f)
                horizontalLineTo(194.07f)
                curveToRelative(-0.28f, -6.39f, 3.73f, -37.55f, -2.43f, -67.66f)
                arcToRelative(108.85f, 108.85f, 0.0f, false, true, 13.93f, -1.76f)
                arcToRelative(151.37f, 151.37f, 0.0f, false, true, 17.7f, 0.0f)
                arcTo(77.87f, 77.87f, 0.0f, false, true, 235.24f, 236.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.2f,
                strokeAlpha
                = 0.2f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(230.61f, 252.75f)
                arcToRelative(37.0f, 37.0f, 0.0f, false, false, 8.22f, 17.82f)
                curveToRelative(0.17f, -3.73f, 0.24f, -7.08f, 0.23f, -10.07f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(227.0f, 242.32f)
                curveToRelative(-1.0f, 7.37f, 13.5f, 26.19f, 13.5f, 26.19f)
                lineToRelative(14.59f, -10.0f)
                reflectiveCurveToRelative(-4.05f, -10.37f, -10.86f, -17.17f)
                curveTo(236.47f, 233.54f, 228.14f, 234.3f, 227.0f, 242.32f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(282.0f, 278.39f)
                lineToRelative(5.94f, -2.44f)
                lineToRelative(6.11f, 4.62f)
                reflectiveCurveToRelative(-5.83f, 5.16f, -11.13f, 3.15f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(291.86f, 275.71f)
                lineToRelative(4.48f, 3.2f)
                lineToRelative(-2.32f, 1.66f)
                lineToRelative(-6.11f, -4.62f)
                lineToRelative(3.95f, -0.24f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(209.91f, 203.61f)
                curveToRelative(-2.08f, 7.44f, -3.68f, 11.69f, -1.3f, 16.7f)
                curveToRelative(3.57f, 7.55f, 13.72f, 7.34f, 18.0f, 0.68f)
                curveToRelative(3.84f, -6.0f, 6.6f, -17.16f, 0.48f, -22.53f)
                arcTo(10.54f, 10.54f, 0.0f, false, false, 209.91f, 203.61f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(225.75f, 203.69f)
                curveToRelative(-3.0f, 8.54f, -6.53f, 17.92f, -15.15f, 16.14f)
                curveToRelative(-5.56f, -1.15f, -6.0f, -10.1f, -8.0f, -14.17f)
                curveToRelative(-1.77f, -3.57f, -1.66f, -10.75f, 2.0f, -10.84f)
                curveToRelative(-1.64f, -2.38f, 6.65f, -11.09f, 12.31f, -6.49f)
                curveToRelative(1.45f, -2.0f, 11.9f, -7.89f, 19.64f, 1.46f)
                curveTo(243.84f, 198.63f, 240.27f, 206.0f, 225.75f, 203.69f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(219.0f, 209.15f)
                arcToRelative(8.8f, 8.8f, 0.0f, false, false, 1.22f, 5.47f)
                curveToRelative(1.33f, 2.14f, 3.28f, 1.32f, 4.0f, -0.87f)
                curveToRelative(0.64f, -2.0f, 0.71f, -5.45f, -1.14f, -6.84f)
                reflectiveCurveTo(219.28f, 206.85f, 219.0f, 209.15f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(282.88f, 406.88f)
                curveToRelative(1.0f, 0.0f, 2.0f, -0.14f, 2.29f, -0.59f)
                arcToRelative(0.71f, 0.71f, 0.0f, false, false, 0.0f, -0.81f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, false, -0.58f, -0.43f)
                curveToRelative(-1.3f, -0.35f, -4.0f, 1.2f, -4.13f, 1.27f)
                arcToRelative(0.17f, 0.17f, 0.0f, false, false, -0.09f, 0.19f)
                arcToRelative(0.19f, 0.19f, 0.0f, false, false, 0.14f, 0.15f)
                arcTo(13.6f, 13.6f, 0.0f, false, false, 282.88f, 406.88f)
                close()
                moveTo(284.12f, 405.37f)
                arcToRelative(1.25f, 1.25f, 0.0f, false, true, 0.35f, 0.0f)
                arcToRelative(0.52f, 0.52f, 0.0f, false, true, 0.36f, 0.26f)
                curveToRelative(0.13f, 0.23f, 0.09f, 0.34f, 0.0f, 0.4f)
                curveToRelative(-0.36f, 0.51f, -2.33f, 0.52f, -3.78f, 0.31f)
                arcTo(8.35f, 8.35f, 0.0f, false, true, 284.12f, 405.37f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(280.53f, 406.66f)
                lineToRelative(0.08f, 0.0f)
                curveToRelative(0.94f, -0.42f, 2.8f, -2.1f, 2.65f, -3.0f)
                curveToRelative(0.0f, -0.21f, -0.18f, -0.46f, -0.68f, -0.52f)
                arcToRelative(1.36f, 1.36f, 0.0f, false, false, -1.0f, 0.32f)
                curveToRelative(-1.0f, 0.81f, -1.2f, 2.91f, -1.21f, 3.0f)
                arcToRelative(0.19f, 0.19f, 0.0f, false, false, 0.07f, 0.17f)
                arcTo(0.23f, 0.23f, 0.0f, false, false, 280.53f, 406.66f)
                close()
                moveTo(282.43f, 403.51f)
                horizontalLineToRelative(0.11f)
                curveToRelative(0.33f, 0.0f, 0.35f, 0.17f, 0.36f, 0.21f)
                curveToRelative(0.09f, 0.53f, -1.18f, 1.85f, -2.14f, 2.42f)
                arcToRelative(4.3f, 4.3f, 0.0f, false, true, 1.0f, -2.39f)
                arcTo(1.0f, 1.0f, 0.0f, false, true, 282.43f, 403.51f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(271.48f, 406.48f)
                lineToRelative(7.78f, 0.0f)
                lineToRelative(1.77f, -18.03f)
                lineToRelative(-7.78f, 0.0f)
                lineToRelative(-1.77f, 18.03f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(279.7f, 405.58f)
                horizontalLineToRelative(-8.49f)
                arcToRelative(0.69f, 0.69f, 0.0f, false, false, -0.67f, 0.54f)
                lineTo(269.0f, 413.0f)
                arcToRelative(1.13f, 1.13f, 0.0f, false, false, 1.12f, 1.39f)
                curveToRelative(3.06f, -0.05f, 7.49f, -0.23f, 11.34f, -0.23f)
                curveToRelative(4.51f, 0.0f, 8.4f, 0.24f, 13.68f, 0.24f)
                curveToRelative(3.2f, 0.0f, 4.09f, -3.23f, 2.75f, -3.52f)
                curveToRelative(-6.09f, -1.33f, -11.06f, -1.47f, -16.32f, -4.72f)
                arcTo(3.61f, 3.61f, 0.0f, false, false, 279.7f, 405.58f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.2f,
                strokeAlpha
                = 0.2f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(281.02f, 388.46f)
                lineToRelative(-0.9f, 9.29f)
                lineToRelative(-7.79f, 0.0f)
                lineToRelative(0.91f, -9.29f)
                lineToRelative(7.78f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(251.69f, 406.87f)
                arcToRelative(2.51f, 2.51f, 0.0f, false, false, 1.87f, -0.51f)
                arcToRelative(0.74f, 0.74f, 0.0f, false, false, 0.0f, -0.87f)
                arcToRelative(0.85f, 0.85f, 0.0f, false, false, -0.58f, -0.43f)
                curveToRelative(-1.14f, -0.27f, -3.29f, 1.2f, -3.38f, 1.26f)
                arcToRelative(0.21f, 0.21f, 0.0f, false, false, -0.08f, 0.19f)
                arcToRelative(0.18f, 0.18f, 0.0f, false, false, 0.14f, 0.14f)
                arcTo(9.58f, 9.58f, 0.0f, false, false, 251.69f, 406.87f)
                close()
                moveTo(252.69f, 405.39f)
                arcToRelative(0.78f, 0.78f, 0.0f, false, true, 0.22f, 0.0f)
                arcToRelative(0.46f, 0.46f, 0.0f, false, true, 0.34f, 0.24f)
                curveToRelative(0.17f, 0.31f, 0.07f, 0.43f, 0.0f, 0.48f)
                curveToRelative(-0.33f, 0.43f, -1.92f, 0.44f, -3.09f, 0.25f)
                arcTo(6.0f, 6.0f, 0.0f, false, true, 252.67f, 405.39f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(249.71f, 406.66f)
                lineToRelative(0.08f, 0.0f)
                curveToRelative(0.81f, -0.44f, 2.37f, -2.14f, 2.2f, -3.0f)
                arcToRelative(0.6f, 0.6f, 0.0f, false, false, -0.59f, -0.49f)
                arcToRelative(1.0f, 1.0f, 0.0f, false, false, -0.78f, 0.24f)
                curveToRelative(-0.9f, 0.74f, -1.09f, 3.0f, -1.1f, 3.08f)
                arcToRelative(0.2f, 0.2f, 0.0f, false, false, 0.09f, 0.17f)
                arcTo(0.17f, 0.17f, 0.0f, false, false, 249.71f, 406.66f)
                close()
                moveTo(251.28f, 403.51f)
                horizontalLineToRelative(0.08f)
                curveToRelative(0.24f, 0.0f, 0.26f, 0.14f, 0.27f, 0.19f)
                curveToRelative(0.1f, 0.51f, -0.9f, 1.8f, -1.7f, 2.41f)
                arcToRelative(4.41f, 4.41f, 0.0f, false, true, 0.92f, -2.44f)
                arcTo(0.66f, 0.66f, 0.0f, false, true, 251.28f, 403.51f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primaryContainer),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(240.79f, 406.48f)
                lineToRelative(7.78f, 0.0f)
                lineToRelative(3.91f, -18.03f)
                lineToRelative(-7.78f, 0.0f)
                lineToRelative(-3.91f, 18.03f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(249.12f, 405.58f)
                horizontalLineToRelative(-8.74f)
                arcToRelative(0.68f, 0.68f, 0.0f, false, false, -0.66f, 0.54f)
                lineTo(238.18f, 413.0f)
                arcToRelative(1.15f, 1.15f, 0.0f, false, false, 1.14f, 1.39f)
                curveToRelative(3.05f, -0.05f, 4.54f, -0.23f, 8.38f, -0.23f)
                curveToRelative(2.37f, 0.0f, 5.4f, 0.24f, 8.67f, 0.24f)
                reflectiveCurveToRelative(3.69f, -3.23f, 2.35f, -3.52f)
                curveToRelative(-6.0f, -1.31f, -6.41f, -3.12f, -8.33f, -4.85f)
                arcTo(1.9f, 1.9f, 0.0f, false, false, 249.12f, 405.58f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.2f,
                strokeAlpha
                = 0.2f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(244.7f, 388.46f)
                lineToRelative(7.78f, 0.0f)
                lineToRelative(-2.02f, 9.29f)
                lineToRelative(-7.78f, 0.0f)
                lineToRelative(2.02f, -9.29f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(235.7f, 303.27f)
                reflectiveCurveToRelative(58.3f, 6.17f, 57.66f, 35.91f)
                arcToRelative(164.52f, 164.52f, 0.0f, false, true, -10.62f, 54.62f)
                horizontalLineTo(271.18f)
                reflectiveCurveToRelative(0.59f, -18.87f, 0.2f, -49.32f)
                curveToRelative(-0.12f, -9.13f, -18.55f, -12.14f, -36.56f, -13.2f)
                arcToRelative(41.0f, 41.0f, 0.0f, false, true, -35.24f, -25.07f)
                lineToRelative(-1.23f, -2.94f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(285.57f, 390.7f)
                curveToRelative(0.06f, 0.0f, -0.75f, 5.47f, -0.75f, 5.47f)
                horizontalLineToRelative(-16.0f)
                lineToRelative(-0.44f, -4.88f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.secondary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(223.22f, 303.27f)
                reflectiveCurveToRelative(42.26f, 14.0f, 43.33f, 31.36f)
                curveToRelative(2.0f, 31.94f, -13.71f, 59.17f, -13.71f, 59.17f)
                horizontalLineTo(241.17f)
                reflectiveCurveToRelative(4.43f, -13.5f, 6.0f, -42.44f)
                curveToRelative(2.0f, -36.35f, -65.83f, -0.06f, -53.12f, -48.09f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(256.37f, 390.7f)
                curveToRelative(0.06f, 0.0f, -1.47f, 5.47f, -1.47f, 5.47f)
                horizontalLineToRelative(-16.0f)
                lineToRelative(0.29f, -4.88f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(253.7f, 333.89f)
                horizontalLineToRelative(-65.0f)
                arcToRelative(7.42f, 7.42f, 0.0f, false, true, 7.42f, -7.42f)
                horizontalLineToRelative(50.16f)
                arcToRelative(7.42f, 7.42f, 0.0f, false, true, 7.42f, 7.42f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(191.71f, 333.88f)
                lineToRelative(-9.8f, 82.16f)
                lineToRelative(2.1f, 0.0f)
                lineToRelative(12.43f, -82.16f)
                lineToRelative(-4.73f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.4f,
                strokeAlpha
                = 0.4f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(191.71f, 333.88f)
                lineToRelative(-9.8f, 82.16f)
                lineToRelative(2.1f, 0.0f)
                lineToRelative(12.43f, -82.16f)
                lineToRelative(-4.73f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(204.82f, 333.88f)
                lineToRelative(-0.19f, 82.16f)
                lineToRelative(2.09f, 0.0f)
                lineToRelative(2.83f, -82.16f)
                lineToRelative(-4.73f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.4f,
                strokeAlpha
                = 0.4f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(204.82f, 333.88f)
                lineToRelative(-0.19f, 82.16f)
                lineToRelative(2.09f, 0.0f)
                lineToRelative(2.83f, -82.16f)
                lineToRelative(-4.73f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(250.69f, 333.88f)
                lineToRelative(9.8f, 82.16f)
                lineToRelative(-2.1f, 0.0f)
                lineToRelative(-12.42f, -82.16f)
                lineToRelative(4.72f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.4f,
                strokeAlpha
                = 0.4f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(250.69f, 333.88f)
                lineToRelative(9.8f, 82.16f)
                lineToRelative(-2.1f, 0.0f)
                lineToRelative(-12.42f, -82.16f)
                lineToRelative(4.72f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.primary), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(237.58f, 333.88f)
                lineToRelative(0.19f, 82.16f)
                lineToRelative(-2.09f, 0.0f)
                lineToRelative(-2.83f, -82.16f)
                lineToRelative(4.73f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(VLRTheme.colorScheme.onPrimaryContainer),
                stroke = null,
                fillAlpha = 0.4f,
                strokeAlpha
                = 0.4f,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(237.58f, 333.88f)
                lineToRelative(0.19f, 82.16f)
                lineToRelative(-2.09f, 0.0f)
                lineToRelative(-2.83f, -82.16f)
                lineToRelative(4.73f, 0.0f)
                close()
            }
        }
            .build()
        return _gaming!!
    }

private var _gaming: ImageVector? = null

@Preview
@Composable
private fun Preview(): Unit {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = Illustration.Gaming, contentDescription = "")
    }
}
