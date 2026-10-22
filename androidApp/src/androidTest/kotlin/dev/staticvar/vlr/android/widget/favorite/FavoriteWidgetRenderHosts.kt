/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal data class FavoriteWidgetRenderHost(
  val size: DpSize,
  val fontScale: Float = 1f,
  val densityDpi: Int = 0,
  val dark: Boolean = false,
) {
  fun parameters(): Array<Any> = arrayOf(size.width.value, size.height.value, dark, fontScale, densityDpi)
}

// Picker preview sizes, then the launcher extremes (smallest, widest, tallest) across densities and large font scales.
internal val favoriteWidgetRenderHosts = listOf(
  FavoriteWidgetRenderHost(DpSize(280.dp, 136.dp)),
  FavoriteWidgetRenderHost(DpSize(280.dp, 213.dp)),
  FavoriteWidgetRenderHost(DpSize(280.dp, 213.dp), dark = true),
  FavoriteWidgetRenderHost(DpSize(280.dp, 330.dp)),
  FavoriteWidgetRenderHost(DpSize(292.57144.dp, 229.33333.dp)),
  FavoriteWidgetRenderHost(DpSize(338.42694.dp, 577.0786.dp), densityDpi = 356),
  FavoriteWidgetRenderHost(DpSize(235.dp, 103.dp), 2f, 420),
  FavoriteWidgetRenderHost(DpSize(537.9048.dp, 137.14285.dp), 2f, 420),
  FavoriteWidgetRenderHost(DpSize(235.dp, 344.dp), 1.3f, 500),
  FavoriteWidgetRenderHost(DpSize(251.52.dp, 153.6.dp), 1.3f, 500),
  FavoriteWidgetRenderHost(DpSize(235.55556.dp, 174.81482.dp), 2f, 540),
)
