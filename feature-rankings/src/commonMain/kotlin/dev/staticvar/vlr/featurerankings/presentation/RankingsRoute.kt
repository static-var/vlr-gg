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
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconStyle
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice
import dev.staticvar.vlr.sharedui.spoilers.SpoilerScore

@Composable
public fun RankingsRoute(
  uiState: RankingsUiState,
  onRegionSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
) {
  RankingsScreen(
    uiState = uiState,
    onRegionSelected = onRegionSelected,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
    onRefresh = onRefresh,
  )
}

@Composable
internal fun RankingsScreen(
  uiState: RankingsUiState,
  onRegionSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
) {
  val isOnline = LocalIsOnline.current
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val selectedRegion = uiState.selectedRegion
  val selectedRanking = remember(uiState.regions, selectedRegion) {
    uiState.regions.firstOrNull { it.region == selectedRegion }
  }
  val displayedTeams = remember(selectedRanking?.teams, spoilersHidden) {
    val teams = selectedRanking?.teams.orEmpty()
    if (spoilersHidden) teams.sortedBy { it.teamName.lowercase() } else teams
  }

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = "Ranking",
        subtitle = "The top teams in every region",
        actions = {
          SharedRefreshButton(
            isLoading = uiState.isLoading,
            isRefreshing = uiState.isRefreshing,
            hasContent = uiState.regions.isNotEmpty(),
            onRefresh = onRefresh,
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
        tabs = uiState.regions.map { PrismTab(id = it.region, label = it.region) },
        selectedTabId = selectedRegion ?: uiState.regions.first().region,
        onTabSelected = { onRegionSelected(it.id) },
      )
    }

    when {
      (!LocalIsOnline.current || uiState.isLoading || uiState.isRefreshing) && uiState.regions.isEmpty() -> {
        SharedScreenLoading(label = "Loading rankings", modifier = Modifier.fillMaxSize())
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
            title = "No rankings yet",
            message = selectedRanking?.let { "Team rankings for ${it.region} have not been published." }
              ?: "Regional team rankings have not been published.",
            modifier = Modifier.fillMaxWidth().weight(1f),
          )
        }
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          if (selectedRanking.teams.isNotEmpty()) {
            item {
              Text(
                text = if (spoilersHidden) {
                  "${selectedRanking.teams.size} teams • ${selectedRanking.region}"
                } else {
                  "Top ${selectedRanking.teams.size} • ${selectedRanking.region}"
                },
                style = Prism.typography.label,
                color = Prism.color.labelColor,
              )
            }
          }
          if (spoilersHidden) {
            item {
              SpoilerHiddenNotice()
            }
          }
          items(displayedTeams, key = { it.teamId }) { team ->
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
                Text(
                  text = if (spoilersHidden) team.teamName else "#${team.rank} ${team.teamName}",
                  modifier = Modifier.weight(1f),
                  style = Prism.typography.cardTitle,
                  color = if (team.isFavorite) Prism.color.accent else Prism.color.titleColor,
                )
                if (team.isFavorite) {
                  PrismFavoriteIcon(
                    selected = true,
                    size = PrismFavoriteIconSize.Small,
                    style = PrismFavoriteIconStyle.Bare,
                    contentDescription = "Favorite team",
                  )
                }
              }
              Row(
                modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(text = "${team.country} •", style = Prism.typography.bodySmall, color = Prism.color.labelColor)
                SpoilerScore(
                  text = "${team.points} pts",
                  style = Prism.typography.bodySmall,
                  color = Prism.color.labelColor,
                )
              }
            }
          }
        }
      }
    }
  }
}
