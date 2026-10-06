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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.favorite_team
import vlr.feature_rankings.generated.resources.loading_rankings
import vlr.feature_rankings.generated.resources.no_rankings_yet
import vlr.feature_rankings.generated.resources.ranking_elo
import vlr.feature_rankings.generated.resources.ranking_record
import vlr.feature_rankings.generated.resources.ranking_team_label
import vlr.feature_rankings.generated.resources.rankings_beta
import vlr.feature_rankings.generated.resources.rankings_info_title
import vlr.feature_rankings.generated.resources.rankings_subtitle
import vlr.feature_rankings.generated.resources.rankings_title
import vlr.feature_rankings.generated.resources.rankings_unpublished
import vlr.feature_rankings.generated.resources.region_all
import vlr.feature_rankings.generated.resources.region_americas
import vlr.feature_rankings.generated.resources.region_emea
import vlr.feature_rankings.generated.resources.region_pacific
import vlr.feature_rankings.generated.resources.region_china
import vlr.feature_rankings.generated.resources.region_rankings_unpublished
import vlr.feature_rankings.generated.resources.top_teams_in_region
import vlr.feature_rankings.generated.resources.search_teams
import vlr.feature_rankings.generated.resources.top_teams_all_circuits
import kotlin.math.roundToInt

@Composable
public fun RankingsRoute(
  uiState: RankingsUiState,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRegionSelected: (RankingRegion?) -> Unit = {},
  onRefresh: () -> Unit = {},
  searchState: TeamSearchUiState = TeamSearchUiState(),
  onOpenSearch: () -> Unit = {},
  onCloseSearch: () -> Unit = {},
  onSearchQueryChanged: (String) -> Unit = {},
  onRetrySearch: () -> Unit = {},
) {
  RankingsScreen(
    uiState = uiState,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onRegionSelected = onRegionSelected,
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
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRegionSelected: (RankingRegion?) -> Unit = {},
  onRefresh: () -> Unit = {},
  searchState: TeamSearchUiState = TeamSearchUiState(),
  onOpenSearch: () -> Unit = {},
  onCloseSearch: () -> Unit = {},
  onSearchQueryChanged: (String) -> Unit = {},
  onRetrySearch: () -> Unit = {},
) {
  var showRankingInfo by rememberSaveable { mutableStateOf(false) }
  val hasContent = uiState.teams.isNotEmpty()
  val visibleTeams = uiState.visibleTeams
  val tabs = listOf(
    PrismTab(id = "all", label = stringResource(Res.string.region_all)),
  ) + RankingRegion.entries.map { region ->
    PrismTab(id = region.apiValue, label = regionLabel(region))
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxSize()
        .then(if (searchState.isOpen || showRankingInfo) Modifier.clearAndSetSemantics {} else Modifier)
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
            SharedRefreshButton(
              isLoading = uiState.isLoading,
              isRefreshing = uiState.isRefreshing,
              hasContent = hasContent,
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
          hasContent = hasContent,
          isRefreshing = false,
          errorMessage = uiState.errorMessage.takeIf { hasContent },
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
        )
      }

      PrismTabs(
        tabs = tabs,
        selectedTabId = uiState.selectedRegion?.apiValue ?: "all",
        onTabSelected = { tab ->
          onRegionSelected(RankingRegion.entries.firstOrNull { it.apiValue == tab.id })
        },
      )

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

        visibleTeams.isEmpty() -> {
          SharedEmptyState(
            artwork = EmptyStateArtwork.NoLiveMatches,
            title = stringResource(Res.string.no_rankings_yet),
            message = uiState.selectedRegion?.let { region ->
              stringResource(Res.string.region_rankings_unpublished, regionLabel(region))
            } ?: stringResource(Res.string.rankings_unpublished),
            modifier = Modifier.fillMaxWidth().weight(1f),
          )
        }

        else -> {
          key(uiState.selectedRegion) {
            RankingsContent(
              teams = visibleTeams,
              selectedRegion = uiState.selectedRegion,
              onTeamSelected = onTeamSelected,
              modifier = Modifier.fillMaxSize(),
            )
          }
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
  RankingsInfoSheet(visible = showRankingInfo, onDismiss = { showRankingInfo = false })
}

@Composable
private fun RankingsContent(
  teams: List<TeamRanking>,
  selectedRegion: RankingRegion?,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.cardMascotViewport(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    item {
      Text(
        text = selectedRegion?.let { region ->
          stringResource(Res.string.top_teams_in_region, teams.size, regionLabel(region))
        } ?: stringResource(Res.string.top_teams_all_circuits, teams.size),
        style = Prism.typography.label,
        color = Prism.color.labelColor,
      )
    }
    items(teams, key = { it.teamId }) { team ->
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
          Text(
            text = listOf(
              stringResource(Res.string.ranking_elo, team.elo.roundToInt()),
              stringResource(Res.string.ranking_record, team.wins, team.losses),
              team.country,
            ).filter(String::isNotBlank).joinToString(separator = " • "),
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
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

@Composable
private fun regionLabel(region: RankingRegion): String = stringResource(
  when (region) {
    RankingRegion.Americas -> Res.string.region_americas
    RankingRegion.Emea -> Res.string.region_emea
    RankingRegion.Pacific -> Res.string.region_pacific
    RankingRegion.China -> Res.string.region_china
  },
)
