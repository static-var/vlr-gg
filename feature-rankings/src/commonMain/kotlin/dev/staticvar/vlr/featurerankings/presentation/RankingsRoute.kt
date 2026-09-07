/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus

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
  val selectedRegion = uiState.selectedRegion
  val selectedRanking = remember(uiState.regions, selectedRegion) {
    uiState.regions.firstOrNull { it.region == selectedRegion }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = "Ranking",
        subtitle = "The top teams in every region",
      )

      SharedRefreshStatus(
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage,
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
      uiState.isLoading -> {
        PrismStateMessage(text = "Loading rankings…")
      }

      selectedRanking == null -> {
        PrismStateMessage(text = "No rankings available yet.")
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          item {
            Text(
              text = "Top ${selectedRanking.teams.size} • ${selectedRanking.region}",
              style = Prism.typography.label,
              color = Prism.color.labelColor,
            )
          }
          items(selectedRanking.teams, key = { it.teamId }) { team ->
            PrismCard(
              modifier = Modifier.fillMaxWidth(),
              style = PrismCardStyle.Outlined,
              onClick = { onTeamSelected(team.teamId) },
            ) {
              Text(
                text = "#${team.rank} ${team.teamName}",
                style = Prism.typography.cardTitle,
                color = Prism.color.titleColor,
              )
              Text(
                text = "${team.country} • ${team.points} pts",
                modifier = Modifier.padding(top = Prism.dimens.spacingXs),
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
