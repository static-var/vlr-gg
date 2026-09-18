/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.build_number
import vlr.feature_about.generated.resources.version_number

@Composable
internal actual fun appVersionText(): String {
  val context = LocalContext.current
  val info = remember(context) {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0)
  }
  @Suppress("DEPRECATION")
  val build = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
  val versionText = info.versionName?.let { stringResource(Res.string.version_number, it) }
  val buildText = stringResource(Res.string.build_number, build)
  return listOfNotNull(versionText, buildText).joinToString(" · ")
}
