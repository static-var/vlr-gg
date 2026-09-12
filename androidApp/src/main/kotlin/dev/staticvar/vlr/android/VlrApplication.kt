/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.app.Application
import dev.staticvar.vlr.shared.di.initializeAppKoin
import dev.staticvar.vlr.shared.network.androidNetworkModule
import dev.staticvar.vlr.shared.telemetry.initializeSentryTelemetry
import org.koin.android.ext.koin.androidContext

class VlrApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    initializeAppKoin(
      appDeclaration = {
        androidContext(this@VlrApplication)
        modules(androidNetworkModule(this@VlrApplication))
      },
      authToken = BuildConfig.TOKEN
        .trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .takeIf(String::isNotBlank),
    )
    initializeSentryTelemetry(
      dsn = BuildConfig.SENTRY_DSN,
      environment = BuildConfig.SENTRY_ENVIRONMENT.ifBlank { if (BuildConfig.DEBUG) "development" else "production" },
      release = "vlr@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}",
      dist = "android-${BuildConfig.VERSION_CODE}",
      enabled = BuildConfig.SENTRY_ENABLED,
    )
  }
}
