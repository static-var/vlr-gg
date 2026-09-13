/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.material3.Text
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconStyle
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.vlr.sharedui.component.common.SharedScreenTitleBar
import dev.staticvar.vlr.sharedui.component.common.SharedScrollingDetails
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventTeam
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.featureevents.presentation.mascot.eventMascotCues
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPreviewHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailMatchItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPrizeItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailStandingItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailTeamItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGroupSelector
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGrouping
import dev.staticvar.vlr.sharedui.component.event.detail.groupEventMatches
import dev.staticvar.vlr.sharedui.mascot.MascotCelebration
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.rememberMascot
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice

@Composable
public fun EventDetailsRoute(
  uiState: EventDetailsUiState,
  eventPreview: EventPreview? = null,
  section: EventDetailSection,
  matchGrouping: EventMatchGrouping,
  selectedMatchGroupName: String?,
  onSectionSelected: (EventDetailSection) -> Unit,
  onMatchGroupingSelected: (EventMatchGrouping) -> Unit,
  onMatchGroupSelected: (String) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  EventDetailsScreen(
    uiState = uiState,
    eventPreview = eventPreview,
    section = section,
    matchGrouping = matchGrouping,
    selectedMatchGroupName = selectedMatchGroupName,
    onSectionSelected = onSectionSelected,
    onMatchGroupingSelected = onMatchGroupingSelected,
    onMatchGroupSelected = onMatchGroupSelected,
    onBack = onBack,
    onMatchSelected = onMatchSelected,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onToggleFavorite = onToggleFavorite,
  )
}

@Composable
internal fun EventDetailsScreen(
  uiState: EventDetailsUiState,
  eventPreview: EventPreview? = null,
  section: EventDetailSection,
  matchGrouping: EventMatchGrouping,
  selectedMatchGroupName: String?,
  onSectionSelected: (EventDetailSection) -> Unit,
  onMatchGroupingSelected: (EventMatchGrouping) -> Unit,
  onMatchGroupSelected: (String) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  val event = uiState.event
  val isOnline = LocalIsOnline.current
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val bodyReady = event != null && (
      event.teams.isNotEmpty() || event.matches.isNotEmpty() ||
        event.standings.isNotEmpty() || event.prizes.isNotEmpty() ||
        (!uiState.isLoading && !uiState.isDetailLoadPending)
      )
  val bodyFade = rememberEventContentFade(bodyReady)

  var isGroupingMenuExpanded by remember(event?.id) { mutableStateOf(false) }
  var isNavigatingAway by remember(event?.id) { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val participantsState = rememberLazyListState()
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val mascotCharacter = LocalMascotCharacter.current
  val candidates = remember(event, uiState.favoriteTeamIds, spoilersHidden) {
    if (spoilersHidden) emptyList() else event?.let { eventMascotCues(it, uiState.favoriteTeamIds) }.orEmpty()
  }
  val mascotState = rememberMascot(
    screenKey = event?.id ?: "event-details",
    candidates = candidates,
    isScreenActive = mascotCharacter != null && lifecycleState == Lifecycle.State.RESUMED && !isNavigatingAway,
    isContentReady = event != null && !uiState.isLoading && !uiState.isRefreshing &&
      !uiState.isDetailLoadPending && uiState.errorMessage == null,
    isInteracting = uiState.isSavingFavorite || listState.isScrollInProgress || participantsState.isScrollInProgress || isGroupingMenuExpanded,
  )
  fun leaveScreen(action: () -> Unit) {
    isNavigatingAway = true
    mascotState.onFinished()
    action()
  }

  Box(modifier = modifier.fillMaxSize().clipToBounds()) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      EventDetailsChrome(
        title = event?.title ?: eventPreview?.title ?: "Event details",
        isLoading = uiState.isLoading || uiState.isDetailLoadPending,
        isRefreshing = uiState.isRefreshing,
        hasContent = event != null,
        favoriteErrorMessage = uiState.favoriteErrorMessage,
        errorMessage = uiState.errorMessage.takeIf { event != null },
        errorDetails = uiState.errorDetails,
        onBack = { leaveScreen(onBack) },
        onRefresh = onRefresh,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )

      if (event == null && eventPreview != null) {
        EventDetailPreviewHeaderItem(
          event = eventPreview,
          modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
        )
      }

      when {
        (!isOnline || uiState.isLoading || uiState.isRefreshing) && event == null -> EventDetailsLoading(
          modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM),
          label = "Loading event",
        )

        uiState.errorMessage != null && event == null ->
          SharedLoadError(
            errorMessage = uiState.errorMessage,
            errorDetails = uiState.errorDetails,
            onRefresh = onRefresh,
            centered = true,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM),
          )

        event == null -> SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveEvents,
          title = "No event details yet",
          message = "Details will appear when this event is published.",
          modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM),
        )

        else -> EventDetailsContent(
          event = event,
          section = section,
          matchGrouping = matchGrouping,
          selectedMatchGroupName = selectedMatchGroupName,
          isSavingFavorite = uiState.isSavingFavorite,
          canShowEmptySection = isOnline && !uiState.isLoading && !uiState.isDetailLoadPending &&
            !uiState.isRefreshing && uiState.errorMessage == null,
          spoilersHidden = spoilersHidden,
          listState = listState,
          participantsState = participantsState,
          contentAlpha = bodyFade,
          showContent = bodyReady,
          onToggleFavorite = onToggleFavorite,
          onSectionSelected = onSectionSelected,
          onMatchGroupingSelected = onMatchGroupingSelected,
          onMatchGroupSelected = onMatchGroupSelected,
          onGroupingMenuExpandedChange = { isGroupingMenuExpanded = it },
          onMatchSelected = { matchId -> leaveScreen { onMatchSelected(matchId) } },
          onTeamSelected = { teamId -> leaveScreen { onTeamSelected(teamId) } },
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
    }

    if (!spoilersHidden && mascotCharacter != null) {
      mascotState.cue?.let { cue ->
        MascotCelebration(
          visible = true,
          character = mascotCharacter,
          message = cue.message,
          onFinished = mascotState::onFinished,
          modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding(),
        )
      }
    }
  }
}

