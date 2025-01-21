package dev.staticvar.vlr.ui.team_rank

import android.Manifest
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.ui.Local16DP_8DPPadding
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.Local8DP_4DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.analytics.AnalyticsEvent
import dev.staticvar.vlr.ui.analytics.LogEvent
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.PullToRefreshPill
import dev.staticvar.vlr.ui.common.VlrTabRowForViewPager
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.plus
import dev.staticvar.vlr.ui.match.NoMatchUI
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting
import dev.staticvar.vlr.utils.openAsCustomTab
import dev.staticvar.vlr.utils.readableDateAndTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun TeamScreen(viewModel: VlrViewModel, id: String) {

  LogEvent(event = AnalyticsEvent.TEAM_OVERVIEW, extra = mapOf("team_id" to id))

  val teamDetails by
  remember(id) { viewModel.getTeamDetails(id) }.collectAsState(initial = Waiting())
  var rosterCard by remember { mutableStateOf(false) }

  val trackerString = id.toTeamTopic()

  var triggerRefresh by remember(viewModel, id) { mutableStateOf(true) }
  val updateState by
  remember(triggerRefresh) { viewModel.refreshTeamDetails(id) }
    .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(
      refreshing = updateState.get() ?: false,
      { triggerRefresh = triggerRefresh.not() },
    )

  val modifier: Modifier = Modifier

  val progressBarVisibility by
  remember(updateState.get(), swipeRefresh.progress) {
    derivedStateOf { updateState.get() == true || swipeRefresh.progress != 0f }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    teamDetails
      .onPass {
        data?.let { teamDetail ->
          Box(modifier = Modifier
            .pullRefresh(swipeRefresh)
            .fillMaxSize()) {
            PullToRefreshPill(
              modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
                .align(Alignment.TopCenter),
              show = progressBarVisibility,
            )
            LazyColumn(
              modifier = modifier.fillMaxSize(),
              contentPadding =
                WindowInsets.statusBars.asPaddingValues() +
                    WindowInsets.navigationBars.asPaddingValues(),
            ) {
              updateState.getError()?.let {
                item { ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString()) }
              }
              item {
                TeamBanner(
                  modifier = modifier.testTag("team:banner"),
                  teamDetails = teamDetail,
                  id = id,
                  isTracked = teamDetail.markedFav,
                ) {
                  when (teamDetail.markedFav) {
                    true -> {
                      Firebase.messaging.unsubscribeFromTopic(trackerString).await()
                      viewModel.untrackTeam(teamDetail.id)
                    }

                    false -> {
                      Firebase.messaging.subscribeToTopic(trackerString).await()
                      viewModel.trackTeam(teamDetail.id)
                    }
                  }
                }
              }
              item {
                RosterCard(
                  modifier = modifier,
                  expanded = rosterCard,
                  data = StableHolder(teamDetail.roster),
                  onExpand = { rosterCard = it },
                  onClick = { viewModel.action.player(it) },
                )
              }
              item {
                TeamMatchData(
                  modifier = modifier,
                  upcoming = StableHolder(teamDetail.upcoming),
                  completed = StableHolder(teamDetail.completed),
                  teamName = teamDetail.name,
                  onClick = { viewModel.action.match(it) },
                )
              }
            }
          }
        }
          ?: kotlin.run {
            updateState.getError()?.let {
              ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
            } ?: LinearProgressIndicator(modifier.animateContentSize())
          }
      }
      .onWaiting { LinearProgressIndicator(modifier) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TeamBanner(
  modifier: Modifier = Modifier,
  teamDetails: TeamDetails,
  id: String,
  isTracked: Boolean,
  onSubButton: suspend () -> Unit,
) {

  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val animatedBorder by animateDpAsState(
    targetValue = if (isTracked) 1.dp else -1.dp,
    tween(300)
  )
  val notificationPermission =
    rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

  CardView(
    modifier.border(
      animatedBorder,
      VLRTheme.colorScheme.primary,
      shape = RoundedCornerShape(8.dp)
    )
  ) {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(Local16DP_8DPPadding.current),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = teamDetails.name,
        modifier = modifier.padding(Local2DPPadding.current),
        style = VLRTheme.typography.titleMedium,
        color = VLRTheme.colorScheme.primary,
      )
      if (teamDetails.tag?.isNotBlank() == true)
        Text(
          text = "[${teamDetails.tag}]",
          modifier = modifier.padding(Local2DPPadding.current),
          style = VLRTheme.typography.labelMedium,
        )
    }
    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(Local16DP_8DPPadding.current),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Server might send rank 0 when ranks are not found over the website.
      if (teamDetails.rank != 0)
        Text(text = "#${teamDetails.rank} in ", style = VLRTheme.typography.labelMedium)
      Text(text = teamDetails.region, style = VLRTheme.typography.labelMedium)
      Text(text = ", from " + teamDetails.country, style = VLRTheme.typography.labelMedium)
    }

    var processingTopicSubscription by remember { mutableStateOf(false) }

    Button(
      onClick = {
        if (notificationPermission.status.isGranted) {
          if (!processingTopicSubscription) {
            processingTopicSubscription = true
            scope.launch(Dispatchers.IO) {
              onSubButton()
              processingTopicSubscription = false
            }
          }
        } else notificationPermission.launchPermissionRequest()
      },
      modifier = modifier
        .fillMaxWidth()
        .padding(Local8DP_4DPPadding.current),
      shape = VLRTheme.shapes.small,
    ) {
      if (processingTopicSubscription) {
        LinearProgressIndicator()
      } else if (isTracked) Text(text = stringResource(R.string.unsubscribe))
      else Text(text = stringResource(R.string.get_notified))
    }
    Button(
      onClick = { (Constants.VLR_BASE + "team/" + id).openAsCustomTab(context) },
      modifier = modifier
        .fillMaxWidth()
        .padding(Local8DP_4DPPadding.current),
      shape = VLRTheme.shapes.small,
    ) {
      Text(text = stringResource(id = R.string.view_at_vlr), maxLines = 1)
    }
  }
}

