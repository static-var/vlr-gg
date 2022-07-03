package dev.staticvar.vlr.ui.events

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentPreview
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.VlrHorizontalViewPager
import dev.staticvar.vlr.ui.common.VlrTabRowForViewPager
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*

@Composable
fun EventScreen(viewModel: VlrViewModel) {

  val allTournaments by
    remember(viewModel) { viewModel.getEvents() }.collectAsState(initial = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshEvents() }.collectAsState(initial = Ok(false))

  val swipeRefresh = rememberSwipeRefreshState(isRefreshing = updateState.get() ?: false)

  val primaryContainer = VLRTheme.colorScheme.surface
  val systemUiController = rememberSystemUiController()
  val isDarkMode = isSystemInDarkTheme()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer, darkIcons = !isDarkMode) }

  val modifier: Modifier = Modifier

  Column(
    modifier = modifier.fillMaxSize().statusBarsPadding(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    allTournaments
      .onPass {
        data?.let { list ->
          TournamentPreviewContainer(
            modifier = Modifier,
            action = viewModel.action,
            list = StableHolder(list),
            swipeRefresh,
            updateState,
            triggerRefresh = { triggerRefresh = triggerRefresh.not() }
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
  swipeRefresh: SwipeRefreshState,
  updateState: Result<Boolean, Throwable?>,
  triggerRefresh: () -> Unit
) {

  val pagerState = rememberPagerState()

  val tabs =
    listOf(
      stringResource(id = R.string.ongoing),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed)
    )
  val mapByStatus by remember(list) { mutableStateOf(list.item.groupBy { it.status }) }

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

  SwipeRefresh(state = swipeRefresh, onRefresh = triggerRefresh, indicator = { _, _ -> }) {
    Column(
      modifier = modifier.fillMaxSize().animateContentSize(),
      verticalArrangement = Arrangement.Top
    ) {
      if (updateState.get() == true || swipeRefresh.isSwipeInProgress)
        LinearProgressIndicator(
          modifier.fillMaxWidth().padding(Local16DPPadding.current).animateContentSize()
        )
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
            LazyColumn(
              modifier.fillMaxSize(),
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
            LazyColumn(
              modifier.fillMaxSize(),
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
            LazyColumn(
              modifier.fillMaxSize(),
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
        style = VLRTheme.typography.titleSmall,
        modifier = modifier.padding(Local4DPPadding.current),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = VLRTheme.colorScheme.primary,
      )

      Row(
        modifier = modifier.padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Outlined.LocationOn,
          contentDescription = stringResource(R.string.location),
          modifier = modifier.size(16.dp),
        )
        Text(text = tournamentPreview.location.uppercase(), style = VLRTheme.typography.labelMedium)
        Text(
          text = tournamentPreview.prize,
          modifier = modifier.padding(Local4DPPadding.current).weight(1f),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.labelMedium
        )
        Icon(
          Icons.Outlined.DateRange,
          contentDescription = "Date",
          modifier = modifier.size(16.dp),
        )
        Text(text = tournamentPreview.dates, style = VLRTheme.typography.labelMedium)
      }
    }
  }
}
