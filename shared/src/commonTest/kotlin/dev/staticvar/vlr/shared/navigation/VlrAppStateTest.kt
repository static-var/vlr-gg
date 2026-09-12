/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.navigation3.runtime.NavKey
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VlrAppStateTest {
  @Test
  fun matchListPreviewSurvivesBackUntilLeavingTheList() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Matches))
    val preview = matchTransitionPreview()

    appState.showMatchDetailsFromPreview(preview)

    assertEquals(AppRoute.MatchDetails(preview.id), appState.backStack.last())
    assertEquals(preview, appState.matchTransitionPreview)

    appState.navigateUp()

    assertEquals(AppRoute.Matches, appState.backStack.last())
    assertEquals(preview, appState.matchTransitionPreview)

    appState.navigateUp()

    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
    assertNull(appState.matchTransitionPreview)
  }

  @Test
  fun homeMatchPreviewSurvivesBackUntilLeavingHome() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    val preview = matchTransitionPreview()

    appState.showMatchDetailsFromPreview(preview)

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.MatchDetails(preview.id)), appState.backStack)
    assertEquals(preview, appState.matchTransitionPreview)

    appState.navigateUp()

    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
    assertEquals(preview, appState.matchTransitionPreview)

    appState.selectRoot(AppRoute.Matches)

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Matches), appState.backStack)
    assertNull(appState.matchTransitionPreview)
  }

  @Test
  fun directMatchNavigationDoesNotReuseAMatchListPreview() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Matches))
    val preview = matchTransitionPreview()
    appState.showMatchDetailsFromPreview(preview)
    appState.navigateUp()

    appState.showRootMatchDetails(preview.id)

    assertEquals(AppRoute.MatchDetails(preview.id), appState.backStack.last())
    assertNull(appState.matchTransitionPreview)

    val newsState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.News))
    newsState.showMatchDetailsFromPreview(preview)

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.News, AppRoute.MatchDetails(preview.id)), newsState.backStack)
    assertNull(newsState.matchTransitionPreview)
  }

  @Test
  fun openingAnotherDetailClearsTheMatchTransitionPreview() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Matches))
    appState.showMatchDetailsFromPreview(matchTransitionPreview())

    appState.showTeamDetails("team-1")

    assertEquals(AppRoute.TeamDetails("team-1"), appState.backStack.last())
    assertNull(appState.matchTransitionPreview)
  }

  @Test
  fun homeEventAndMatchTransitionsReplaceEachOthersPreview() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    val eventPreview = eventTransitionPreview()
    val matchPreview = matchTransitionPreview()
    appState.showEventDetailsFromPreview(eventPreview)
    appState.navigateUp()
    assertEquals(eventPreview, appState.eventTransitionPreview)

    appState.showMatchDetailsFromPreview(matchPreview)

    assertNull(appState.eventTransitionPreview)
    assertEquals(matchPreview, appState.matchTransitionPreview)

    appState.navigateUp()
    appState.showEventDetailsFromPreview(eventPreview)

    assertNull(appState.matchTransitionPreview)
    assertEquals(eventPreview, appState.eventTransitionPreview)
  }

  @Test
  fun eventListPreviewSurvivesBackUntilLeavingTheList() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Events))
    val preview = eventTransitionPreview()

    appState.showEventDetailsFromPreview(preview)

    assertEquals(AppRoute.EventDetails(preview.id), appState.backStack.last())
    assertEquals(preview, appState.eventTransitionPreview)

    appState.navigateUp()

    assertEquals(AppRoute.Events, appState.backStack.last())
    assertEquals(preview, appState.eventTransitionPreview)

    appState.navigateUp()

    assertEquals(AppRoute.Home, appState.backStack.last())
    assertNull(appState.eventTransitionPreview)
  }

  @Test
  fun homeEventPreviewSurvivesBackUntilLeavingHome() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    val preview = eventTransitionPreview()

    appState.showEventDetailsFromPreview(preview)

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.EventDetails(preview.id)), appState.backStack)
    assertEquals(preview, appState.eventTransitionPreview)

    appState.navigateUp()

    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
    assertEquals(preview, appState.eventTransitionPreview)

    appState.selectRoot(AppRoute.Events)

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Events), appState.backStack)
    assertNull(appState.eventTransitionPreview)
  }

  @Test
  fun directEventNavigationDoesNotReuseAnEventListPreview() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Events))
    val preview = eventTransitionPreview()
    appState.showEventDetailsFromPreview(preview)
    appState.navigateUp()

    appState.showRootEventDetails(preview.id)

    assertEquals(AppRoute.EventDetails(preview.id), appState.backStack.last())
    assertNull(appState.eventTransitionPreview)

    val newsState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.News))
    newsState.showEventDetailsFromPreview(preview)

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.News, AppRoute.EventDetails(preview.id)), newsState.backStack)
    assertNull(newsState.eventTransitionPreview)
  }

  @Test
  fun settingsAndAboutReturnToHomeWithNoSettingsTab() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    appState.showSettings()
    appState.showAbout()
    appState.showAbout()

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Settings, AppRoute.About), appState.backStack)
    assertEquals("home", appState.selectedNavigationItemId)
    assertFalse(appState.shouldShowBottomNavigation)

    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Settings), appState.backStack)
    assertFalse(appState.shouldShowBottomNavigation)

    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
    assertTrue(appState.shouldShowBottomNavigation)
    assertFalse(appState.canNavigateBack)
  }

  @Test
  fun selectRootClearsSecondaryRoutes() {
    val appState =
      VlrAppState(
        backStack = mutableListOf<NavKey>(AppRoute.Home, AppRoute.MatchDetails(matchId = "match-1")),
      )

    appState.selectRoot(AppRoute.Events)

    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Events), appState.backStack)
    assertEquals(AppRoute.Events, appState.selectedRootRoute)
    assertTrue(appState.canNavigateBack)
  }

  @Test
  fun emptyStackStartsAtHome() {
    val appState = VlrAppState(mutableListOf())

    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
    assertEquals("home", appState.selectedNavigationItemId)
    assertTrue(appState.shouldShowBottomNavigation)
    assertFalse(appState.canNavigateBack)
  }

  @Test
  fun restoredDetailStackKeepsItsSelectedRootAndTail() {
    val restoredStack = mutableListOf<NavKey>(
      AppRoute.Home,
      AppRoute.Events,
      AppRoute.EventDetails("event-1"),
    )
    val appState = VlrAppState(restoredStack.toMutableList())

    assertEquals(restoredStack, appState.backStack)
    assertEquals(AppRoute.Events, appState.selectedRootRoute)
    assertFalse(appState.shouldShowBottomNavigation)
  }

  @Test
  fun allFiveNavigationItemsSelectTheirRoot() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    val roots = listOf(
      "home" to AppRoute.Home,
      "matches" to AppRoute.Matches,
      "events" to AppRoute.Events,
      "rankings" to AppRoute.Rankings,
      "news" to AppRoute.News,
    )

    for ((id, route) in roots) {
      appState.selectRoot(id)
      assertEquals(route, appState.selectedRootRoute)
      assertEquals(id, appState.selectedNavigationItemId)
      assertTrue(appState.shouldShowBottomNavigation)
    }
  }

  @Test
  fun settingsIsNeverSelectableAsARoot() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))

    appState.selectRoot(AppRoute.Settings)

    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
  }

  @Test
  fun backFromEverySecondaryRootReturnsToHome() {
    for (root in listOf(AppRoute.Matches, AppRoute.Events, AppRoute.Rankings, AppRoute.News)) {
      val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
      appState.selectRoot(root)
      assertEquals(root, appState.selectedRootRoute)
      assertTrue(appState.canNavigateBack)

      appState.navigateUp()

      assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
      assertEquals("home", appState.selectedNavigationItemId)
      assertFalse(appState.canNavigateBack)
      appState.navigateUp()
      assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
    }
  }

  @Test
  fun switchingTabsKeepsOnlyHomeAndSelectedRoot() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    appState.selectRoot(AppRoute.Matches)
    appState.showMatchDetails("match-1")
    appState.selectRoot(AppRoute.Events)
    appState.selectRoot(AppRoute.Events)
    assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Events), appState.backStack)
    assertEquals("events", appState.selectedNavigationItemId)
    appState.selectRoot(AppRoute.Home)
    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
    assertFalse(appState.canNavigateBack)
  }

  @Test
  fun replacingDetailPreservesSelectedRootBeforeReturningToHome() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    appState.selectRoot(AppRoute.Events)
    appState.showEventDetails("event-1")
    appState.showMatchDetails("match-1")
    appState.showRootEventDetails("event-2")
    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.Events, AppRoute.EventDetails("event-2")),
      appState.backStack,
    )
    assertEquals("events", appState.selectedNavigationItemId)
    appState.navigateUp()
    assertEquals(AppRoute.Events, appState.backStack.last())
    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
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
      listOf<NavKey>(AppRoute.Home, AppRoute.Events, AppRoute.EventDetails(eventId = "event-9")),
      appState.backStack,
    )
    assertTrue(appState.canNavigateBack)
  }

  @Test
  fun rootPlayerDetailsReturnToHome() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))

    appState.showRootPlayerDetails(playerId = "player-1")
    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.PlayerDetails(playerId = "player-1")),
      appState.backStack,
    )

    appState.navigateUp()
    assertEquals(listOf<NavKey>(AppRoute.Home), appState.backStack)
  }

  @Test
  fun rootDetailsDiscardPushedSettings() {
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
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
          AppRoute.Home,
          AppRoute.News,
          AppRoute.MatchDetails(matchId = "match-1"),
          AppRoute.PlayerDetails(playerId = "player-1"),
        ),
      )

    appState.showRootNewsArticle(articleId = "article-9")

    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.News, AppRoute.NewsArticle(articleId = "article-9")),
      appState.backStack,
    )
    assertEquals(AppRoute.News, AppRoute.NewsArticle(articleId = "article-9").rootDestination)
    assertTrue(appState.canNavigateBack)
  }

  @Test
  fun bottomNavigationVisibleOnlyOnRootRoutes() {
    val appState =
      VlrAppState(
        backStack = mutableListOf<NavKey>(AppRoute.Home),
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
      listOf<NavKey>(AppRoute.Home, AppRoute.Rankings, AppRoute.TeamDetails(teamId = "team-new")),
      appState.backStack,
    )
  }
}

private fun eventTransitionPreview(): EventPreview = EventPreview(
  id = "event-transition",
  title = "Game Changers",
  status = EventStatus.ONGOING,
  prize = "$60,000",
  dates = "Sep 1–18",
  region = "North America",
  logoUrl = "https://example.com/event.png",
  isFavorite = true,
)

private fun matchTransitionPreview(): MatchPreview = MatchPreview(
  id = "match-transition",
  event = "Game Changers",
  series = "Grand Final",
  status = MatchStatus.COMPLETED,
  team1 = TeamPreview(
    id = "team-1",
    name = "Team One",
    region = "North America",
    img = "https://example.com/team-1.png",
    score = 3,
    isWinner = true,
  ),
  team2 = TeamPreview(
    id = "team-2",
    name = "Team Two",
    region = "Europe",
    img = "https://example.com/team-2.png",
    score = 1,
    isWinner = false,
  ),
  time = "2026-09-12T12:00:00Z",
  eventId = "event-transition",
)
