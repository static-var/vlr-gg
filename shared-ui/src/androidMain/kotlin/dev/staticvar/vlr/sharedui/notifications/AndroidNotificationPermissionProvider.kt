/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider

@Composable
public fun rememberAndroidNotificationPermissionProvider(
  supportsLiveUpdates: () -> Boolean,
  supportsMatchAlerts: () -> Boolean = { false },
  onAuthorizationChanged: () -> Unit = {},
): NotificationPermissionProvider {
  val context = LocalContext.current.applicationContext
  val activity = LocalActivity.current
  val provider = remember(context, supportsLiveUpdates, supportsMatchAlerts, onAuthorizationChanged) {
    AndroidNotificationPermissionProvider(context, supportsLiveUpdates, supportsMatchAlerts, onAuthorizationChanged)
  }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    provider.completeRequest()
  }
  DisposableEffect(provider, launcher, activity) {
    provider.launchRequest = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    provider.shouldShowRationale = {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
    }
    onDispose { provider.dispose() }
  }
  return provider
}

/** Reads and requests Android notification permission and opens notification settings. */
private class AndroidNotificationPermissionProvider(
  private val context: Context,
  private val supportsLiveNotifications: () -> Boolean,
  private val supportsOrdinaryMatchAlerts: () -> Boolean,
  private val onAuthorizationChanged: () -> Unit,
) : NotificationPermissionProvider {
  private val mainHandler = Handler(Looper.getMainLooper())
  private val preferences = context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
  private var pendingResult: ((NotificationAuthorization) -> Unit)? = null
  var launchRequest: (() -> Unit)? = null
  var shouldShowRationale: (() -> Boolean)? = null

  override fun supportsLiveUpdates(): Boolean = supportsLiveNotifications()

  override fun supportsMatchAlerts(): Boolean = supportsOrdinaryMatchAlerts()

  override fun areLiveActivitiesEnabled(): Boolean? = null

  override fun canPromoteNotifications(): Boolean? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
    try {
      context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
    } catch (_: RuntimeException) {
      null
    }
  } else {
    null
  }

  override fun readNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    onMain {
      onResult(readAuthorization())
      onAuthorizationChanged()
    }
  }

  override fun requestNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    onMain {
      val authorization = readAuthorization()
      if (authorization == NotificationAuthorization.Authorized ||
        authorization == NotificationAuthorization.Error ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
      ) {
        onResult(authorization)
        onAuthorizationChanged()
      } else {
        val launch = launchRequest
        if (launch == null || pendingResult != null) {
          onResult(NotificationAuthorization.Error)
        } else {
          pendingResult = onResult
          try {
            launch()
          } catch (_: RuntimeException) {
            finishRequest(NotificationAuthorization.Error)
          }
        }
      }
    }
  }

  override fun openSettings() {
    onMain { openNotificationSettings() }
  }

  override fun openPromotionSettings() {
    onMain {
      val opened = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && tryOpenSettings(
        Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
          .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
      )
      if (!opened) openNotificationSettings()
    }
  }

  /** Falls back to app details when an OEM does not provide notification settings. */
  private fun openNotificationSettings() {
    val opened = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && tryOpenSettings(
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
    if (!opened) {
      tryOpenSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
  }

  private fun tryOpenSettings(intent: Intent): Boolean = try {
    if (intent.resolveActivity(context.packageManager) == null) {
      false
    } else {
      context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      true
    }
  } catch (_: RuntimeException) {
    false
  }

  fun completeRequest() {
    finishRequest(readAuthorization())
  }

  fun dispose() {
    launchRequest = null
    shouldShowRationale = null
    finishRequest(NotificationAuthorization.Error)
  }

  private fun readAuthorization(): NotificationAuthorization = try {
    val enabled = context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    val hasResponse = enabled || shouldShowRationale?.invoke() == true
    if (hasResponse && !preferences.getBoolean(HAS_PERMISSION_RESPONSE, false)) {
      preferences.edit().putBoolean(HAS_PERMISSION_RESPONSE, true).apply()
    }
    when {
      enabled -> NotificationAuthorization.Authorized
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        preferences.getBoolean(HAS_PERMISSION_RESPONSE, false) -> NotificationAuthorization.Denied
      else -> NotificationAuthorization.NotDetermined
    }
  } catch (_: RuntimeException) {
    NotificationAuthorization.Error
  }

  private fun finishRequest(authorization: NotificationAuthorization) {
    val onResult = pendingResult
    pendingResult = null
    onResult?.let {
      it(authorization)
      onAuthorizationChanged()
    }
  }

  private fun onMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
  }

  /** Defines the preference key that records a notification permission response. */
  private companion object {
    const val HAS_PERMISSION_RESPONSE = "has_permission_response"
  }
}
