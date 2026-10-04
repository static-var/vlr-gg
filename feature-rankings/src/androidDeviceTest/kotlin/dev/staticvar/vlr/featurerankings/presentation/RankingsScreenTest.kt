/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.model.TeamRanking
import org.jetbrains.compose.resources.stringResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.close_team_search
import vlr.feature_rankings.generated.resources.ranking_team_label
import vlr.feature_rankings.generated.resources.rankings_info_close
import vlr.feature_rankings.generated.resources.rankings_info_elo_title
import vlr.feature_rankings.generated.resources.rankings_info_title
import vlr.feature_rankings.generated.resources.rankings_title
import vlr.feature_rankings.generated.resources.search_teams

@RunWith(AndroidJUnit4::class)
class RankingsScreenTest {
  @get:Rule
  val compose = createAndroidComposeRule<RankingsScreenTestActivity>()

  private lateinit var title: String
  private lateinit var infoTitle: String
  private lateinit var eloTitle: String
  private lateinit var done: String
  private lateinit var search: String
  private lateinit var closeSearch: String
  private lateinit var teamLabel: String

  @Test
  fun infoActionOpensSheetAndBothDismissalPathsAllowReopening() {
    compose.setContent { Screen() }

    assertBackgroundAvailable()
    openInfo()
    assertBackgroundHidden()
    compose.onNodeWithText(done).performClick()
    assertInfoClosed()
    assertBackgroundAvailable()

    openInfo()
    assertBackgroundHidden()
    pressBack()
    assertInfoClosed()
    assertBackgroundAvailable()

    openInfo()
    assertBackgroundHidden()
  }

  @Test
  fun savedStateRestoresOpenAndDismissedSheetVisibility() {
    val restoration = StateRestorationTester(compose)
    restoration.setContent { Screen() }

    openInfo()
    restoration.emulateSavedInstanceStateRestore()
    assertInfoOpen()
    assertBackgroundHidden()

    compose.onNodeWithText(done).performClick()
    restoration.emulateSavedInstanceStateRestore()
    assertInfoClosed()
    assertBackgroundAvailable()
    openInfo()
  }

  @Test
  fun searchOverlayStillHidesAndRestoresRankingsSemantics() {
    var searchState by mutableStateOf(TeamSearchUiState())
    compose.setContent {
      Screen(
        searchState = searchState,
        onOpenSearch = { searchState = searchState.copy(isOpen = true) },
        onCloseSearch = { searchState = searchState.copy(isOpen = false) },
      )
    }

    assertBackgroundAvailable()
    compose.onNodeWithContentDescription(search).performClick()
    assertBackgroundHidden()
    compose.onNodeWithContentDescription(closeSearch).assertIsDisplayed().performClick()
    assertBackgroundAvailable()
    openInfo()
  }

  @Composable
  private fun Screen(
    searchState: TeamSearchUiState = TeamSearchUiState(),
    onOpenSearch: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
  ) {
    PrismTheme {
      title = stringResource(Res.string.rankings_title)
      infoTitle = stringResource(Res.string.rankings_info_title)
      eloTitle = stringResource(Res.string.rankings_info_elo_title)
      done = stringResource(Res.string.rankings_info_close)
      search = stringResource(Res.string.search_teams)
      closeSearch = stringResource(Res.string.close_team_search)
      teamLabel = stringResource(Res.string.ranking_team_label, 1, "Test team")
      RankingsScreen(
        uiState = RankingsUiState(
          regions = listOf(
            RegionalRanking(
              region = "eu",
              regionLabel = "Europe",
              teams = listOf(TeamRanking("test", "Test team", "", "EU", 1, "1800")),
            ),
          ),
          selectedRegion = "eu",
          isLoading = false,
        ),
        onRegionSelected = {},
        onTeamSelected = {},
        searchState = searchState,
        onOpenSearch = onOpenSearch,
        onCloseSearch = onCloseSearch,
      )
    }
  }

  private fun openInfo() {
    compose.onNodeWithContentDescription(infoTitle).performClick()
    assertInfoOpen()
  }

  private fun assertInfoOpen() {
    compose.onNodeWithText(infoTitle).assertIsDisplayed()
    compose.onNodeWithText(eloTitle).assertExists()
    compose.onNodeWithText(done).assertIsDisplayed()
  }

  private fun assertInfoClosed() {
    compose.onNodeWithText(infoTitle).assertDoesNotExist()
    compose.onNodeWithText(done).assertDoesNotExist()
  }

  private fun assertBackgroundAvailable() {
    compose.onNodeWithText(title).assertIsDisplayed()
    compose.onNodeWithText("Europe").assertIsDisplayed()
    compose.onNodeWithText(teamLabel).assertIsDisplayed()
    compose.onNodeWithContentDescription(infoTitle).assertIsDisplayed()
    compose.onNodeWithContentDescription(search).assertIsDisplayed()
  }

  private fun assertBackgroundHidden() {
    compose.onNodeWithText(title).assertDoesNotExist()
    compose.onNodeWithText("Europe").assertDoesNotExist()
    compose.onNodeWithText(teamLabel).assertDoesNotExist()
    compose.onNodeWithContentDescription(infoTitle).assertDoesNotExist()
  }
}

class RankingsScreenTestActivity : ComponentActivity()
