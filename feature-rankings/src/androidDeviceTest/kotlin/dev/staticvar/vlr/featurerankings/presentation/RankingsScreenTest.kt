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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.LinkAnnotation
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.vlr.domain.model.RankingMetric
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.domain.model.TeamRanking
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.ranking_team_label
import vlr.feature_rankings.generated.resources.rankings_info_close
import vlr.feature_rankings.generated.resources.rankings_info_elo_title
import vlr.feature_rankings.generated.resources.rankings_info_title
import vlr.feature_rankings.generated.resources.rankings_metric_map_elo
import vlr.feature_rankings.generated.resources.rankings_select_metric
import vlr.feature_rankings.generated.resources.rankings_select_region
import vlr.feature_rankings.generated.resources.region_americas
import vlr.feature_rankings.generated.resources.region_emea
import vlr.feature_rankings.generated.resources.region_pacific

@RunWith(AndroidJUnit4::class)
class RankingsScreenTest {
  @get:Rule
  val compose = createAndroidComposeRule<RankingsScreenTestActivity>()

  private lateinit var regionTitle: String
  private lateinit var metricTitle: String
  private lateinit var americas: String
  private lateinit var emea: String
  private lateinit var pacific: String
  private lateinit var mapRating: String
  private lateinit var info: String
  private lateinit var eloInfo: String
  private lateinit var done: String
  private lateinit var lastTeam: String

  @Test
  fun appBarSwitchPreservesIndependentExploreAndRegionalSelections() {
    var view by mutableStateOf(RankingsView.Explore)
    var region by mutableStateOf<RankingRegion?>(null)
    val query = RankingsQuery(metric = RankingMetric.MapElo, region = RankingRegion.Pacific)
    compose.setContent {
      Screen(
        state = RankingsUiState(teams = teams(), isLoading = false, view = view, exploreQuery = query, selectedRegion = region),
        onViewSelected = { view = it },
        onRegionSelected = { region = it },
      )
    }

    compose.onNodeWithTag("rankings_regional_switch").assertIsOff().performClick()
    compose.onNodeWithTag("rankings_query_sentence").assertDoesNotExist()
    compose.onNodeWithText(emea).performScrollTo().performClick().assertIsSelected()
    compose.onNodeWithTag("rankings_regional_switch").performClick()
    compose.onNodeWithTag("rankings_query_sentence").assertIsDisplayed()
    compose.onNodeWithTag("rankings_query_sentence")
      .assertTextContains(mapRating, substring = true)
      .assertTextContains(pacific, substring = true)
    compose.onNodeWithTag("rankings_regional_switch").performClick().assertIsOn()
    compose.onNodeWithText(emea).assertIsSelected()
  }

  @Test
  fun allFiftyReturnedTeamsAreReachableAndOpenDetailsWithoutSearch() {
    var selected: String? = null
    compose.setContent {
      Screen(RankingsUiState(teams = teams(), isLoading = false), onTeamSelected = { selected = it })
    }

    compose.onNodeWithContentDescription("Search teams").assertDoesNotExist()
    compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    compose.onNodeWithTag("rankings_team_list").performScrollToNode(hasText(lastTeam))
    compose.onNodeWithText(lastTeam).assertIsDisplayed().performClick()
    compose.runOnIdle { assertEquals("50", selected) }
  }

  @Test
  fun rankingExplanationOpensFromToolbarAndDismisses() {
    compose.setContent { Screen(RankingsUiState(teams = teams(), isLoading = false)) }
    compose.onNodeWithContentDescription(info).performClick()
    compose.onNodeWithText(eloInfo).assertExists()
    compose.onNodeWithText(done).performClick()
    compose.onNodeWithTag("rankings_query_sentence").assertIsDisplayed()
  }

  @Test
  fun selectingRegionKeepsItsHeaderAndOptionsUntilSheetExitCompletes() {
    var query by mutableStateOf(RankingsQuery())
    var selectedQuery: RankingsQuery? = null
    compose.setContent {
      Screen(
        RankingsUiState(teams = teams(), isLoading = false, exploreQuery = query),
        onQueryChanged = {
          selectedQuery = it
          query = it
        },
      )
    }
    compose.onNodeWithTag("rankings_query_sentence").performFirstLinkClick {
      (it.item as? LinkAnnotation.Clickable)?.tag == RankingSheet.Region.name
    }
    compose.onNodeWithText(regionTitle).assertIsDisplayed()

    compose.mainClock.autoAdvance = false
    try {
      compose.onNodeWithText(emea).performClick()
      compose.mainClock.advanceTimeByFrame()
      compose.runOnIdle { assertEquals(RankingsQuery(region = RankingRegion.Emea), selectedQuery) }
      compose.onNodeWithText(regionTitle).assertExists()
      compose.onNodeWithText(emea).assertExists()
      compose.onNodeWithText(americas).assertExists()
      compose.onNodeWithText(metricTitle).assertDoesNotExist()

      compose.mainClock.advanceTimeBy(1000)
      compose.mainClock.advanceTimeByFrame()
      compose.onNodeWithText(regionTitle).assertDoesNotExist()
      compose.onNodeWithTag("rankings_query_sentence").assertIsDisplayed()
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Composable
  private fun Screen(
    state: RankingsUiState,
    onViewSelected: (RankingsView) -> Unit = {},
    onRegionSelected: (RankingRegion?) -> Unit = {},
    onQueryChanged: (RankingsQuery) -> Unit = {},
    onTeamSelected: (String) -> Unit = {},
  ) {
    PrismTheme {
      regionTitle = stringResource(Res.string.rankings_select_region)
      metricTitle = stringResource(Res.string.rankings_select_metric)
      americas = stringResource(Res.string.region_americas)
      emea = stringResource(Res.string.region_emea)
      pacific = stringResource(Res.string.region_pacific)
      mapRating = stringResource(Res.string.rankings_metric_map_elo)
      info = stringResource(Res.string.rankings_info_title)
      eloInfo = stringResource(Res.string.rankings_info_elo_title)
      done = stringResource(Res.string.rankings_info_close)
      lastTeam = stringResource(Res.string.ranking_team_label, 50, "Team 50")
      RankingsScreen(
        uiState = state,
        onTeamSelected = onTeamSelected,
        onRegionSelected = onRegionSelected,
        onViewSelected = onViewSelected,
        onExploreQueryChanged = onQueryChanged,
      )
    }
  }

  private fun teams(): List<TeamRanking> = (1..50).map {
    TeamRanking(
      teamId = it.toString(), teamName = "Team $it", teamLogo = "", country = "EU", rank = it,
      elo = 1800.0 - it, mapElo = 1775.0 - it, matchesPlayed = 16, winRate = 0.75,
      wins = 12, losses = 4, isFavorite = it == 1, region = RankingRegion.Emea,
    )
  }
}

class RankingsScreenTestActivity : ComponentActivity()
