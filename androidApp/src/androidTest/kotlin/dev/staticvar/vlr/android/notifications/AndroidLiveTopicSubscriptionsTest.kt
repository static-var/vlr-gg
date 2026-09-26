/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import dev.staticvar.vlr.domain.repository.FavoriteTopicMode
import dev.staticvar.vlr.domain.repository.FavoriteTopicOperation
import dev.staticvar.vlr.domain.repository.FavoriteTopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AndroidLiveTopicSubscriptionsTest {
  @Test
  fun tokenRotationInvalidatesDatabaseBeforeSavingNewToken() = runBlocking {
    val events = mutableListOf<String>()
    val topics = FakeTopicRepository(events)
    val transport = FakeTransport(events, token = "new-token")

    reconcileLiveTopicSubscriptions(topics, { FavoriteTopicMode.Live }, "old-token", transport) {
      events += "token:$it"
    }

    assertEquals(listOf("invalidate", "token:new-token", "read:Live"), events)
  }

  @Test
  fun failedSubscriptionRemainsPendingAndRetriesAfterCompletedWork() = runBlocking {
    val events = mutableListOf<String>()
    val first = FavoriteTopicOperation.Subscribe("live-match-44")
    val second = FavoriteTopicOperation.Subscribe("live-team-55")
    val topics = FakeTopicRepository(events, first, second)
    val transport = FakeTransport(events, failSubscription = second.topic)

    try {
      reconcileLiveTopicSubscriptions(topics, { FavoriteTopicMode.Live }, "token", transport) {}
      fail("Expected subscription failure")
    } catch (expected: IllegalStateException) {
      assertEquals("subscription failed", expected.message)
    }
    assertEquals(listOf(second), topics.pending)
    assertEquals(listOf(first), topics.acknowledged)

    val retry = FakeTransport(events)
    reconcileLiveTopicSubscriptions(topics, { FavoriteTopicMode.Live }, "token", retry) {}
    assertEquals(listOf(second.topic), retry.subscribed)
    assertTrue(topics.pending.isEmpty())
  }

  @Test
  fun failedUnsubscribeIsNotAcknowledged() = runBlocking {
    val events = mutableListOf<String>()
    val operation = FavoriteTopicOperation.Unsubscribe("live-event-22")
    val topics = FakeTopicRepository(events, operation)
    val transport = FakeTransport(events, failUnsubscription = operation.topic)

    try {
      reconcileLiveTopicSubscriptions(topics, { FavoriteTopicMode.Disabled }, "token", transport) {}
      fail("Expected unsubscribe failure")
    } catch (expected: IllegalStateException) {
      assertEquals("unsubscribe failed", expected.message)
    }

    assertEquals(listOf(operation), topics.pending)
    assertTrue(topics.acknowledged.isEmpty())
  }

  @Test
  fun rechecksModeAfterEachNetworkOperation() = runBlocking {
    val events = mutableListOf<String>()
    val operation = FavoriteTopicOperation.Subscribe("live-match-44")
    val topics = FakeTopicRepository(events, operation)
    var mode = FavoriteTopicMode.Live
    val transport = FakeTransport(events, afterSubscribe = { mode = FavoriteTopicMode.Disabled })

    reconcileLiveTopicSubscriptions(topics, { mode }, "token", transport) {}

    assertEquals(
      listOf("read:Live", "subscribe:live-match-44", "ack:live-match-44", "read:Disabled"),
      events,
    )
  }
}

private class FakeTopicRepository(
  private val events: MutableList<String>,
  vararg operations: FavoriteTopicOperation,
) : FavoriteTopicRepository {
  val pending = operations.toMutableList()
  val acknowledged = mutableListOf<FavoriteTopicOperation>()

  override fun observeChanges(): Flow<Unit> = flowOf(Unit)
  override suspend fun importSubscriptions(topics: Set<String>) = Unit
  override suspend fun nextOperation(mode: FavoriteTopicMode): FavoriteTopicOperation? {
    events += "read:$mode"
    return pending.firstOrNull()
  }
  override suspend fun acknowledge(operation: FavoriteTopicOperation) {
    events += "ack:${operation.topic}"
    acknowledged += operation
    pending.remove(operation)
  }
  override suspend fun invalidateAcknowledgements() {
    events += "invalidate"
  }
}

private class FakeTransport(
  private val events: MutableList<String>,
  private val token: String = "token",
  private val failSubscription: String? = null,
  private val failUnsubscription: String? = null,
  private val afterSubscribe: () -> Unit = {},
) : LiveTopicSubscriptionTransport {
  val subscribed = mutableListOf<String>()

  override suspend fun currentToken(): String = token
  override suspend fun subscribe(topic: String) {
    events += "subscribe:$topic"
    subscribed += topic
    if (topic == failSubscription) error("subscription failed")
    afterSubscribe()
  }
  override suspend fun unsubscribe(topic: String) {
    events += "unsubscribe:$topic"
    if (topic == failUnsubscription) error("unsubscribe failed")
  }
}
