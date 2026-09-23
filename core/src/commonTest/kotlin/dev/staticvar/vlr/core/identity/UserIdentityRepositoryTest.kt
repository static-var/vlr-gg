/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** Verifies stable per-install UUIDs and durable old-token cleanup provenance. */
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
  fun cachedBackupRestoresOnlyWhenLocalIdentityIsMissingOrInvalid() {
    for (local in listOf(null, "broken", Uuid.NIL.toString())) {
      val storage = MapSettings()
      local?.let { storage.putString(UserIdentityRepository.IdentityKey, it) }

      assertEquals(CloudId, UserIdentityRepository(storage, CloudId.toString()).id.value)
      assertEquals(CloudId, UserIdentityRepository(storage).id.value)
    }
  }

  @Test
  fun establishedLocalIdentityDoesNotRetireUnrelatedCloudUuid() {
    val storage = MapSettings().apply {
      putString(UserIdentityRepository.IdentityKey, LocalId.toString())
      putBoolean("identity.restorePending", true)
    }
    val repository = UserIdentityRepository(storage, CloudId.toString(), trackCloudBackup = true)

    assertFalse(repository.observeCloudIdentity(CloudId.toString(), confirmedByCloud = true))
    assertEquals(LocalId, repository.id.value)
    assertNull(repository.pendingTokenCleanup.value)
  }

  @Test
  fun lateBackupKeepsGeneratedUuidAndPersistsOldTokenCleanupAcrossRestart() {
    val storage = MapSettings()
    val first = UserIdentityRepository(storage, trackCloudBackup = true)
    val generated = first.id.value
    assertNotEquals(CloudId, generated)

    assertTrue(first.observeCloudIdentity(CloudId.toString()))
    assertEquals(generated, first.id.value)
    assertEquals(CloudId, first.pendingTokenCleanup.value)

    val restarted = UserIdentityRepository(storage, CloudId.toString(), trackCloudBackup = true)
    assertEquals(generated, restarted.id.value)
    assertEquals(CloudId, restarted.pendingTokenCleanup.value)
    assertTrue(restarted.observeCloudIdentity(CloudId.toString()))
    restarted.markTokenCleanupComplete(CloudId)
    assertNull(restarted.pendingTokenCleanup.value)
    assertTrue(UserIdentityRepository(storage).observeCloudIdentity(CloudId.toString()))
  }

  @Test
  fun equalUnconfirmedCacheDoesNotEndLateBackupWindow() {
    val storage = MapSettings()
    val generated = UserIdentityRepository(storage, trackCloudBackup = true).id.value
    val restarted = UserIdentityRepository(storage, generated.toString(), trackCloudBackup = true)

    assertFalse(restarted.observeCloudIdentity(generated.toString()))
    assertTrue(restarted.observeCloudIdentity(CloudId.toString()))
    assertEquals(generated, restarted.id.value)
    assertEquals(CloudId, restarted.pendingTokenCleanup.value)
  }

  @Test
  fun serverConfirmationOrAccountChangeEndsLateBackupWindow() {
    for (endWindow in listOf<(UserIdentityRepository) -> Unit>(
      { it.observeCloudIdentity(it.id.value.toString(), confirmedByCloud = true) },
      { it.stopAwaitingCloudBackup() },
    )) {
      val storage = MapSettings()
      val repository = UserIdentityRepository(storage, trackCloudBackup = true)
      endWindow(repository)

      assertFalse(UserIdentityRepository(storage, trackCloudBackup = true).observeCloudIdentity(CloudId.toString()))
      assertNull(UserIdentityRepository(storage).pendingTokenCleanup.value)
    }
  }

  @Test
  fun currentCloudConfirmationLeavesOldTokenDeletionPending() {
    val storage = MapSettings()
    val repository = UserIdentityRepository(storage, trackCloudBackup = true)
    repository.observeCloudIdentity(CloudId.toString())

    repository.observeCloudIdentity(repository.id.value.toString(), confirmedByCloud = true)

    assertEquals(CloudId, UserIdentityRepository(storage).pendingTokenCleanup.value)
    assertFalse(repository.observeCloudIdentity(LocalId.toString()))
  }

  @Test
  fun invalidCloudValuesAndActiveCleanupNeverChangeIdentity() {
    val repository = UserIdentityRepository(MapSettings(), trackCloudBackup = true)
    val original = repository.id.value

    for (value in listOf(null, "", "broken", Uuid.NIL.toString(), "ffffffff-ffff-ffff-ffff-ffffffffffff")) {
      assertFalse(repository.observeCloudIdentity(value))
      assertEquals(original, repository.id.value)
    }
    repository.markTokenCleanupComplete(original)
    assertNull(repository.pendingTokenCleanup.value)
  }

  /** Distinct UUIDs make local retention and cleanup observable. */
  private companion object {
    val LocalId: Uuid = Uuid.parse("01996ff9-3000-7000-8000-000000000001")
    val CloudId: Uuid = Uuid.parse("01996ff9-3000-7000-8000-000000000002")
  }
}
