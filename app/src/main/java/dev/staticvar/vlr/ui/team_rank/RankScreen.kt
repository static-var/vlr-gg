package dev.staticvar.vlr.ui.team_rank

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.analytics.AnalyticsEvent
import dev.staticvar.vlr.ui.analytics.LogEvent
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.Illustration
import dev.staticvar.vlr.ui.common.PullToRefreshPill
import dev.staticvar.vlr.ui.common.ScrollHelper
import dev.staticvar.vlr.ui.common.VlrScrollableTabRowForViewPager
import dev.staticvar.vlr.ui.common.illustration.Loading
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.DynamicTheme
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun RankScreenAdaptive(
  modifier: Modifier = Modifier,
  viewModel: VlrViewModel,
  innerPadding: PaddingValues,
  hideNav: (Boolean) -> Unit,
) {
  var selectedItem: String? by rememberSaveable { mutableStateOf(null) }
  val paneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
  val navigator = rememberListDetailPaneScaffoldNavigator(scaffoldDirective = paneScaffoldDirective)
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(navigator.currentDestination) {
    if (navigator.currentDestination?.pane == ThreePaneScaffoldRole.Secondary) {
      hideNav(false)
    } else hideNav(true)
  }

  var pageSize by remember { mutableIntStateOf(0) }
  val pagerState = rememberPagerState(pageCount = { pageSize })
  val listOfLazyListState = remember { mutableStateListOf<LazyListState>() }

  LaunchedEffect(pageSize) { listOfLazyListState.clear() }

  if (listOfLazyListState.isEmpty()) {
    repeat(pageSize) { listOfLazyListState.add(rememberLazyListState()) }
  }

  BackHandler(navigator.canNavigateBack()) {
    selectedItem = null
    coroutineScope.launch {
      navigator.navigateBack()
    }
  }

  val layoutDirection = LocalLayoutDirection.current
  ListDetailPaneScaffold(
    listPane = {
      AnimatedPane(modifier = modifier) {
        RankScreen(
          viewModel = viewModel,
          selectedItem = selectedItem ?: " ",
          pageSize = { pageSize = it },
          pagerState = pagerState,
          listOfLazyListState = listOfLazyListState,
          contentPadding =
            PaddingValues(
              start = innerPadding.calculateStartPadding(layoutDirection),
              end = innerPadding.calculateEndPadding(layoutDirection),
              top = 0.dp,
              bottom = innerPadding.calculateBottomPadding(),
            ),
          action = {
            selectedItem = it
            coroutineScope.launch {
              navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
            }
          },
        )
      }
    },
    detailPane = {
      selectedItem?.let {
        AnimatedPane(modifier = modifier) { TeamScreen(viewModel = viewModel, id = it) }
      }
    },
    directive = navigator.scaffoldDirective,
    value = navigator.scaffoldValue,
    modifier = modifier,
  )
}

@Composable
@NonSkippableComposable
fun RankScreen(
  viewModel: VlrViewModel,
  selectedItem: String,
  pagerState: PagerState,
  listOfLazyListState: List<LazyListState>,
  contentPadding: PaddingValues,
  pageSize: (Int) -> Unit,
  action: (String) -> Unit,
) {

  LogEvent(event = AnalyticsEvent.RANKING_OVERVIEW)

  val allTeams by
  remember(viewModel) { viewModel.getRanks() }
    .collectAsStateWithLifecycle(initialValue = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
  remember(triggerRefresh) { viewModel.refreshRanks() }
    .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(
      refreshing = updateState.get() ?: false,
      { triggerRefresh = triggerRefresh.not() },
    )

  val resetScroll by
  remember { viewModel.resetScroll }.collectAsStateWithLifecycle(initialValue = false)

  val modifier: Modifier = Modifier

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    StatusBarSpacer(statusBarType = StatusBarType.TABBED)
    allTeams
      .onPass {
        if (data.isNullOrEmpty()) LinearProgressIndicator(modifier.animateContentSize())
        else
          RanksPreviewContainer(
            modifier = Modifier,
            list = StableHolder(data),
            swipeRefresh,
            updateState,
            pagerState,
            listOfLazyListState,
            resetScroll,
            selectedItem = selectedItem,
            contentPadding = contentPadding,
            action = action,
            postResetScroll = { viewModel.postResetScroll() },
            pageSize = pageSize,
          )
      }
      .onWaiting {
        Image(
          modifier = Modifier.padding(16.dp),
          imageVector = Illustration.Loading,
          contentDescription = stringResource(R.string.loading),
        )
      }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun RanksPreviewContainer(
  modifier: Modifier = Modifier,
  list: StableHolder<List<TeamDetails>>,
  swipeRefresh: PullRefreshState,
  updateState: Result<Boolean, Throwable?>,
  pagerState: PagerState,
  listOfLazyListState: List<LazyListState>,
  resetScroll: Boolean,
  selectedItem: String,
  contentPadding: PaddingValues,
  pageSize: (Int) -> Unit,
  action: (String) -> Unit,
  postResetScroll: () -> Unit,
) {
  val teamMap by
  remember(list) {
    mutableStateOf(
      list.item
        .sortedBy { it.rank }
        .filter { it.region.isNotEmpty() }
        .groupBy { it.region.trim() }
    )
  }
  val tabs by remember { mutableStateOf(teamMap.keys.toList().sorted()) }
  LaunchedEffect(tabs.size) { pageSize(tabs.size) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .animateContentSize()
      .pullRefresh(swipeRefresh),
    verticalArrangement = Arrangement.Top,
  ) {
    if (tabs.isNotEmpty()) {
      updateState.getError()?.let {
        ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
      }
      VlrScrollableTabRowForViewPager(modifier = modifier, pagerState = pagerState, tabs = tabs)

      Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshPill(
          modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp),
          show = updateState.get() == true || swipeRefresh.progress != 0f,
        )
        HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { tabPosition ->
          val lazyListState = listOfLazyListState.getOrNull(tabPosition) ?: rememberLazyListState()
          lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)
          val topTeams = teamMap[tabs[tabPosition]]?.take(25) ?: listOf()
          if (topTeams.isEmpty()) NoTeamsUI()
          else {
            LazyColumn(
              modifier
                .fillMaxSize()
                .testTag("rankOverview:live"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState,
              contentPadding = contentPadding,
            ) {
              items(topTeams, key = { item -> item.id }) {
                TeamRankPreview(team = it, selectedItem = selectedItem, action = action)
              }
            }
          }
        }
      }
    } else {
      Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
      }
    }
  }
}

