/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.repository.FavoriteTopicMode
import dev.staticvar.vlr.domain.repository.FavoriteTopicOperation
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoriteTopicRepositoryImplTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = object : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var repository: FavoriteTopicRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    database = VlrDatabase(driver)
    repository = FavoriteTopicRepositoryImpl(database, dispatchers)
  }

  @AfterTest
  fun teardown() = driver.close()

  @Test
  fun sharedTeamTopicTransfersOwnershipWithoutUnsubscribing() = runTest(dispatcher) {
    player("one", "team")
    player("two", "team")
    database.teamsQueries.addFavoriteTeam("team")
    assertEquals(subscribe("team-team"), repository.nextOperation(FavoriteTopicMode.TeamAlerts))
    repository.acknowledge(subscribe("team-team"))
    database.teamsQueries.removeFavoriteTeam("team")
    database.playersQueries.removeFavoritePlayer("one")
    assertNull(repository.nextOperation(FavoriteTopicMode.TeamAlerts))
    database.playersQueries.removeFavoritePlayer("two")
    assertEquals(unsubscribe("team-team"), repository.nextOperation(FavoriteTopicMode.TeamAlerts))
  }

  @Test
  fun teamAlertsSkipPlayersWithoutTeamsWhileLiveUsesPlayerTopics() = runTest(dispatcher) {
    player("blank", " ")
    player("missing", "temporary")
    driver.execute(null, "UPDATE players SET current_team_id = NULL WHERE id = 'missing'", 0)
    assertNull(repository.nextOperation(FavoriteTopicMode.TeamAlerts))
    assertEquals(subscribe("live-player-blank"), repository.nextOperation(FavoriteTopicMode.Live))
    repository.acknowledge(subscribe("live-player-blank"))
    assertEquals(subscribe("live-player-missing"), repository.nextOperation(FavoriteTopicMode.Live))
    repository.acknowledge(subscribe("live-player-missing"))
    assertNull(repository.nextOperation(FavoriteTopicMode.Live))
  }

  @Test
  fun playerTradeUnsubscribesOldTeamBeforeSubscribingNewTeam() = runTest(dispatcher) {
    player("player", "old")
    repository.nextOperation(FavoriteTopicMode.TeamAlerts)
    repository.acknowledge(subscribe("team-old"))
    driver.execute(null, "UPDATE players SET current_team_id = 'new' WHERE id = 'player'", 0)
    assertEquals(unsubscribe("team-old"), repository.nextOperation(FavoriteTopicMode.TeamAlerts))
    repository.acknowledge(unsubscribe("team-old"))
    assertEquals(subscribe("team-new"), repository.nextOperation(FavoriteTopicMode.TeamAlerts))
  }

  @Test
  fun failedUnsubscribeSurvivesRecreationWhileFavoriteIsHidden() = runTest(dispatcher) {
    database.teamsQueries.addFavoriteTeam("team")
    repository.nextOperation(FavoriteTopicMode.Live)
    repository.acknowledge(subscribe("live-team-team"))
    database.teamsQueries.removeFavoriteTeam("team")
    assertEquals(emptySet(), FavoritesRepositoryImpl(database, dispatchers).observeTeamIds().first())
    val recreated = FavoriteTopicRepositoryImpl(VlrDatabase(driver), dispatchers)
    assertEquals(unsubscribe("live-team-team"), recreated.nextOperation(FavoriteTopicMode.Live))
    assertEquals(unsubscribe("live-team-team"), recreated.nextOperation(FavoriteTopicMode.Live))
    recreated.acknowledge(unsubscribe("live-team-team"))
    assertNull(recreated.nextOperation(FavoriteTopicMode.Live))
    assertEquals(emptyList(), database.favoriteTopicsQueries.getFavoriteTopicRecords().executeAsList())
  }

  @Test
  fun subscribeFailureRetainsStagedOperationAfterRecreation() = runTest(dispatcher) {
    database.eventsQueries.addFavoriteEvent("event")
    assertEquals(subscribe("live-event-event"), repository.nextOperation(FavoriteTopicMode.Live))
    assertEquals(
      subscribe("live-event-event"),
      FavoriteTopicRepositoryImpl(VlrDatabase(driver), dispatchers).nextOperation(FavoriteTopicMode.Live),
    )
  }

  @Test
  fun unsubscribeAcknowledgementAfterReaddSchedulesSubscribe() = runTest(dispatcher) {
    database.matchesQueries.addFavoriteMatch("match")
    repository.nextOperation(FavoriteTopicMode.Live)
    repository.acknowledge(subscribe("live-match-match"))
    database.matchesQueries.removeFavoriteMatch("match")
    val pending = requireNotNull(repository.nextOperation(FavoriteTopicMode.Live))
    database.matchesQueries.addFavoriteMatch("match")
    repository.acknowledge(pending)
    assertEquals(subscribe("live-match-match"), repository.nextOperation(FavoriteTopicMode.Live))
    assertEquals(1L, database.favoriteTopicsQueries.getFavoriteTopicRecords().executeAsList().single().active)
  }

  @Test
  fun subscribeAcknowledgementAfterRemovalDoesNotReviveFavorite() = runTest(dispatcher) {
    database.teamsQueries.addFavoriteTeam("team")
    val pending = requireNotNull(repository.nextOperation(FavoriteTopicMode.Live))
    database.teamsQueries.removeFavoriteTeam("team")
    repository.acknowledge(pending)
    assertEquals(emptySet(), FavoritesRepositoryImpl(database, dispatchers).observeTeamIds().first())
    assertEquals(unsubscribe("live-team-team"), repository.nextOperation(FavoriteTopicMode.Live))
  }

  @Test
  fun tokenInvalidationResubscribesAcknowledgedTopics() = runTest(dispatcher) {
    database.teamsQueries.addFavoriteTeam("team")
    repository.nextOperation(FavoriteTopicMode.Live)
    repository.acknowledge(subscribe("live-team-team"))
    assertNull(repository.nextOperation(FavoriteTopicMode.Live))
    repository.invalidateAcknowledgements()
    assertEquals(subscribe("live-team-team"), repository.nextOperation(FavoriteTopicMode.Live))
  }

  @Test
  fun reminderTopicsIncludeAllFavoritesAndDeduplicatePlayerTeams() = runTest(dispatcher) {
    database.teamsQueries.addFavoriteTeam("team")
    database.matchesQueries.addFavoriteMatch("match")
    database.eventsQueries.addFavoriteEvent("event")
    player("one", "team")
    player("two", "other")
    player("unknown", " ")
    database.teamsQueries.addFavoriteTeam("removed")
    database.teamsQueries.removeFavoriteTeam("removed")

    assertEquals(setOf("team-team", "team-other", "match-match", "event-event"), repository.reminderTopics())
    val subscriptions = reconcile(FavoriteTopicMode.TeamAlerts)
    assertEquals(repository.reminderTopics(), subscriptions.map { it.topic }.toSet())
  }

  @Test
  fun modeChangesRemoveOldTopicsBeforeAddingNewTopics() = runTest(dispatcher) {
    database.teamsQueries.addFavoriteTeam("team")
    database.matchesQueries.addFavoriteMatch("match")
    database.eventsQueries.addFavoriteEvent("event")
    player("player", "team")
    val reminders = setOf("team-team", "match-match", "event-event")
    val live = setOf("live-team-team", "live-match-match", "live-event-event", "live-player-player")
    assertEquals(reminders, reconcile(FavoriteTopicMode.TeamAlerts).map { it.topic }.toSet())

    val toLive = reconcile(FavoriteTopicMode.Live)
    assertEquals(reminders, toLive.take(reminders.size).map { it.topic }.toSet())
    assertEquals(true, toLive.take(reminders.size).all { it is FavoriteTopicOperation.Unsubscribe })
    assertEquals(live, toLive.drop(reminders.size).map { it.topic }.toSet())
    assertEquals(true, toLive.drop(reminders.size).all { it is FavoriteTopicOperation.Subscribe })

    val toReminders = reconcile(FavoriteTopicMode.TeamAlerts)
    assertEquals(live, toReminders.take(live.size).map { it.topic }.toSet())
    assertEquals(true, toReminders.take(live.size).all { it is FavoriteTopicOperation.Unsubscribe })
    assertEquals(reminders, toReminders.drop(live.size).map { it.topic }.toSet())
    assertEquals(true, toReminders.drop(live.size).all { it is FavoriteTopicOperation.Subscribe })
  }

  @Test
  fun settledReconciliationDoesNotInvalidateObservers() = runTest(dispatcher) {
    database.teamsQueries.addFavoriteTeam("team")
    repository.nextOperation(FavoriteTopicMode.Live)
    repository.acknowledge(subscribe("live-team-team"))
    repository.observeChanges().test {
      awaitItem()
      assertNull(repository.nextOperation(FavoriteTopicMode.Live))
      runCurrent()
      expectNoEvents()
    }
  }

  @Test
  fun disabledNotificationsRemoveSubscriptionWithoutRemovingFavorite() = runTest(dispatcher) {
    database.teamsQueries.addFavoriteTeam("team")
    repository.nextOperation(FavoriteTopicMode.Live)
    repository.acknowledge(subscribe("live-team-team"))
    assertEquals(unsubscribe("live-team-team"), repository.nextOperation(FavoriteTopicMode.Disabled))
    repository.acknowledge(unsubscribe("live-team-team"))
    assertNull(repository.nextOperation(FavoriteTopicMode.Disabled))
    assertEquals(setOf("team"), FavoritesRepositoryImpl(database, dispatchers).observeTeamIds().first())
    assertEquals(subscribe("live-team-team"), repository.nextOperation(FavoriteTopicMode.Live))
  }

  private suspend fun reconcile(mode: FavoriteTopicMode): List<FavoriteTopicOperation> = buildList {
    repeat(20) {
      val operation = repository.nextOperation(mode) ?: return@buildList
      add(operation)
      repository.acknowledge(operation)
    }
    error("Topic reconciliation did not settle")
  }

  private fun player(id: String, team: String) {
    driver.execute(null, "INSERT INTO players(id, name, country, current_team_id) VALUES ('$id', '$id', '', '$team')", 0)
    database.playersQueries.addFavoritePlayer(id)
  }

  private fun subscribe(topic: String) = FavoriteTopicOperation.Subscribe(topic)
  private fun unsubscribe(topic: String) = FavoriteTopicOperation.Unsubscribe(topic)
}
