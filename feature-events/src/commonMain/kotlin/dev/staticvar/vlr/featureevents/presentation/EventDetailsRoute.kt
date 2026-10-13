/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconStyle
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.section.PrismSectionTitleAction
import dev.staticvar.designsystem.component.section.PrismSectionTitleStyle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.EventDetailsPreferences
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventTeam
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.featureevents.calendar.calendarFileName
import dev.staticvar.vlr.featureevents.calendar.calendarMatchUids
import dev.staticvar.vlr.featureevents.calendar.upcomingCalendarEntries
import dev.staticvar.vlr.featureevents.presentation.mascot.eventMascotCues
import dev.staticvar.vlr.sharedui.calendar.CalendarEntryStatus
import dev.staticvar.vlr.sharedui.calendar.rememberCalendarManager
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenTitleBar
import dev.staticvar.vlr.sharedui.component.common.SharedScrollingDetails
import dev.staticvar.vlr.sharedui.component.common.TransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.formatMatchPreviewTime
import dev.staticvar.vlr.sharedui.component.common.shortMonthName
import dev.staticvar.vlr.sharedui.component.common.transitionContentFade
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailMatchItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPreviewHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPrizeTile
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailTeamItem
import dev.staticvar.vlr.sharedui.component.event.rememberEventDetailContentFade
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.MascotCelebration
import dev.staticvar.vlr.sharedui.mascot.rememberMascot
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice
import dev.staticvar.vlr.sharedui.text.resolve
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_events.generated.resources.Res
import vlr.feature_events.generated.resources.add_upcoming_matches_to_calendar
import vlr.feature_events.generated.resources.calendar_versus
import vlr.feature_events.generated.resources.date_to_be_announced
import vlr.feature_events.generated.resources.details_will_appear_when_this_event_is_published
import vlr.feature_events.generated.resources.event
import vlr.feature_events.generated.resources.favorite_event
import vlr.feature_events.generated.resources.jump_to_todays_matches
import vlr.feature_events.generated.resources.loading_event
import vlr.feature_events.generated.resources.loading_event_details
import vlr.feature_events.generated.resources.match_day_date
import vlr.feature_events.generated.resources.match_day_date_with_year
import vlr.feature_events.generated.resources.match_fixtures_have_not_been_published_for_this_event
import vlr.feature_events.generated.resources.newest_first
import vlr.feature_events.generated.resources.no_event_details_yet
import vlr.feature_events.generated.resources.no_matches_yet
import vlr.feature_events.generated.resources.oldest_first
import vlr.feature_events.generated.resources.participants
import vlr.feature_events.generated.resources.prizes
import vlr.feature_events.generated.resources.remove_from_favorites
import vlr.feature_events.generated.resources.remove_matches_from_calendar
import vlr.feature_events.generated.resources.show_newest_first
import vlr.feature_events.generated.resources.show_oldest_first
import vlr.feature_events.generated.resources.standings
import vlr.feature_events.generated.resources.standings_legend
import vlr.feature_events.generated.resources.tab_matches
import vlr.feature_events.generated.resources.teams_matches_and_standings
import vlr.feature_events.generated.resources.today
import vlr.feature_events.generated.resources.updating_favorite
import kotlin.time.Clock

// Keeps the last row and the mascot clear of the event options button.
private val FabClearance = 88.dp

@Composable
public fun EventDetailsRoute(
  uiState: EventDetailsUiState,
  eventPreview: EventPreview? = null,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
  onPreferencesChange: (EventDetailsPreferences) -> Unit = {},
) {
  EventDetailsScreen(
    uiState = uiState,
    eventPreview = eventPreview,
    onBack = onBack,
    onMatchSelected = onMatchSelected,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onToggleFavorite = onToggleFavorite,
    onPreferencesChange = onPreferencesChange,
  )
}

