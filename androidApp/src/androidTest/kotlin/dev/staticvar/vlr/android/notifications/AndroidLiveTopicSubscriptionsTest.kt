/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Checks topic synchronization after token changes, disabling, and partial failures. */
class AndroidLiveTopicSubscriptionsTest {
  @Test
  fun reminderTopicsIncludeAllFavoritesUsingCachedPlayerTeams() {
    val snapshot = DirectFavoriteSnapshot(
      teams = listOf(DirectFavorite.Team("11", "Team", "")),
      events = listOf(DirectFavorite.Event("22", "Event", "")),
      matches = listOf(DirectFavorite.Match("33", "Match", "")),
      players = listOf(
        DirectFavorite.Player("44", "Player", "", currentTeamId = "55"),
        DirectFavorite.Player("66", "Teammate", "", currentTeamId = "11"),
        DirectFavorite.Player("77", "Unknown team", ""),
        DirectFavorite.Player("88", "Blank team", "", currentTeamId = ""),
      ),
    )

    assertEquals(
      setOf("team-11", "event-22", "match-33", "team-55"),
      snapshot.notificationTopics(liveUpdatesEnabled = false),
    )
    assertEquals(
      setOf(
        "live-team-11", "live-event-22", "live-match-33",
        "live-player-44", "live-player-66", "live-player-77", "live-player-88",
      ),
      snapshot.notificationTopics(liveUpdatesEnabled = true),
    )
  }

  @Test
  fun switchingToLiveTopicsRemovesReminderTopicsFirst() = runBlocking {
    val transport = FakeTransport(token = "token-a")
    val saved = mutableListOf<LiveTopicSubscriptionState>()

    reconcileLiveTopicSubscriptions(
      desired = setOf("live-team-11"),
      previous = LiveTopicSubscriptionState(token = "token-a", topics = setOf("team-11")),
      transport = transport,
      save = saved::add,
    )

    assertEquals(listOf("team-11"), transport.unsubscribed)
    assertEquals(listOf("live-team-11"), transport.subscribed)
    assertEquals(setOf("live-team-11"), saved.last().topics)
  }

  @Test
  fun switchingToRemindersRemovesLiveTopicsFirst() = runBlocking {
    val transport = FakeTransport(token = "token-a")
    val saved = mutableListOf<LiveTopicSubscriptionState>()

    reconcileLiveTopicSubscriptions(
      desired = setOf("team-11"),
      previous = LiveTopicSubscriptionState(token = "token-a", topics = setOf("live-team-11")),
      transport = transport,
      save = saved::add,
    )

    assertEquals(listOf("live-team-11"), transport.unsubscribed)
    assertEquals(listOf("team-11"), transport.subscribed)
    assertEquals(setOf("team-11"), saved.last().topics)
  }

  @Test
  fun tokenRotationResubscribesDesiredTopics() = runBlocking {
    val topic = "live-team-11"
    val transport = FakeTransport(token = "token-b")
    val saved = mutableListOf<LiveTopicSubscriptionState>()

    reconcileLiveTopicSubscriptions(
      desired = setOf(topic),
      previous = LiveTopicSubscriptionState(token = "token-a", topics = setOf(topic)),
      transport = transport,
      save = saved::add,
    )

    assertTrue(transport.unsubscribed.isEmpty())
    assertEquals(listOf(topic), transport.subscribed)
    assertEquals(
      listOf(
        LiveTopicSubscriptionState(token = "token-b", topics = emptySet()),
        LiveTopicSubscriptionState(token = "token-b", topics = setOf(topic)),
      ),
      saved,
    )
  }

  @Test
  fun blockedNotificationsUnsubscribeEveryStoredTopic() = runBlocking {
    val first = "live-event-22"
    val second = "live-player-33"
    val transport = FakeTransport(token = "token-a")
    val saved = mutableListOf<LiveTopicSubscriptionState>()

    reconcileLiveTopicSubscriptions(
      desired = emptySet(),
      previous = LiveTopicSubscriptionState(token = "token-a", topics = linkedSetOf(first, second)),
      transport = transport,
      save = saved::add,
    )

    assertEquals(listOf(first, second), transport.unsubscribed)
    assertTrue(transport.subscribed.isEmpty())
    assertEquals(emptySet<String>(), saved.last().topics)
  }

  @Test
  fun partialFailurePersistsOnlyCompletedSubscriptions() = runBlocking {
    val first = "live-match-44"
    val second = "live-team-55"
    val transport = FakeTransport(token = "token-a", failSubscription = second)
    val saved = mutableListOf<LiveTopicSubscriptionState>()

    try {
      reconcileLiveTopicSubscriptions(
        desired = linkedSetOf(first, second),
        previous = LiveTopicSubscriptionState(token = "token-a", topics = emptySet()),
        transport = transport,
        save = saved::add,
      )
      fail("Expected the second subscription to fail")
    } catch (expected: IllegalStateException) {
      assertEquals("subscription failed", expected.message)
    }

    assertEquals(listOf(first, second), transport.subscribed)
    assertEquals(listOf(LiveTopicSubscriptionState(token = "token-a", topics = setOf(first))), saved)
  }
}

/** Records topic operations and can fail a subscription for tests. */
private class FakeTransport(
  private val token: String,
  private val failSubscription: String? = null,
) : LiveTopicSubscriptionTransport {
  val subscribed = mutableListOf<String>()
  val unsubscribed = mutableListOf<String>()

  override suspend fun currentToken(): String = token

  override suspend fun subscribe(topic: String) {
    subscribed += topic
    if (topic == failSubscription) throw IllegalStateException("subscription failed")
  }

  override suspend fun unsubscribe(topic: String) {
    unsubscribed += topic
  }
}
