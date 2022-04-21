package dev.staticvar.vlr.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.launch

@Composable
fun MatchOverview(viewModel: VlrViewModel) {

  val allMatches by
    remember(viewModel) { viewModel.getAllMatches() }.collectAsState(initial = Waiting())

  val primaryContainer = VLRTheme.colorScheme.surface
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }

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
              list = list,
              onClick = { id -> viewModel.action.match(id) }
            )
          }
        }
        .onWaiting { LinearProgressIndicator(modifier) }
        .onFail { Text(text = message()) }
    }
  }
}

@Composable
fun MatchOverviewContainer(
  modifier: Modifier = Modifier,
  list: List<MatchPreviewInfo>,
  onClick: (String) -> Unit
) {
  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()

  val (ongoing, upcoming, completed) =
    list.groupBy { it.status.startsWith("LIVE", ignoreCase = true) }.let {
      Triple(
        it[true].orEmpty(),
        it[false]
          ?.groupBy { it.status.startsWith("upcoming", ignoreCase = true) }
          ?.get(true)
          .orEmpty()
          .sortedBy { it.time?.timeToEpoch },
        it[false]
          ?.groupBy { it.status.startsWith("completed", ignoreCase = true) }
          ?.get(true)
          .orEmpty()
          .sortedByDescending { it.time?.timeToEpoch }
      )
    }

  Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
    TabRow(
      selectedTabIndex = pagerState.currentPage,
      indicator = { indicators -> VLRTabIndicator(indicators, pagerState.currentPage) }
    ) {
      Tab(
        selected = pagerState.currentPage == 0,
        onClick = { scope.launch { pagerState.scrollToPage(0) } }
      ) {
        Text(
          text = stringResource(R.string.ongoing),
          modifier = modifier.padding(Local16DPPadding.current)
        )
      }
      Tab(
        selected = pagerState.currentPage == 1,
        onClick = { scope.launch { pagerState.scrollToPage(1) } }
      ) {
        Text(
          text = stringResource(R.string.upcoming),
          modifier = modifier.padding(Local16DPPadding.current)
        )
      }
      Tab(
        selected = pagerState.currentPage == 2,
        onClick = { scope.launch { pagerState.scrollToPage(2) } }
      ) {
        Text(
          text = stringResource(R.string.completed),
          modifier = modifier.padding(Local16DPPadding.current)
        )
      }
    }

    HorizontalPager(count = 3, state = pagerState, modifier = modifier.fillMaxSize()) { tabPosition
      ->
      when (tabPosition) {
        0 -> {
          if (ongoing.isEmpty()) {
            NoMatchUI()
          } else {
            LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
              items(ongoing) { MatchOverviewPreview(matchPreviewInfo = it, onClick = onClick) }
            }
          }
        }
        1 -> {
          if (upcoming.isEmpty()) {
            NoMatchUI()
          } else {
            LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
              val groupedUpcomingMatches = upcoming.groupBy { it.time?.readableDate }
              groupedUpcomingMatches.forEach { (date, match) ->
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
                items(match) { MatchOverviewPreview(matchPreviewInfo = it, onClick = onClick) }
              }
            }
          }
        }
        else -> {
          if (completed.isEmpty()) {
            NoMatchUI(modifier = modifier)
          } else {
            LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
              val groupedCompletedMatches = completed.groupBy { it.time?.readableDate }
              groupedCompletedMatches.forEach { (date, match) ->
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
                items(match) { MatchOverviewPreview(matchPreviewInfo = it, onClick = onClick) }
              }
            }
          }
        }
      }
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
      textAlign = TextAlign.Center
    )
    Spacer(modifier = modifier.weight(1f))
  }
}

@Composable
fun MatchOverviewPreview(
  modifier: Modifier = Modifier,
  matchPreviewInfo: MatchPreviewInfo,
  onClick: (String) -> Unit
) {
  CardView(modifier = modifier.clickable { onClick(matchPreviewInfo.id) }) {
    Column(modifier = modifier.padding(Local4DPPadding.current)) {
      Text(
        text =
          if (matchPreviewInfo.status.equals("LIVE", true)) "LIVE"
          else
            matchPreviewInfo.time?.timeDiff?.plus(" (${matchPreviewInfo.time.readableTime})") ?: "",
        modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.displaySmall
      )
      Row(
        modifier = modifier.padding(Local8DP_4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = matchPreviewInfo.team1.name,
          style = VLRTheme.typography.titleSmall,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.team1.score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
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
          style = VLRTheme.typography.titleSmall,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.team2.score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
      }
      Text(
        text = "${matchPreviewInfo.event} - ${matchPreviewInfo.series}",
        modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}
