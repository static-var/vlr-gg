/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize

/** Adds a thin light edge only when a transparent logo would disappear against a dark surface. */
public fun Bitmap.withNeutralLogoOutline(radiusPx: Float): Bitmap {
  val source = asImageBitmap()
  if (!analyzeLogoBitmap(source).needsOutline(Color.Black)) return this
  return renderOutlinedLogo(
    source = source,
    pixelSize = IntSize(width, height),
    radiusPx = radiusPx,
    outlineColor = Color.White,
  ).asAndroidBitmap()
}
