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

/** Controls push token collection using notification settings and platform access. */
internal class PushTokenRegistrationCoordinator(
  private val pushTokenProvider: PushTokenProvider,
  private val permissionProvider: NotificationPermissionProvider,
  private val notificationPreferences: LiveMatchNotificationPreferencesRepository,
  private val uploader: PushTokenRegistrationUploader,
  private val mainScope: CoroutineScope,
  private val onEligibilityChanged: (LiveUpdateEligibility) -> Unit = {},
  private val onSyncRequested: () -> Unit = {},
) {
  private var observationJob: Job? = null
  private var running: Boolean = false
  private var preferencesLoaded: Boolean = false
  private var enabled: Boolean = false
  private var authorization: NotificationAuthorization? = null
  private var supportsLiveUpdates: Boolean = false
  private var requiresNotificationPermission: Boolean = true
  private var liveActivitiesEnabled: Boolean? = null
  private var tokenProviderStarted: Boolean = false
  private var permissionReadPending: Boolean = false
  private var refreshTokenAfterPermissionRead: Boolean = false
  private var permissionGeneration: Int = 0
  private var tokenGeneration: Int = 0
  private var reportedEligibility: LiveUpdateEligibility? = null

  /**
   * Observes the saved notification preference and checks access when enabled.
   * Repeated starts reuse the existing observer.
   */
  fun start() {
    if (observationJob != null) return
    running = true
    observationJob = mainScope.launch {
      notificationPreferences.preferences.collect { value ->
        val becameEnabled = value.enabled && !enabled
        preferencesLoaded = true
        enabled = value.enabled
        if (becameEnabled) {
          authorization = null
          refreshAccess()
        }
        updateTokenProvider()
      }
    }
  }

  /**
   * Stops observing settings and collecting tokens.
   * Invalidates outstanding permission and token callbacks.
   */
  fun stop() {
    running = false
    permissionGeneration++
    permissionReadPending = false
    refreshTokenAfterPermissionRead = false
    preferencesLoaded = false
    enabled = false
    authorization = null
    observationJob?.cancel()
    observationJob = null
    stopTokenProvider()
  }

  /**
   * Refreshes platform access and requests a fresh token after the access check.
   * Also retries synchronization once that check finishes.
   */
  fun onForeground() {
    refreshTokenAfterPermissionRead = true
    refreshAccess()
  }

  fun onAuthorizationChanged(value: NotificationAuthorization) {
    mainScope.launch {
      if (!running || !requiresNotificationPermission) return@launch
      permissionGeneration++
      permissionReadPending = false
      authorization = value
      liveActivitiesEnabled = readLiveActivityCapability()
      finishAccessRefresh()
    }
  }

  /**
   * Reads platform capabilities and notification permission when required.
   * Uses a generation counter to discard results from obsolete permission checks.
   */
  private fun refreshAccess() {
    if (!running || permissionReadPending) return
    supportsLiveUpdates = readSupportsLiveUpdates()
    requiresNotificationPermission = readRequiresNotificationPermission()
    liveActivitiesEnabled = readLiveActivityCapability()
    if (!supportsLiveUpdates || !requiresNotificationPermission) {
      permissionGeneration++
      permissionReadPending = false
      authorization = null
      finishAccessRefresh()
      return
    }
    permissionReadPending = true
    val generation = ++permissionGeneration
    try {
      permissionProvider.readNotificationAuthorization { value ->
        mainScope.launch {
          if (!running || generation != permissionGeneration) return@launch
          permissionReadPending = false
          authorization = value
          liveActivitiesEnabled = readLiveActivityCapability()
          finishAccessRefresh()
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

  /**
   * Reports current eligibility and starts or stops native token collection.
   * Ignores token callbacks from previous collection sessions.
   */
  private fun updateTokenProvider() {
    val eligibility = liveUpdateEligibility()
    if (eligibility != reportedEligibility) {
      reportedEligibility = eligibility
      onEligibilityChanged(eligibility)
    }
    val shouldStart = eligibility == LiveUpdateEligibility.Enabled
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

  /**
   * Combines the saved preference, platform support, and access status.
   * Keeps unresolved permission checks pending instead of treating them as denied.
   */
  private fun liveUpdateEligibility(): LiveUpdateEligibility = when {
    !preferencesLoaded -> LiveUpdateEligibility.Pending
    !enabled -> LiveUpdateEligibility.Disabled
    !supportsLiveUpdates || liveActivitiesEnabled == false -> LiveUpdateEligibility.Disabled
    !requiresNotificationPermission || authorization == NotificationAuthorization.Authorized -> LiveUpdateEligibility.Enabled
    authorization == NotificationAuthorization.Denied ||
      authorization == NotificationAuthorization.NotDetermined -> LiveUpdateEligibility.Disabled
    else -> LiveUpdateEligibility.Pending
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

  private fun readSupportsLiveUpdates(): Boolean = try {
    permissionProvider.supportsLiveUpdates()
  } catch (_: Exception) {
    false
  }

  private fun readRequiresNotificationPermission(): Boolean = try {
    permissionProvider.requiresNotificationPermission()
  } catch (_: Exception) {
    true
  }

  private fun finishAccessRefresh() {
    val syncRequested = refreshTokenAfterPermissionRead
    refreshTokenAfterPermissionRead = false
    if (syncRequested && tokenProviderStarted) stopTokenProvider()
    updateTokenProvider()
    if (syncRequested) onSyncRequested()
  }
}
