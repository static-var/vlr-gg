package dev.staticvar.vlr.ui.match

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DP_4DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.analytics.AnalyticsEvent
import dev.staticvar.vlr.ui.analytics.LogEvent
import dev.staticvar.vlr.ui.common.DateChip
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.ScrollHelper
import dev.staticvar.vlr.ui.common.VlrHorizontalViewPager
import dev.staticvar.vlr.ui.common.VlrSegmentedButtons
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.ShareDialog
import dev.staticvar.vlr.ui.helper.SharingAppBar
import dev.staticvar.vlr.ui.match.details_ui.NewMatchDetails
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting
import dev.staticvar.vlr.utils.readableDate
import dev.staticvar.vlr.utils.readableTime
import dev.staticvar.vlr.utils.timeDiff
import dev.staticvar.vlr.utils.timeToEpoch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MatchOverviewAdaptive(
  modifier: Modifier = Modifier,
  viewModel: VlrViewModel,
  hideNav: (Boolean) -> Unit,
) {
  var selectedItem: String? by rememberSaveable { mutableStateOf(null) }
  val paneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
  val navigator = rememberListDetailPaneScaffoldNavigator(scaffoldDirective = paneScaffoldDirective)
  val listOfLazyListState = remember { mutableStateListOf<LazyListState>() }

  if (listOfLazyListState.isEmpty()) {
    repeat(3) { listOfLazyListState.add(rememberLazyListState()) }
  }

  LaunchedEffect(navigator.currentDestination) {
    if (navigator.currentDestination?.pane == ThreePaneScaffoldRole.Secondary) {
      hideNav(false)
    } else hideNav(true)
  }

  val tabs =
    listOf(
      stringResource(id = R.string.live),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed),
    )
  val pagerState = rememberPagerState(pageCount = { tabs.size })

  BackHandler(navigator.canNavigateBack()) {
    selectedItem = null
    navigator.navigateBack()
  }

  ListDetailPaneScaffold(
    listPane = {
      AnimatedPane(modifier = modifier) {
        MatchOverview(
          viewModel = viewModel,
          pagerState = pagerState,
          selectedItem = selectedItem ?: " ",
          listOfLazyListState = listOfLazyListState,
          action = {
            selectedItem = it
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
          },
        )
      }
    },
    detailPane = {
      selectedItem?.let {
        AnimatedPane(modifier = modifier) { NewMatchDetails(viewModel = viewModel, id = it) }
      }
    },
    directive = navigator.scaffoldDirective,
    value = navigator.scaffoldValue,
    modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
  )
}

