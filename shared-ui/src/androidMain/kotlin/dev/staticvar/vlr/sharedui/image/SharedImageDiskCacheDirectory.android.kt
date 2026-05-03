/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

internal actual fun sharedImageDiskCacheDirectory(context: PlatformContext): Path =
  context.cacheDir.resolve(ImageCacheDirectory).toOkioPath()

private const val ImageCacheDirectory = "image_cache"
