/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconStyle
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.TeamCompletedMatch
import dev.staticvar.vlr.domain.model.TeamPlayer
import dev.staticvar.vlr.domain.model.TeamUpcomingMatch
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice
import dev.staticvar.vlr.sharedui.spoilers.SpoilerScore
import org.jetbrains.compose.resources.stringResource
import vlr.feature_team.generated.resources.Res
import vlr.feature_team.generated.resources.add_team_favorite
import vlr.feature_team.generated.resources.completed
import vlr.feature_team.generated.resources.event_stage
import vlr.feature_team.generated.resources.favorite_player
import vlr.feature_team.generated.resources.loading_team
import vlr.feature_team.generated.resources.middle_dot
import vlr.feature_team.generated.resources.no_completed_matches
import vlr.feature_team.generated.resources.no_team_details
import vlr.feature_team.generated.resources.no_team_results
import vlr.feature_team.generated.resources.no_upcoming_matches
import vlr.feature_team.generated.resources.no_upcoming_team_fixtures
import vlr.feature_team.generated.resources.players
import vlr.feature_team.generated.resources.remove_team_favorite
import vlr.feature_team.generated.resources.roster
import vlr.feature_team.generated.resources.team_details
import vlr.feature_team.generated.resources.team_not_published
import vlr.feature_team.generated.resources.team_rank
import vlr.feature_team.generated.resources.team_subtitle
import vlr.feature_team.generated.resources.unranked
import vlr.feature_team.generated.resources.upcoming
import vlr.feature_team.generated.resources.updating_favorite

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
  val isOnline = LocalIsOnline.current
  val team = uiState.team
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val itemSeparator = " ${stringResource(Res.string.middle_dot)} "

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    TeamDetailsChrome(
      title = team?.name ?: stringResource(Res.string.team_details),
      isFavorite = team?.isFavorite,
      isUpdatingFavorite = uiState.isUpdatingFavorite,
      hasContent = team != null,
      isRefreshing = team != null && (uiState.isRefreshing || uiState.isLoading),
      errorMessage = uiState.errorMessage.takeIf { team != null },
      errorDetails = uiState.errorDetails,
      onBack = onBack,
      onRefresh = onRefresh,
      onToggleFavorite = onToggleFavorite,
    )

    uiState.favoriteErrorMessage?.let { message ->
      PrismStateMessage(text = stringResource(message))
    }

    when {
      (!isOnline || uiState.isLoading || uiState.isRefreshing) && team == null ->
        SharedScreenLoading(
          label = stringResource(Res.string.loading_team),
          modifier = Modifier.fillMaxSize(),
        )

      uiState.errorMessage != null && team == null ->
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      team == null ->
        SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveMatches,
          title = stringResource(Res.string.no_team_details),
          message = stringResource(Res.string.team_not_published),
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      else ->
        TeamDetailsLoadedContent(
          name = team.name,
          logoUrl = team.logoUrl,
          metadata =
            listOfNotNull(
                team.tag.takeIf(String::isNotBlank),
                team.regionLabel.ifBlank { team.region },
                team.country,
              )
              .joinToString(itemSeparator),
          rank = team.rank,
          roster = team.roster,
          upcomingMatches = team.upcomingMatches,
          completedMatches = team.completedMatches,
          section = section,
          spoilersHidden = spoilersHidden,
          canShowEmpty =
            isOnline && !uiState.isRefreshing && !uiState.isLoading && uiState.errorMessage == null,
          onSectionSelected = onSectionSelected,
          onMatchSelected = onMatchSelected,
          onPlayerSelected = onPlayerSelected,
          onEventSelected = onEventSelected,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
  }
}