@Composable
private fun EventDetailsChrome(
  title: String,
  isLoading: Boolean,
  isRefreshing: Boolean,
  hasContent: Boolean,
  favoriteErrorMessage: String?,
  errorMessage: String?,
  errorDetails: String?,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    SharedScreenTitleBar(
      title = title,
      subtitle = "Teams, matches and standings",
      actions = {
        SharedRefreshButton(
          isLoading = isLoading,
          animateWhileLoading = true,
          isRefreshing = isRefreshing,
          hasContent = hasContent,
          onRefresh = onRefresh,
        )
      },
      onBackPress = onBack,
    )
    favoriteErrorMessage?.let { PrismStateMessage(text = it) }
    SharedRefreshStatus(
      hasContent = hasContent,
      isRefreshing = false,
      errorMessage = errorMessage,
      errorDetails = errorDetails,
      onRefresh = onRefresh,
    )
  }
}

@Composable
private fun EventDetailsContent(
  event: EventDetails,
  section: EventDetailSection,
  matchGrouping: EventMatchGrouping,
  selectedMatchGroupName: String?,
  isSavingFavorite: Boolean,
  canShowEmptySection: Boolean,
  spoilersHidden: Boolean,
  listState: LazyListState,
  participantsState: LazyListState,
  contentAlpha: State<Float>,
  showContent: Boolean,
  onToggleFavorite: () -> Unit,
  onSectionSelected: (EventDetailSection) -> Unit,
  onMatchGroupingSelected: (EventMatchGrouping) -> Unit,
  onMatchGroupSelected: (String) -> Unit,
  onGroupingMenuExpandedChange: (Boolean) -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val groupedMatches = remember(event.matches, matchGrouping) { event.matches.groupEventMatches(matchGrouping) }
  val groupNames = groupedMatches.keys.toList()
  val resolvedMatchGroupName = selectedMatchGroupName.takeIf { it in groupedMatches } ?: groupNames.firstOrNull()
  val visibleMatches = resolvedMatchGroupName?.let { groupName -> groupedMatches[groupName] }.orEmpty()
  val favoriteReasons = if (event.isFavorite) {
    listOf(MatchFavoriteReason(MatchFavoriteSource.EVENT, event.id, event.title))
  } else {
    emptyList()
  }

  SharedScrollingDetails(
    state = listState,
    contentAlpha = contentAlpha,
    showContent = showContent,
    modifier = modifier,
    hero = {
      EventDetailsHero(
        event = event,
        isSavingFavorite = isSavingFavorite,
        onToggleFavorite = onToggleFavorite,
      )
    },
    loading = { loadingModifier ->
      EventDetailsLoading(label = "Loading event details", modifier = loadingModifier)
    },
  ) {
    if (event.teams.isNotEmpty()) {
      item {
        EventParticipantsRail(
          teams = event.teams.take(8),
          listState = participantsState,
          onTeamSelected = onTeamSelected,
        )
      }
    }
    item {
      EventDetailsTabs(section = section, onSectionSelected = onSectionSelected)
    }
    when (section) {
      EventDetailSection.Matches -> eventMatchItems(
        matches = event.matches,
        visibleMatches = visibleMatches,
        favoriteReasons = favoriteReasons,
        grouping = matchGrouping,
        groupNames = groupNames,
        selectedGroupName = resolvedMatchGroupName,
        canShowEmptySection = canShowEmptySection,
        onGroupingSelected = onMatchGroupingSelected,
        onGroupSelected = onMatchGroupSelected,
        onMenuExpandedChange = onGroupingMenuExpandedChange,
        onMatchSelected = onMatchSelected,
      )

      EventDetailSection.Standings -> eventStandingItems(
        standings = event.standings,
        spoilersHidden = spoilersHidden,
        canShowEmptySection = canShowEmptySection,
      )

      EventDetailSection.Prizes -> eventPrizeItems(
        prizes = event.prizes,
        spoilersHidden = spoilersHidden,
        canShowEmptySection = canShowEmptySection,
        onTeamSelected = onTeamSelected,
      )
    }
    item {
      Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
    }
  }
}

