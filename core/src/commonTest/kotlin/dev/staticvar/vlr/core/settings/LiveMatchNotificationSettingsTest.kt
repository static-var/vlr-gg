/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies live-update preferences and platform permission handling. */
class LiveMatchNotificationSettingsTest {
  @Test
  fun preferenceDefaultsOffAndRestoresEnabledAndDisabled() {
    val storage = MapSettings()
    val repository = LiveMatchNotificationPreferencesRepository(storage)
    assertEquals(LiveMatchNotificationPreferences(), repository.preferences.value)
    repository.setEnabled(true)
    assertTrue(LiveMatchNotificationPreferencesRepository(storage).preferences.value.enabled)
    repository.setEnabled(false)
    assertFalse(LiveMatchNotificationPreferencesRepository(storage).preferences.value.enabled)
  }

  @Test
  fun optingInRequestsOnceAndDenialPreservesTheChoiceWithoutReprompting() {
    val provider = FakeProvider()
    val controller = LiveMatchNotificationSettingsController(LiveMatchNotificationPreferencesRepository(MapSettings()), provider)
    controller.refresh()
    provider.readResult!!(NotificationAuthorization.NotDetermined)
    controller.setEnabled(true)
    controller.setEnabled(true)
    assertEquals(1, provider.requests)
    assertTrue(controller.access.value.requesting)
    provider.requestResult!!(NotificationAuthorization.Denied)
    assertEquals(LiveMatchNotificationPreferences(true), controller.preferences.value)
    assertFalse(controller.access.value.requesting)
    controller.setEnabled(false)
    controller.setEnabled(true)
    assertEquals(1, provider.requests)
  }

  @Test
  fun oldStatusReadCannotOverwritePromptResultAndRefreshSeesSystemChanges() {
    val provider = FakeProvider()
    val controller = LiveMatchNotificationSettingsController(LiveMatchNotificationPreferencesRepository(MapSettings()), provider)
    controller.refresh()
    val staleRead = provider.readResult!!
    controller.setEnabled(true)
    provider.requestResult!!(NotificationAuthorization.Authorized)
    staleRead(NotificationAuthorization.NotDetermined)
    assertEquals(NotificationAuthorization.Authorized, controller.access.value.notifications)
    provider.activitiesEnabled = false
    controller.refresh()
    provider.readResult!!(NotificationAuthorization.Denied)
    assertEquals(false, controller.access.value.activitiesEnabled)
    assertEquals(NotificationAuthorization.Denied, controller.access.value.notifications)
    assertTrue(controller.preferences.value.enabled)
  }

  @Test
  fun androidNeedsNoLiveActivityCapabilityAndErrorsCanBeRetried() {
    val provider = FakeProvider().apply { activitiesEnabled = null }
    val controller = LiveMatchNotificationSettingsController(LiveMatchNotificationPreferencesRepository(MapSettings()), provider)
    controller.setEnabled(true)
    provider.requestResult!!(NotificationAuthorization.Error)
    assertFalse(controller.access.value.requesting)
    controller.requestNotifications()
    provider.requestResult!!(NotificationAuthorization.Authorized)
    assertEquals(2, provider.requests)
    assertEquals(null, controller.access.value.activitiesEnabled)
    assertEquals(NotificationAuthorization.Authorized, controller.access.value.notifications)
    controller.openSettings()
    assertEquals(1, provider.settingsOpened)
  }

  @Test
  fun liveActivitiesNeverReadOrRequestOrdinaryNotificationPermission() {
    val provider = FakeProvider().apply {
      requiresPermission = false
      activitiesEnabled = true
    }
    val controller = LiveMatchNotificationSettingsController(
      LiveMatchNotificationPreferencesRepository(MapSettings()),
      provider,
    )

    controller.refresh()
    controller.setEnabled(true)
    controller.requestNotifications()

    assertEquals(0, provider.reads)
    assertEquals(0, provider.requests)
    assertEquals(null, controller.access.value.notifications)
    assertFalse(controller.access.value.requiresNotificationPermission)
    assertTrue(controller.preferences.value.enabled)
  }

