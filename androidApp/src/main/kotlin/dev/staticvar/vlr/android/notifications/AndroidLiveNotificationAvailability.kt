/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import dev.staticvar.vlr.android.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.FirebaseApp

/** Separates Firebase delivery support from Android's promoted Live Update support. */
internal object AndroidLiveNotificationAvailability {
  private val lastStatuses = mutableMapOf<String, String>()

  fun isAvailable(context: Context): Boolean {
    val available = supportsMatchAlerts(context)
    val status = when {
      !available -> "firebase_unavailable"
      Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA -> "unsupported_android_version"
      else -> "available"
    }
    recordStatus("live_updates", status)
    return status == "available"
  }

  fun usesLiveUpdates(context: Context, enabled: Boolean): Boolean {
    if (!enabled || !isAvailable(context)) return false
    val channel = context.getSystemService(NotificationManager::class.java)
      .getNotificationChannel("live_matches")
    return channel?.importance != NotificationManager.IMPORTANCE_NONE
  }

  /** Ordinary match alerts require Firebase and Play services, regardless of Android version. */
  fun supportsMatchAlerts(context: Context): Boolean {
    val status = try {
      val playServices = GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context)
      if (playServices != ConnectionResult.SUCCESS) {
        "play_services_error_$playServices"
      } else {
        FirebaseApp.getInstance().options.let { options ->
          if (options.applicationId.isNotBlank() && !options.gcmSenderId.isNullOrBlank()) {
            "available"
          } else {
            "firebase_configuration_unavailable"
          }
        }
      }
    } catch (_: IllegalStateException) {
      "firebase_not_initialized"
    } catch (error: RuntimeException) {
      if (recordStatus("firebase", "availability_check_failed")) {
        LiveNotificationDiagnostics.failed("availability", error)
      }
      return false
    }
    recordStatus("firebase", status)
    return status == "available"
  }

  private fun recordStatus(feature: String, status: String): Boolean = synchronized(this) {
    if (lastStatuses[feature] == status) return@synchronized false
    lastStatuses[feature] = status
    if (BuildConfig.DEBUG) {
      Log.d("LiveMatchNotifications", "$feature=$status, sdk=${Build.VERSION.SDK_INT}")
    }
    true
  }
}
