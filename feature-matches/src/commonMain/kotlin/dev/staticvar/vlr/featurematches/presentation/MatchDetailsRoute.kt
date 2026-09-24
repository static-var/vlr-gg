/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.featurematches.presentation.mascot.matchMascotCues
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.component.common.SharedScreenTitleBar
import dev.staticvar.vlr.sharedui.component.common.SharedScrollingDetails
import dev.staticvar.vlr.sharedui.component.common.TransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.currentTransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.rememberTransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.transitionContentFade
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailHeadToHeadItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailMapsItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailPreviewHeaderItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailVideoItem
import dev.staticvar.vlr.sharedui.component.match.detail.resolveSelectedMapIndex
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.MascotCelebration
import dev.staticvar.vlr.sharedui.mascot.rememberMascot
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.text.resolve
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_matches.generated.resources.Res
import vlr.feature_matches.generated.resources.add_match_to_favorites
import vlr.feature_matches.generated.resources.details_will_appear_when_this_match_is_published
import vlr.feature_matches.generated.resources.favorite_match
import vlr.feature_matches.generated.resources.loading_match
import vlr.feature_matches.generated.resources.loading_match_details
import vlr.feature_matches.generated.resources.map_statistics_have_not_been_published_for_this_match
import vlr.feature_matches.generated.resources.maps_scores_and_player_stats
import vlr.feature_matches.generated.resources.match
import vlr.feature_matches.generated.resources.media
import vlr.feature_matches.generated.resources.no_match_breakdown_yet
import vlr.feature_matches.generated.resources.no_match_details_yet
import vlr.feature_matches.generated.resources.remove_match_from_favorites
import vlr.feature_matches.generated.resources.stream
import vlr.feature_matches.generated.resources.streams_vods
import vlr.feature_matches.generated.resources.updating_favorite
import vlr.feature_matches.generated.resources.vod

@Composable
public fun MatchDetailsRoute(
  uiState: MatchDetailsUiState,
  matchPreview: MatchPreview? = null,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onMatchSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onPreferencesChange: (MatchDetailsPreferences) -> Unit = {},
  onFavoriteClick: () -> Unit = {},
) {
  MatchDetailsScreen(
    uiState = uiState,
    matchPreview = matchPreview,
    onBack = onBack,
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    onPlayerSelected = onPlayerSelected,
    onMatchSelected = onMatchSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onPreferencesChange = onPreferencesChange,
    onFavoriteClick = onFavoriteClick,
  )
}