@Composable
fun MatchOverview(
  viewModel: VlrViewModel,
  pagerState: PagerState,
  selectedItem: String,
  listOfLazyListState: SnapshotStateList<LazyListState>,
  action: (String) -> Unit,
) {

  LogEvent(event = AnalyticsEvent.MATCH_OVERVIEW)

  val allMatches by
    remember(viewModel) { viewModel.getMatches() }
      .collectAsStateWithLifecycle(initialValue = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshMatches() }
      .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(updateState.get() ?: false, { triggerRefresh = triggerRefresh.not() })

  val resetScroll by
    remember { viewModel.resetScroll }.collectAsStateWithLifecycle(initialValue = false)

  val modifier: Modifier = Modifier
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT)
    Box(modifier) {
      allMatches
        .onPass {
          data?.let { list ->
            MatchOverviewContainer(
              modifier = modifier,
              list = list,
              swipeRefresh = swipeRefresh,
              pagerState = pagerState,
              listOfLazyListState = listOfLazyListState,
              updateState = updateState,
              resetScroll = resetScroll,
              selectedItem = selectedItem,
              onClick = action,
              postResetScroll = { viewModel.postResetScroll() },
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
  list: List<MatchPreviewInfo>,
  swipeRefresh: PullRefreshState,
  pagerState: PagerState,
  listOfLazyListState: SnapshotStateList<LazyListState>,
  updateState: Result<Boolean, Throwable?>,
  resetScroll: Boolean,
  selectedItem: String,
  onClick: (String) -> Unit,
  postResetScroll: () -> Unit,
) {
  val shareMatchList = remember { mutableStateListOf<MatchPreviewInfo>() }
  var shareState by remember { mutableStateOf(false) }
  var shareDialog by remember { mutableStateOf(false) }
  val haptic = LocalHapticFeedback.current

  val tabs =
    listOf(
      stringResource(id = R.string.live),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed),
    )

  if (shareDialog) {
    ShareDialog(matches = shareMatchList) { shareDialog = false }
  }

  val mapByStatus by remember(list) { mutableStateOf(list.groupBy { it.status }) }

  val (ongoing, upcoming, completed) =
    remember(list) {
      mapByStatus.let {
        Triple(
          it[tabs[0].lowercase()].orEmpty(),
          it[tabs[1].lowercase()].orEmpty().sortedBy { match -> match.time?.timeToEpoch },
          it[tabs[2].lowercase()].orEmpty().sortedByDescending { match -> match.time?.timeToEpoch },
        )
      }
    }

  Box {
    Column(
      modifier = modifier.fillMaxSize().animateContentSize().pullRefresh(swipeRefresh),
      verticalArrangement = Arrangement.Top,
    ) {
      AnimatedVisibility(visible = updateState.get() == true || swipeRefresh.progress != 0f) {
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
          shareConfirm = { shareDialog = true },
        )
      }

      //    VlrTabRowForViewPager(modifier = modifier, pagerState = pagerState, tabs = tabs)

      VlrHorizontalViewPager(
        modifier = modifier,
        pagerState = pagerState,
        {
          if (ongoing.isEmpty()) {
            NoMatchUI()
          } else {
            val lazyListState = listOfLazyListState[0]
            lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)

            LazyColumn(
              modifier.fillMaxSize().testTag("matchOverview:live"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState,
            ) {
              items(ongoing, key = { item -> item.id }) {
                MatchOverviewPreview(
                  matchPreviewInfo = it,
                  shareMode = shareState,
                  isSelected = it in shareMatchList,
                  selectedItem = selectedItem,
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
                  },
                )
              }
            }
          }
        },
        {
          if (upcoming.isEmpty()) {
            NoMatchUI()
          } else {
            val lazyListState = listOfLazyListState[1]
            lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)

            val groupedUpcomingMatches =
              remember(upcoming) { upcoming.groupBy { it.time?.readableDate } }
            LazyColumn(
              modifier.fillMaxSize().testTag("matchOverview:upcoming"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState,
            ) {
              groupedUpcomingMatches.forEach { (date, match)
                -> // Group heading based on date for sticky header
                stickyHeader { date?.let { date -> DateChip(date = date) } }
                items(match, key = { item -> item.id }) {
                  MatchOverviewPreview(
                    matchPreviewInfo = it,
                    shareMode = shareState,
                    isSelected = it in shareMatchList,
                    selectedItem = selectedItem,
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
                    },
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
            val lazyListState = listOfLazyListState[2]
            lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)

            val groupedCompletedMatches =
              remember(completed) { completed.groupBy { it.time?.readableDate } }
            LazyColumn(
              modifier.fillMaxSize().testTag("matchOverview:result"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState,
            ) {
              groupedCompletedMatches.forEach { (date, match)
                -> // Group heading based on date for sticky header
                stickyHeader { date?.let { date -> DateChip(date = date) } }
                items(match, key = { item -> item.id }) {
                  MatchOverviewPreview(
                    matchPreviewInfo = it,
                    shareMode = shareState,
                    isSelected = it in shareMatchList,
                    selectedItem = selectedItem,
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
                    },
                  )
                }
              }
            }
          }
        },
      )
    }

    val scope = rememberCoroutineScope()
    VlrSegmentedButtons(
      modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(8.dp),
      highlighted = pagerState.currentPage,
      items = tabs,
    ) { _, index ->
      scope.launch { pagerState.animateScrollToPage(index) }
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
      color = VLRTheme.colorScheme.primary,
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
  selectedItem: String,
  onAction: (Boolean, MatchPreviewInfo) -> Unit,
) {
  CardView(
    modifier =
      modifier.pointerInput(Unit) {
        detectTapGestures(
          onPress = {},
          onDoubleTap = {},
          onLongPress = { onAction(true, matchPreviewInfo) },
          onTap = { onAction(false, matchPreviewInfo) },
        )
      },
    colors =
      if (matchPreviewInfo.id == selectedItem) {
        CardDefaults.elevatedCardColors(
          containerColor = VLRTheme.colorScheme.secondaryContainer,
          contentColor = VLRTheme.colorScheme.onSecondaryContainer,
        )
      } else {
        CardDefaults.elevatedCardColors()
      },
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
          style = VLRTheme.typography.bodyMedium,
        )

        if (shareMode)
          Checkbox(checked = isSelected, onCheckedChange = { onAction(false, matchPreviewInfo) })
      }
      Row(
        modifier = modifier.padding(Local8DP_4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically,
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
        verticalAlignment = Alignment.CenterVertically,
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
        style = VLRTheme.typography.labelMedium,
      )
    }
  }
}
