/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VlrAppStateTest {
  @Test
  fun aboutOpensOnceUnderSettingsAndBackRestoresRoot() {
    val appState = VlrAppState(
      backStack = mutableListOf<NavKey>(AppRoute.Settings),
    )
    appState.showAbout()
    appState.showAbout()
    assertEquals(listOf<NavKey>(AppRoute.News, AppRoute.Settings, AppRoute.About), appState.backStack)
    assertEquals(AppRoute.Settings, AppRoute.About.rootDestination)
    assertEquals("settings", appState.selectedNavigationItemId)
    assertFalse(appState.shouldShowBottomNavigation)
    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.News, AppRoute.Settings), appState.backStack)
    assertTrue(appState.shouldShowBottomNavigation)
  }

  @Test
  fun selectRootClearsSecondaryRoutes() {
    val appState =
      VlrAppState(
        backStack = mutableListOf<NavKey>(AppRoute.News, AppRoute.MatchDetails(matchId = "match-1")),
      )

    appState.selectRoot(AppRoute.Events)

    assertEquals(listOf<NavKey>(AppRoute.News, AppRoute.Events), appState.backStack)
    assertEquals(AppRoute.Events, appState.selectedRootRoute)
    assertTrue(appState.canNavigateBack)
  }

  @Test
  fun initialHomeModeReconcilesAStackRestoredFromNewsMode() {
    val appState = VlrAppState(
      backStack = mutableListOf<NavKey>(
        AppRoute.News,
        AppRoute.Events,
        AppRoute.EventDetails("event-1"),
      ),
      initialHomeEnabled = true,
    )

    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.Events, AppRoute.EventDetails("event-1")),
      appState.backStack,
    )
    assertEquals(AppRoute.Events, appState.selectedRootRoute)
    assertFalse(appState.shouldShowBottomNavigation)
  }

  @Test
  fun initialNewsModeReconcilesAStackRestoredFromHomeMode() {
    val appState = VlrAppState(
      backStack = mutableListOf<NavKey>(AppRoute.Home, AppRoute.MatchDetails("match-1")),
      initialHomeEnabled = false,
    )

    assertEquals(
      listOf<NavKey>(AppRoute.News, AppRoute.MatchDetails("match-1")),
      appState.backStack,
    )
    assertEquals(AppRoute.News, appState.selectedRootRoute)
  }

  @Test
  fun liveFavoriteChangesReplaceOnlyTheBaseAndPreserveTheActiveTail() {
    val appState = VlrAppState(
      mutableListOf<NavKey>(AppRoute.News, AppRoute.Events, AppRoute.EventDetails("event-1")),
    )

    appState.updateHomeEnabled(enabled = true)
    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.Events, AppRoute.EventDetails("event-1")),
      appState.backStack,
    )

    appState.updateHomeEnabled(enabled = false)
    assertEquals(
      listOf<NavKey>(AppRoute.News, AppRoute.Events, AppRoute.EventDetails("event-1")),
      appState.backStack,
    )
  }

  @Test
  fun selectedNewsSurvivesIdempotentHomeUpdates() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.News))
    appState.updateHomeEnabled(enabled = true)
    appState.selectRoot(AppRoute.News)
    appState.showRootNewsArticle("article-1")

    appState.updateHomeEnabled(enabled = true)
    appState.updateHomeEnabled(enabled = true)

    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.News, AppRoute.NewsArticle("article-1")),
      appState.backStack,
    )
    assertEquals(AppRoute.News, appState.selectedRootRoute)
  }

  @Test
  fun homeSettingsAndAboutHaveBackNavigationWithoutSelectingSettings() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home), initialHomeEnabled = true)

    appState.showSettings()
    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Settings), appState.backStack)
    assertEquals(AppRoute.Home, appState.selectedRootRoute)
    assertFalse(appState.shouldShowBottomNavigation)

    appState.showAbout()
    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Settings), appState.backStack)

    appState.updateHomeEnabled(enabled = false)
    assertEquals(listOf<NavKey>(AppRoute.News, AppRoute.Settings), appState.backStack)
    assertEquals(AppRoute.Settings, appState.selectedRootRoute)
    assertTrue(appState.shouldShowBottomNavigation)
  }

  @Test
  fun backFromEverySecondaryRootReturnsToNews() {
    for (root in listOf(AppRoute.Matches, AppRoute.Events, AppRoute.Rankings, AppRoute.Settings)) {
      val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.News))
      appState.selectRoot(root)
      assertEquals(root, appState.selectedRootRoute)
      assertTrue(appState.canNavigateBack)

      appState.navigateUp()

      assertEquals(listOf<NavKey>(AppRoute.News), appState.backStack)
      assertEquals("news", appState.selectedNavigationItemId)
      assertFalse(appState.canNavigateBack)
      appState.navigateUp()
      assertEquals(listOf<NavKey>(AppRoute.News), appState.backStack)
    }
  }

  @Test
  fun switchingTabsKeepsOnlyNewsAndSelectedRoot() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.News))
    appState.selectRoot(AppRoute.Matches)
    appState.showMatchDetails("match-1")
    appState.selectRoot(AppRoute.Events)
    appState.selectRoot(AppRoute.Events)
    assertEquals(listOf<NavKey>(AppRoute.News, AppRoute.Events), appState.backStack)
    assertEquals("events", appState.selectedNavigationItemId)
    appState.selectRoot(AppRoute.News)
    assertEquals(listOf<NavKey>(AppRoute.News), appState.backStack)
    assertFalse(appState.canNavigateBack)
  }

  @Test
  fun replacingDetailPreservesSelectedRootBeforeReturningToNews() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.News))
    appState.selectRoot(AppRoute.Events)
    appState.showEventDetails("event-1")
    appState.showMatchDetails("match-1")
    appState.showRootEventDetails("event-2")
    assertEquals(
      listOf<NavKey>(AppRoute.News, AppRoute.Events, AppRoute.EventDetails("event-2")),
      appState.backStack,
    )
    assertEquals("events", appState.selectedNavigationItemId)
    appState.navigateUp()
    assertEquals(AppRoute.Events, appState.backStack.last())
    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.News), appState.backStack)
  }

  @Test
  fun showRootEventDetailsReplacesNestedTail() {
    val appState =
      VlrAppState(
        backStack =
        mutableListOf<NavKey>(
          AppRoute.Events,
          AppRoute.MatchDetails(matchId = "match-1"),
          AppRoute.PlayerDetails(playerId = "player-1"),
        ),
      )

    appState.showRootEventDetails(eventId = "event-9")

    assertEquals(
      listOf<NavKey>(AppRoute.News, AppRoute.Events, AppRoute.EventDetails(eventId = "event-9")),
      appState.backStack,
    )
    assertTrue(appState.canNavigateBack)
  }

  @Test
  fun rootPlayerDetailsReturnToHome() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home), initialHomeEnabled = true)

    appState.showRootPlayerDetails(playerId = "player-1")
    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.PlayerDetails(playerId = "player-1")),
      appState.backStack,
    )

    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
  }

  @Test
  fun rootDetailsDiscardPushedSettingsInHomeMode() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home), initialHomeEnabled = true)
    appState.showSettings()

    appState.showRootMatchDetails(matchId = "match-1")

    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.MatchDetails(matchId = "match-1")),
      appState.backStack,
    )
  }

  @Test
  fun showRootNewsArticleReplacesNestedTail() {
    val appState =
      VlrAppState(
        backStack =
        mutableListOf<NavKey>(
          AppRoute.News,
          AppRoute.MatchDetails(matchId = "match-1"),
          AppRoute.PlayerDetails(playerId = "player-1"),
        ),
      )

    appState.showRootNewsArticle(articleId = "article-9")

    assertEquals(
      listOf<NavKey>(AppRoute.News, AppRoute.NewsArticle(articleId = "article-9")),
      appState.backStack,
    )
    assertEquals(AppRoute.News, AppRoute.NewsArticle(articleId = "article-9").rootDestination)
    assertTrue(appState.canNavigateBack)
  }

  @Test
  fun bottomNavigationVisibleOnlyOnRootRoutes() {
    val appState =
      VlrAppState(
        backStack = mutableListOf<NavKey>(AppRoute.News),
      )

    assertTrue(appState.shouldShowBottomNavigation)

    appState.showRootNewsArticle(articleId = "article-1")

    assertFalse(appState.shouldShowBottomNavigation)

    appState.selectRoot(AppRoute.Matches)

    assertTrue(appState.shouldShowBottomNavigation)
  }

  @Test
  fun replaceTeamDetailsDropsExistingTeamAndPlayerRoutes() {
    val appState =
      VlrAppState(
        backStack =
        mutableListOf<NavKey>(
          AppRoute.Rankings,
          AppRoute.TeamDetails(teamId = "team-old"),
          AppRoute.PlayerDetails(playerId = "player-1"),
        ),
      )

    appState.replaceTeamDetails(teamId = "team-new")

    assertEquals(
      listOf<NavKey>(AppRoute.News, AppRoute.Rankings, AppRoute.TeamDetails(teamId = "team-new")),
      appState.backStack,
    )
  }
}
