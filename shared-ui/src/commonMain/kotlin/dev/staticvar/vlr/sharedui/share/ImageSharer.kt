/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.share

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

public interface ImageSharer {
  public suspend fun share(image: ImageBitmap, text: String)
}

public val LocalImageSharer: ProvidableCompositionLocal<ImageSharer?> = staticCompositionLocalOf { null }
