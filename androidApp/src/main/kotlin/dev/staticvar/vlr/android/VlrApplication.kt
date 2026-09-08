/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.app.Application
import dev.staticvar.vlr.shared.telemetry.initializeSentryTelemetry

class VlrApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    initializeSentryTelemetry(
      dsn = BuildConfig.SENTRY_DSN,
      environment = BuildConfig.SENTRY_ENVIRONMENT.ifBlank { if (BuildConfig.DEBUG) "development" else "production" },
      release = "vlr@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}",
      dist = "android-${BuildConfig.VERSION_CODE}",
      enabled = BuildConfig.SENTRY_ENABLED,
    )
  }
}