@Composable
private fun EventDetailsHero(
  event: EventDetails,
  isSavingFavorite: Boolean,
  onToggleFavorite: () -> Unit,
) {
  EventDetailHeaderItem(
    event = event,
    modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
    favoriteAction = {
      PrismButton(
        onClick = onToggleFavorite,
        enabled = !isSavingFavorite,
        modifier = Modifier.fillMaxWidth(),
        style = PrismButtonStyle.Primary,
      ) {
        PrismFavoriteIcon(
          selected = event.isFavorite,
          size = PrismFavoriteIconSize.Medium,
          style = PrismFavoriteIconStyle.Inline,
          contentDescription = null,
        )
        Text(
          when {
            isSavingFavorite -> "Updating favorite"
            event.isFavorite -> "Remove from favorites"
            else -> "Favorite event"
          },
        )
      }
    },
  )
}

@Composable
private fun EventDetailsTabs(
  section: EventDetailSection,
  onSectionSelected: (EventDetailSection) -> Unit,
) {
  PrismTabs(
    modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
    tabs = EventDetailSection.entries.map { PrismTab(id = it.name, label = it.name) },
    selectedTabId = section.name,
    onTabSelected = { onSectionSelected(EventDetailSection.valueOf(it.id)) },
  )
}

private fun LazyListScope.eventMatchItems(
  matches: List<EventMatch>,
  visibleMatches: List<EventMatch>,
  favoriteReasons: List<MatchFavoriteReason>,
  grouping: EventMatchGrouping,
  groupNames: List<String>,
  selectedGroupName: String?,
  canShowEmptySection: Boolean,
  onGroupingSelected: (EventMatchGrouping) -> Unit,
  onGroupSelected: (String) -> Unit,
  onMenuExpandedChange: (Boolean) -> Unit,
  onMatchSelected: (String) -> Unit,
) {
  if (matches.isEmpty()) {
    item {
      EventEmptySection(
        visible = canShowEmptySection,
        title = "No matches yet",
        message = "Match fixtures have not been published for this event.",
      )
    }
  } else {
    item {
      EventMatchGroupSelector(
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
        grouping = grouping,
        groupNames = groupNames,
        selectedGroupName = selectedGroupName,
        onGroupingSelected = onGroupingSelected,
        onGroupSelected = onGroupSelected,
        onMenuExpandedChange = onMenuExpandedChange,
      )
    }
    items(visibleMatches, key = EventMatch::matchId) { match ->
      EventDetailMatchItem(
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM).cardMascotEligible(
          topClearance = Prism.dimens.spacingM + Prism.dimens.spacingS + Prism.dimens.spacingXs,
        ),
        match = match,
        favoriteReasons = favoriteReasons,
        onClick = { onMatchSelected(match.matchId) },
      )
    }
  }
}

