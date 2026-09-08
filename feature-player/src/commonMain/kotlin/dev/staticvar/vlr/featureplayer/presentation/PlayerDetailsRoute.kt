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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus

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
  val player = uiState.player

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = player?.alias?.ifBlank { player.name } ?: "Player details",
        subtitle = "Stats, agents and team history",
        onBackPress = onBack,
        actions = {
          if (player != null) {
            PrismFavoriteIcon(
              selected = player.isFavorite,
              size = PrismFavoriteIconSize.Large,
              contentDescription = if (uiState.isUpdatingFavorite) {
                "Updating favorite"
              } else if (player.isFavorite) {
                "Remove player from favorites"
              } else {
                "Add player to favorites"
              },
              modifier = Modifier.clickable(
                enabled = !uiState.isUpdatingFavorite,
                role = Role.Button,
                onClick = onToggleFavorite,
              ),
            )
          }
        },
      )

      SharedRefreshStatus(
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage.takeIf { player != null },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }

    uiState.favoriteErrorMessage?.let { message ->
      PrismStateMessage(text = message)
    }

    when {
      uiState.isLoading && player == null -> PrismStateMessage(text = "Loading player details…")

      uiState.errorMessage != null && player == null ->
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      player == null -> PrismStateMessage(text = "No player details published yet.")

      else -> {
        PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
          Text(
            text = player.alias.ifBlank { player.name },
            style = Prism.typography.sectionTitle,
            color = Prism.color.titleColor,
          )
          Text(
            text = listOfNotNull(player.realName, player.country).joinToString(" • "),
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
          )
          Text(
            text = "$${player.totalWinnings}",
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            style = Prism.typography.label,
            color = Prism.color.bodyColor,
          )
          player.currentTeam?.let { team ->
            val teamId = team.id
            Text(
              text = team.name,
              modifier = Modifier
                .padding(top = Prism.dimens.spacingM)
                .let { base -> if (teamId != null) base.clickable { onTeamSelected(teamId) } else base },
              style = Prism.typography.cardTitle,
              color = if (teamId != null) Prism.color.accent else Prism.color.titleColor,
            )
          }
        }
        if (player.agentStats.isNotEmpty()) {
          PrismSectionTitle(title = "Agent stats", preLabel = "pool")
          LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
          ) {
            items(player.agentStats, key = { it.agentName }) { stat ->
              PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
                Text(text = stat.agentName, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
                Text(
                  text = "${stat.usagePercent}% usage • ${stat.matchesLabel()}",
                  modifier = Modifier.padding(top = Prism.dimens.spacingXs),
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
        if (player.pastTeams.isNotEmpty()) {
          PrismSectionTitle(title = "Team history", preLabel = "history")
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
          ) {
            items(player.pastTeams) { team ->
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
            item {
              Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
            }
          }
        }
      }
    }
  }
}

private fun dev.staticvar.vlr.domain.model.PlayerAgentStat.matchesLabel(): String =
  "${this.usageCount} picks • ${this.roundsPlayed} rounds"
