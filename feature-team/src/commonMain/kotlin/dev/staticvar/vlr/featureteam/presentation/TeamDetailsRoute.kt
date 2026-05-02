/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import org.koin.mp.KoinPlatform

@Composable
public fun TeamDetailsRoute(
  teamId: String,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: TeamDetailsViewModel = remember(teamId) { KoinPlatform.getKoin().get<TeamDetailsViewModel>() }
  val uiState: TeamDetailsUiState by viewModel.uiState.collectAsState()
  var section: TeamMatchesSection by rememberSaveable { mutableStateOf(TeamMatchesSection.Upcoming) }

  LaunchedEffect(teamId) {
    viewModel.openTeam(teamId)
  }

  DisposableEffect(viewModel) {
    onDispose(viewModel::clear)
  }

  TeamDetailsScreen(
    uiState = uiState,
    section = section,
    onSectionSelected = { section = it },
    onBack = onBack,
    onMatchSelected = onMatchSelected,
    onPlayerSelected = onPlayerSelected,
    onEventSelected = onEventSelected,
    modifier = modifier,
  )
}

@Composable
internal fun TeamDetailsScreen(
  uiState: TeamDetailsUiState,
  section: TeamMatchesSection,
  onSectionSelected: (TeamMatchesSection) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val team = uiState.team

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = team?.name ?: "Team details",
      subtitle = team?.tag?.ifBlank { team.country } ?: "Roster and recent form",
      preLabel = "team",
      actions = {
        PrismButton(onClick = onBack, style = PrismButtonStyle.Tertiary) {
          Text(text = "Back")
        }
      },
    )

    when {
      uiState.isLoading -> PrismStateMessage(text = "Loading team details…")

      uiState.errorMessage != null && team == null ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load team details.")

      team == null -> PrismStateMessage(text = "Team detail is unavailable.")

      else -> {
        PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
          Text(text = team.name, style = Prism.typography.sectionTitle, color = Prism.color.titleColor)
          Text(
            text = listOfNotNull(team.tag.takeIf(String::isNotBlank), team.region, team.country).joinToString(" • "),
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
          )
          Text(
            text = if (team.rank > 0) "#${team.rank}" else "Unranked",
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            style = Prism.typography.label,
            color = Prism.color.bodyColor,
          )
        }
        if (team.roster.isNotEmpty()) {
          PrismSectionTitle(title = "Roster", preLabel = "players")
          LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
          ) {
            items(team.roster, key = { it.id }) { player ->
              PrismCard(
                modifier = Modifier.fillMaxWidth(),
                style = PrismCardStyle.Outlined,
                onClick = { onPlayerSelected(player.id) },
              ) {
                Text(text = player.alias, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
                Text(
                  text = listOfNotNull(
                    player.name.takeIf(String::isNotBlank),
                    player.role,
                    player.country,
                  ).joinToString(" • "),
                  modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                  style = Prism.typography.bodySmall,
                  color = Prism.color.labelColor,
                )
              }
            }
          }
        }
        PrismTabs(
          tabs = TeamMatchesSection.entries.map { PrismTab(id = it.name, label = it.name) },
          selectedTabId = section.name,
          onTabSelected = { onSectionSelected(TeamMatchesSection.valueOf(it.id)) },
        )
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          when (section) {
            TeamMatchesSection.Upcoming -> {
              if (team.upcomingMatches.isEmpty()) {
                item { PrismStateMessage(text = "No upcoming matches published yet.") }
              } else {
                items(team.upcomingMatches, key = { it.matchId }) { match ->
                  val eventId = match.eventId
                  PrismCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = PrismCardStyle.Outlined,
                    onClick = { onMatchSelected(match.matchId) },
                  ) {
                    Text(text = match.opponent, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
                    Text(
                      text = "${match.eventName} • ${match.stage}",
                      modifier = Modifier
                        .padding(top = Prism.dimens.spacingXs)
                        .let { base -> if (eventId != null) base.clickable { onEventSelected(eventId) } else base },
                      style = Prism.typography.bodySmall,
                      color = if (eventId != null) Prism.color.accent else Prism.color.labelColor,
                    )
                    Text(
                      text = listOfNotNull(match.eta, match.date).joinToString(" • "),
                      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                      style = Prism.typography.label,
                      color = Prism.color.bodyColor,
                    )
                  }
                }
              }
            }

            TeamMatchesSection.Completed -> {
              if (team.completedMatches.isEmpty()) {
                item { PrismStateMessage(text = "No completed matches published yet.") }
              } else {
                items(team.completedMatches, key = { it.matchId }) { match ->
                  val eventId = match.eventId
                  PrismCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = PrismCardStyle.Outlined,
                    onClick = { onMatchSelected(match.matchId) },
                  ) {
                    Text(text = match.opponent, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
                    Text(
                      text = "${match.eventName} • ${match.stage}",
                      modifier = Modifier
                        .padding(top = Prism.dimens.spacingXs)
                        .let { base -> if (eventId != null) base.clickable { onEventSelected(eventId) } else base },
                      style = Prism.typography.bodySmall,
                      color = if (eventId != null) Prism.color.accent else Prism.color.labelColor,
                    )
                    Text(
                      text = "${match.result} • ${match.date}",
                      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                      style = Prism.typography.label,
                      color = Prism.color.bodyColor,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
