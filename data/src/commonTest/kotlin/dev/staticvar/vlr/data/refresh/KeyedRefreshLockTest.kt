/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.refresh

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KeyedRefreshLockTest {
  @Test
  fun serializesSameKeyIncludingNewCallersAfterTheFirstFinishes() = runTest {
    val lock = KeyedRefreshLock()
    val releaseFirst = CompletableDeferred<Unit>()
    val releaseSecond = CompletableDeferred<Unit>()
    val started = mutableListOf<Int>()
    val first = async {
      lock.withLock("team") {
        started += 1
        releaseFirst.await()
      }
    }
    runCurrent()
    val second = async {
      lock.withLock("team") {
        started += 2
        releaseSecond.await()
      }
    }
    runCurrent()
    assertEquals(listOf(1), started)

    releaseFirst.complete(Unit)
    first.await()
    runCurrent()
    val third = async { lock.withLock("team") { started += 3 } }
    runCurrent()
    assertEquals(listOf(1, 2), started)

    releaseSecond.complete(Unit)
    second.await()
    third.await()
    assertEquals(listOf(1, 2, 3), started)
  }

  @Test
  fun unrelatedKeysCanRefreshWhileAnotherKeyIsBlocked() = runTest {
    val lock = KeyedRefreshLock()
    val release = CompletableDeferred<Unit>()
    val first = async { lock.withLock("first") { release.await() } }
    runCurrent()
    val second = async { lock.withLock("second") { "refreshed" } }
    runCurrent()

    assertTrue(second.isCompleted)
    assertEquals("refreshed", second.await())
    assertFalse(first.isCompleted)
    release.complete(Unit)
    first.await()
  }

  @Test
  fun cancellingWaiterPreservesExclusionAndCancellingHolderReleasesNextCaller() = runTest {
    val lock = KeyedRefreshLock()
    val holder = async { lock.withLock("team") { awaitCancellation() } }
    runCurrent()
    var cancelledWaiterRan = false
    val waiter = async { lock.withLock("team") { cancelledWaiterRan = true } }
    runCurrent()
    waiter.cancelAndJoin()

    val retry = async { lock.withLock("team") { "refreshed" } }
    runCurrent()
    assertFalse(cancelledWaiterRan)
    assertFalse(retry.isCompleted)

    holder.cancelAndJoin()
    assertEquals("refreshed", retry.await())
    assertEquals("again", lock.withLock("team") { "again" })
  }

  @Test
  fun failedActionReleasesKeyForRetry() = runTest {
    val lock = KeyedRefreshLock()
    val failure = IllegalStateException("refresh failed")

    assertSame(failure, assertFailsWith<IllegalStateException> {
      lock.withLock("team") { throw failure }
    })
    assertEquals("refreshed", lock.withLock("team") { "refreshed" })
  }
}
