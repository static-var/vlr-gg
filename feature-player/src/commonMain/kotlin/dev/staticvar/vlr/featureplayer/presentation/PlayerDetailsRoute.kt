/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.PlayerAgentStat
import dev.staticvar.vlr.domain.model.PlayerTeam
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerContent
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice
import dev.staticvar.vlr.sharedui.spoilers.SpoilerScore

@Composable
public fun PlayerDetailsRoute(
  uiState: PlayerDetailsUiState,
  onBack: () -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  PlayerDetailsScreen(
    uiState = uiState,
    onBack = onBack,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onToggleFavorite = onToggleFavorite,
  )
}

@Composable
internal fun PlayerDetailsScreen(
  uiState: PlayerDetailsUiState,
  onBack: () -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  val isOnline = LocalIsOnline.current
  val player = uiState.player
  val spoilersHidden = LocalSpoilerMode.current.enabled

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PlayerDetailsChrome(
      title = player?.alias?.ifBlank { player.name } ?: "Player details",
      isFavorite = player?.isFavorite,
      isUpdatingFavorite = uiState.isUpdatingFavorite,
      hasContent = player != null,
      isRefreshing = player != null && (uiState.isRefreshing || uiState.isLoading),
      errorMessage = uiState.errorMessage.takeIf { player != null },
      errorDetails = uiState.errorDetails,
      onBack = onBack,
      onRefresh = onRefresh,
      onToggleFavorite = onToggleFavorite,
    )

    uiState.favoriteErrorMessage?.let { message ->
      PrismStateMessage(text = message)
    }

    when {
      (!isOnline || uiState.isLoading || uiState.isRefreshing) && player == null ->
        SharedScreenLoading(
          label = "Loading player",
          modifier = Modifier.fillMaxSize(),
        )

      uiState.errorMessage != null && player == null ->
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      player == null ->
        SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveMatches,
          title = "No player details yet",
          message = "This player profile has not been published.",
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      else ->
        PlayerDetailsLoadedContent(
          displayName = player.alias.ifBlank { player.name },
          metadata = listOfNotNull(player.realName, player.country).joinToString(" • "),
          totalWinnings = player.totalWinnings,
          currentTeam = player.currentTeam,
          agentStats = player.agentStats,
          pastTeams = player.pastTeams,
          spoilersHidden = spoilersHidden,
          onTeamSelected = onTeamSelected,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
  }
}

@Composable
private fun PlayerDetailsChrome(
  title: String,
  isFavorite: Boolean?,
  isUpdatingFavorite: Boolean,
  hasContent: Boolean,
  isRefreshing: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onToggleFavorite: () -> Unit,
) {
  Column {
    PrismScreenTitleBar(
      title = title,
      subtitle = "Stats, agents and team history",
      onBackPress = onBack,
      actions = {
        if (isFavorite != null) {
          PrismFavoriteIcon(
            selected = isFavorite,
            size = PrismFavoriteIconSize.Large,
            contentDescription =
              if (isUpdatingFavorite) {
                "Updating favorite"
              } else if (isFavorite) {
                "Remove player from favorites"
              } else {
                "Add player to favorites"
              },
            modifier =
              Modifier.clickable(
                enabled = !isUpdatingFavorite,
                role = Role.Button,
                onClick = onToggleFavorite,
              ),
          )
        }
      },
    )

    SharedRefreshStatus(
      hasContent = hasContent,
      isRefreshing = isRefreshing,
      errorMessage = errorMessage,
      errorDetails = errorDetails,
      onRefresh = onRefresh,
    )
  }
}

@Composable
private fun PlayerDetailsLoadedContent(
  displayName: String,
  metadata: String,
  totalWinnings: Double,
  currentTeam: PlayerTeam?,
  agentStats: List<PlayerAgentStat>,
  pastTeams: List<PlayerTeam>,
  spoilersHidden: Boolean,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val displayedAgentStats =
    remember(agentStats, spoilersHidden) {
      if (spoilersHidden) agentStats.sortedBy { it.agentName } else agentStats
    }

  LazyColumn(
    modifier = modifier.cardMascotViewport(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    item {
      PlayerSummaryCard(
        displayName = displayName,
        metadata = metadata,
        totalWinnings = totalWinnings,
        currentTeam = currentTeam,
        onTeamSelected = onTeamSelected,
      )
    }
    playerAgentStatItems(agentStats = displayedAgentStats, spoilersHidden = spoilersHidden)
    playerTeamHistoryItems(teams = pastTeams, onTeamSelected = onTeamSelected)
    item {
      Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
    }
  }
}

@Composable
private fun PlayerSummaryCard(
  displayName: String,
  metadata: String,
  totalWinnings: Double,
  currentTeam: PlayerTeam?,
  onTeamSelected: (String) -> Unit,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth().cardMascotEligible(topClearance = Prism.dimens.spacingM),
    style = PrismCardStyle.Outlined,
  ) {
    Text(
      text = displayName,
      style = Prism.typography.sectionTitle,
      color = Prism.color.titleColor,
    )
    Text(
      text = metadata,
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
    )
    SpoilerScore(
      text = "$$totalWinnings",
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.label,
      color = Prism.color.bodyColor,
    )
    currentTeam?.let { team ->
      val teamId = team.id
      Text(
        text = team.name,
        modifier =
          Modifier.padding(top = Prism.dimens.spacingM).let { base ->
            if (teamId != null) base.clickable { onTeamSelected(teamId) } else base
          },
        style = Prism.typography.cardTitle,
        color = if (teamId != null) Prism.color.accent else Prism.color.titleColor,
      )
    }
  }
}

private fun LazyListScope.playerAgentStatItems(
  agentStats: List<PlayerAgentStat>,
  spoilersHidden: Boolean,
) {
  if (agentStats.isEmpty()) return

  item {
    PrismSectionTitle(title = "Agent stats", preLabel = "pool")
  }
  if (spoilersHidden) {
    item {
      SpoilerHiddenNotice()
    }
  }
  items(agentStats, key = { it.agentName }) { stat ->
    PlayerAgentStatItem(stat = stat)
  }
}

@Composable
private fun PlayerAgentStatItem(stat: PlayerAgentStat) {
  PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
    Text(text = stat.agentName, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
    SpoilerContent(modifier = Modifier.padding(top = Prism.dimens.spacingXs)) {
      Column {
        Text(
          text = "${stat.usagePercent}% usage • ${stat.matchesLabel()}",
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
        )
        Text(
          text = "ACS ${stat.acs} • ADR ${stat.adr} • K/D ${stat.kdRatio}",
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.label,
          color = Prism.color.bodyColor,
        )
      }
    }
  }
}

private fun LazyListScope.playerTeamHistoryItems(
  teams: List<PlayerTeam>,
  onTeamSelected: (String) -> Unit,
) {
  if (teams.isEmpty()) return

  item {
    PrismSectionTitle(title = "Team history", preLabel = "history")
  }
  items(teams) { team ->
    PlayerTeamHistoryItem(team = team, onTeamSelected = onTeamSelected)
  }
}

@Composable
private fun PlayerTeamHistoryItem(team: PlayerTeam, onTeamSelected: (String) -> Unit) {
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
    onClick = {
      val id: String = team.id ?: return@PrismCard
      onTeamSelected(id)
    },
  ) {
    Text(text = team.name, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
    Text(
      text = if (team.isCurrent) "Current team" else "Previous team",
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
    )
  }
}

private fun PlayerAgentStat.matchesLabel(): String = "$usageCount picks • $roundsPlayed rounds"
