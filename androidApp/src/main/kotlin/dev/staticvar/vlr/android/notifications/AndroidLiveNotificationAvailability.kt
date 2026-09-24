/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import android.os.Build
import android.util.Log
import dev.staticvar.vlr.android.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.FirebaseApp

/** Checks Android version, Google Play services, and Firebase support for live notifications. */
internal object AndroidLiveNotificationAvailability {
  private var lastStatus: String? = null

  fun isAvailable(context: Context): Boolean {
    val status = availabilityStatus(context)
    synchronized(this) {
      if (lastStatus != status) {
        lastStatus = status
        if (BuildConfig.DEBUG) Log.d("LiveMatchNotifications", "availability=$status, sdk=${Build.VERSION.SDK_INT}")
      }
    }
    return status == "available"
  }

  private fun availabilityStatus(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return "unsupported_android_version"
    return try {
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
      LiveNotificationDiagnostics.failed("availability", error)
      "availability_check_failed"
    }
  }
}