  @Test
  fun promotionDeniedDoesNotDisableNotificationsAndRefreshTracksChanges() {
    val provider = FakeProvider().apply { promotionAllowed = false }
    val controller = LiveMatchNotificationSettingsController(LiveMatchNotificationPreferencesRepository(MapSettings()), provider)
    controller.setEnabled(true)
    provider.requestResult!!(NotificationAuthorization.Authorized)

    assertTrue(controller.preferences.value.enabled)
    assertTrue(controller.access.value.supportsLiveUpdates)
    assertEquals(NotificationAuthorization.Authorized, controller.access.value.notifications)
    assertEquals(false, controller.access.value.promotionAllowed)
    controller.openPromotionSettings()
    assertEquals(1, provider.promotionSettingsOpened)
    assertEquals(0, provider.settingsOpened)

    provider.promotionAllowed = true
    controller.refresh()
    provider.readResult!!(NotificationAuthorization.Authorized)
    assertEquals(true, controller.access.value.promotionAllowed)
  }

  @Test
  fun permissionDenialKeepsRegularSettingsActionSeparateFromPromotion() {
    val provider = FakeProvider().apply { promotionAllowed = false }
    val controller = LiveMatchNotificationSettingsController(LiveMatchNotificationPreferencesRepository(MapSettings()), provider)
    controller.setEnabled(true)
    provider.requestResult!!(NotificationAuthorization.Denied)
    controller.openSettings()

    assertEquals(1, provider.settingsOpened)
    assertEquals(0, provider.promotionSettingsOpened)
  }

  @Test
  fun ordinaryMatchAlertsRequestPermissionWithoutLiveUpdatesOrPromotion() {
    val provider = FakeProvider().apply {
      supported = false
      matchAlertsSupported = true
      promotionAllowed = false
    }
    val controller = LiveMatchNotificationSettingsController(LiveMatchNotificationPreferencesRepository(MapSettings()), provider)
    controller.refresh()
    provider.readResult!!(NotificationAuthorization.NotDetermined)
    controller.setEnabled(true)
    provider.requestResult!!(NotificationAuthorization.Authorized)

    assertEquals(1, provider.requests)
    assertTrue(controller.access.value.supportsNotifications)
    assertFalse(controller.access.value.supportsLiveUpdates)
    assertEquals(null, controller.access.value.promotionAllowed)
    controller.openSettings()
    controller.openPromotionSettings()
    assertEquals(1, provider.settingsOpened)
    assertEquals(0, provider.promotionSettingsOpened)
  }

  @Test
  fun unsupportedLiveUpdatesDoNotRequestPermission() {
    val provider = FakeProvider().apply { supported = false }
    val controller = LiveMatchNotificationSettingsController(
      LiveMatchNotificationPreferencesRepository(MapSettings()),
      provider,
    )

    controller.setEnabled(true)
    controller.refresh()
    controller.requestNotifications()

    assertFalse(controller.access.value.supportsLiveUpdates)
    assertEquals(0, provider.reads)
    assertEquals(0, provider.requests)
  }
}

/** Controls permission responses and records platform requests in settings tests. */
private class FakeProvider : NotificationPermissionProvider {
  var matchAlertsSupported = false
  var supported = true
  var requiresPermission = true
  var activitiesEnabled: Boolean? = true
  var reads = 0
  var requests = 0
  var settingsOpened = 0
  var promotionSettingsOpened = 0
  var promotionAllowed: Boolean? = null
  var readResult: ((NotificationAuthorization) -> Unit)? = null
  var requestResult: ((NotificationAuthorization) -> Unit)? = null
  override fun canPromoteNotifications(): Boolean? = promotionAllowed
  override fun supportsLiveUpdates(): Boolean = supported
  override fun supportsMatchAlerts(): Boolean = matchAlertsSupported
  override fun requiresNotificationPermission(): Boolean = requiresPermission
  override fun areLiveActivitiesEnabled(): Boolean? = activitiesEnabled
  override fun readNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    reads++
    readResult = onResult
  }
  override fun requestNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    requests++
    requestResult = onResult
  }
  override fun openSettings() { settingsOpened++ }
  override fun openPromotionSettings() { promotionSettingsOpened++ }
}
