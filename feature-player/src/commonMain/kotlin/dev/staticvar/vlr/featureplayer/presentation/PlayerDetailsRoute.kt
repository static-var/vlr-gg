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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import org.koin.mp.KoinPlatform

@Composable
public fun PlayerDetailsRoute(
  playerId: String,
  onBack: () -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: PlayerDetailsViewModel = remember(playerId) { KoinPlatform.getKoin().get<PlayerDetailsViewModel>() }
  val uiState: PlayerDetailsUiState by viewModel.uiState.collectAsState()

  LaunchedEffect(playerId) {
    viewModel.openPlayer(playerId)
  }

  DisposableEffect(viewModel) {
    onDispose(viewModel::clear)
  }

  PlayerDetailsScreen(
    uiState = uiState,
    onBack = onBack,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
  )
}

@Composable
internal fun PlayerDetailsScreen(
  uiState: PlayerDetailsUiState,
  onBack: () -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val player = uiState.player

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = player?.alias?.ifBlank { player.name } ?: "Player details",
      subtitle = player?.realName ?: player?.country ?: "Agent pool and team history",
      onBackPress = onBack,
    )

    when {
      uiState.isLoading -> PrismStateMessage(text = "Loading player details…")

      uiState.errorMessage != null && player == null ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load player details.")

      player == null -> PrismStateMessage(text = "Player detail is unavailable.")

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
            items(player.pastTeams, key = { (it.id ?: it.name) + it.isCurrent }) { team ->
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
