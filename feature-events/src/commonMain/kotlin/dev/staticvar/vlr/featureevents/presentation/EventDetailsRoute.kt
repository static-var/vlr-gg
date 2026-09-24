/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconStyle
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventTeam
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.featureevents.presentation.mascot.eventMascotCues
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.component.common.SharedScreenTitleBar
import dev.staticvar.vlr.sharedui.component.common.SharedScrollingDetails
import dev.staticvar.vlr.sharedui.component.common.TransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.transitionContentFade
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailMatchItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPreviewHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPrizeItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailStandingItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailTeamItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGroupSelector
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGrouping
import dev.staticvar.vlr.sharedui.component.event.detail.eventFormattingLabels
import dev.staticvar.vlr.sharedui.component.event.detail.groupEventMatches
import dev.staticvar.vlr.sharedui.component.event.rememberEventDetailContentFade
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.MascotCelebration
import dev.staticvar.vlr.sharedui.mascot.rememberMascot
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice
import dev.staticvar.vlr.sharedui.text.resolve
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_events.generated.resources.Res
import vlr.feature_events.generated.resources.details_will_appear_when_this_event_is_published
import vlr.feature_events.generated.resources.event
import vlr.feature_events.generated.resources.favorite_event
import vlr.feature_events.generated.resources.loading_event
import vlr.feature_events.generated.resources.loading_event_details
import vlr.feature_events.generated.resources.match_fixtures_have_not_been_published_for_this_event
import vlr.feature_events.generated.resources.no_event_details_yet
import vlr.feature_events.generated.resources.no_matches_yet
import vlr.feature_events.generated.resources.no_prize_breakdown_yet
import vlr.feature_events.generated.resources.no_standings_yet
import vlr.feature_events.generated.resources.participants
import vlr.feature_events.generated.resources.placements
import vlr.feature_events.generated.resources.prize_placements_have_not_been_published_for_this_event
import vlr.feature_events.generated.resources.prizes
import vlr.feature_events.generated.resources.remove_from_favorites
import vlr.feature_events.generated.resources.standings
import vlr.feature_events.generated.resources.tab_matches
import vlr.feature_events.generated.resources.tab_prizes
import vlr.feature_events.generated.resources.tab_standings
import vlr.feature_events.generated.resources.table
import vlr.feature_events.generated.resources.team_standings_have_not_been_published_for_this_event
import vlr.feature_events.generated.resources.teams
import vlr.feature_events.generated.resources.teams_matches_and_standings
import vlr.feature_events.generated.resources.updating_favorite

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
  val transitionContentFade = rememberEventDetailContentFade()
  val bodyReady = event != null && (
      event.teams.isNotEmpty() || event.matches.isNotEmpty() ||
        event.standings.isNotEmpty() || event.prizes.isNotEmpty() ||
        (!uiState.isLoading && !uiState.isDetailLoadPending)
      )
  val bodyFade = rememberEventDetailContentFade(bodyReady)

  var isGroupingMenuExpanded by remember(event?.id) { mutableStateOf(false) }
  var isNavigatingAway by remember(event?.id) { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val participantsState = rememberLazyListState()
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val mascotCharacter = LocalMascotCharacter.current
  val candidates = remember(event, uiState.favoriteTeamIds, spoilersHidden, mascotCharacter) {
    if (spoilersHidden || mascotCharacter == null) emptyList() else event?.let { eventMascotCues(it, uiState.favoriteTeamIds) }.orEmpty()
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
        title = stringResource(Res.string.event),
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

      if (event == null) {
        eventPreview?.let { preview ->
          EventDetailPreviewHeaderItem(
            event = preview,
            extraContentFade = transitionContentFade,
            modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
          )
        }
      }

      when {
        (!isOnline || uiState.isLoading || uiState.isRefreshing) && event == null -> EventDetailsLoading(
          modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM)
            .transitionContentFade(transitionContentFade),
          label = stringResource(Res.string.loading_event),
        )

        uiState.errorMessage != null && event == null ->
          SharedLoadError(
            errorMessage = uiState.errorMessage,
            errorDetails = uiState.errorDetails,
            onRefresh = onRefresh,
            centered = true,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM)
              .transitionContentFade(transitionContentFade),
          )

        event == null -> SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveEvents,
          title = stringResource(Res.string.no_event_details_yet),
          message = stringResource(Res.string.details_will_appear_when_this_event_is_published),
          modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Prism.dimens.spacingM)
            .transitionContentFade(transitionContentFade),
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
          contentAlpha = bodyFade.alpha,
          showContent = bodyReady,
          showLoading = !bodyReady,
          contentEnabled = bodyFade.acceptsInput,
          extraContentFade = transitionContentFade,
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
          message = cue.message.resolve(),
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
  favoriteErrorMessage: StringResource?,
  errorMessage: String?,
  errorDetails: String?,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    SharedScreenTitleBar(
      title = title,
      subtitle = stringResource(Res.string.teams_matches_and_standings),
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
    favoriteErrorMessage?.let { PrismStateMessage(text = stringResource(it)) }
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
  showLoading: Boolean,
  contentEnabled: Boolean,
  extraContentFade: TransitionContentFade,
  onToggleFavorite: () -> Unit,
  onSectionSelected: (EventDetailSection) -> Unit,
  onMatchGroupingSelected: (EventMatchGrouping) -> Unit,
  onMatchGroupSelected: (String) -> Unit,
  onGroupingMenuExpandedChange: (Boolean) -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val labels = eventFormattingLabels()
  val groupedMatches = remember(event.matches, matchGrouping, labels) { event.matches.groupEventMatches(matchGrouping, labels) }
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
    showLoading = showLoading,
    contentEnabled = contentEnabled,
    modifier = modifier,
    hero = {
      EventDetailsHero(
        event = event,
        extraContentFade = extraContentFade,
        isSavingFavorite = isSavingFavorite,
        onToggleFavorite = onToggleFavorite,
      )
    },
    loading = { loadingModifier ->
      EventDetailsLoading(
        label = stringResource(Res.string.loading_event_details),
        modifier = loadingModifier.transitionContentFade(extraContentFade),
      )
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
  extraContentFade: TransitionContentFade,
  isSavingFavorite: Boolean,
  onToggleFavorite: () -> Unit,
) {
  EventDetailHeaderItem(
    event = event,
    extraContentFade = extraContentFade,
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
            isSavingFavorite -> stringResource(Res.string.updating_favorite)
            event.isFavorite -> stringResource(Res.string.remove_from_favorites)
            else -> stringResource(Res.string.favorite_event)
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
    tabs = EventDetailSection.entries.map { section ->
      PrismTab(
        id = section.name,
        label = stringResource(
          when (section) {
            EventDetailSection.Matches -> Res.string.tab_matches
            EventDetailSection.Standings -> Res.string.tab_standings
            EventDetailSection.Prizes -> Res.string.tab_prizes
          },
        ),
      )
    },
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
        title = stringResource(Res.string.no_matches_yet),
        message = stringResource(Res.string.match_fixtures_have_not_been_published_for_this_event),
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
        title = stringResource(Res.string.no_standings_yet),
        message = stringResource(Res.string.team_standings_have_not_been_published_for_this_event),
      )
    }
  } else if (spoilersHidden) {
    item {
      PrismSectionTitle(
        title = stringResource(Res.string.standings),
        preLabel = stringResource(Res.string.table),
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }
    item { SpoilerHiddenNotice(modifier = Modifier.padding(horizontal = Prism.dimens.spacingM)) }
  } else {
    item {
      PrismSectionTitle(
        title = stringResource(Res.string.standings),
        preLabel = stringResource(Res.string.table),
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
        title = stringResource(Res.string.no_prize_breakdown_yet),
        message = stringResource(Res.string.prize_placements_have_not_been_published_for_this_event),
      )
    }
  } else if (spoilersHidden) {
    item {
      PrismSectionTitle(
        title = stringResource(Res.string.prizes),
        preLabel = stringResource(Res.string.placements),
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
    }
    item { SpoilerHiddenNotice(modifier = Modifier.padding(horizontal = Prism.dimens.spacingM)) }
  } else {
    item {
      PrismSectionTitle(
        title = stringResource(Res.string.prizes),
        preLabel = stringResource(Res.string.placements),
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
      PrismSectionTitle(title = stringResource(Res.string.participants), preLabel = stringResource(Res.string.teams), modifier = Modifier.padding(horizontal = Prism.dimens.spacingM))
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
private fun EventDetailsLoading(label: String, modifier: Modifier = Modifier) {
  if (LocalIsOnline.current) {
    PrismFullscreenLoader(modifier = modifier, size = PrismLoaderSize.Large, label = label)
  } else {
    SharedScreenLoading(label = label, modifier = modifier)
  }
}
