/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSBundle
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.build_number
import vlr.feature_about.generated.resources.development_build
import vlr.feature_about.generated.resources.version_number

@Composable
internal actual fun appVersionText(): String {
  val (version, build) = remember {
    val bundle = NSBundle.mainBundle
    (bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String) to
      (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)
  }
  val versionText = version?.let { stringResource(Res.string.version_number, it) }
  val buildText = build?.let { stringResource(Res.string.build_number, it) }
  return listOfNotNull(versionText, buildText)
    .joinToString(" · ").ifEmpty { stringResource(Res.string.development_build) }
}
