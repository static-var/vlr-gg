package dev.staticvar.vlr.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentPreview
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.launch

@Composable
fun EventScreen(viewModel: VlrViewModel) {

  val allTournaments by
    remember(viewModel) { viewModel.getTournaments() }.collectAsState(initial = Waiting())

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
            list = StableHolder(list)
          )
        }
      }
      .onWaiting { LinearProgressIndicator(modifier) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TournamentPreviewContainer(
  modifier: Modifier = Modifier,
  action: Action,
  list: StableHolder<List<TournamentPreview>>
) {

  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()

  val tabs =
    listOf(
      stringResource(id = R.string.ongoing),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed)
    )
  val mapByStatus by remember(list) { mutableStateOf(list.item.groupBy { it.status }) }

  list.item.forEach { println(it.status) }
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
  Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
    TabRow(
      selectedTabIndex = pagerState.currentPage,
      indicator = { indicators -> VLRTabIndicator(indicators, pagerState.currentPage) }
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = pagerState.currentPage == index,
          onClick = { scope.launch { pagerState.scrollToPage(index) } }
        ) { Text(text = title, modifier = modifier.padding(Local16DPPadding.current)) }
      }
    }

    HorizontalPager(count = 3, state = pagerState, modifier = modifier.fillMaxSize()) { tabPosition
      ->
      when (tabPosition) {
        0 -> {
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
        }
        1 -> {
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
        }
        else -> {
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
      }
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
