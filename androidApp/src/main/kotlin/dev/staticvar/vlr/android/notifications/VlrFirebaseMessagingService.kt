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
    val app = application as VlrApplication
    if (AndroidLiveNotificationAvailability.isAvailable(this)) {
      app.liveMatchNotifications.handle(message.data)
    } else {
      app.matchAlertNotifications.handle(message.data)
    }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val app = application as VlrApplication
    app.pushTokenProvider.onNewToken(token)
    app.liveTopicSubscriptions.refresh()
  }
}
