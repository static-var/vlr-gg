/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.country_with_separator
import vlr.feature_rankings.generated.resources.favorite_team
import vlr.feature_rankings.generated.resources.loading_rankings
import vlr.feature_rankings.generated.resources.no_rankings_yet
import vlr.feature_rankings.generated.resources.ranking_points
import vlr.feature_rankings.generated.resources.ranking_team_label
import vlr.feature_rankings.generated.resources.rankings_subtitle
import vlr.feature_rankings.generated.resources.rankings_title
import vlr.feature_rankings.generated.resources.region_rankings_unpublished
import vlr.feature_rankings.generated.resources.regional_rankings_unpublished
import vlr.feature_rankings.generated.resources.search_teams
import vlr.feature_rankings.generated.resources.top_teams_in_region

@Composable
public fun RankingsRoute(
  uiState: RankingsUiState,
  onRegionSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  searchState: TeamSearchUiState = TeamSearchUiState(),
  onOpenSearch: () -> Unit = {},
  onCloseSearch: () -> Unit = {},
  onSearchQueryChanged: (String) -> Unit = {},
  onRetrySearch: () -> Unit = {},
) {
  RankingsScreen(
    uiState = uiState,
    onRegionSelected = onRegionSelected,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    searchState = searchState,
    onOpenSearch = onOpenSearch,
    onCloseSearch = onCloseSearch,
    onSearchQueryChanged = onSearchQueryChanged,
    onRetrySearch = onRetrySearch,
  )
}

@Composable
internal fun RankingsScreen(
  uiState: RankingsUiState,
  onRegionSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  searchState: TeamSearchUiState = TeamSearchUiState(),
  onOpenSearch: () -> Unit = {},
  onCloseSearch: () -> Unit = {},
  onSearchQueryChanged: (String) -> Unit = {},
  onRetrySearch: () -> Unit = {},
) {
  val isOnline = LocalIsOnline.current
  val selectedRegion = uiState.selectedRegion
  val selectedRanking = remember(uiState.regions, selectedRegion) {
    uiState.regions.firstOrNull { it.region == selectedRegion }
  }
  val tabs = remember(uiState.regions) {
    uiState.regions.map { ranking ->
      PrismTab(id = ranking.region, label = ranking.regionLabel.ifBlank { ranking.region })
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxSize()
        .then(if (searchState.isOpen) Modifier.clearAndSetSemantics {} else Modifier)
        .padding(horizontal = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Column {
        PrismScreenTitleBar(
          title = stringResource(Res.string.rankings_title),
          subtitle = stringResource(Res.string.rankings_subtitle),
          actions = {
            SharedRefreshButton(
              isLoading = uiState.isLoading,
              isRefreshing = uiState.isRefreshing,
              hasContent = uiState.regions.isNotEmpty(),
              onRefresh = onRefresh,
            )
            PrismIconButton(
              icon = TeamSearchIcon,
              contentDescription = stringResource(Res.string.search_teams),
              size = PrismIconButtonSize.Toolbar,
              onClick = onOpenSearch,
            )
          },
        )

        SharedRefreshStatus(
          hasContent = uiState.regions.isNotEmpty(),
          isRefreshing = false,
          errorMessage = uiState.errorMessage.takeIf { uiState.regions.isNotEmpty() },
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
        )
      }

      if (uiState.regions.isNotEmpty()) {
        PrismTabs(
          tabs = tabs,
          selectedTabId = selectedRegion ?: uiState.regions.first().region,
          onTabSelected = { onRegionSelected(it.id) },
        )
      }

      when {
        (!isOnline || uiState.isLoading || uiState.isRefreshing) && uiState.regions.isEmpty() -> {
          SharedScreenLoading(label = stringResource(Res.string.loading_rankings), modifier = Modifier.fillMaxSize())
        }

        uiState.errorMessage != null && uiState.regions.isEmpty() -> {
          SharedLoadError(
            errorMessage = uiState.errorMessage,
            errorDetails = uiState.errorDetails,
            onRefresh = onRefresh,
            centered = true,
            modifier = Modifier.fillMaxWidth().weight(1f),
          )
        }

        selectedRanking == null || selectedRanking.teams.isEmpty() -> {
          if ((isOnline || uiState.regions.isEmpty()) && !uiState.isRefreshing && !uiState.isLoading && uiState.errorMessage == null) {
            SharedEmptyState(
              artwork = EmptyStateArtwork.NoLiveMatches,
              title = stringResource(Res.string.no_rankings_yet),
              message = selectedRanking?.let {
                stringResource(Res.string.region_rankings_unpublished, it.regionLabel.ifBlank { it.region })
              }
                ?: stringResource(Res.string.regional_rankings_unpublished),
              modifier = Modifier.fillMaxWidth().weight(1f),
            )
          }
        }

        else -> {
          RankingsContent(
            selectedRanking = selectedRanking,
            onTeamSelected = onTeamSelected,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
    TeamSearchOverlay(
      state = searchState,
      onClose = onCloseSearch,
      onQueryChanged = onSearchQueryChanged,
      onRetry = onRetrySearch,
      onTeamSelected = onTeamSelected,
    )
  }
}

@Composable
private fun RankingsContent(
  selectedRanking: RegionalRanking,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.cardMascotViewport(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    if (selectedRanking.teams.isNotEmpty()) {
      item {
        Text(
          text = stringResource(
            Res.string.top_teams_in_region,
            selectedRanking.teams.size,
            selectedRanking.regionLabel.ifBlank { selectedRanking.region },
          ),
          style = Prism.typography.label,
          color = Prism.color.labelColor,
        )
      }
    }
    items(selectedRanking.teams, key = { it.teamId }) { team ->
      RankingTeamItem(
        team = team,
        onTeamSelected = onTeamSelected,
        modifier = Modifier.fillMaxWidth().cardMascotEligible(
          topClearance = Prism.dimens.spacingS * 2 + Prism.dimens.spacingXs,
        ),
      )
    }
  }
}

@Composable
private fun RankingTeamItem(
  team: TeamRanking,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val favoriteDescription = stringResource(Res.string.favorite_team)
  FavoriteTicketCardBox(
    selected = team.isFavorite,
    modifier = modifier,
    favoriteModifier = Modifier.clearAndSetSemantics { contentDescription = favoriteDescription },
  ) {
    PrismCard(
      modifier = Modifier.fillMaxWidth(),
      style = PrismCardStyle.Outlined,
      onClick = { onTeamSelected(team.teamId) },
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(Res.string.ranking_team_label, team.rank, team.teamName),
            style = Prism.typography.cardTitle,
            color = if (team.isFavorite) Prism.color.accent else Prism.color.titleColor,
          )
          Row(
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(Res.string.country_with_separator, team.country),
              style = Prism.typography.bodySmall,
              color = Prism.color.labelColor,
            )
            Text(
              text = stringResource(Res.string.ranking_points, team.points),
              style = Prism.typography.bodySmall,
              color = Prism.color.labelColor,
            )
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
}