@Composable
internal fun MatchDetailsScreen(
  uiState: MatchDetailsUiState,
  matchPreview: MatchPreview? = null,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onMatchSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onPreferencesChange: (MatchDetailsPreferences) -> Unit = {},
  onFavoriteClick: () -> Unit = {},
) {
  val isOnline = LocalIsOnline.current
  val match = uiState.match
  val hasDetailedContent = match != null && (
      match.matchData.isNotEmpty() || match.head2head.isNotEmpty() ||
        match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty()
      )
  val bodyReady = match != null && (hasDetailedContent || (!uiState.isLoading && !uiState.isDetailLoadPending))
  val extraContentFade = currentTransitionContentFade()
  val bodyFade = rememberTransitionContentFade(bodyReady)
  val uriHandler = LocalUriHandler.current
  var selectedMapIndex: Int? by remember(match?.id) { mutableStateOf<Int?>(null) }
  val maps = match?.matchData.orEmpty()
  val resolvedMapIndex = maps.resolveSelectedMapIndex(selectedMapIndex)
  LaunchedEffect(match?.id, maps.size) {
    if (selectedMapIndex != null && selectedMapIndex !in maps.indices) {
      selectedMapIndex = null
    }
  }
  val listState = rememberLazyListState()
  var optionsExpanded by remember(match?.id) { mutableStateOf(false) }
  var mapMenuExpanded by remember(match?.id) { mutableStateOf(false) }
  var isLeaving by remember(match?.id) { mutableStateOf(false) }
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val mascotCharacter = LocalMascotCharacter.current
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val candidates = remember(match, uiState.favoriteTeamIds, uiState.favoritePlayerIds, resolvedMapIndex, spoilersHidden, mascotCharacter) {
    match?.takeUnless { spoilersHidden || mascotCharacter == null }?.let {
      matchMascotCues(
        it,
        uiState.favoriteTeamIds,
        uiState.favoritePlayerIds,
        selectedMapIndex = resolvedMapIndex,
      )
    }.orEmpty()
  }
  val mascot = rememberMascot(
    screenKey = match?.id.orEmpty(),
    candidates = candidates,
    isScreenActive = !spoilersHidden && mascotCharacter != null && lifecycleState == Lifecycle.State.RESUMED && !isLeaving,
    isContentReady = match != null && !uiState.isLoading && !uiState.isRefreshing &&
      !uiState.isDetailLoadPending && uiState.errorMessage == null,
    isInteracting = listState.isScrollInProgress || optionsExpanded || mapMenuExpanded,
  )
  fun leaveScreen(action: () -> Unit) {
    isLeaving = true
    mascot.onFinished()
    action()
  }

  Box(modifier = modifier.fillMaxSize().clipToBounds()) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      MatchDetailsChrome(
        title = stringResource(Res.string.match),
        isLoading = uiState.isLoading || uiState.isDetailLoadPending,
        isRefreshing = uiState.isRefreshing,
        hasContent = match != null,
        favoriteErrorMessage = uiState.favoriteErrorMessage,
        errorMessage = uiState.errorMessage.takeIf { match != null },
        errorDetails = uiState.errorDetails,
        onBack = { leaveScreen(onBack) },
        onRefresh = onRefresh,
      )
      if (match == null && matchPreview != null) {
        MatchDetailPreviewHeaderItem(match = matchPreview)
      }
      when {
        (!isOnline || uiState.isLoading || uiState.isDetailLoadPending || uiState.isRefreshing) && match == null -> MatchDetailsLoading(
          modifier = Modifier.fillMaxSize().transitionContentFade(extraContentFade),
          label = stringResource(Res.string.loading_match),
        )

        uiState.errorMessage != null && match == null ->
          SharedLoadError(
            errorMessage = uiState.errorMessage,
            errorDetails = uiState.errorDetails,
            onRefresh = onRefresh,
            centered = true,
            modifier = Modifier.fillMaxWidth().weight(1f).transitionContentFade(extraContentFade),
          )

        match == null -> SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveMatches,
          title = stringResource(Res.string.no_match_details_yet),
          message = stringResource(Res.string.details_will_appear_when_this_match_is_published),
          modifier = Modifier.fillMaxWidth().weight(1f).transitionContentFade(extraContentFade),
        )

        else -> MatchDetailsContent(
          match = match,
          preferences = uiState.preferences,
          showBreakdownEmpty = !hasDetailedContent && uiState.preferences.showBreakdown &&
            isOnline && !uiState.isLoading && !uiState.isDetailLoadPending && !uiState.isRefreshing && uiState.errorMessage == null,
          selectedMapIndex = resolvedMapIndex,
          isFavoritePending = uiState.isFavoritePending,
          isFavoriteInherited = uiState.isFavoriteInherited,
          canToggleFavorite = uiState.canToggleFavorite,
          listState = listState,
          contentFade = bodyFade,
          bodyReady = bodyReady,
          extraContentFade = extraContentFade,
          modifier = Modifier.fillMaxWidth().weight(1f),
          onFavoriteClick = onFavoriteClick,
          onEventSelected = { id -> leaveScreen { onEventSelected(id) } },
          onTeamSelected = { id -> leaveScreen { onTeamSelected(id) } },
          onMapSelected = { selectedMapIndex = it },
          onMapMenuExpandedChange = { mapMenuExpanded = it },
          onPlayerSelected = { id -> leaveScreen { onPlayerSelected(id) } },
          onMatchSelected = { id -> leaveScreen { onMatchSelected(id) } },
          onVideoSelected = { url -> uriHandler.openUri(url.asExternalUrl()) },
        )
      }
    }
    if (!spoilersHidden && mascotCharacter != null) {
      mascot.cue?.let { cue ->
        MascotCelebration(
          visible = true,
          character = mascotCharacter,
          message = cue.message.resolve(),
          secondaryMessage = cue.secondaryMessage?.resolve(),
          onFinished = mascot::onFinished,
          modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 88.dp),
        )
      }
    }
    if (match != null && (
        match.matchData.isNotEmpty() || match.head2head.isNotEmpty() ||
          match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty()
        )
    ) {
      MatchDetailOptionsSheet(
        match = match,
        preferences = uiState.preferences,
        expanded = optionsExpanded,
        onExpandedChange = { optionsExpanded = it },
        onPreferencesChange = onPreferencesChange,
      )
    }
  }
}

