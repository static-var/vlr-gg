/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import com.google.firebase.messaging.FirebaseMessaging
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.notifications.PushTokenProvider

internal class AndroidPushTokenProvider : PushTokenProvider {
  var onTokenChanged: (() -> Unit)? = null
  override val platform: PushPlatform = PushPlatform.Android

  private val callbackLock = Any()
  private var generation: Long = 0
  private var callback: ((String) -> Unit)? = null

  override fun start(onToken: (String) -> Unit) {
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
    onTokenChanged?.invoke()
    publish(token)
  }

  private fun publish(token: String, requestGeneration: Long? = null) {
    if (token.isBlank()) return
    val currentCallback = synchronized(callbackLock) {
      if (requestGeneration != null && requestGeneration != generation) null else callback
    }
    currentCallback?.invoke(token)
  }
}