@Composable
fun AnimatedProgressBar(modifier: Modifier = Modifier, show: Boolean) {
  AnimatedVisibility(show) {
    LinearProgressIndicator(
      modifier
        .fillMaxWidth()
        .padding(Local16DPPadding.current)
        .animateContentSize()
        .testTag("common:loader")
    )
  }
}

@Composable
fun NoTeamsUI(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize()) {
    Spacer(modifier = modifier.weight(1f))
    Text(
      text = stringResource(R.string.no_teams),
      modifier = modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.bodyLarge,
      color = VLRTheme.colorScheme.primary,
    )
    Spacer(modifier = modifier.weight(1f))
  }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TeamRankPreview(
  modifier: Modifier = Modifier,
  team: TeamDetails,
  selectedItem: String,
  action: (String) -> Unit,
) {
  val animatedBorder by animateDpAsState(
    targetValue = if (team.markedFav) 1.dp else -1.dp,
    tween(300)
  )

  DynamicTheme(
    model = team.img,
    fallback = VLRTheme.colorScheme.primary,
    useDarkTheme = isSystemInDarkTheme(),
  ) {
    CardView(
      modifier = modifier
        .clickable { action(team.id) }
        .height(120.dp)
        .border(
          animatedBorder, VLRTheme.colorScheme.onSurface, shape = RoundedCornerShape(8.dp)
        ),
      colors =
        if (selectedItem == team.id) {
          CardDefaults.elevatedCardColors(
            containerColor = VLRTheme.colorScheme.secondaryContainer,
            contentColor = VLRTheme.colorScheme.onSecondaryContainer,
          )
        } else {
          CardDefaults.elevatedCardColors()
        },
    ) {
      BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Column(modifier = modifier.padding(Local8DPPadding.current)) {
          val teamRankAnnotatedString = buildAnnotatedString {
            pushStyle(
              SpanStyle(
                VLRTheme.colorScheme.onSurface,
                fontSize = VLRTheme.typography.headlineMedium.fontSize,
              )
            )
            append("#${team.rank} ")
            pop()
            pushStyle(
              SpanStyle(VLRTheme.colorScheme.primary, VLRTheme.typography.titleMedium.fontSize)
            )
            append(team.name)
            pop()
          }

          Text(
            text = teamRankAnnotatedString,
            style = VLRTheme.typography.titleMedium,
            modifier = modifier.padding(start = 4.dp, end = 120.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = VLRTheme.colorScheme.primary,
          )
          val annotatedLocationString = buildAnnotatedString {
            appendInlineContent(id = "location")
            append(team.country.uppercase())
          }
          val inlineLocationContentMap =
            mapOf(
              "location" to
                  InlineTextContent(
                    Placeholder(
                      16.sp,
                      16.sp,
                      PlaceholderVerticalAlign.TextCenter
                    )
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.LocationOn,
                      modifier = modifier.size(16.dp),
                      contentDescription = "",
                    )
                  }
            )
          val annotatedDateString = buildAnnotatedString {
            appendInlineContent(id = "points")
            append((team.points ?: 0).toString())
          }
          val inlineDateContentMap =
            mapOf(
              "points" to
                  InlineTextContent(
                    Placeholder(
                      16.sp,
                      16.sp,
                      PlaceholderVerticalAlign.TextCenter
                    )
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Insights,
                      modifier = modifier.size(16.dp),
                      contentDescription = "",
                    )
                  }
            )
          Text(
            text = annotatedLocationString,
            style = VLRTheme.typography.bodyMedium,
            inlineContent = inlineLocationContentMap,
            modifier = modifier.padding(Local4DPPadding.current),
            textAlign = TextAlign.Start,
          )
          Text(
            text = annotatedDateString,
            inlineContent = inlineDateContentMap,
            modifier = modifier.padding(Local4DPPadding.current),
            textAlign = TextAlign.Start,
            style = VLRTheme.typography.bodyMedium,
          )
        }

        AsyncImage(
          model = team.img,
          contentDescription = "",
          contentScale = ContentScale.Fit,
          modifier =
            modifier
              .align(Alignment.CenterEnd)
              .padding(horizontal = 24.dp, vertical = 8.dp)
              .size(120.dp),
        )
      }
    }
  }
}