@Composable
private fun TeamDetailsChrome(
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
      subtitle = stringResource(Res.string.team_subtitle),
      onBackPress = onBack,
      actions = {
        if (isFavorite != null) {
          PrismFavoriteIcon(
            selected = isFavorite,
            size = PrismFavoriteIconSize.Large,
            contentDescription =
              if (isUpdatingFavorite) {
                stringResource(Res.string.updating_favorite)
              } else if (isFavorite) {
                stringResource(Res.string.remove_team_favorite)
              } else {
                stringResource(Res.string.add_team_favorite)
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
private fun TeamDetailsLoadedContent(
  name: String,
  logoUrl: String,
  metadata: String,
  rank: Int,
  roster: List<TeamPlayer>,
  upcomingMatches: List<TeamUpcomingMatch>,
  completedMatches: List<TeamCompletedMatch>,
  section: TeamMatchesSection,
  spoilersHidden: Boolean,
  canShowEmpty: Boolean,
  onSectionSelected: (TeamMatchesSection) -> Unit,
  onMatchSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.cardMascotViewport(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    item(key = "summary") {
      TeamSummaryCard(name = name, logoUrl = logoUrl, metadata = metadata, rank = rank)
    }
    teamRosterItems(roster = roster, onPlayerSelected = onPlayerSelected)
    item(key = "match-tabs") {
      PrismTabs(
        modifier = Modifier.padding(bottom = Prism.dimens.spacingS),
        tabs = TeamMatchesSection.entries.map { tab ->
          PrismTab(
            id = tab.name,
            label = stringResource(
              when (tab) {
                TeamMatchesSection.Upcoming -> Res.string.upcoming
                TeamMatchesSection.Completed -> Res.string.completed
              },
            ),
          )
        },
        selectedTabId = section.name,
        onTabSelected = { onSectionSelected(TeamMatchesSection.valueOf(it.id)) },
      )
    }
    when (section) {
      TeamMatchesSection.Upcoming ->
        teamUpcomingMatchItems(
          matches = upcomingMatches,
          canShowEmpty = canShowEmpty,
          onMatchSelected = onMatchSelected,
          onEventSelected = onEventSelected,
        )

      TeamMatchesSection.Completed ->
        teamCompletedMatchItems(
          matches = completedMatches,
          spoilersHidden = spoilersHidden,
          canShowEmpty = canShowEmpty,
          onMatchSelected = onMatchSelected,
          onEventSelected = onEventSelected,
        )
    }
    item {
      Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
    }
  }
}

@Composable
private fun TeamSummaryCard(name: String, logoUrl: String, metadata: String, rank: Int) {
  PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
    val hasLogo = logoUrl.isNotBlank()
    Box(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier =
          Modifier.fillMaxWidth(if (hasLogo) 0.65f else 1f)
            .padding(end = if (hasLogo) Prism.dimens.spacingS else 0.dp),
      ) {
        Text(text = name, style = Prism.typography.sectionTitle, color = Prism.color.titleColor)
        Text(
          text = metadata,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
        )
        SpoilerScore(
          text = if (rank > 0) stringResource(Res.string.team_rank, rank) else stringResource(Res.string.unranked),
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.label,
          color = Prism.color.bodyColor,
        )
      }
      if (hasLogo) {
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
          AsyncImage(
            model = logoUrl,
            contentDescription = name,
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.35f),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterEnd,
          )
        }
      }
    }
  }
}

private fun LazyListScope.teamRosterItems(
  roster: List<TeamPlayer>,
  onPlayerSelected: (String) -> Unit,
) {
  if (roster.isEmpty()) return

  item(key = "roster-title") {
    PrismSectionTitle(title = stringResource(Res.string.roster), preLabel = stringResource(Res.string.players))
  }
  items(roster, key = { "player:${it.id}" }) { player ->
    TeamRosterItem(player = player, onPlayerSelected = onPlayerSelected)
  }
}

@Composable
private fun TeamRosterItem(player: TeamPlayer, onPlayerSelected: (String) -> Unit) {
  val itemSeparator = " ${stringResource(Res.string.middle_dot)} "
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
    onClick = { onPlayerSelected(player.id) },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = player.alias,
        modifier = Modifier.weight(1f),
        style = Prism.typography.cardTitle,
        color = if (player.isFavorite) Prism.color.accent else Prism.color.titleColor,
      )
      if (player.isFavorite) {
        PrismFavoriteIcon(
          selected = true,
          size = PrismFavoriteIconSize.Small,
          style = PrismFavoriteIconStyle.Bare,
          contentDescription = stringResource(Res.string.favorite_player),
        )
      }
    }
    Text(
      text =
        listOfNotNull(player.name, player.role, player.country)
          .filter(String::isNotBlank)
          .joinToString(itemSeparator),
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
    )
  }
}

