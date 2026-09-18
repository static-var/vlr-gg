/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.max
import kotlin.math.min

internal class LogoEdgeAnalysis(private val edgeColors: List<Color>) {
  fun needsOutline(background: Color): Boolean {
    if (edgeColors.isEmpty()) return false
    val backgroundLuminance = background.luminance()
    val lowContrast = edgeColors.count { color ->
      val luminance = color.compositeOver(background).luminance()
      (max(luminance, backgroundLuminance) + 0.05f) / (min(luminance, backgroundLuminance) + 0.05f) < 2f
    }
    return lowContrast.toFloat() / edgeColors.size >= 0.35f
  }

  internal companion object {
    val Empty = LogoEdgeAnalysis(emptyList())
  }
}

internal fun analyzeLogoEdges(sample: ImageBitmap): LogoEdgeAnalysis {
  val pixels = sample.toPixelMap()
  fun visible(x: Int, y: Int): Boolean =
    x in 0 until sample.width && y in 0 until sample.height && pixels[x, y].alpha >= 0.5f
  val edges = mutableListOf<Color>()
  var hasTransparency = false
  for (y in 0 until sample.height) {
    for (x in 0 until sample.width) {
      if (!visible(x, y)) {
        hasTransparency = true
      } else if (!visible(x - 1, y) || !visible(x + 1, y) || !visible(x, y - 1) || !visible(x, y + 1)) {
        edges += pixels[x, y]
      }
    }
  }
  return LogoEdgeAnalysis(if (hasTransparency) edges else emptyList())
}
