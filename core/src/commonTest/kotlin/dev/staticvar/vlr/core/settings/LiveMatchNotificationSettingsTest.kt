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
}

private class FakeProvider : NotificationPermissionProvider {
  var activitiesEnabled: Boolean? = true
  var requests = 0
  var settingsOpened = 0
  var readResult: ((NotificationAuthorization) -> Unit)? = null
  var requestResult: ((NotificationAuthorization) -> Unit)? = null
  override fun areLiveActivitiesEnabled(): Boolean? = activitiesEnabled
  override fun readNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) { readResult = onResult }
  override fun requestNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    requests++
    requestResult = onResult
  }
  override fun openSettings() { settingsOpened++ }
}
