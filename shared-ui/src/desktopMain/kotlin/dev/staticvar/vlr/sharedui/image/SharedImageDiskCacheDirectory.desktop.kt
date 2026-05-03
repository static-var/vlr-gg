/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath

internal actual fun sharedImageDiskCacheDirectory(context: PlatformContext): Path =
  "${System.getProperty("java.io.tmpdir")}/vlr-gg/image_cache".toPath()