@Composable
internal fun EventDetailsScreen(
  uiState: EventDetailsUiState,
  eventPreview: EventPreview? = null,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
  onPreferencesChange: (EventDetailsPreferences) -> Unit = {},
) {
  val event = uiState.event
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val transitionContentFade = rememberEventDetailContentFade()
  val bodyReady = event != null && (
      event.teams.isNotEmpty() || event.matches.isNotEmpty() ||
        event.standings.isNotEmpty() || event.prizes.isNotEmpty() ||
        (!uiState.isLoading && !uiState.isDetailLoadPending)
      )
  val bodyFade = rememberEventDetailContentFade(bodyReady)

  var optionsExpanded by remember(event?.id) { mutableStateOf(false) }
  var selectedMatchOrder by rememberSaveable(event?.id) { mutableStateOf(EventMatchOrder.OldestFirst) }
  var isNavigatingAway by remember(event?.id) { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val participantsState = rememberLazyListState()
  val prizesState = rememberLazyListState()
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
    isInteracting = uiState.isSavingFavorite || listState.isScrollInProgress || participantsState.isScrollInProgress ||
      prizesState.isScrollInProgress || optionsExpanded,
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
            modifier = Modifier.padding(horizontal = Prism.dimens.spacingM).padding(top = Prism.dimens.spacingXs),
          )
        }
      }

      when {
        (uiState.isLoading || uiState.isRefreshing) && event == null -> EventDetailsLoading(
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
          preferences = uiState.preferences,
          matchOrder = selectedMatchOrder,
          isScreenResumed = lifecycleState == Lifecycle.State.RESUMED,
          isSavingFavorite = uiState.isSavingFavorite,
          canShowEmptySection = !uiState.isLoading && !uiState.isDetailLoadPending &&
            !uiState.isRefreshing && uiState.errorMessage == null,
          spoilersHidden = spoilersHidden,
          listState = listState,
          participantsState = participantsState,
          prizesState = prizesState,
          contentAlpha = bodyFade.alpha,
          showContent = bodyReady,
          showLoading = !bodyReady,
          contentEnabled = bodyFade.acceptsInput,
          extraContentFade = transitionContentFade,
          onToggleFavorite = onToggleFavorite,
          onMatchOrderChange = { selectedMatchOrder = it },
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
          modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = FabClearance),
        )
      }
    }
    if (event != null) {
      EventDetailOptionsSheet(
        event = event,
        preferences = uiState.preferences,
        expanded = optionsExpanded,
        onExpandedChange = { optionsExpanded = it },
        onPreferencesChange = onPreferencesChange,
      )
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
  preferences: EventDetailsPreferences,
  matchOrder: EventMatchOrder,
  isScreenResumed: Boolean,
  isSavingFavorite: Boolean,
  canShowEmptySection: Boolean,
  spoilersHidden: Boolean,
  listState: LazyListState,
  participantsState: LazyListState,
  prizesState: LazyListState,
  contentAlpha: State<Float>,
  showContent: Boolean,
  showLoading: Boolean,
  contentEnabled: Boolean,
  extraContentFade: TransitionContentFade,
  onToggleFavorite: () -> Unit,
  onMatchOrderChange: (EventMatchOrder) -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val effectiveOrder = event.status.effectiveMatchOrder(matchOrder)
  val timeZone = remember(isScreenResumed) { TimeZone.currentSystemDefault() }
  val today by produceState(
    initialValue = Clock.System.now().toLocalDateTime(timeZone).date,
    key1 = isScreenResumed,
    key2 = timeZone,
  ) {
    if (isScreenResumed) eventLocalDateFlow(timeZone).collect { value = it }
  }
  val matchDays = remember(event.matches, timeZone, effectiveOrder) {
    event.matches.chronologicalMatchDays(timeZone, effectiveOrder)
  }
  val rows = remember(event, preferences, matchDays, spoilersHidden) {
    buildEventDetailsRows(event, preferences, matchDays, spoilersHidden)
  }
  val todayIndex = rows.todayHeadingIndex(today)
  val scope = rememberCoroutineScope()
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
    items(items = rows, key = EventDetailsRow::key, contentType = EventDetailsRow::contentType) { row ->
      when (row) {
        is EventDetailsRow.Participants -> EventParticipantsRail(
          teams = row.teams,
          listState = participantsState,
          onTeamSelected = onTeamSelected,
        )

        is EventDetailsRow.Prizes -> EventPrizesRail(
          prizes = row.prizes,
          spoilersHidden = spoilersHidden,
          listState = prizesState,
          onTeamSelected = onTeamSelected,
        )

        EventDetailsRow.StandingsTitle -> EventStandingsTitle(showLegend = !spoilersHidden)

        EventDetailsRow.StandingsHidden -> SpoilerHiddenNotice(
          modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
        )

        is EventDetailsRow.StandingsTable -> EventStandingsTable(
          group = row.group,
          modifier = Modifier.padding(horizontal = Prism.dimens.spacingM)
            .cardMascotEligible(topClearance = Prism.dimens.spacingM),
        )

        EventDetailsRow.MatchesTitle -> EventMatchesTitle(
          action = event.status.matchesHeaderAction(
            hasMatches = event.matches.isNotEmpty(),
            hasMatchesToday = todayIndex != null,
          ),
          order = effectiveOrder,
          onTodayClick = {
            // The hero is the first list item, so row indexes are offset by one.
            todayIndex?.let { index -> scope.launch { listState.animateScrollToItem(index + 1) } }
          },
          onOrderChange = onMatchOrderChange,
        )

        EventDetailsRow.MatchesEmpty -> EventEmptySection(
          visible = canShowEmptySection,
          title = stringResource(Res.string.no_matches_yet),
          message = stringResource(Res.string.match_fixtures_have_not_been_published_for_this_event),
        )

        is EventDetailsRow.MatchDay -> EventMatchDayHeading(date = row.date, today = today)

        is EventDetailsRow.Match -> EventDetailMatchItem(
          modifier = Modifier.padding(horizontal = Prism.dimens.spacingM).cardMascotEligible(
            topClearance = Prism.dimens.spacingM + Prism.dimens.spacingS + Prism.dimens.spacingXs,
          ),
          match = row.match.match,
          favoriteReasons = favoriteReasons,
          schedule = row.match.localSchedule(),
          onClick = { onMatchSelected(row.match.match.matchId) },
        )

        EventDetailsRow.BottomSpacer -> Spacer(
          modifier = Modifier.navigationBarsPadding().fillMaxWidth().height(FabClearance),
        )
      }
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
  val versus = stringResource(Res.string.calendar_versus)
  val hasUpcomingMatches = remember(event.matches, versus) { event.upcomingCalendarEntries(versus).isNotEmpty() }
  val calendar = rememberCalendarManager()
  val calendarUids = remember(event.matches) { event.calendarMatchUids }
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  var calendarStatus by remember(event.id) { mutableStateOf<CalendarEntryStatus?>(null) }
  val scope = rememberCoroutineScope()
  var isUpdatingCalendar by remember(event.id) { mutableStateOf(false) }
  var calendarFeedback by remember(event.id) { mutableStateOf<EventCalendarFeedback?>(null) }

  LaunchedEffect(event.id, calendarUids, lifecycleState, isUpdatingCalendar) {
    if (lifecycleState == Lifecycle.State.RESUMED && !isUpdatingCalendar) {
      val status = calendar.status(calendarUids)
      if (calendarStatus != status) calendarFeedback = null
      calendarStatus = status
    }
  }
  val canRemove = calendarStatus == CalendarEntryStatus.Added

  EventDetailHeaderItem(
    event = event,
    extraContentFade = extraContentFade,
    modifier = Modifier.padding(horizontal = Prism.dimens.spacingM)
      .padding(top = Prism.dimens.spacingXs)
      .cardMascotEligible(topClearance = Prism.dimens.spacingM),
    favoriteAction = {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          PrismButton(
            onClick = onToggleFavorite,
            enabled = !isSavingFavorite,
            modifier = Modifier.weight(1f),
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
          if (hasUpcomingMatches || canRemove) {
            PrismIconButton(
              icon = if (canRemove) Prism.icons.calendarRemove else Prism.icons.calendarAdd,
              contentDescription = stringResource(
                if (canRemove) Res.string.remove_matches_from_calendar else Res.string.add_upcoming_matches_to_calendar,
              ),
              onClick = {
                if (!isUpdatingCalendar) {
                  isUpdatingCalendar = true
                  scope.launch {
                    try {
                      val action = if (canRemove) EventCalendarAction.Remove else EventCalendarAction.Add
                      val result = when (action) {
                        EventCalendarAction.Remove -> calendar.remove(calendarUids)
                        EventCalendarAction.Add -> {
                          val upcoming = event.upcomingCalendarEntries(versus)
                          if (upcoming.isEmpty()) return@launch
                          calendar.add(event.calendarFileName, upcoming)
                        }
                      }
                      calendarFeedback = EventCalendarFeedback(action, result)
                      calendarStatus = calendar.status(calendarUids)
                    } finally {
                      isUpdatingCalendar = false
                    }
                  }
                }
              },
              size = PrismIconButtonSize.Toolbar,
              enabled = !isUpdatingCalendar && calendarStatus != null,
            )
          }
        }
        val calendarMessage = eventCalendarMessage(calendarStatus, calendarFeedback)
        calendarMessage?.let { message ->
          Text(
            text = stringResource(message),
            modifier = Modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingS),
            textAlign = TextAlign.Center,
            style = Prism.typography.bodySmall,
            color = Prism.color.bodyColor,
          )
        }
      }
    },
  )
}

