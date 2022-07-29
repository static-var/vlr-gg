package dev.staticvar.vlr.ui.match

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.*
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.ShareDialog
import dev.staticvar.vlr.ui.helper.SharingAppBar
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*

@Composable
fun MatchOverview(viewModel: VlrViewModel) {
  StatusBarColorForHomeWithTabs()

  val allMatches by
    remember(viewModel) { viewModel.getMatches() }
      .collectAsStateWithLifecycle(initialValue = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshMatches() }
      .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh = rememberSwipeRefreshState(isRefreshing = updateState.get() ?: false)

  val resetScroll by
    remember { viewModel.resetScroll }.collectAsStateWithLifecycle(initialValue = false)

  val modifier: Modifier = Modifier
  Column(
    modifier = modifier.fillMaxSize().statusBarsPadding(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(modifier) {
      allMatches
        .onPass {
          data?.let { list ->
            MatchOverviewContainer(
              modifier,
              list = StableHolder(list),
              swipeRefresh,
              updateState,
              resetScroll,
              onClick = { id -> viewModel.action.match(id) },
              triggerRefresh = { triggerRefresh = triggerRefresh.not() },
              postResetScroll = { viewModel.postResetScroll() }
            )
          }
        }
        .onWaiting { LinearProgressIndicator(modifier.animateContentSize()) }
        .onFail { Text(text = message()) }
    }
  }
}

const val MAX_SHARABLE_ITEMS = 6

@Composable
fun MatchOverviewContainer(
  modifier: Modifier = Modifier,
  list: StableHolder<List<MatchPreviewInfo>>,
  swipeRefresh: SwipeRefreshState,
  updateState: Result<Boolean, Throwable?>,
  resetScroll: Boolean,
  onClick: (String) -> Unit,
  triggerRefresh: () -> Unit,
  postResetScroll: () -> Unit,
) {
  val pagerState = rememberPagerState()
  val shareMatchList = remember { mutableStateListOf<MatchPreviewInfo>() }
  var shareState by remember { mutableStateOf(false) }
  var shareDialog by remember { mutableStateOf(false) }
  val haptic = LocalHapticFeedback.current

  val tabs =
    listOf(
      stringResource(id = R.string.live),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed)
    )

  if (shareDialog) {
    ShareDialog(matches = StableHolder(shareMatchList)) { shareDialog = false }
  }
  val mapByStatus by remember(list) { mutableStateOf(list.item.groupBy { it.status }) }

  val (ongoing, upcoming, completed) =
    remember(list) {
      mapByStatus.let {
        Triple(
          it[tabs[0].lowercase()].orEmpty(),
          it[tabs[1].lowercase()].orEmpty().sortedBy { match -> match.time?.timeToEpoch },
          it[tabs[2].lowercase()].orEmpty().sortedByDescending { match -> match.time?.timeToEpoch }
        )
      }
    }

  SwipeRefresh(state = swipeRefresh, onRefresh = triggerRefresh, indicator = { _, _ -> }) {
    Column(
      modifier = modifier.fillMaxSize().animateContentSize(),
      verticalArrangement = Arrangement.Top
    ) {
      AnimatedVisibility(visible = updateState.get() == true || swipeRefresh.isSwipeInProgress) {
        LinearProgressIndicator(
          modifier
            .fillMaxWidth()
            .padding(Local16DPPadding.current)
            .animateContentSize()
            .testTag("common:loader")
        )
      }
      updateState.getError()?.let {
        ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
      }
      AnimatedVisibility(visible = shareState) {
        SharingAppBar(
          modifier = modifier,
          items = shareMatchList,
          shareMode = {
            shareState = it
            shareMatchList.clear()
          },
          shareConfirm = { shareDialog = true }
        )
      }

      VlrTabRowForViewPager(modifier = modifier, pagerState = pagerState, tabs = tabs)

      VlrHorizontalViewPager(
        modifier = modifier,
        pagerState = pagerState,
        {
          if (ongoing.isEmpty()) {
            NoMatchUI()
          } else {
            val lazyListState = rememberLazyListState()
            lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)

            LazyColumn(
              modifier.fillMaxSize().testTag("matchOverview:live"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState
            ) {
              items(ongoing, key = { item -> item.id }) {
                MatchOverviewPreview(
                  matchPreviewInfo = it,
                  shareMode = shareState,
                  isSelected = it in shareMatchList,
                  onAction = { longPress, match ->
                    if (longPress) shareState = true // If long press enable share bar
                    when {
                      shareState && shareMatchList.contains(match) -> {
                        // If in share mode &
                        // If match is already in the list and is being clicked again, remove the
                        // item
                        shareMatchList.remove(match)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      }
                      shareState &&
                        !shareMatchList.contains(match) &&
                        shareMatchList.size < MAX_SHARABLE_ITEMS -> {
                        // If in share mode &
                        // If list does not have 6 items and if the clicked icon is not already in
                        // the list
                        shareMatchList.add(match)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      }
                      !shareState -> onClick(match.id) // Its a normal click, navigate to the action
                    }
                  }
                )
              }
            }
          }
        },
        {
          if (upcoming.isEmpty()) {
            NoMatchUI()
          } else {
            val lazyListState = rememberLazyListState()
            lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)

            val groupedUpcomingMatches =
              remember(upcoming) { upcoming.groupBy { it.time?.readableDate } }
            LazyColumn(
              modifier.fillMaxSize().testTag("matchOverview:upcoming"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState
            ) {
              groupedUpcomingMatches.forEach { (date, match)
                -> // Group heading based on date for sticky header
                stickyHeader {
                  date?.let {
                    Column(
                      modifier.fillMaxWidth().background(VLRTheme.colorScheme.primaryContainer)
                    ) {
                      Text(
                        it,
                        modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current),
                        textAlign = TextAlign.Center,
                        color = VLRTheme.colorScheme.primary
                      )
                    }
                  }
                }
                items(match, key = { item -> item.id }) {
                  MatchOverviewPreview(
                    matchPreviewInfo = it,
                    shareMode = shareState,
                    isSelected = it in shareMatchList,
                    onAction = { longPress, match ->
                      if (longPress) shareState = true // If long press enable share bar
                      when {
                        shareState && shareMatchList.contains(match) -> {
                          // If in share mode &
                          // If match is already in the list and is being clicked again, remove
                          // the
                          // item
                          shareMatchList.remove(match)
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        shareState &&
                          !shareMatchList.contains(match) &&
                          shareMatchList.size < MAX_SHARABLE_ITEMS -> {
                          // If in share mode &
                          // If list does not have 6 items and if the clicked icon is not already
                          // in
                          // the list
                          shareMatchList.add(match)
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        !shareState ->
                          onClick(match.id) // Its a normal click, navigate to the action
                      }
                    }
                  )
                }
              }
            }
          }
        },
        {
          if (completed.isEmpty()) {
            NoMatchUI(modifier = modifier)
          } else {
            val lazyListState = rememberLazyListState()
            lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)

            val groupedCompletedMatches =
              remember(completed) { completed.groupBy { it.time?.readableDate } }
            LazyColumn(
              modifier.fillMaxSize().testTag("matchOverview:result"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState
            ) {
              groupedCompletedMatches.forEach { (date, match)
                -> // Group heading based on date for sticky header
                stickyHeader {
                  date?.let {
                    Column(
                      modifier.fillMaxWidth().background(VLRTheme.colorScheme.primaryContainer),
                    ) {
                      Text(
                        it,
                        modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current),
                        textAlign = TextAlign.Center,
                        color = VLRTheme.colorScheme.primary
                      )
                    }
                  }
                }
                items(match, key = { item -> item.id }) {
                  MatchOverviewPreview(
                    matchPreviewInfo = it,
                    shareMode = shareState,
                    isSelected = it in shareMatchList,
                    onAction = { longPress, match ->
                      if (longPress) shareState = true // If long press enable share bar
                      when {
                        shareState && shareMatchList.contains(match) -> {
                          // If in share mode &
                          // If match is already in the list and is being clicked again, remove
                          // the
                          // item
                          shareMatchList.remove(match)
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        shareState &&
                          !shareMatchList.contains(match) &&
                          shareMatchList.size < MAX_SHARABLE_ITEMS -> {
                          // If in share mode &
                          // If list does not have 6 items and if the clicked icon is not already
                          // in
                          // the list
                          shareMatchList.add(match)
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        !shareState ->
                          onClick(match.id) // Its a normal click, navigate to the action
                      }
                    }
                  )
                }
              }
            }
          }
        }
      )
    }
  }
}

@Composable
fun NoMatchUI(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize()) {
    Spacer(modifier = modifier.weight(1f))
    Text(
      text = stringResource(R.string.no_match),
      modifier = modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.bodyLarge,
      color = VLRTheme.colorScheme.primary
    )
    Spacer(modifier = modifier.weight(1f))
  }
}

@Composable
fun MatchOverviewPreview(
  modifier: Modifier = Modifier,
  matchPreviewInfo: MatchPreviewInfo,
  shareMode: Boolean,
  isSelected: Boolean,
  onAction: (Boolean, MatchPreviewInfo) -> Unit,
) {
  CardView(
    modifier =
      modifier
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {},
            onDoubleTap = {},
            onLongPress = { onAction(true, matchPreviewInfo) },
            onTap = { onAction(false, matchPreviewInfo) }
          )
        }
        .apply { if (isSelected) background(VLRTheme.colorScheme.secondaryContainer) }
  ) {
    Column(modifier = modifier.padding(Local4DPPadding.current).animateContentSize()) {
      Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
          text =
            if (matchPreviewInfo.status.equals(stringResource(id = R.string.live), true))
              stringResource(id = R.string.live)
            else
              matchPreviewInfo.time?.timeDiff?.plus(" (${matchPreviewInfo.time.readableTime})")
                ?: "",
          modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.bodyMedium
        )

        if (shareMode)
          Checkbox(checked = isSelected, onCheckedChange = { onAction(false, matchPreviewInfo) })
      }
      Row(
        modifier = modifier.padding(Local8DP_4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = matchPreviewInfo.team1.name,
          style = VLRTheme.typography.titleMedium,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.team1.score?.toString() ?: "-",
          style = VLRTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
      }
      Row(
        modifier = modifier.padding(Local8DP_4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = matchPreviewInfo.team2.name,
          style = VLRTheme.typography.titleMedium,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.team2.score?.toString() ?: "-",
          style = VLRTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
      }
      Text(
        text = "${matchPreviewInfo.event} - ${matchPreviewInfo.series}",
        modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelMedium
      )
    }
  }
}
