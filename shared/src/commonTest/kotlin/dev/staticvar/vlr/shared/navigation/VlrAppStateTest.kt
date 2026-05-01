package dev.staticvar.vlr.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VlrAppStateTest {

  @Test
  fun selectRootClearsSecondaryRoutes() {
    val appState =
      VlrAppState(
        backStack = mutableListOf<NavKey>(AppRoute.News, AppRoute.MatchDetails(matchId = "match-1")),
        navigationItems = emptyList(),
      )

    appState.selectRoot(AppRoute.Events)

    assertEquals(listOf<NavKey>(AppRoute.Events), appState.backStack)
    assertEquals(AppRoute.Events, appState.selectedRootRoute)
    assertFalse(appState.canNavigateBack)
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
        navigationItems = emptyList(),
      )

    appState.showRootEventDetails(eventId = "event-9")

    assertEquals(
      listOf<NavKey>(AppRoute.Events, AppRoute.EventDetails(eventId = "event-9")),
      appState.backStack,
    )
    assertTrue(appState.canNavigateBack)
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
        navigationItems = emptyList(),
      )

    appState.replaceTeamDetails(teamId = "team-new")

    assertEquals(
      listOf<NavKey>(AppRoute.Rankings, AppRoute.TeamDetails(teamId = "team-new")),
      appState.backStack,
    )
  }
}