@Composable
private fun EventStandingsTitle(showLegend: Boolean) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    PrismSectionTitle(title = stringResource(Res.string.standings))
    if (showLegend) {
      Text(
        text = stringResource(Res.string.standings_legend),
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
      )
    }
  }
}

@Composable
private fun EventMatchesTitle(
  action: EventMatchesAction,
  order: EventMatchOrder,
  onTodayClick: () -> Unit,
  onOrderChange: (EventMatchOrder) -> Unit,
) {
  PrismSectionTitle(
    title = stringResource(Res.string.tab_matches),
    modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
    trailing = if (action == EventMatchesAction.None) {
      null
    } else {
      {
        when (action) {
          EventMatchesAction.Today -> EventTodayButton(onClick = onTodayClick)
          EventMatchesAction.Order -> EventMatchOrderButton(order = order, onOrderChange = onOrderChange)
          EventMatchesAction.None -> Unit
        }
      }
    },
  )
}

/** Shown only when today has matches; see [matchesHeaderAction]. */
@Composable
private fun EventTodayButton(onClick: () -> Unit) {
  val jumpLabel = stringResource(Res.string.jump_to_todays_matches)
  PrismSectionTitleAction(
    text = stringResource(Res.string.today),
    onClick = onClick,
    modifier = Modifier.semantics { onClick(label = jumpLabel, action = null) },
  )
}

