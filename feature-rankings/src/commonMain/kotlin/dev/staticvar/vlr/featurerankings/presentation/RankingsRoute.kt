/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.icon.about.StairStepAbout
import dev.staticvar.vlr.domain.model.RankingMetric
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.loading_rankings
import vlr.feature_rankings.generated.resources.no_rankings_yet
import vlr.feature_rankings.generated.resources.ranking_elo
import vlr.feature_rankings.generated.resources.ranking_map_elo
import vlr.feature_rankings.generated.resources.ranking_record
import vlr.feature_rankings.generated.resources.ranking_series_count
import vlr.feature_rankings.generated.resources.ranking_team_label
import vlr.feature_rankings.generated.resources.ranking_win_rate
import vlr.feature_rankings.generated.resources.rankings_beta
import vlr.feature_rankings.generated.resources.rankings_info_title
import vlr.feature_rankings.generated.resources.rankings_no_matching_teams
import vlr.feature_rankings.generated.resources.rankings_subtitle
import vlr.feature_rankings.generated.resources.rankings_title
import vlr.feature_rankings.generated.resources.region_all
import vlr.feature_rankings.generated.resources.top_teams_all_circuits
import vlr.feature_rankings.generated.resources.top_teams_in_region

@Composable
public fun RankingsRoute(
  uiState: RankingsUiState,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRegionSelected: (RankingRegion?) -> Unit = {},
  onViewSelected: (RankingsView) -> Unit = {},
  onExploreQueryChanged: (RankingsQuery) -> Unit = {},
  onRefresh: () -> Unit = {},
) {
  RankingsScreen(uiState, onTeamSelected, modifier, onRegionSelected, onViewSelected, onExploreQueryChanged, onRefresh)
}

@Composable
internal fun RankingsScreen(
  uiState: RankingsUiState,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRegionSelected: (RankingRegion?) -> Unit = {},
  onViewSelected: (RankingsView) -> Unit = {},
  onExploreQueryChanged: (RankingsQuery) -> Unit = {},
  onRefresh: () -> Unit = {},
) {
  var sheet by rememberSaveable { mutableStateOf<RankingSheet?>(null) }
  var showRankingInfo by rememberSaveable { mutableStateOf(false) }
  val hasContent = uiState.teams.isNotEmpty()

  Column(
    modifier = modifier.fillMaxSize()
      .then(if (sheet != null || showRankingInfo) Modifier.clearAndSetSemantics {} else Modifier)
      .padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = stringResource(Res.string.rankings_title),
        subtitle = stringResource(Res.string.rankings_subtitle),
        titleAccessory = { PrismTag(text = stringResource(Res.string.rankings_beta), style = PrismTagStyle.Accent) },
        actions = {
          PrismIconButton(
            icon = StairStepAbout,
            contentDescription = stringResource(Res.string.rankings_info_title),
            size = PrismIconButtonSize.Toolbar,
            onClick = { showRankingInfo = true },
          )
        },
      )
      SharedRefreshStatus(
        hasContent = hasContent,
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage.takeIf { hasContent },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }

    if (uiState.view == RankingsView.Regional) {
      RegionalTabs(uiState.selectedRegion, onRegionSelected)
    } else {
      RankingsQuerySentence(query = uiState.exploreQuery, onSelection = { sheet = it })
    }

    when {
      (uiState.isLoading || uiState.isRefreshing) && !hasContent -> {
        SharedScreenLoading(label = stringResource(Res.string.loading_rankings), modifier = Modifier.fillMaxSize())
      }
      uiState.errorMessage != null && !hasContent -> {
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
      uiState.visibleTeams.isEmpty() -> {
        SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveMatches,
          title = stringResource(Res.string.no_rankings_yet),
          message = stringResource(Res.string.rankings_no_matching_teams),
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
      else -> key(uiState.view, uiState.query) {
        RankingsContent(uiState, onTeamSelected, Modifier.fillMaxWidth().weight(1f))
      }
    }
  }
  RankingsSelectionSheet(
    selection = sheet,
    query = uiState.exploreQuery,
    onDismiss = { sheet = null },
    onQueryChanged = onExploreQueryChanged,
  )
  RankingsInfoSheet(visible = showRankingInfo, onDismiss = { showRankingInfo = false })
}

@Composable
private fun RegionalTabs(selectedRegion: RankingRegion?, onRegionSelected: (RankingRegion?) -> Unit) {
  PrismTabs(
    tabs = listOf(PrismTab(id = "all", label = stringResource(Res.string.region_all))) + RankingRegion.entries.map {
      PrismTab(id = it.apiValue, label = regionLabel(it))
    },
    selectedTabId = selectedRegion?.apiValue ?: "all",
    onTabSelected = { tab -> onRegionSelected(RankingRegion.entries.firstOrNull { it.apiValue == tab.id }) },
  )
}

@Composable
private fun RankingsContent(uiState: RankingsUiState, onTeamSelected: (String) -> Unit, modifier: Modifier) {
  LazyColumn(
    modifier = modifier.cardMascotViewport().testTag("rankings_team_list"),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    if (uiState.view == RankingsView.Regional) {
      item {
        Text(
          text = uiState.selectedRegion?.let {
            stringResource(Res.string.top_teams_in_region, uiState.visibleTeams.size, regionLabel(it))
          } ?: stringResource(Res.string.top_teams_all_circuits, uiState.visibleTeams.size),
          style = Prism.typography.label,
          color = Prism.color.labelColor,
        )
      }
    }
    items(uiState.visibleTeams, key = { it.teamId }) { team ->
      RankingTeamItem(
        team = team,
        metric = uiState.query.metric,
        onTeamSelected = onTeamSelected,
        modifier = Modifier.fillMaxWidth().cardMascotEligible(
          topClearance = Prism.dimens.spacingS * 2 + Prism.dimens.spacingXs,
        ),
      )
    }
  }
}

@Composable
private fun RankingTeamItem(team: TeamRanking, metric: RankingMetric, onTeamSelected: (String) -> Unit, modifier: Modifier) {
  PrismCard(modifier = modifier, style = PrismCardStyle.Outlined, onClick = { onTeamSelected(team.teamId) }) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(Res.string.ranking_team_label, team.rank, team.teamName),
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = listOf(metricValue(team, metric), stringResource(Res.string.ranking_record, team.wins, team.losses))
            .joinToString(separator = " • "),
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
        )
        if (team.country.isNotBlank()) {
          Text(text = team.country, style = Prism.typography.bodySmall, color = Prism.color.labelColor)
        }
      }
      SharedNetworkIcon(
        imageUrl = team.teamLogo,
        contentDescription = team.teamName,
        size = PrismIconSize.Large,
        style = PrismIconStyle.Plain,
        parentBackground = PrismCardStyle.Outlined.containerColor,
        tint = PrismIconTint.None,
      )
    }
  }
}

@Composable
private fun metricValue(team: TeamRanking, metric: RankingMetric): String = when (metric) {
  RankingMetric.Elo -> stringResource(Res.string.ranking_elo, team.elo.roundToInt())
  RankingMetric.MapElo -> stringResource(Res.string.ranking_map_elo, team.mapElo.roundToInt())
  RankingMetric.Matches -> stringResource(Res.string.ranking_series_count, team.matchesPlayed)
  RankingMetric.WinRate -> stringResource(Res.string.ranking_win_rate, roundedWinRate(team.winRate))
}

internal fun roundedWinRate(rate: Double): String {
  val tenths = (rate * 1000).roundToInt()
  return "${tenths / 10}.${tenths % 10}"
}
