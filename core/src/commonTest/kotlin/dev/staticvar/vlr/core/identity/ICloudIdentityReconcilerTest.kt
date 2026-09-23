/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** Verifies cloud reconciliation against delayed downloads and rejected writes. */
class ICloudIdentityReconcilerTest {
  @Test
  fun cloudIdentityArrivingBeforeRetryKeepsLocalIdentityAndQueuesCleanup() {
    val fixture = Fixture()
    val provisionalId = fixture.repository.id.value
    fixture.reconciler.seed()
    assertEquals(listOf(provisionalId.toString()), fixture.cloud.writes)

    fixture.reconciler.onInitialSyncChange()
    fixture.cloud.value = CloudId.toString()
    fixture.scheduler.runNext()

    assertEquals(provisionalId, fixture.repository.id.value)
    assertEquals(CloudId, fixture.repository.pendingTokenCleanup.value)
    assertTrue(fixture.cloud.writes.all { it == provisionalId.toString() })
  }

  @Test
  fun rejectedSeedIsRetriedWithFreshCloudReadsAndBoundedDelays() {
    val fixture = Fixture()
    fixture.cloud.rejectWrites = true
    fixture.reconciler.seed()
    fixture.reconciler.onInitialSyncChange()

    while (fixture.scheduler.hasPending()) fixture.scheduler.runNext()

    assertEquals(4, fixture.cloud.writes.size)
    assertTrue(fixture.cloud.writes.all { it == fixture.repository.id.value.toString() })
    assertEquals(listOf(1_000L, 3_000L, 9_000L), fixture.scheduler.delays)
    assertTrue(fixture.repository.observeCloudIdentity(CloudId.toString()))
    assertEquals(CloudId, fixture.repository.pendingTokenCleanup.value)
  }

  @Test
  fun validCloudIdentityIsNeverOverwrittenWhenLocalIdentityIsEstablished() {
    val existingCloud = Fixture(localId = LocalId, cloudId = CloudId.toString())
    existingCloud.reconciler.seed()
    existingCloud.reconciler.onInitialSyncChange()
    assertEquals(LocalId, existingCloud.repository.id.value)
    assertTrue(existingCloud.cloud.writes.isEmpty())
    assertFalse(existingCloud.scheduler.hasPending())

    val lateCloud = Fixture(localId = LocalId)
    lateCloud.reconciler.seed()
    lateCloud.reconciler.onInitialSyncChange()
    lateCloud.cloud.value = CloudId.toString()
    lateCloud.scheduler.runNext()
    assertEquals(LocalId, lateCloud.repository.id.value)
    assertEquals(listOf(LocalId.toString()), lateCloud.cloud.writes)
    assertFalse(lateCloud.scheduler.hasPending())
  }

  @Test
  fun accountChangeAndCloseCancelOutstandingRetries() {
    for (stop in listOf<(ICloudIdentityReconciler) -> Unit>(
      { it.onAccountChange() },
      { it.close() },
    )) {
      val fixture = Fixture()
      fixture.reconciler.seed()
      fixture.reconciler.onInitialSyncChange()
      val writesBeforeStop = fixture.cloud.writes.toList()

      stop(fixture.reconciler)
      assertFalse(fixture.scheduler.hasPending())
      fixture.scheduler.runCancelledAction()
      fixture.reconciler.onInitialSyncChange()
      fixture.reconciler.seed()

      assertEquals(writesBeforeStop, fixture.cloud.writes)
    }
  }

  @Test
  fun accountChangePreventsAnotherAccountsUuidFromBeingRetiredAfterRestart() {
    val fixture = Fixture()
    fixture.reconciler.seed()
    fixture.reconciler.onAccountChange()

    val restarted = fixture.restartRepository()
    assertFalse(restarted.observeCloudIdentity(CloudId.toString()))
    assertEquals(null, restarted.pendingTokenCleanup.value)
  }

  @Test
  fun completedDeletionDoesNotStopCloudRewriteAfterRestart() {
    val fixture = Fixture()
    fixture.reconciler.seed()
    fixture.cloud.rejectWrites = true
    fixture.cloud.value = CloudId.toString()
    fixture.reconciler.onServerChange()
    fixture.repository.markTokenCleanupComplete(CloudId)
    val writesBeforeRestart = fixture.cloud.writes.size

    val restarted = fixture.restartRepository()
    ICloudIdentityReconciler(
      restarted,
      readCloudId = { fixture.cloud.value },
      writeCloudId = fixture.cloud::write,
      scheduleRetry = fixture.scheduler::schedule,
    ).seed()

    assertEquals(writesBeforeRestart + 1, fixture.cloud.writes.size)
    assertEquals(restarted.id.value.toString(), fixture.cloud.writes.last())
    assertEquals(null, restarted.pendingTokenCleanup.value)
  }

  @Test
  fun serverChangeRetainsGeneratedIdentityAndPublishesItToCloud() {
    val fixture = Fixture()
    val generated = fixture.repository.id.value
    fixture.reconciler.seed()
    fixture.reconciler.onInitialSyncChange()
    fixture.cloud.value = CloudId.toString()

    fixture.reconciler.onServerChange()

    assertEquals(generated, fixture.repository.id.value)
    assertEquals(CloudId, fixture.repository.pendingTokenCleanup.value)
    assertEquals(generated.toString(), fixture.cloud.writes.last())
    fixture.repository.markTokenCleanupComplete(CloudId)
    fixture.cloud.value = CloudId.toString()
    assertTrue(fixture.repository.observeCloudIdentity(fixture.cloud.value))
  }

  /** Holds one local repository, cloud store, and retry scheduler for a sequence. */
  private class Fixture(localId: Uuid? = null, cloudId: String? = null) {
    private val settings = MapSettings().apply {
      localId?.let { putString(UserIdentityRepository.IdentityKey, it.toString()) }
    }
    val repository = UserIdentityRepository(settings, trackCloudBackup = true)
    val cloud = FakeCloud(cloudId)
    val scheduler = FakeScheduler()
    val reconciler = ICloudIdentityReconciler(
      repository,
      readCloudId = { cloud.value },
      writeCloudId = cloud::write,
      scheduleRetry = scheduler::schedule,
    )

    fun restartRepository(): UserIdentityRepository = UserIdentityRepository(settings, trackCloudBackup = true)
  }

  /** Mimics the local iCloud cache, including an initial write rejection. */
  private class FakeCloud(var value: String?) {
    var rejectWrites = false
    val writes = mutableListOf<String>()

    fun write(id: String) {
      writes += id
      if (!rejectWrites) value = id
    }
  }

  /** Runs scheduled callbacks only when a test advances them. */
  private class FakeScheduler {
    /** A callback remains pending until run or canceled. */
    private class Task(val action: () -> Unit, var pending: Boolean = true)
    private val tasks = mutableListOf<Task>()
    val delays = mutableListOf<Long>()

    fun schedule(delayMillis: Long, action: () -> Unit): () -> Unit {
      delays += delayMillis
      val task = Task(action)
      tasks += task
      return { task.pending = false }
    }

    fun hasPending(): Boolean = tasks.any { it.pending }

    fun runNext() {
      val task = tasks.first { it.pending }
      task.pending = false
      task.action()
    }

    fun runCancelledAction() {
      tasks.last().action()
    }
  }

  /** Distinct UUIDs make cloud restoration and overwrite checks observable. */
  private companion object {
    val LocalId = Uuid.parse("01996ff9-3000-7000-8000-000000000001")
    val CloudId = Uuid.parse("01996ff9-3000-7000-8000-000000000002")
  }
}
