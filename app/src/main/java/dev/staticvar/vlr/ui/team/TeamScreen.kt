package dev.staticvar.vlr.ui.team

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.ui.CARD_ALPHA
import dev.staticvar.vlr.ui.COLOR_ALPHA
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.launch

@Composable
fun TeamScreen(viewModel: VlrViewModel, id: String) {
  val teamDetails by
    remember(viewModel) { viewModel.getTeamDetails(id) }.collectAsState(initial = Waiting())
  var rosterCard by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(modifier = Modifier.statusBarsPadding())

    teamDetails
      .onPass {
        data?.let { teamDetail ->
          e { "$teamDetail" }
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { TeamBanner(teamDetails = teamDetail) }
            item {
              RosterCard(
                expanded = rosterCard,
                onExpand = { rosterCard = it },
                data = teamDetail.roster
              )
            }
            item {
              TeamMatchData(
                upcoming = teamDetail.upcoming,
                completed = teamDetail.completed,
                teamName = teamDetail.name,
                onClick = { viewModel.action.match(it) }
              )
            }
          }
        }
      }
      .onWaiting { LinearProgressIndicator() }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TeamBanner(teamDetails: TeamDetails) {
  OutlinedCard(
    Modifier.fillMaxWidth().padding(8.dp),
    contentColor = VLRTheme.colorScheme.onPrimaryContainer,
    containerColor = VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA),
    border = BorderStroke(1.dp, VLRTheme.colorScheme.primaryContainer)
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = teamDetails.name,
        modifier = Modifier.padding(horizontal = 2.dp),
        style = VLRTheme.typography.titleMedium
      )
      Text(
        text = teamDetails.tag,
        modifier = Modifier.padding(horizontal = 2.dp),
        style = VLRTheme.typography.labelMedium
      )
    }

    Row(
      modifier =
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "#${teamDetails.rank} in ", style = VLRTheme.typography.labelMedium)
      Text(text = teamDetails.region, style = VLRTheme.typography.labelMedium)
      Text(text = ", from " + teamDetails.country, style = VLRTheme.typography.labelMedium)
    }
  }
}

@Composable
fun RosterCard(expanded: Boolean, onExpand: (Boolean) -> Unit, data: List<TeamDetails.Roster>) {
  OutlinedCard(
    Modifier.fillMaxWidth().padding(8.dp),
    contentColor = VLRTheme.colorScheme.onPrimaryContainer,
    containerColor = VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA),
    border = BorderStroke(1.dp, VLRTheme.colorScheme.primaryContainer),
  ) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize(tween(500))) {
      if (!expanded) {
        Row(
          Modifier.fillMaxWidth().padding(8.dp).clickable { onExpand(true) },
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Roster", style = VLRTheme.typography.titleSmall)
          Icon(Icons.Outlined.ArrowDownward, contentDescription = "Expand")
        }
      } else {
        Row(
          Modifier.fillMaxWidth().padding(8.dp).clickable { onExpand(false) },
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Roster", style = VLRTheme.typography.titleSmall)
          Icon(Icons.Outlined.ArrowUpward, contentDescription = "Collapse")
        }
        data.forEach { player ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            contentColor = VLRTheme.colorScheme.onPrimaryContainer,
            containerColor = VLRTheme.colorScheme.primaryContainer
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
              modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
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
  upcoming: List<TeamDetails.Games>,
  completed: List<TeamDetails.Games>,
  teamName: String,
  onClick: (String) -> Unit
) {
  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
    TabRow(
      selectedTabIndex = pagerState.currentPage,
      containerColor = VLRTheme.colorScheme.primaryContainer,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp))
    ) {
      Tab(
        selected = pagerState.currentPage == 0,
        onClick = { scope.launch { pagerState.scrollToPage(0) } }
      ) { Text(text = "Upcoming", modifier = Modifier.padding(16.dp)) }
      Tab(
        selected = pagerState.currentPage == 1,
        onClick = { scope.launch { pagerState.scrollToPage(1) } }
      ) { Text(text = "Completed", modifier = Modifier.padding(16.dp)) }
    }
    HorizontalPager(count = 2, state = pagerState, modifier = Modifier.fillMaxSize()) {
      when (pagerState.currentPage) {
        0 -> {
          Column(modifier = Modifier.fillMaxSize()) {
            upcoming.forEach { games ->
              GameOverviewPreview(
                matchPreviewInfo = games,
                team = teamName,
                onClick = onClick,
              )
            }
          }
        }
        1 -> {
          Column(modifier = Modifier.fillMaxSize()) {
            completed.forEach { games ->
              GameOverviewPreview(
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
  matchPreviewInfo: TeamDetails.Games,
  team: String,
  onClick: (String) -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable {
        onClick(matchPreviewInfo.id)
      },
    shape = RoundedCornerShape(16.dp),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(
        text = matchPreviewInfo.eta ?: matchPreviewInfo.date.readableTime,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.displaySmall
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = team,
          style = VLRTheme.typography.displaySmall,
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center
        )
        Text(
          text = matchPreviewInfo.opponent,
          style = VLRTheme.typography.displaySmall,
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center
        )
      }
      Text(
        text = matchPreviewInfo.score,
        style = VLRTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().padding(2.dp),
        textAlign = TextAlign.Center
      )
      Text(
        text = matchPreviewInfo.event + " - " + matchPreviewInfo.stage,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}
