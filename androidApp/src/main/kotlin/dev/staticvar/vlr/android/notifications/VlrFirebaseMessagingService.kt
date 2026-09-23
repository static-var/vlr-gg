/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import dev.staticvar.vlr.android.VlrApplication

public class VlrFirebaseMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    (application as VlrApplication).pushTokenProvider.onNewToken(token)
  }
}
