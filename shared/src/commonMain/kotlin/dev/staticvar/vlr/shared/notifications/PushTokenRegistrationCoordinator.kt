/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider
import dev.staticvar.vlr.core.notifications.PushTokenProvider
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class PushTokenRegistrationCoordinator(
  private val pushTokenProvider: PushTokenProvider,
  private val permissionProvider: NotificationPermissionProvider,
  private val notificationPreferences: LiveMatchNotificationPreferencesRepository,
  private val uploader: PushTokenRegistrationUploader,
  private val mainScope: CoroutineScope,
) {
  private var observationJob: Job? = null
  private var running: Boolean = false
  private var enabled: Boolean = false
  private var authorization: NotificationAuthorization? = null
  private var liveActivitiesEnabled: Boolean? = null
  private var tokenProviderStarted: Boolean = false
  private var permissionReadPending: Boolean = false
  private var refreshTokenAfterPermissionRead: Boolean = false
  private var permissionGeneration: Int = 0
  private var tokenGeneration: Int = 0

  fun start() {
    if (observationJob != null) return
    running = true
    observationJob = mainScope.launch {
      notificationPreferences.preferences.collect { value ->
        val becameEnabled = value.enabled && !enabled
        enabled = value.enabled
        if (becameEnabled) {
          authorization = null
          refreshAuthorization()
        }
        updateTokenProvider()
      }
    }
  }

  fun stop() {
    running = false
    permissionGeneration++
    permissionReadPending = false
    refreshTokenAfterPermissionRead = false
    enabled = false
    authorization = null
    observationJob?.cancel()
    observationJob = null
    stopTokenProvider()
  }

  fun onForeground() {
    refreshTokenAfterPermissionRead = true
    refreshAuthorization()
  }

  fun onAuthorizationChanged(value: NotificationAuthorization) {
    mainScope.launch {
      if (!running) return@launch
      permissionGeneration++
      permissionReadPending = false
      authorization = value
      liveActivitiesEnabled = readLiveActivityCapability()
      restartTokenProviderAfterForegroundRead()
      updateTokenProvider()
    }
  }

  private fun refreshAuthorization() {
    if (!running || permissionReadPending) return
    permissionReadPending = true
    liveActivitiesEnabled = readLiveActivityCapability()
    val generation = ++permissionGeneration
    try {
      permissionProvider.readNotificationAuthorization { value ->
        mainScope.launch {
          if (!running || generation != permissionGeneration) return@launch
          permissionReadPending = false
          authorization = value
          liveActivitiesEnabled = readLiveActivityCapability()
          restartTokenProviderAfterForegroundRead()
          updateTokenProvider()
        }
      }
    } catch (_: Exception) {
      if (generation == permissionGeneration) {
        permissionReadPending = false
        authorization = NotificationAuthorization.Error
        updateTokenProvider()
      }
    }
  }

  private fun updateTokenProvider() {
    val shouldStart = enabled &&
      authorization == NotificationAuthorization.Authorized &&
      liveActivitiesEnabled != false
    if (shouldStart && !tokenProviderStarted) {
      val generation = ++tokenGeneration
      tokenProviderStarted = true
      try {
        pushTokenProvider.start { token ->
          mainScope.launch {
            if (running && tokenProviderStarted && generation == tokenGeneration) {
              uploader.activate(pushTokenProvider.platform, token)
            }
          }
        }
      } catch (_: Exception) {
        tokenGeneration++
        tokenProviderStarted = false
        uploader.deactivate()
      }
    } else if (!shouldStart && tokenProviderStarted) {
      stopTokenProvider()
    }
  }

  private fun stopTokenProvider() {
    tokenGeneration++
    if (tokenProviderStarted) {
      try {
        pushTokenProvider.stop()
      } catch (_: Exception) {
        // A platform teardown failure must not keep the registration gate open.
      }
    }
    tokenProviderStarted = false
    uploader.deactivate()
  }

  private fun readLiveActivityCapability(): Boolean? = try {
    permissionProvider.areLiveActivitiesEnabled()
  } catch (_: Exception) {
    false
  }

  private fun restartTokenProviderAfterForegroundRead() {
    if (!refreshTokenAfterPermissionRead) return
    refreshTokenAfterPermissionRead = false
    if (tokenProviderStarted) stopTokenProvider()
  }
}
