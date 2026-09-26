/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.app.Application
import coil3.SingletonImageLoader
import dev.staticvar.vlr.android.notifications.AndroidMatchAlertNotifications
import dev.staticvar.vlr.android.notifications.AndroidLiveMatchNotifications
import dev.staticvar.vlr.android.notifications.AndroidLiveTopicSubscriptions
import dev.staticvar.vlr.android.notifications.AndroidPushTokenProvider
import dev.staticvar.vlr.core.di.DispatcherQualifiers
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.shared.di.initializeAppKoin
import dev.staticvar.vlr.shared.network.androidNetworkModule
import dev.staticvar.vlr.shared.telemetry.initializeSentryTelemetry
import dev.staticvar.vlr.sharedui.image.createSharedImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext

class VlrApplication : Application() {
  internal val pushTokenProvider by lazy { AndroidPushTokenProvider(this) }
  internal lateinit var liveTopicSubscriptions: AndroidLiveTopicSubscriptions
    private set
  internal lateinit var liveMatchNotifications: AndroidLiveMatchNotifications
    private set

  internal lateinit var matchAlertNotifications: AndroidMatchAlertNotifications
    private set

  override fun onCreate() {
    super.onCreate()
    configureLeakDetection()
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
    val scope = getKoin().get<CoroutineScope>(DispatcherQualifiers.AppScope)
    SingletonImageLoader.setSafe { context -> createSharedImageLoader(context) }
    val notificationPreferences = getKoin().get<LiveMatchNotificationPreferencesRepository>()
    val spoilerPreferences = getKoin().get<SpoilerPreferencesRepository>()
    liveMatchNotifications = AndroidLiveMatchNotifications(
      context = this,
      json = getKoin().get(),
      preferences = notificationPreferences,
      spoilerPreferences = spoilerPreferences,
      logoScope = scope,
    )
    matchAlertNotifications = AndroidMatchAlertNotifications(
      context = this,
      json = getKoin().get(),
      enabled = { notificationPreferences.preferences.value.enabled },
    )
    liveTopicSubscriptions = AndroidLiveTopicSubscriptions(
      context = this,
      topics = getKoin().get(),
      preferences = notificationPreferences,
      scope = scope,
    )
    pushTokenProvider.onTokenChanged = liveTopicSubscriptions::refresh
    scope.launch {
      notificationPreferences.preferences.collect { preferences ->
        if (!preferences.enabled) matchAlertNotifications.cancelAll()
      }
    }
    scope.launch {
      combine(notificationPreferences.preferences, spoilerPreferences.enabled) { notifications, hidden ->
        !notifications.enabled || hidden
      }.collect { shouldRemove ->
        if (shouldRemove) liveMatchNotifications.cancelAll()
      }
    }
  }
}
