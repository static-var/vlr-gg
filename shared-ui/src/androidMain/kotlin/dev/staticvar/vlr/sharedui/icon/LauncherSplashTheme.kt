/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.icon

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

public fun Activity.syncLauncherSplashTheme(): Int {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0
  val launcher = Intent(Intent.ACTION_MAIN)
    .addCategory(Intent.CATEGORY_LAUNCHER)
    .setPackage(packageName)
  val theme = packageManager.queryIntentActivities(launcher, PackageManager.GET_META_DATA)
    .singleOrNull()?.activityInfo?.metaData
    ?.getInt("dev.staticvar.vlr.SPLASH_THEME", 0) ?: 0
  splashScreen.setSplashScreenTheme(theme)
  return theme
}
