package dev.staticvar.vlr.utils

import android.content.Context

val Context.currentAppVersion: String
  get() = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
