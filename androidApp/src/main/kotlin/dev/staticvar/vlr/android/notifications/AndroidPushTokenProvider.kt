/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.notifications.PushTokenProvider

/** Delivers the current Firebase push token and later token changes. */
internal class AndroidPushTokenProvider(context: Context) : PushTokenProvider {
  private val appContext = context.applicationContext
  var onTokenChanged: (() -> Unit)? = null
  override val platform: PushPlatform = PushPlatform.Android

  private val callbackLock = Any()
  private var generation: Long = 0
  private var callback: ((String) -> Unit)? = null

  /**
   * Requests the current Firebase token and registers its recipient.
   * A generation check discards results from an earlier start or a stopped request.
   */
  override fun start(onToken: (String) -> Unit) {
    if (!AndroidLiveNotificationAvailability.isAvailable(appContext)) return
    val requestGeneration = synchronized(callbackLock) {
      generation += 1
      callback = onToken
      generation
    }
    try {
      FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token -> publish(token, requestGeneration) }
        .addOnFailureListener { error -> LiveNotificationDiagnostics.failed("token_request", error) }
    } catch (error: RuntimeException) {
      LiveNotificationDiagnostics.failed("token_request", error)
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

  /**
   * Delivers a usable token to the current recipient.
   * When a request generation is supplied, ignores callbacks from superseded requests.
   */
  private fun publish(token: String, requestGeneration: Long? = null) {
    if (token.isBlank() || !AndroidLiveNotificationAvailability.isAvailable(appContext)) return
    val currentCallback = synchronized(callbackLock) {
      if (requestGeneration != null && requestGeneration != generation) null else callback
    }
    currentCallback?.invoke(token)
  }
}
