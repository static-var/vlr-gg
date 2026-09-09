/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.illustration

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal class IllustrationColors(palette: PrismColorPalette) {
  val ink: Color = lerp(palette.contentPrimary, palette.accent, 0.2f)
  val muted: Color = lerp(palette.contentSecondary, palette.background, 0.2f)
  val faint: Color = lerp(palette.accent, palette.background, 0.78f)
  val accent: Color = palette.accent
  val soft: Color = lerp(palette.accent, palette.background, 0.8f)
  val mist: Color = lerp(palette.accent, palette.background, 0.94f)
  val panel: Color = palette.background
  val shade: Color = lerp(palette.accent, palette.background, 0.65f)
}

internal fun illustrationVector(name: String, block: ImageVector.Builder.() -> Unit): ImageVector = ImageVector.Builder(
  name = name,
  defaultWidth = 360.dp,
  defaultHeight = 460.dp,
  viewportWidth = 360f,
  viewportHeight = 460f,
).apply(block).build()

internal fun ImageVector.Builder.shape(data: String, fill: Color?, stroke: Color?, width: Float) {
  addPath(
    pathData = addPathNodes(data),
    fill = fill?.let(::SolidColor),
    stroke = stroke?.let(::SolidColor),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
  )
}
