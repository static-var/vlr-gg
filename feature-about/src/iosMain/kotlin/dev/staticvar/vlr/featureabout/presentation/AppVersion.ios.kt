/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle

@Composable
internal actual fun appVersionText(): String = remember {
  val bundle = NSBundle.mainBundle
  val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
  val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
  listOfNotNull(version?.let { "Version $it" }, build?.let { "Build $it" })
    .joinToString(" · ").ifEmpty { "Development build" }
}
