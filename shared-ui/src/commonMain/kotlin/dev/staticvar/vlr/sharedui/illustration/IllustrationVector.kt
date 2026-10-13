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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import kotlin.math.abs
import kotlin.math.cbrt

internal class IllustrationColors(palette: PrismColorPalette) {
  val ink: Color = lerp(palette.contentPrimary, palette.accent, 0.2f)
  val muted: Color = lerp(palette.contentSecondary, palette.background, 0.2f)
  val faint: Color = palette.accentTint(background = 0.78f, minLightnessGap = 14f)
  val accent: Color = palette.accent
  val soft: Color = palette.accentTint(background = 0.8f, minLightnessGap = 13f)
  val mist: Color = palette.accentTint(background = 0.94f, minLightnessGap = 6f)
  val panel: Color = palette.background
  val shade: Color = palette.accentTint(background = 0.65f, minLightnessGap = 21f)
}

/**
 * Accent mixed into the background, keeping at least [minLightnessGap] CIE L* from the background.
 *
 * A fixed mix all but vanishes on near-black backgrounds, so the accent share grows until the tint
 * stands apart.
 */
private fun PrismColorPalette.accentTint(background: Float, minLightnessGap: Float): Color {
  val base = this.background.lightness()
  var share = background
  var tint = lerp(accent, this.background, share)
  while (share > 0f && abs(tint.lightness() - base) < minLightnessGap) {
    share = (share - 0.01f).coerceAtLeast(0f)
    tint = lerp(accent, this.background, share)
  }
  return tint
}

private fun Color.lightness(): Float {
  val y = luminance()
  return if (y > 0.008856f) 116f * cbrt(y) - 16f else 903.3f * y
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
