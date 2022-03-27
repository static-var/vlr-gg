package dev.staticvar.vlr.ui.team

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.launch

@Composable
fun TeamScreen(viewModel: VlrViewModel, id: String) {
  val teamDetails by
    remember(viewModel) { viewModel.getTeamDetails(id) }.collectAsState(initial = Waiting())
  var rosterCard by remember { mutableStateOf(false) }

  val modifier: Modifier = Modifier

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    teamDetails
      .onPass {
        data?.let { teamDetail ->
          LazyColumn(modifier = modifier.fillMaxSize()) {
            item { Spacer(modifier = modifier.statusBarsPadding()) }
            item { TeamBanner(modifier = modifier, teamDetails = teamDetail) }
            item {
              RosterCard(
                modifier = modifier,
                expanded = rosterCard,
                onExpand = { rosterCard = it },
                data = teamDetail.roster
              )
            }
            item {
              TeamMatchData(
                modifier = modifier,
                upcoming = teamDetail.upcoming,
                completed = teamDetail.completed,
                teamName = teamDetail.name,
                onClick = { viewModel.action.match(it) }
              )
            }
          }
        }
      }
      .onWaiting { LinearProgressIndicator(modifier) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TeamBanner(modifier: Modifier = Modifier, teamDetails: TeamDetails) {
  CardView(modifier) {
    Row(
      modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = teamDetails.name,
        modifier = modifier.padding(Local2DPPadding.current),
        style = VLRTheme.typography.titleMedium,
        color = VLRTheme.colorScheme.primary,
      )
      Text(
        text = teamDetails.tag,
        modifier = modifier.padding(Local2DPPadding.current),
        style = VLRTheme.typography.labelMedium
      )
    }

    Row(
      modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "#${teamDetails.rank} in ", style = VLRTheme.typography.labelMedium)
      Text(text = teamDetails.region, style = VLRTheme.typography.labelMedium)
      Text(text = ", from " + teamDetails.country, style = VLRTheme.typography.labelMedium)
    }
  }
}

@Composable
fun RosterCard(
  modifier: Modifier = Modifier,
  expanded: Boolean,
  onExpand: (Boolean) -> Unit,
  data: List<TeamDetails.Roster>
) {
  CardView() {
    Column(modifier = modifier.fillMaxWidth().animateContentSize(tween(500))) {
      if (!expanded) {
        Row(
          modifier.fillMaxWidth().padding(Local8DPPadding.current).clickable { onExpand(true) },
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = stringResource(R.string.roster),
            style = VLRTheme.typography.titleSmall,
            color = VLRTheme.colorScheme.primary,
          )
          Icon(
            Icons.Outlined.ArrowDownward,
            contentDescription = stringResource(R.string.expand),
            tint = VLRTheme.colorScheme.primary,
          )
        }
      } else {
        Row(
          modifier.fillMaxWidth().padding(Local8DPPadding.current).clickable { onExpand(false) },
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = stringResource(R.string.roster),
            style = VLRTheme.typography.titleSmall,
            color = VLRTheme.colorScheme.primary,
          )
          Icon(
            Icons.Outlined.ArrowUpward,
            contentDescription = stringResource(R.string.collapse),
            tint = VLRTheme.colorScheme.primary,
          )
        }
        data.forEach { player ->
          Card(
            modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
            contentColor = VLRTheme.colorScheme.onPrimaryContainer,
            containerColor = VLRTheme.colorScheme.primaryContainer
          ) {
            Row(
              modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = player.alias, style = VLRTheme.typography.titleSmall)
              Text(
                text = player.role?.replaceFirstChar { it.uppercase() } ?: "",
                style = VLRTheme.typography.labelMedium
              )
            }
            Text(
              text = player.name ?: "",
              modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
              style = VLRTheme.typography.labelMedium
            )
          }
        }
      }
    }
  }
}

@Composable
fun TeamMatchData(
  modifier: Modifier = Modifier,
  upcoming: List<TeamDetails.Games>,
  completed: List<TeamDetails.Games>,
  teamName: String,
  onClick: (String) -> Unit
) {
  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()

  Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
    TabRow(
      selectedTabIndex = pagerState.currentPage,
      containerColor = VLRTheme.colorScheme.primaryContainer,
      modifier =
        modifier.fillMaxWidth().padding(Local16DPPadding.current).clip(RoundedCornerShape(16.dp))
    ) {
      Tab(
        selected = pagerState.currentPage == 0,
        onClick = { scope.launch { pagerState.scrollToPage(0) } }
      ) {
        Text(
          text = stringResource(R.string.upcoming),
          modifier = modifier.padding(Local16DPPadding.current)
        )
      }
      Tab(
        selected = pagerState.currentPage == 1,
        onClick = { scope.launch { pagerState.scrollToPage(1) } }
      ) {
        Text(
          text = stringResource(R.string.completed),
          modifier = Modifier.padding(Local16DPPadding.current)
        )
      }
    }
    HorizontalPager(count = 2, state = pagerState, modifier = Modifier.fillMaxSize()) {
      when (pagerState.currentPage) {
        0 -> {
          Column(modifier = modifier.fillMaxSize()) {
            upcoming.forEach { games ->
              GameOverviewPreview(
                modifier = modifier,
                matchPreviewInfo = games,
                team = teamName,
                onClick = onClick,
              )
            }
          }
        }
        1 -> {
          Column(modifier = modifier.fillMaxSize()) {
            completed.forEach { games ->
              GameOverviewPreview(
                modifier = modifier,
                matchPreviewInfo = games,
                team = teamName,
                onClick = onClick,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun GameOverviewPreview(
  modifier: Modifier = Modifier,
  matchPreviewInfo: TeamDetails.Games,
  team: String,
  onClick: (String) -> Unit,
) {
  CardView(
    modifier = modifier.clickable { onClick(matchPreviewInfo.id) },
  ) {
    Column(modifier = modifier.padding(Local8DPPadding.current)) {
      Text(
        text = matchPreviewInfo.eta ?: matchPreviewInfo.date.readableDateAndTime,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.displaySmall
      )
      Row(
        modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = team,
          style = VLRTheme.typography.displaySmall,
          modifier = modifier.weight(1f).padding(Local4DPPadding.current),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.opponent,
          style = VLRTheme.typography.displaySmall,
          modifier = modifier.weight(1f).padding(Local4DPPadding.current),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          color = VLRTheme.colorScheme.primary,
        )
      }
      Text(
        text = matchPreviewInfo.score.ifBlank { "TBP" },
        style = VLRTheme.typography.titleSmall,
        modifier = modifier.fillMaxWidth().padding(Local2DPPadding.current),
        textAlign = TextAlign.Center,
        color = VLRTheme.colorScheme.primary,
      )
      Text(
        text = "${matchPreviewInfo.event} - ${matchPreviewInfo.stage}",
        modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}
