/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.vlr.featurehome.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.vlr.sharedui.component.common.SharedScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.carousel.PrismCarousel
import dev.staticvar.designsystem.component.carousel.PrismCarouselVariant
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.component.event.overview.EventPreviewItem
import dev.staticvar.vlr.sharedui.component.match.overview.MatchPreviewItem

private const val MaximumVisibleIndicators: Int = 5

@Composable
public fun HomeRoute(
  uiState: HomeUiState,
  onRefresh: () -> Unit,
  onSettings: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onEventPreviewSelected: (EventPreview) -> Unit = { onEventSelected(it.id) },
) {
  val feed = uiState.feed
  val hasContent = feed.hasDirectFavorites

  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    SharedScreenTitleBar(
      title = "Home",
      subtitle = "Your Favorites",
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      actions = {
        PrismIconButton(
          icon = Prism.icons.settings.unselected,
          contentDescription = "Settings",
          size = PrismIconButtonSize.Toolbar,
          onClick = onSettings,
        )
        SharedRefreshButton(
          isLoading = uiState.isLoading,
          isRefreshing = uiState.isRefreshing,
          hasContent = hasContent,
          onRefresh = onRefresh,
        )
      },
    )
    SharedRefreshStatus(
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),      hasContent = hasContent,
      isRefreshing = false,
      errorMessage = uiState.errorMessage.takeIf { hasContent },
      errorDetails = uiState.errorDetails,
      onRefresh = onRefresh,
    )

    when {
      uiState.isLoading && !hasContent -> SharedScreenLoading(
        label = "Loading your favorites",
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM),
      )

      uiState.errorMessage != null && !hasContent -> SharedLoadError(
        errorMessage = uiState.errorMessage,
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
        centered = true,
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM),
      )

      else -> HomeFeedContent(
        feed = feed,
        onMatchSelected = onMatchSelected,
        onEventSelected = onEventSelected,
        onEventPreviewSelected = onEventPreviewSelected,
        onTeamSelected = onTeamSelected,
        onPlayerSelected = onPlayerSelected,
        modifier = Modifier.fillMaxWidth().weight(1f),
      )
    }
  }
}

@Composable
private fun HomeFeedContent(
  feed: HomeFeed,
  onEventPreviewSelected: (EventPreview) -> Unit,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(bottom = Prism.dimens.spacingL),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    item(key = "personalized-matches") {
      PersonalizedMatches(
        matches = feed.personalizedMatches,
        onMatchSelected = onMatchSelected,
      )
    }
    item(key = "personalized-events") {
      PersonalizedEvents(
        events = feed.personalizedEvents,
        onEventSelected = onEventPreviewSelected,
      )
    }
    item(key = "favorites-title") {
      PrismSectionTitle(
        title = "Favorites",
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }

    favoriteGroup(
      title = "Teams",
      kind = "team",
      favorites = feed.directFavorites.teams,
      onSelected = onTeamSelected,
    )
    favoriteGroup(
      title = "Events",
      kind = "event",
      favorites = feed.directFavorites.events,
      onSelected = onEventSelected,
    )
    favoriteGroup(
      title = "Matches",
      kind = "match",
      favorites = feed.directFavorites.matches,
      onSelected = onMatchSelected,
    )
    favoriteGroup(
      title = "Players",
      kind = "player",
      favorites = feed.directFavorites.players,
      onSelected = onPlayerSelected,
    )
  }
}

@Composable
private fun PersonalizedMatches(
  matches: List<MatchPreview>,
  onMatchSelected: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismSectionTitle(
      title = "Matches",
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      trailing = { RailCount(matches.size) },
    )
    if (matches.isEmpty()) {
      EmptyRailMessage("No matches currently connect to your favorites. New matchups will appear here automatically.")
    } else {
      PrismCarousel(
        itemCount = matches.size,
        variant = PrismCarouselVariant.Multibrowse,
        showIndicators = matches.size <= MaximumVisibleIndicators,
        key = { page -> matches[page].id },
      ) { page ->
        val match = matches[page]
        MatchPreviewItem(
          matchPreview = match,
          modifier = Modifier.fillMaxWidth(),
          onClick = { onMatchSelected(match.id) },
        )
      }
    }
  }
}

@Composable
private fun PersonalizedEvents(
  events: List<EventPreview>,
  onEventSelected: (EventPreview) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismSectionTitle(
      title = "Events",
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      trailing = { RailCount(events.size) },
    )
    if (events.isEmpty()) {
      EmptyRailMessage(
        "No current events are linked to your favorites. Related tournaments will appear as schedules update.",
      )
    } else {
      PrismCarousel(
        itemCount = events.size,
        variant = PrismCarouselVariant.Multibrowse,
        showIndicators = events.size <= MaximumVisibleIndicators,
        key = { page -> events[page].id },
      ) { page ->
        val event = events[page]
        EventPreviewItem(
          eventPreview = event,
          modifier = Modifier.fillMaxWidth(),
          onClick = { onEventSelected(event) },
        )
      }
    }
  }
}

@Composable
private fun RailCount(count: Int) {
  Text(
    text = count.toString(),
    style = Prism.typography.label,
    color = Prism.color.labelColor,
  )
}

@Composable
private fun EmptyRailMessage(message: String) {
  Text(
    text = message,
    modifier = Modifier.fillMaxWidth().padding(
      horizontal = Prism.dimens.spacingM,
      vertical = Prism.dimens.spacingS,
    ),
    style = Prism.typography.bodySmall,
    color = Prism.color.bodyColor,
  )
}

private fun <T : DirectFavorite> androidx.compose.foundation.lazy.LazyListScope.favoriteGroup(
  title: String,
  kind: String,
  favorites: List<T>,
  onSelected: (String) -> Unit,
) {
  if (favorites.isEmpty()) return

  item(key = "favorite-$kind-title") {
    PrismSectionTitle(
      title = title,
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      showDivider = false,
    )
  }
  items(
    items = favorites,
    key = { favorite -> "$kind:${favorite.id}" },
  ) { favorite ->
    DirectFavoriteItem(
      favorite = favorite,
      onClick = { onSelected(favorite.id) },
    )
  }
}

@Composable
private fun DirectFavoriteItem(
  favorite: DirectFavorite,
  onClick: () -> Unit,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth().padding(horizontal = Prism.dimens.spacingM),
    style = PrismCardStyle.Outlined,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SharedNetworkIcon(
        imageUrl = favorite.imageUrl,
        contentDescription = favorite.title,
        size = PrismIconSize.Large,
        style = PrismIconStyle.Bordered,
        tint = PrismIconTint.None,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = favorite.title,
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = "→",
        style = Prism.typography.sectionTitle,
        color = Prism.color.accent,
      )
    }
  }
}
