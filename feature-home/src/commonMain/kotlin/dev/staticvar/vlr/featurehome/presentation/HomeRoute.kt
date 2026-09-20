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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.component.common.SharedScreenTitleBar
import dev.staticvar.vlr.sharedui.component.event.overview.EventPreviewItem
import dev.staticvar.vlr.sharedui.component.match.overview.MatchPreviewItem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_home.generated.resources.Res
import vlr.feature_home.generated.resources.empty_related_events
import vlr.feature_home.generated.resources.empty_related_matches
import vlr.feature_home.generated.resources.events
import vlr.feature_home.generated.resources.favorites
import vlr.feature_home.generated.resources.home
import vlr.feature_home.generated.resources.loading_your_favorites
import vlr.feature_home.generated.resources.matches
import vlr.feature_home.generated.resources.players
import vlr.feature_home.generated.resources.settings
import vlr.feature_home.generated.resources.teams
import vlr.feature_home.generated.resources.your_favorites

private const val MaximumVisibleIndicators: Int = 5

@Composable
public fun HomeRoute(
  uiState: HomeUiState,
  onRefresh: () -> Unit,
  onSettings: () -> Unit,
  onBrowseMatches: () -> Unit,
  onBrowseEvents: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  announcement: @Composable () -> Unit = {},
  onEventPreviewSelected: (EventPreview) -> Unit = { onEventSelected(it.id) },
  onMatchPreviewSelected: (MatchPreview) -> Unit = { onMatchSelected(it.id) },
) {
  val feed = uiState.feed
  val hasContent = feed.hasDirectFavorites

  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    SharedScreenTitleBar(
      title = stringResource(Res.string.home),
      subtitle = stringResource(Res.string.your_favorites),
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      actions = {
        PrismIconButton(
          icon = Prism.icons.settings.unselected,
          contentDescription = stringResource(Res.string.settings),
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
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      hasContent = hasContent,
      isRefreshing = false,
      errorMessage = uiState.errorMessage.takeIf { hasContent },
      errorDetails = uiState.errorDetails,
      onRefresh = onRefresh,
    )

    announcement()

    HomeBody(
      feed = feed,
      hasLoadedFeed = uiState.hasLoadedFeed,
      onBrowseMatches = onBrowseMatches,
      onBrowseEvents = onBrowseEvents,
      onMatchSelected = onMatchSelected,
      onEventSelected = onEventSelected,
      onEventPreviewSelected = onEventPreviewSelected,
      onMatchPreviewSelected = onMatchPreviewSelected,
      onTeamSelected = onTeamSelected,
      onPlayerSelected = onPlayerSelected,
      modifier = Modifier.fillMaxWidth().weight(1f),
    )
  }
}

@Composable
private fun HomeBody(
  feed: HomeFeed,
  hasLoadedFeed: Boolean,
  onBrowseMatches: () -> Unit,
  onBrowseEvents: () -> Unit,
  onEventPreviewSelected: (EventPreview) -> Unit,
  onMatchPreviewSelected: (MatchPreview) -> Unit,
  onMatchSelected: (String) -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  when {
    !hasLoadedFeed && !feed.hasDirectFavorites -> SharedScreenLoading(
      label = stringResource(Res.string.loading_your_favorites),
      modifier = modifier.padding(horizontal = Prism.dimens.spacingM),
    )

    !feed.hasDirectFavorites -> HomeEmptyFavorites(
      onBrowseMatches = onBrowseMatches,
      onBrowseEvents = onBrowseEvents,
      modifier = modifier.padding(horizontal = Prism.dimens.spacingM),
    )

    else -> HomeFeedContent(
      feed = feed,
      onMatchSelected = onMatchSelected,
      onEventSelected = onEventSelected,
      onEventPreviewSelected = onEventPreviewSelected,
      onMatchPreviewSelected = onMatchPreviewSelected,
      onTeamSelected = onTeamSelected,
      onPlayerSelected = onPlayerSelected,
      modifier = modifier,
    )
  }
}

@Composable
private fun HomeFeedContent(
  feed: HomeFeed,
  onEventPreviewSelected: (EventPreview) -> Unit,
  onMatchPreviewSelected: (MatchPreview) -> Unit,
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
        onMatchSelected = onMatchPreviewSelected,
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
        title = stringResource(Res.string.favorites),
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }

    favoriteGroup(
      title = Res.string.teams,
      kind = "team",
      favorites = feed.directFavorites.teams,
      onSelected = onTeamSelected,
    )
    favoriteGroup(
      title = Res.string.events,
      kind = "event",
      favorites = feed.directFavorites.events,
      onSelected = onEventSelected,
    )
    favoriteGroup(
      title = Res.string.matches,
      kind = "match",
      favorites = feed.directFavorites.matches,
      onSelected = onMatchSelected,
    )
    favoriteGroup(
      title = Res.string.players,
      kind = "player",
      favorites = feed.directFavorites.players,
      onSelected = onPlayerSelected,
    )
  }
}

@Composable
private fun PersonalizedMatches(
  matches: List<MatchPreview>,
  onMatchSelected: (MatchPreview) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismSectionTitle(
      title = stringResource(Res.string.matches),
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      trailing = { RailCount(matches.size) },
    )
    if (matches.isEmpty()) {
      EmptyRailMessage(stringResource(Res.string.empty_related_matches))
    } else {
      val pagerState = rememberPagerState(
        initialPage = initialHomeMatchPage(matches),
        pageCount = { matches.size },
      )
      PrismCarousel(
        itemCount = matches.size,
        pagerState = pagerState,
        variant = PrismCarouselVariant.Multibrowse,
        showIndicators = matches.size <= MaximumVisibleIndicators,
        key = { page -> matches[page].id },
      ) { page ->
        val match = matches[page]
        MatchPreviewItem(
          matchPreview = match,
          modifier = Modifier.fillMaxWidth(),
          onClick = { onMatchSelected(match) },
        )
      }
    }
  }
}

internal fun initialHomeMatchPage(matches: List<MatchPreview>): Int =
  matches.indexOfFirst { it.status == MatchStatus.LIVE }.coerceAtLeast(0)

@Composable
private fun PersonalizedEvents(
  events: List<EventPreview>,
  onEventSelected: (EventPreview) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismSectionTitle(
      title = stringResource(Res.string.events),
      modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      trailing = { RailCount(events.size) },
    )
    if (events.isEmpty()) {
      EmptyRailMessage(
        stringResource(Res.string.empty_related_events),
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
  title: StringResource,
  kind: String,
  favorites: List<T>,
  onSelected: (String) -> Unit,
) {
  if (favorites.isEmpty()) return

  item(key = "favorite-$kind-title") {
    PrismSectionTitle(
      title = stringResource(title),
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
        conditionalOutline = favorite is DirectFavorite.Team || favorite is DirectFavorite.Event,
        contentDescription = favorite.title,
        size = PrismIconSize.Large,
        style = PrismIconStyle.Borderless,
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
