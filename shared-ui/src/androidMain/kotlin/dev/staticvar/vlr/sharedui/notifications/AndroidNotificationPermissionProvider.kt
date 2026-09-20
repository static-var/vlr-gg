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
public fun rememberAndroidNotificationPermissionProvider(): NotificationPermissionProvider {
  val context = LocalContext.current.applicationContext
  val activity = LocalActivity.current
  val provider = remember(context) { AndroidNotificationPermissionProvider(context) }
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

private class AndroidNotificationPermissionProvider(private val context: Context) : NotificationPermissionProvider {
  private val mainHandler = Handler(Looper.getMainLooper())
  private val preferences = context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
  private var pendingResult: ((NotificationAuthorization) -> Unit)? = null
  var launchRequest: (() -> Unit)? = null
  var shouldShowRationale: (() -> Boolean)? = null

  override fun areLiveActivitiesEnabled(): Boolean? = null

  override fun readNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    onMain { onResult(readAuthorization()) }
  }

  override fun requestNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    onMain {
      val authorization = readAuthorization()
      if (authorization == NotificationAuthorization.Authorized ||
        authorization == NotificationAuthorization.Error ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
      ) {
        onResult(authorization)
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
    onMain {
      val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
      }
      context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
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
    onResult?.invoke(authorization)
  }

  private fun onMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
  }

  private companion object {
    const val HAS_PERMISSION_RESPONSE = "has_permission_response"
  }
}
