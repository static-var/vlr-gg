/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.uuid.Uuid

class UserIdentityRepositoryTest {
  @Test
  fun firstLaunchGeneratesV7AndRelaunchReusesIt() {
    val storage = MapSettings()
    val first = UserIdentityRepository(storage).id.value

    assertEquals('7', first.toString()[14])
    assertEquals(first.toString(), storage.getStringOrNull(UserIdentityRepository.IdentityKey))
    assertEquals(first, UserIdentityRepository(storage).id.value)
  }

  @Test
  fun backupRestoresMissingOrCorruptLocalIdentityWithoutGeneratingAnother() {
    for (local in listOf(null, "broken", Uuid.NIL.toString())) {
      val storage = MapSettings()
      local?.let { storage.putString(UserIdentityRepository.IdentityKey, it) }

      assertEquals(CloudId, UserIdentityRepository(storage, CloudId.toString()).id.value)
      assertEquals(CloudId, UserIdentityRepository(storage).id.value)
    }
  }

  @Test
  fun localIdentityIsUsedBeforeCloudSynchronization() {
    val storage = MapSettings().apply { putString(UserIdentityRepository.IdentityKey, LocalId.toString()) }

    val repository = UserIdentityRepository(storage, CloudId.toString(), allowDelayedRestore = true)
    repository.restoreFromBackup(CloudId.toString())
    assertEquals(LocalId, repository.id.value)
  }

  @Test
  fun delayedRestoreReplacesGeneratedIdentityAndSurvivesRelaunch() {
    val storage = MapSettings()
    val repository = UserIdentityRepository(storage, allowDelayedRestore = true)
    assertNotEquals(CloudId, repository.id.value)

    repository.restoreFromBackup(CloudId.toString())

    assertEquals(CloudId, repository.id.value)
    assertEquals(CloudId, UserIdentityRepository(storage).id.value)
  }

  @Test
  fun pendingRestoreSurvivesRelaunchAndCachedWritesAreNotCloudConfirmation() {
    val storage = MapSettings()
    val generated = UserIdentityRepository(storage, allowDelayedRestore = true).id.value
    val restarted = UserIdentityRepository(storage, generated.toString(), allowDelayedRestore = true)
    restarted.restoreFromBackup(generated.toString(), confirmedByCloud = false)
    restarted.restoreFromBackup(CloudId.toString())
    assertEquals(CloudId, restarted.id.value)

    restarted.restoreFromBackup(LocalId.toString())
    assertEquals(CloudId, restarted.id.value)
    assertEquals(CloudId, UserIdentityRepository(storage, allowDelayedRestore = true).id.value)
  }

  @Test
  fun restoredAndroidPreferencesAreReusedAtStartup() {
    val restoredPreferences = MapSettings().apply {
      putString(UserIdentityRepository.IdentityKey, CloudId.toString())
    }

    assertEquals(CloudId, UserIdentityRepository(restoredPreferences).id.value)
  }

  @Test
  fun invalidOrRemovedCloudValuesDoNotEraseTheLocalIdentity() {
    val repository = UserIdentityRepository(MapSettings(), allowDelayedRestore = true)
    val original = repository.id.value

    for (value in listOf(null, "", "broken", Uuid.NIL.toString(), "ffffffff-ffff-ffff-ffff-ffffffffffff")) {
      repository.restoreFromBackup(value)
      assertEquals(original, repository.id.value)
    }
  }

  private companion object {
    val LocalId: Uuid = Uuid.parse("01996ff9-3000-7000-8000-000000000001")
    val CloudId: Uuid = Uuid.parse("01996ff9-3000-7000-8000-000000000002")
  }
}
