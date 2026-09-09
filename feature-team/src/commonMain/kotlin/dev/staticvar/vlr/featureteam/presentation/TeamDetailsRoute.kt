/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

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
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus

@Composable
public fun TeamDetailsRoute(
  uiState: TeamDetailsUiState,
  section: TeamMatchesSection,
  onSectionSelected: (TeamMatchesSection) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  TeamDetailsScreen(
    uiState = uiState,
    section = section,
    onSectionSelected = onSectionSelected,
    onBack = onBack,
    onMatchSelected = onMatchSelected,
    onPlayerSelected = onPlayerSelected,
    onEventSelected = onEventSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onToggleFavorite = onToggleFavorite,
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
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  val team = uiState.team

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = team?.name ?: "Team details",
        subtitle = "Roster, results and recent form",
        onBackPress = onBack,
        actions = {
          if (team != null) {
            PrismFavoriteIcon(
              selected = team.isFavorite,
              size = PrismFavoriteIconSize.Large,
              contentDescription = if (uiState.isUpdatingFavorite) {
                "Updating favorite"
              } else if (team.isFavorite) {
                "Remove team from favorites"
              } else {
                "Add team to favorites"
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
        errorMessage = uiState.errorMessage.takeIf { team != null },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }

    uiState.favoriteErrorMessage?.let { message ->
      PrismStateMessage(text = message)
    }

    when {
      uiState.isLoading && team == null -> PrismStateMessage(text = "Loading team details…")

      uiState.errorMessage != null && team == null ->
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      team == null -> PrismStateMessage(text = "No team details published yet.")

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f).cardMascotViewport(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          item(key = "summary") {
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
          }
          if (team.roster.isNotEmpty()) {
            item(key = "roster-title") {
              PrismSectionTitle(title = "Roster", preLabel = "players")
            }
            items(team.roster, key = { "player:${it.id}" }) { player ->
              PrismCard(
                modifier = Modifier.fillMaxWidth(),
                style = PrismCardStyle.Outlined,
                onClick = { onPlayerSelected(player.id) },
              ) {
                Text(text = player.alias, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
                Text(
                  text = listOfNotNull(
                    player.name,
                    player.role,
                    player.country,
                  ).filter(String::isNotBlank).joinToString(" • "),
                  modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                  style = Prism.typography.bodySmall,
                  color = Prism.color.labelColor,
                )
              }
            }
          }
          item(key = "match-tabs") {
            PrismTabs(
              modifier = Modifier.padding(bottom = Prism.dimens.spacingS),
              tabs = TeamMatchesSection.entries.map { PrismTab(id = it.name, label = it.name) },
              selectedTabId = section.name,
              onTabSelected = { onSectionSelected(TeamMatchesSection.valueOf(it.id)) },
            )
          }
          when (section) {
            TeamMatchesSection.Upcoming -> {
              if (team.upcomingMatches.isEmpty()) {
                item { PrismStateMessage(text = "No upcoming matches published yet.") }
              } else {
                items(team.upcomingMatches, key = { "upcoming:${it.matchId}" }) { match ->
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
                items(team.completedMatches, key = { "completed:${it.matchId}" }) { match ->
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
          item {
            Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
          }
        }
      }
    }
  }
}