@Composable
private fun MatchDetailsChrome(
  title: String,
  isLoading: Boolean,
  isRefreshing: Boolean,
  hasContent: Boolean,
  favoriteErrorMessage: StringResource?,
  errorMessage: String?,
  errorDetails: String?,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
) {
  Column {
    SharedScreenTitleBar(
      title = title,
      subtitle = stringResource(Res.string.maps_scores_and_player_stats),
      onBackPress = onBack,
      actions = {
        SharedRefreshButton(
          isLoading = isLoading,
          animateWhileLoading = true,
          isRefreshing = isRefreshing,
          hasContent = hasContent,
          onRefresh = onRefresh,
        )
      },
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
private fun MatchDetailsContent(
  match: MatchDetails,
  preferences: MatchDetailsPreferences,
  showBreakdownEmpty: Boolean,
  selectedMapIndex: Int?,
  isFavoritePending: Boolean,
  isFavoriteInherited: Boolean,
  canToggleFavorite: Boolean,
  listState: LazyListState,
  contentFade: TransitionContentFade,
  bodyReady: Boolean,
  extraContentFade: TransitionContentFade,
  modifier: Modifier = Modifier,
  onFavoriteClick: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onMapSelected: (Int?) -> Unit,
  onMapMenuExpandedChange: (Boolean) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onMatchSelected: (String) -> Unit,
  onVideoSelected: (String) -> Unit,
) {
  SharedScrollingDetails(
    state = listState,
    contentAlpha = contentFade.alpha,
    showContent = bodyReady,
    showLoading = !bodyReady,
    contentEnabled = contentFade.acceptsInput,
    modifier = modifier,
    hero = {
      MatchDetailsHero(
        match = match,
        isFavoritePending = isFavoritePending,
        isFavoriteInherited = isFavoriteInherited,
        canToggleFavorite = canToggleFavorite,
        onFavoriteClick = onFavoriteClick,
        onEventSelected = onEventSelected,
        onTeamSelected = onTeamSelected,
      )
    },
    loading = { loadingModifier ->
      MatchDetailsLoading(
        label = stringResource(Res.string.loading_match_details),
        modifier = loadingModifier.transitionContentFade(extraContentFade),
      )
    },
  ) {
    matchDetailItems(
      match = match,
      preferences = preferences,
      showBreakdownEmpty = showBreakdownEmpty,
      selectedMapIndex = selectedMapIndex,
      onMapSelected = onMapSelected,
      onMapMenuExpandedChange = onMapMenuExpandedChange,
      onPlayerSelected = onPlayerSelected,
      onMatchSelected = onMatchSelected,
      onVideoSelected = onVideoSelected,
    )
  }
}

@Composable
private fun MatchDetailsHero(
  match: MatchDetails,
  isFavoritePending: Boolean,
  isFavoriteInherited: Boolean,
  canToggleFavorite: Boolean,
  onFavoriteClick: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
) {
  MatchDetailHeaderItem(
    match = match,
    favoriteAction = {
      PrismFavoriteIcon(
        selected = match.isFavorite,
        size = PrismFavoriteIconSize.Large,
        enabled = canToggleFavorite,
        contentDescription = when {
          isFavoritePending -> stringResource(Res.string.updating_favorite)
          isFavoriteInherited -> stringResource(Res.string.favorite_match)
          match.isFavorite -> stringResource(Res.string.remove_match_from_favorites)
          else -> stringResource(Res.string.add_match_to_favorites)
        },
        modifier = Modifier.clickable(
          enabled = canToggleFavorite,
          role = Role.Button,
          onClick = onFavoriteClick,
        ),
      )
    },
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    actions = if (match.shouldShowCalendarAction()) {
      { MatchCalendarAction(match) }
    } else {
      null
    },
  )
}

private fun LazyListScope.matchDetailItems(
  match: MatchDetails,
  preferences: MatchDetailsPreferences,
  showBreakdownEmpty: Boolean,
  selectedMapIndex: Int?,
  onMapSelected: (Int?) -> Unit,
  onMapMenuExpandedChange: (Boolean) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onMatchSelected: (String) -> Unit,
  onVideoSelected: (String) -> Unit,
) {
  if (showBreakdownEmpty) {
    item {
      SharedEmptyState(
        artwork = EmptyStateArtwork.NoLiveMatches,
        title = stringResource(Res.string.no_match_breakdown_yet),
        message = stringResource(Res.string.map_statistics_have_not_been_published_for_this_match),
        compact = true,
      )
    }
  }
  if (preferences.showBreakdown && match.matchData.isNotEmpty()) {
    item {
      MatchDetailMapsItem(
        maps = match.matchData,
        selectedMapIndex = selectedMapIndex,
        onMapSelected = onMapSelected,
        onPlayerSelected = onPlayerSelected,
        onMenuExpandedChange = onMapMenuExpandedChange,
      )
    }
  }
  if (preferences.showHeadToHead && match.head2head.isNotEmpty()) {
    item {
      MatchDetailHeadToHeadItem(
        encounters = match.head2head,
        onEncounterSelected = onMatchSelected,
      )
    }
  }
  if (preferences.showMedia && (match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty())) {
    item { PrismSectionTitle(title = stringResource(Res.string.streams_vods), preLabel = stringResource(Res.string.media)) }
    item { MatchDetailMediaRow(videos = match.videos, onVideoSelected = onVideoSelected) }
  }
  item {
    Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth().height(88.dp))
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MatchDetailMediaRow(videos: MatchVideos, onVideoSelected: (String) -> Unit) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    videos.streams.forEach { video ->
      MatchDetailVideoItem(
        video = video,
        typeLabel = stringResource(Res.string.stream),
        onClick = video.url.takeIf(String::isNotBlank)?.let { url ->
          { onVideoSelected(url) }
        },
      )
    }
    videos.vods.forEach { video ->
      MatchDetailVideoItem(
        video = video,
        typeLabel = stringResource(Res.string.vod),
        onClick = video.url.takeIf(String::isNotBlank)?.let { url ->
          { onVideoSelected(url) }
        },
      )
    }
  }
}

private fun String.asExternalUrl(): String =
  if (startsWith("http://") || startsWith("https://")) this else "https://$this"

@Composable
private fun MatchDetailsLoading(label: String, modifier: Modifier = Modifier) {
  if (LocalIsOnline.current) {
    PrismFullscreenLoader(modifier = modifier, size = PrismLoaderSize.Large, label = label)
  } else {
    SharedScreenLoading(label = label, modifier = modifier)
  }
}
