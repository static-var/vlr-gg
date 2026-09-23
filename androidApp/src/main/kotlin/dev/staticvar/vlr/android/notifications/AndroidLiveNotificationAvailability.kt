/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.FirebaseApp

/** Checks Android version, Google Play services, and Firebase support for live notifications. */
internal object AndroidLiveNotificationAvailability {
  fun isAvailable(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA ||
      Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.BAKLAVA_1
    ) return false

    return try {
      GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS &&
        FirebaseApp.getInstance().options.let { options ->
          options.applicationId.isNotBlank() && !options.gcmSenderId.isNullOrBlank()
        }
    } catch (_: RuntimeException) {
      false
    }
  }
}
