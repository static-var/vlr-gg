package dev.staticvar.vlr.ui.team_rank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.circular.CircularRevealPlugin
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.ui.Action
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.ScrollHelper
import dev.staticvar.vlr.ui.common.StatusBarColorForHomeWithTabs
import dev.staticvar.vlr.ui.common.VlrScrollableTabRowForViewPager
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting

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

  val swipeRefresh =
    rememberPullRefreshState(
      refreshing = updateState.get() ?: false,
      { triggerRefresh = triggerRefresh.not() })

  val resetScroll by
  remember { viewModel.resetScroll }.collectAsStateWithLifecycle(initialValue = false)

  val modifier: Modifier = Modifier

  Column(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    allTeams
      .onPass {
        if (data == null || data.isEmpty()) LinearProgressIndicator(modifier.animateContentSize())
        else
          RanksPreviewContainer(
            modifier = Modifier,
            action = viewModel.action,
            list = StableHolder(data),
            swipeRefresh,
            updateState,
            resetScroll,
            postResetScroll = { viewModel.postResetScroll() }
          )
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
  swipeRefresh: PullRefreshState,
  updateState: Result<Boolean, Throwable?>,
  resetScroll: Boolean,
  postResetScroll: () -> Unit,
) {
  val pagerState = rememberPagerState()
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

  Column(
    modifier = modifier
      .fillMaxSize()
      .animateContentSize()
      .pullRefresh(swipeRefresh),
    verticalArrangement = Arrangement.Top
  ) {
    if (tabs.isNotEmpty()) {
      AnimatedVisibility(
        visible = updateState.get() == true || swipeRefresh.progress != 0f,
      ) {
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
      VlrScrollableTabRowForViewPager(modifier = modifier, pagerState = pagerState, tabs = tabs)
      HorizontalPager(
        count = tabs.size,
        state = pagerState,
        modifier = modifier.fillMaxSize()
      ) { tabPosition ->
        val lazyListState = rememberLazyListState()
        lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)
        val topTeams = teamMap[tabs[tabPosition]]?.take(25) ?: listOf()
        if (topTeams.isEmpty()) NoTeamsUI()
        else {
          LazyColumn(
            modifier
              .fillMaxSize()
              .testTag("rankOverview:live"),
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
        AnimatedVisibility(
          visible = updateState.get() == true || swipeRefresh.progress != 0f,
        ) {
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
      color = VLRTheme.colorScheme.primary
    )
    Spacer(modifier = modifier.weight(1f))
  }
}

@Composable
fun TeamRankPreview(modifier: Modifier = Modifier, team: TeamDetails, action: Action) {
  val imageComponent = rememberImageComponent {
    add(CircularRevealPlugin())
  }

  CardView(modifier = modifier
    .clickable { action.team(team.id) }
    .height(120.dp)) {
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
        imageModel = { team.img },
        modifier = modifier
          .align(Alignment.CenterEnd)
          .padding(24.dp)
          .size(120.dp),
        imageOptions =
        ImageOptions(contentScale = ContentScale.Fit, alignment = Alignment.CenterEnd),
        component = imageComponent
      )
    }
  }
}