private fun LazyListScope.teamUpcomingMatchItems(
  matches: List<TeamUpcomingMatch>,
  canShowEmpty: Boolean,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
) {
  if (matches.isEmpty()) {
    if (canShowEmpty) {
      item {
        SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveMatches,
          title = stringResource(Res.string.no_upcoming_matches),
          message = stringResource(Res.string.no_upcoming_team_fixtures),
          compact = true,
        )
      }
    }
    return
  }

  items(matches, key = { "upcoming:${it.matchId}" }) { match ->
    TeamUpcomingMatchItem(
      match = match,
      onMatchSelected = onMatchSelected,
      onEventSelected = onEventSelected,
    )
  }
}

@Composable
private fun TeamUpcomingMatchItem(
  match: TeamUpcomingMatch,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
) {
  val eventId = match.eventId
  val itemSeparator = " ${stringResource(Res.string.middle_dot)} "
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
    onClick = { onMatchSelected(match.matchId) },
  ) {
    Text(text = match.opponent, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
    Text(
      text = stringResource(Res.string.event_stage, match.eventName, match.stage),
      modifier =
        Modifier.padding(top = Prism.dimens.spacingXs).let { base ->
          if (eventId != null) base.clickable { onEventSelected(eventId) } else base
        },
      style = Prism.typography.bodySmall,
      color = if (eventId != null) Prism.color.accent else Prism.color.labelColor,
    )
    Text(
      text = listOfNotNull(match.eta, match.date).joinToString(itemSeparator),
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.label,
      color = Prism.color.bodyColor,
    )
  }
}

private fun LazyListScope.teamCompletedMatchItems(
  matches: List<TeamCompletedMatch>,
  spoilersHidden: Boolean,
  canShowEmpty: Boolean,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
) {
  if (matches.isEmpty()) {
    if (canShowEmpty) {
      item {
        SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveMatches,
          title = stringResource(Res.string.no_completed_matches),
          message = stringResource(Res.string.no_team_results),
          compact = true,
        )
      }
    }
    return
  }

  if (spoilersHidden) {
    item(key = "completed-spoiler-notice") {
      SpoilerHiddenNotice()
    }
  }
  items(matches, key = { "completed:${it.matchId}" }) { match ->
    TeamCompletedMatchItem(
      match = match,
      spoilersHidden = spoilersHidden,
      onMatchSelected = onMatchSelected,
      onEventSelected = onEventSelected,
    )
  }
}

@Composable
private fun TeamCompletedMatchItem(
  match: TeamCompletedMatch,
  spoilersHidden: Boolean,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
) {
  val eventId = match.eventId
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
    onClick = { onMatchSelected(match.matchId) },
  ) {
    Text(text = match.opponent, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
    Text(
      text = stringResource(Res.string.event_stage, match.eventName, match.stage),
      modifier =
        Modifier.padding(top = Prism.dimens.spacingXs).let { base ->
          if (eventId != null) base.clickable { onEventSelected(eventId) } else base
        },
      style = Prism.typography.bodySmall,
      color = if (eventId != null) Prism.color.accent else Prism.color.labelColor,
    )
    Row(
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (spoilersHidden || match.result.isNotBlank()) {
        SpoilerScore(
          text = match.result,
          style = Prism.typography.label,
          color = Prism.color.bodyColor,
        )
      }
      if (match.date.isNotBlank()) {
        if (spoilersHidden || match.result.isNotBlank()) {
          Text(text = stringResource(Res.string.middle_dot), style = Prism.typography.label, color = Prism.color.bodyColor)
        }
        Text(text = match.date, style = Prism.typography.label, color = Prism.color.bodyColor)
      }
    }
  }
}