private fun LazyListScope.eventStandingItems(
  standings: List<EventStanding>,
  spoilersHidden: Boolean,
  canShowEmptySection: Boolean,
) {
  if (standings.isEmpty()) {
    item {
      EventEmptySection(
        visible = canShowEmptySection,
        title = "No standings yet",
        message = "Team standings have not been published for this event.",
      )
    }
  } else if (spoilersHidden) {
    item {
      PrismSectionTitle(
        title = "Standings",
        preLabel = "table",
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }
    item { SpoilerHiddenNotice(modifier = Modifier.padding(horizontal = Prism.dimens.spacingM)) }
  } else {
    item {
      PrismSectionTitle(
        title = "Standings",
        preLabel = "table",
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }
    items(standings, key = EventStanding::teamName) { standing ->
      EventDetailStandingItem(
        standing = standing,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM).cardMascotEligible(topClearance = Prism.dimens.spacingM),
      )
    }
  }
}

private fun LazyListScope.eventPrizeItems(
  prizes: List<EventPrize>,
  spoilersHidden: Boolean,
  canShowEmptySection: Boolean,
  onTeamSelected: (String) -> Unit,
) {
  if (prizes.isEmpty()) {
    item {
      EventEmptySection(
        visible = canShowEmptySection,
        title = "No prize breakdown yet",
        message = "Prize placements have not been published for this event.",
      )
    }
  } else if (spoilersHidden) {
    item {
      PrismSectionTitle(
        title = "Prizes",
        preLabel = "placements",
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }
    item { SpoilerHiddenNotice(modifier = Modifier.padding(horizontal = Prism.dimens.spacingM)) }
  } else {
    item {
      PrismSectionTitle(
        title = "Prizes",
        preLabel = "placements",
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }
    itemsIndexed(
      items = prizes,
      key = { index, prize -> "${prize.position}-${prize.prize}-${prize.team?.id}-$index" },
    ) { _, prize ->
      val prizeTeamId = prize.team?.id
      EventDetailPrizeItem(
        prize = prize,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM).cardMascotEligible(topClearance = Prism.dimens.spacingM),
        onTeamClick = prizeTeamId?.let { teamId -> { onTeamSelected(teamId) } },
      )
    }
  }
}

@Composable
private fun EventEmptySection(
  visible: Boolean,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
) {
  if (visible) {
    SharedEmptyState(artwork = EmptyStateArtwork.NoLiveEvents, title = title, message = message, compact = true, modifier = modifier.padding(horizontal = Prism.dimens.spacingM))
  }
}

@Composable
private fun EventParticipantsRail(
  teams: List<EventTeam>,
  listState: LazyListState,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (teams.isNotEmpty()) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      PrismSectionTitle(title = "Participants", preLabel = "teams", modifier = Modifier.padding(horizontal = Prism.dimens.spacingM))
      LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Prism.dimens.spacingM),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        items(teams) { team ->
          val teamId = team.id
          EventDetailTeamItem(
            team = team,
            modifier = Modifier.width(148.dp),
            onClick = teamId?.let { id -> { onTeamSelected(id) } },
          )
        }
      }
    }
  }
}

@Composable
private fun rememberEventContentFade(ready: Boolean): State<Float> {
  val alpha = remember { Animatable(0f) }
  val animation = Prism.anim.standard
  LaunchedEffect(ready, alpha) {
    if (ready) {
      alpha.animateTo(1f, tween(durationMillis = animation.durationMillis, easing = animation.easing))
    }
  }
  return alpha.asState()
}

@Composable
private fun EventDetailsLoading(label: String, modifier: Modifier = Modifier) {
  if (LocalIsOnline.current) {
    PrismFullscreenLoader(modifier = modifier, size = PrismLoaderSize.Large, label = label)
  } else {
    SharedScreenLoading(label = label, modifier = modifier)
  }
}
