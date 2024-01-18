package dev.staticvar.vlr.ui.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
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
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentPreview
import dev.staticvar.vlr.ui.Action
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.ScrollHelper
import dev.staticvar.vlr.ui.common.VlrHorizontalViewPager
import dev.staticvar.vlr.ui.common.VlrTabRowForViewPager
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting

@Composable
@NonSkippableComposable
fun EventScreen(viewModel: VlrViewModel) {

  val allTournaments by
  remember(viewModel) { viewModel.getEvents() }
    .collectAsStateWithLifecycle(initialValue = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
  remember(triggerRefresh) { viewModel.refreshEvents() }
    .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(updateState.get() ?: false, { triggerRefresh = triggerRefresh.not() })

  val resetScroll by
  remember { viewModel.resetScroll }.collectAsStateWithLifecycle(initialValue = false)

  val modifier: Modifier = Modifier

  Column(
    modifier = modifier
      .fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    StatusBarSpacer(statusBarType = StatusBarType.TABBED)
    allTournaments
      .onPass {
        data?.let { list ->
          TournamentPreviewContainer(
            modifier = Modifier,
            action = viewModel.action,
            list = StableHolder(list),
            swipeRefresh,
            updateState,
            resetScroll,
            postResetScroll = { viewModel.postResetScroll() }
          )
        }
      }
      .onWaiting { LinearProgressIndicator(modifier.animateContentSize()) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TournamentPreviewContainer(
  modifier: Modifier = Modifier,
  action: Action,
  list: StableHolder<List<TournamentPreview>>,
  swipeRefresh: PullRefreshState,
  updateState: Result<Boolean, Throwable?>,
  resetScroll: Boolean,
  postResetScroll: () -> Unit,
) {

  val tabs =
    listOf(
      stringResource(id = R.string.ongoing),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed)
    )
  val mapByStatus by remember(list) { mutableStateOf(list.item.groupBy { it.status }) }

  val pagerState = rememberPagerState(pageCount = { tabs.size })

  val (ongoing, upcoming, completed) =
    remember(list) {
      mapByStatus.let {
        Triple(
          it[tabs[0].lowercase()].orEmpty(),
          it[tabs[1].lowercase()].orEmpty(),
          it[tabs[2].lowercase()].orEmpty()
        )
      }
    }

  Column(
    modifier = modifier
      .fillMaxSize()
      .animateContentSize()
      .pullRefresh(swipeRefresh),
    verticalArrangement = Arrangement.Top
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
    VlrTabRowForViewPager(modifier = modifier, pagerState = pagerState, tabs = tabs)

    VlrHorizontalViewPager(
      modifier = modifier,
      pagerState = pagerState,
      {
        if (ongoing.isEmpty()) {
          NoEventUI(modifier = modifier)
        } else {
          val lazyListState = rememberLazyListState()
          lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)
          LazyColumn(
            modifier
              .fillMaxSize()
              .testTag("eventOverview:live"),
            verticalArrangement = Arrangement.Top,
            state = lazyListState
          ) {
            items(ongoing, key = { item -> item.id }) {
              TournamentPreview(modifier = modifier, tournamentPreview = it, action)
            }
          }
        }
      },
      {
        if (upcoming.isEmpty()) {
          NoEventUI(modifier = modifier)
        } else {
          val lazyListState = rememberLazyListState()
          lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)
          LazyColumn(
            modifier
              .fillMaxSize()
              .testTag("eventOverview:upcoming"),
            verticalArrangement = Arrangement.Top,
            state = lazyListState
          ) {
            items(upcoming, key = { item -> item.id }) {
              TournamentPreview(modifier = modifier, tournamentPreview = it, action)
            }
          }
        }
      },
      {
        if (completed.isEmpty()) {
          NoEventUI(modifier = modifier)
        } else {
          val lazyListState = rememberLazyListState()
          lazyListState.ScrollHelper(resetScroll = resetScroll, postResetScroll)
          LazyColumn(
            modifier
              .fillMaxSize()
              .testTag("eventOverview:result"),
            verticalArrangement = Arrangement.Top,
            state = lazyListState
          ) {
            items(completed, key = { item -> item.id }) {
              TournamentPreview(modifier = modifier, tournamentPreview = it, action)
            }
          }
        }
      }
    )
  }
}

@Composable
fun NoEventUI(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize()) {
    Spacer(modifier = modifier.weight(1f))
    Text(
      text = stringResource(R.string.no_ongoing_event),
      modifier = modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.bodyLarge,
      color = VLRTheme.colorScheme.primary
    )
    Spacer(modifier = modifier.weight(1f))
  }
}

@Composable
fun TournamentPreview(
  modifier: Modifier = Modifier,
  tournamentPreview: TournamentPreview,
  action: Action
) {
  CardView(modifier = modifier.clickable { action.event(tournamentPreview.id) }) {
    Column(modifier = modifier.padding(Local8DPPadding.current)) {
      Text(
        text = tournamentPreview.title,
        style = VLRTheme.typography.titleMedium,
        modifier = modifier.padding(Local4DPPadding.current),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = VLRTheme.colorScheme.primary,
      )

      Row(
        modifier = modifier.padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val annotatedLocationString = buildAnnotatedString {
          appendInlineContent(id = "location")
          append(tournamentPreview.location.uppercase())
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
          appendInlineContent(id = "date")
          append(" ")
          append(tournamentPreview.dates)
        }
        val inlineDateContentMap =
          mapOf(
            "date" to
                InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)) {
                  Icon(
                    imageVector = Icons.Outlined.DateRange,
                    modifier = modifier.size(16.dp),
                    contentDescription = ""
                  )
                }
          )
        Text(
          annotatedLocationString,
          style = VLRTheme.typography.bodyMedium,
          inlineContent = inlineLocationContentMap,
          modifier = modifier
            .padding(Local4DPPadding.current)
            .weight(1f),
          textAlign = TextAlign.Start,
        )
        Text(
          text = tournamentPreview.prize,
          modifier = modifier.padding(Local4DPPadding.current),
          textAlign = TextAlign.Start,
          style = VLRTheme.typography.bodyMedium
        )
        Text(
          annotatedDateString,
          style = VLRTheme.typography.bodyMedium,
          inlineContent = inlineDateContentMap,
          modifier = modifier
            .padding(Local4DPPadding.current)
            .weight(1f),
          textAlign = TextAlign.End,
        )
      }
    }
  }
}
