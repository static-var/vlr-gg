/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.image

import coil3.PlatformContext
import okio.Path

internal expect fun sharedImageDiskCacheDirectory(context: PlatformContext): Path
