package dev.staticvar.vlr.ui.team_rank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.ScrollHelper
import dev.staticvar.vlr.ui.common.StatusBarColorForHomeWithTabs
import dev.staticvar.vlr.ui.common.VlrScrollableTabRowForViewPager
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*

@Composable
fun RankScreen(viewModel: VlrViewModel) {
  StatusBarColorForHomeWithTabs()

  val allTeams by
    remember(viewModel) { viewModel.getRanks() }
      .collectAsStateWithLifecycle(initialValue = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshRanks() }
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
    allTeams
      .onPass {
        data?.let { list ->
          RanksPreviewContainer(
            modifier = Modifier,
            action = viewModel.action,
            list = StableHolder(list),
            swipeRefresh,
            updateState,
            resetScroll,
            triggerRefresh = { triggerRefresh = triggerRefresh.not() },
            postResetScroll = { viewModel.postResetScroll() }
          )
        }
      }
      .onWaiting { LinearProgressIndicator(modifier.animateContentSize()) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun RanksPreviewContainer(
  modifier: Modifier = Modifier,
  action: Action,
  list: StableHolder<List<TeamDetails>>,
  swipeRefresh: SwipeRefreshState,
  updateState: Result<Boolean, Throwable?>,
  resetScroll: Boolean,
  triggerRefresh: () -> Unit,
  postResetScroll: () -> Unit,
) {
  val pagerState = rememberPagerState()
  val teamMap by
    remember(list) { mutableStateOf(list.item.sortedBy { it.rank }.groupBy { it.region }) }
  val tabs by remember { mutableStateOf(teamMap.keys.toList().sorted()) }
  SwipeRefresh(state = swipeRefresh, onRefresh = triggerRefresh, indicator = { _, _ -> }) {
    Column(
      modifier = modifier.fillMaxSize().animateContentSize(),
      verticalArrangement = Arrangement.Top
    ) {
      if (tabs.isNotEmpty()) {
        AnimatedProgressBar(
          modifier,
          show = updateState.get() == true || swipeRefresh.isSwipeInProgress
        )
        updateState.getError()?.let {
          ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
        }
        VlrScrollableTabRowForViewPager(modifier = modifier, pagerState = pagerState, tabs = tabs)
        HorizontalPager(count = tabs.size, state = pagerState, modifier = modifier.fillMaxSize()) {
          tabPosition ->
          val lazyListState = rememberLazyListState()
          lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)
          val topTeams = teamMap[tabs[tabPosition]]?.take(25) ?: listOf()
          if (topTeams.isEmpty()) NoTeamsUI()
          else {
            LazyColumn(
              modifier.fillMaxSize().testTag("rankOverview:live"),
              verticalArrangement = Arrangement.Top,
              state = lazyListState
            ) {
              items(topTeams, key = { item -> item.id }) {
                TeamRankPreview(team = it, action = action)
              }
            }
          }
        }
      } else {
        Column(
          modifier = modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          AnimatedProgressBar(
            modifier,
            show = updateState.get() == true || swipeRefresh.isSwipeInProgress
          )
          updateState.getError()?.let {
            ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
          }
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
      color = VLRTheme.colorScheme.primary
    )
    Spacer(modifier = modifier.weight(1f))
  }
}

@Composable
fun TeamRankPreview(modifier: Modifier = Modifier, team: TeamDetails, action: Action) {

  CardView(modifier = modifier.clickable { action.team(team.id) }.height(120.dp)) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
      Column(modifier = modifier.padding(Local8DPPadding.current)) {
        val teamRankAnnotatedString = buildAnnotatedString {
          pushStyle(
            SpanStyle(
              VLRTheme.colorScheme.onSurface,
              fontSize = VLRTheme.typography.headlineMedium.fontSize
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
              InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)) {
                Icon(
                  imageVector = Icons.Outlined.LocationOn,
                  modifier = modifier.size(16.dp),
                  contentDescription = ""
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
              InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)) {
                Icon(
                  imageVector = Icons.Outlined.Insights,
                  modifier = modifier.size(16.dp),
                  contentDescription = ""
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
          style = VLRTheme.typography.bodyMedium
        )
      }
      GlideImage(
        imageModel = team.img,
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterEnd,
        modifier = modifier.align(Alignment.CenterEnd).padding(24.dp).size(120.dp),
        circularReveal = CircularReveal(400),
      )
    }
  }
}
