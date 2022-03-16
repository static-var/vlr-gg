package dev.staticvar.vlr.ui.match

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.CARD_ALPHA
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.launch

@Composable
fun MatchOverview(viewModel: VlrViewModel) {

  val allMatches by
    remember(viewModel) { viewModel.getAllMatches() }.collectAsState(initial = Waiting())

  Column(
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box() {
      allMatches
        .onPass {
          data?.let { list ->
            MatchOverviewContainer(list = list, onClick = { id -> viewModel.action.match(id) })
          }
        }
        .onWaiting { LinearProgressIndicator() }
        .onFail { Text(text = message()) }
    }
  }
}

@Composable
fun MatchOverviewContainer(list: List<MatchPreviewInfo>, onClick: (String) -> Unit) {
  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()

  val (ongoing, upcoming, completed) =
    list.groupBy { it.status.startsWith("LIVE", ignoreCase = true) }.let {
      Triple(
        it[true].orEmpty(),
        it[false]
          ?.groupBy { it.status.startsWith("upcoming", ignoreCase = true) }
          ?.get(true)
          .orEmpty(),
        it[false]
          ?.groupBy { it.status.startsWith("completed", ignoreCase = true) }
          ?.get(true)
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
              items(ongoing) { MatchOverviewPreview(matchPreviewInfo = it, onClick) }
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
              items(upcoming) { MatchOverviewPreview(matchPreviewInfo = it, onClick) }
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
              items(completed) { MatchOverviewPreview(matchPreviewInfo = it, onClick) }
            }
          }
        }
      }
    }
  }
}

@Composable
fun MatchOverviewPreview(matchPreviewInfo: MatchPreviewInfo, onClick: (String) -> Unit) {
  Card(
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).clickable {
        onClick(matchPreviewInfo.id)
      },
    shape = RoundedCornerShape(16.dp),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(
        text =
          if (matchPreviewInfo.status.equals("LIVE", true)) "LIVE"
          else matchPreviewInfo.time?.timeDiff ?: "",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.displaySmall
      )
      Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = matchPreviewInfo.team1.name,
          style = VLRTheme.typography.titleSmall,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = matchPreviewInfo.team1.score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = matchPreviewInfo.team2.name,
          style = VLRTheme.typography.titleSmall,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = matchPreviewInfo.team2.score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = "${matchPreviewInfo.event} - ${matchPreviewInfo.series}",
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}
