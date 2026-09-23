/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.notifications.PushTokenProvider

internal class AndroidPushTokenProvider(context: Context) : PushTokenProvider {
  private val appContext = context.applicationContext
  var onTokenChanged: (() -> Unit)? = null
  override val platform: PushPlatform = PushPlatform.Android

  private val callbackLock = Any()
  private var generation: Long = 0
  private var callback: ((String) -> Unit)? = null

  override fun start(onToken: (String) -> Unit) {
    if (!AndroidLiveNotificationAvailability.isAvailable(appContext)) return
    val requestGeneration = synchronized(callbackLock) {
      generation += 1
      callback = onToken
      generation
    }
    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
      publish(token, requestGeneration)
    }
  }

  override fun stop() {
    synchronized(callbackLock) {
      generation += 1
      callback = null
    }
  }

  internal fun onNewToken(token: String) {
    if (!AndroidLiveNotificationAvailability.isAvailable(appContext)) return
    onTokenChanged?.invoke()
    publish(token)
  }

  private fun publish(token: String, requestGeneration: Long? = null) {
    if (token.isBlank() || !AndroidLiveNotificationAvailability.isAvailable(appContext)) return
    val currentCallback = synchronized(callbackLock) {
      if (requestGeneration != null && requestGeneration != generation) null else callback
    }
    currentCallback?.invoke(token)
  }
}
