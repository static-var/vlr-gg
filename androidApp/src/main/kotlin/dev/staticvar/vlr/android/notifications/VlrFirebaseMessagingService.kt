/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.staticvar.vlr.android.VlrApplication

/** Routes Firebase match messages and refreshed tokens to the app. */
public class VlrFirebaseMessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    (application as VlrApplication).liveMatchNotifications.handle(message.data)
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    (application as VlrApplication).pushTokenProvider.onNewToken(token)
  }
}
