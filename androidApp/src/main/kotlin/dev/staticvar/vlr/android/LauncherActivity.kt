/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.SplashScreen

/**
 * Entry point behind every launcher icon alias.
 *
 * Changing the app icon disables the alias the app was opened from, and the system removes any task
 * rooted in a disabled alias. This activity keeps the alias in a short-lived task of its own and hands
 * off to [MainActivity], whose task therefore survives icon changes.
 */
class LauncherActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Activities started by the app itself get an icon-less splash unless the icon style is requested.
    val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityOptions.makeBasic().setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON).toBundle()
    } else {
      null
    }
    startActivity(
      Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
      options,
    )
    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
  }
}