@Composable
private fun EventMatchOrderButton(order: EventMatchOrder, onOrderChange: (EventMatchOrder) -> Unit) {
  val switchLabel = stringResource(
    if (order == EventMatchOrder.OldestFirst) Res.string.show_newest_first else Res.string.show_oldest_first,
  )
  PrismSectionTitleAction(
    text = stringResource(if (order == EventMatchOrder.OldestFirst) Res.string.oldest_first else Res.string.newest_first),
    onClick = { onOrderChange(order.toggled) },
    modifier = Modifier.semantics { onClick(label = switchLabel, action = null) },
  )
}

@Composable
private fun EventMatchDayHeading(date: LocalDate?, today: LocalDate) {
  val label = when {
    date == null -> stringResource(Res.string.date_to_be_announced)
    date.year == today.year -> stringResource(Res.string.match_day_date, shortMonthName(date.month), date.day)
    else -> stringResource(Res.string.match_day_date_with_year, shortMonthName(date.month), date.day, date.year)
  }
  PrismSectionTitle(
    title = label,
    prefix = if (date == today) stringResource(Res.string.today) else null,
    style = PrismSectionTitleStyle.Subtle,
    modifier = Modifier.padding(horizontal = Prism.dimens.spacingM).padding(top = Prism.dimens.spacingXs),
  )
}

/** Start time in the viewer's time zone, so cards agree with the local day headings. */
@Composable
private fun ScheduledEventMatch.localSchedule(): String? {
  val localStart = start?.let { formatMatchPreviewTime(it.toString()) } ?: return null
  return listOfNotNull(match.eta?.takeIf(String::isNotBlank), localStart).joinToString(separator = " • ")
}

@Composable
private fun EventPrizesRail(
  prizes: List<EventPrize>,
  spoilersHidden: Boolean,
  listState: LazyListState,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismSectionTitle(title = stringResource(Res.string.prizes), modifier = Modifier.padding(horizontal = Prism.dimens.spacingM))
    if (spoilersHidden) {
      SpoilerHiddenNotice(modifier = Modifier.padding(horizontal = Prism.dimens.spacingM))
    } else {
      LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Prism.dimens.spacingM),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        itemsIndexed(
          items = prizes,
          key = { index, prize -> "${prize.position}-${prize.prize}-${prize.team?.id}-$index" },
        ) { _, prize ->
          val prizeTeamId = prize.team?.id
          EventDetailPrizeTile(
            prize = prize,
            modifier = Modifier.width(148.dp),
            onTeamClick = prizeTeamId?.let { teamId -> { onTeamSelected(teamId) } },
          )
        }
      }
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
      PrismSectionTitle(title = stringResource(Res.string.participants), modifier = Modifier.padding(horizontal = Prism.dimens.spacingM))
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
  PrismFullscreenLoader(modifier = modifier, size = PrismLoaderSize.Large, label = label)
}