@Composable
fun RosterCard(
  modifier: Modifier = Modifier,
  expanded: Boolean,
  data: StableHolder<List<TeamDetails.Roster>>,
  onExpand: (Boolean) -> Unit,
  onClick: (String) -> Unit,
) {
  CardView(modifier = modifier.testTag("team:roster")) {
    Column(modifier = modifier
      .fillMaxWidth()
      .animateContentSize(tween(500))) {
      if (!expanded) {
        Row(
          modifier
            .fillMaxWidth()
            .padding(Local16DP_8DPPadding.current)
            .clickable {
              onExpand(true)
            },
          horizontalArrangement = Arrangement.SpaceBetween,
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
          modifier
            .fillMaxWidth()
            .padding(Local8DPPadding.current)
            .clickable { onExpand(false) },
          horizontalArrangement = Arrangement.SpaceBetween,
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
        data.item.forEach { player ->
          Card(
            modifier =
              modifier
                .fillMaxWidth()
                .padding(Local8DP_4DPPadding.current)
                .clickable { onClick(player.id) }
                .testTag("team:player"),
            colors =
              CardDefaults.cardColors(
                contentColor = VLRTheme.colorScheme.onPrimaryContainer,
                containerColor = VLRTheme.colorScheme.primaryContainer,
              ),
          ) {
            Row(
              modifier = modifier
                .fillMaxWidth()
                .padding(Local8DP_4DPPadding.current),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(text = player.alias, style = VLRTheme.typography.titleSmall)
              Text(
                text = player.role?.replaceFirstChar { it.uppercase() } ?: "",
                style = VLRTheme.typography.labelMedium,
              )
            }
            Text(
              text = player.name ?: "",
              modifier = modifier
                .fillMaxWidth()
                .padding(Local8DP_4DPPadding.current),
              style = VLRTheme.typography.labelMedium,
            )
          }
        }
      }
    }
  }
}

@Composable
fun LazyItemScope.TeamMatchData(
  modifier: Modifier = Modifier,
  upcoming: StableHolder<List<TeamDetails.Games>>,
  completed: StableHolder<List<TeamDetails.Games>>,
  teamName: String,
  onClick: (String) -> Unit,
) {

  val tabs = listOf(stringResource(R.string.upcoming), stringResource(R.string.completed))

  val pagerState = rememberPagerState(pageCount = { tabs.size })

  Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
    VlrTabRowForViewPager(modifier = modifier, pagerState = pagerState, tabs = tabs)
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) {
      when (pagerState.currentPage) {
        0 -> {
          Column(modifier = modifier.fillParentMaxSize()) {
            if (upcoming.item.isNotEmpty())
              upcoming.item.forEach { games ->
                GameOverviewPreview(
                  modifier = modifier,
                  matchPreviewInfo = games,
                  team = teamName,
                  onClick = onClick,
                )
              }
            else NoMatchUI(modifier)
          }
        }

        1 -> {
          Column(modifier = modifier.fillMaxSize()) {
            if (completed.item.isNotEmpty())
              completed.item.forEach { games ->
                GameOverviewPreview(
                  modifier = modifier,
                  matchPreviewInfo = games,
                  team = teamName,
                  onClick = onClick,
                )
              }
            else NoMatchUI(modifier)
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
  CardView(modifier = modifier.clickable { onClick(matchPreviewInfo.id) }) {
    Column(modifier = modifier.padding(Local8DPPadding.current)) {
      Text(
        text = matchPreviewInfo.eta ?: matchPreviewInfo.date.readableDateAndTime,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.bodyMedium,
      )
      Row(
        modifier = modifier
          .fillMaxWidth()
          .padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = team,
          modifier = modifier
            .weight(1f)
            .padding(Local4DPPadding.current),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.opponent,
          modifier = modifier
            .weight(1f)
            .padding(Local4DPPadding.current),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          color = VLRTheme.colorScheme.primary,
        )
      }
      Text(
        text = matchPreviewInfo.score.ifBlank { "TBP" },
        style = VLRTheme.typography.titleSmall,
        modifier = modifier
          .fillMaxWidth()
          .padding(Local2DPPadding.current),
        textAlign = TextAlign.Center,
        color = VLRTheme.colorScheme.primary,
      )
      Text(
        text = "${matchPreviewInfo.event} - ${matchPreviewInfo.stage}",
        modifier = modifier
          .fillMaxWidth()
          .padding(Local8DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall,
      )
    }
  }
}

private fun String.toTeamTopic() = "team-$this"
