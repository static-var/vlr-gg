/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal actual fun sharedImageDiskCacheDirectory(context: PlatformContext): Path {
  val cacheDirectory = NSFileManager.defaultManager
    .URLsForDirectory(directory = NSCachesDirectory, inDomains = NSUserDomainMask)
    .firstOrNull()
    .let { url -> url as? NSURL }
    ?.path
    ?: NSTemporaryDirectory()

  return "$cacheDirectory/vlr-gg/image_cache".toPath()
}
