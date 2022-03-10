package dev.staticvar.vlr.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentPreview
import dev.staticvar.vlr.ui.Action
import dev.staticvar.vlr.ui.CARD_ALPHA
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting
import kotlinx.coroutines.launch

@Composable
fun EventScreen(viewModel: VlrViewModel) {

  val allTournaments by
    remember(viewModel) { viewModel.getTournaments() }.collectAsState(initial = Waiting())

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(modifier = Modifier.statusBarsPadding())

    allTournaments
      .onPass {
        data?.let { list -> TournamentPreviewContainer(viewModel = viewModel, list = list) }
      }
      .onWaiting { LinearProgressIndicator() }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TournamentPreviewContainer(viewModel: VlrViewModel, list: List<TournamentPreview>) {
  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()

  val (ongoing, upcoming, completed) =
    list.groupBy { it.status.startsWith("ongoing", ignoreCase = true) }.let {
      Triple(
        it[true].orEmpty(),
        it[false]
          ?.groupBy { it.status.startsWith("upcoming", ignoreCase = true) }
          ?.get(true)
          .orEmpty(),
        it[false]
          ?.groupBy { it.status.startsWith("upcoming", ignoreCase = true) }
          ?.get(false)
          .orEmpty()
      )
    }
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
    TabRow(
      selectedTabIndex = pagerState.currentPage,
      containerColor = VLRTheme.colorScheme.primaryContainer
    ) {
      Tab(
        selected = pagerState.currentPage == 0,
        onClick = { scope.launch { pagerState.scrollToPage(0) } }
      ) { Text(text = stringResource(R.string.ongoing), modifier = Modifier.padding(16.dp)) }
      Tab(
        selected = pagerState.currentPage == 1,
        onClick = { scope.launch { pagerState.scrollToPage(1) } }
      ) { Text(text = stringResource(R.string.upcoming), modifier = Modifier.padding(16.dp)) }
      Tab(
        selected = pagerState.currentPage == 2,
        onClick = { scope.launch { pagerState.scrollToPage(2) } }
      ) { Text(text = stringResource(R.string.completed), modifier = Modifier.padding(16.dp)) }
    }
    HorizontalPager(count = 3, state = pagerState, modifier = Modifier.fillMaxSize()) { tabPosition
      ->
      when (tabPosition) {
        0 -> {
          if (ongoing.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(text = stringResource(R.string.no_ongoing_event))
            Spacer(modifier = Modifier.weight(1f))
          } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
              items(ongoing) { TournamentPreview(tournamentPreview = it, viewModel.action) }
            }
          }
        }
        1 -> {
          if (upcoming.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(text = stringResource(R.string.no_ongoing_event))
            Spacer(modifier = Modifier.weight(1f))
          } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
              items(upcoming) { TournamentPreview(tournamentPreview = it, viewModel.action) }
            }
          }
        }
        else -> {
          if (completed.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(text = stringResource(R.string.no_ongoing_event))
            Spacer(modifier = Modifier.weight(1f))
          } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
              items(completed) { TournamentPreview(tournamentPreview = it, viewModel.action) }
            }
          }
        }
      }
    }
  }
}

@Composable
fun TournamentPreview(tournamentPreview: TournamentPreview, action: Action) {
  Card(
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).clickable {
        action.event(tournamentPreview.id)
      },
    shape = RoundedCornerShape(16.dp),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(
        text = tournamentPreview.title,
        style = VLRTheme.typography.titleSmall,
        modifier = Modifier.padding(4.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Outlined.LocationOn,
          contentDescription = stringResource(R.string.location),
          modifier = Modifier.size(16.dp)
        )
        Text(text = tournamentPreview.location.uppercase(), style = VLRTheme.typography.labelMedium)
        Text(
          text = tournamentPreview.prize,
          modifier = Modifier.padding(4.dp).weight(1f),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.labelMedium
        )
        Icon(Icons.Outlined.DateRange, contentDescription = "Date", modifier = Modifier.size(16.dp))
        Text(text = tournamentPreview.dates, style = VLRTheme.typography.labelMedium)
      }
    }
  }
}
