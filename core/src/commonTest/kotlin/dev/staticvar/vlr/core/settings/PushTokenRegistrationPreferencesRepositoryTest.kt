/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.notifications.PushPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PushTokenRegistrationPreferencesRepositoryTest {
  @Test
  fun tokenAndSuccessfulUploadArePersistedSeparately() {
    val storage = MapSettings()
    val repository = PushTokenRegistrationPreferencesRepository(storage)

    repository.storeToken(PushPlatform.Android, "opaque token:+/=")

    assertEquals("opaque token:+/=", repository.preferences.value.token)
    assertFalse(repository.preferences.value.wasUploaded(ClientId, PushPlatform.Android, "opaque token:+/="))

    repository.markUploaded(ClientId, PushPlatform.Android, "opaque token:+/=")
    val restored = PushTokenRegistrationPreferencesRepository(storage).preferences.value

    assertTrue(restored.wasUploaded(ClientId, PushPlatform.Android, "opaque token:+/="))
    assertFalse(restored.wasUploaded(OtherClientId, PushPlatform.Android, "opaque token:+/="))
    assertFalse(restored.wasUploaded(ClientId, PushPlatform.Ios, "opaque token:+/="))
    assertFalse(restored.wasUploaded(ClientId, PushPlatform.Android, "rotated-token"))
  }

  private companion object {
    const val ClientId: String = "01996ff9-3000-7000-8000-000000000001"
    const val OtherClientId: String = "01996ff9-3000-7000-8000-000000000002"
  }
}
