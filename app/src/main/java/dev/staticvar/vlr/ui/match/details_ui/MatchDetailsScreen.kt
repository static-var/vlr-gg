package dev.staticvar.vlr.ui.match.details_ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.LocalColorExtractor
import dev.staticvar.vlr.ui.PaddingLocalCompositions
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.ColorExtractor
import dev.staticvar.vlr.utils.DomainVerificationStatus
import dev.staticvar.vlr.utils.DynamicTheme
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.domainVerificationStatus
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.timeDiff
import kotlinx.coroutines.tasks.await

@Composable
fun NewMatchDetails(viewModel: VlrViewModel, id: String) {

  val modifier: Modifier = Modifier

  val details by remember(id) { viewModel.getMatchDetails(id) }.collectAsState(Waiting())
  val trackerString = id.toMatchTopic()
  val isTracked by
    remember(id) { viewModel.isTopicTracked(trackerString) }.collectAsStateWithLifecycle(null)

  var triggerRefresh by remember { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh, id) { viewModel.refreshMatchInfo(id) }
      .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(
      refreshing = updateState.get() ?: false,
      { triggerRefresh = triggerRefresh.not() },
    )
  val rememberListState = rememberLazyListState()

  val context = LocalContext.current

  val progressBarVisibility by
    remember(updateState.get(), swipeRefresh.progress) {
      mutableStateOf(updateState.get() == true || swipeRefresh.progress != 0f)
    }

  Column(
    modifier = modifier.fillMaxSize().testTag("matchDetails:root"),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    details
      .onPass {
        data?.let { matchInfo ->
          AnimatedContent(targetState = progressBarVisibility, label = "progress") {
            if (it)
              Column {
                StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT)
                LinearProgressIndicator(
                  modifier
                    .fillMaxWidth()
                    .padding(Local16DPPadding.current)
                    .animateContentSize()
                    .testTag("common:loader")
                    .align(Alignment.CenterHorizontally)
                )
              }
          }

          Box(modifier = Modifier.pullRefresh(swipeRefresh).fillMaxSize()) {
            LazyColumn(modifier = modifier.fillMaxSize(), state = rememberListState) {
              item { StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT) }

              updateState.getError()?.let {
                item { ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString()) }
              }
              item {
                MatchOverallAndEventOverview(
                  modifier = modifier,
                  detailData = matchInfo,
                  onClick = viewModel.action.team,
                )
              }
              item {
                MatchInfoMoreOptions(
                  detailData = matchInfo,
                  isTracked = isTracked ?: false,
                  eventId = matchInfo.event.id,
                  onEventClick = viewModel.action.event,
                  onSubButton = {
                    when (isTracked) {
                      true -> {
                        Firebase.messaging.unsubscribeFromTopic(trackerString).await()
                        viewModel.removeTopic(trackerString)
                      }

                      false -> {
                        Firebase.messaging.subscribeToTopic(trackerString).await()
                        viewModel.trackTopic(trackerString)
                      }

                      else -> {}
                    }
                  },
                )
              }
              item { VideoReferenceUi(videos = matchInfo.videos) }
              if (matchInfo.matchData.isNotEmpty()) {
                item {
                  MapBox(modifier, matchInfo, onPlayerClick = { viewModel.action.player(it) })
                }
              }
              if (matchInfo.mapCount > matchInfo.matchData.size) {
                item {
                  Text(
                    text = stringResource(R.string.matches_tbp),
                    modifier = modifier.fillMaxWidth().padding(Local16DPPadding.current),
                    textAlign = TextAlign.Center,
                    style = VLRTheme.typography.titleSmall,
                    color = VLRTheme.colorScheme.primary,
                  )
                }
              }
              if (matchInfo.head2head.isNotEmpty()) {
                item {
                  PreviousMatches(modifier, matchInfo.head2head) { viewModel.action.match(it) }
                }
              }
              if (domainVerificationStatus(context) == DomainVerificationStatus.NOT_VERIFIED) {
                item { DomainVerificationUi(modifier) }
              }
              item { Spacer(modifier = modifier.navigationBarsPadding()) }
            }
          }
        }
          ?: kotlin.run {
            updateState.getError()?.let {
              ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
            } ?: LinearProgressIndicator(modifier.animateContentSize())
          }
      }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun MatchOverallAndEventOverview(
  modifier: Modifier = Modifier,
  detailData: MatchInfo,
  onClick: (String) -> Unit,
) {

  Column {
    detailData.event.status?.let {
      MatchStatusUi(modifier = modifier, state = it, date = detailData.event.date)
    }
    Row(modifier = modifier.fillMaxWidth()) {
      HeroScoreBox(
        modifier =
          Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp, end = 8.dp).weight(1f),
        id = detailData.teams[0].id ?: "",
        teamName = detailData.teams[0].name,
        score = detailData.teams[0].score ?: 0,
        imageUrl = detailData.teams[0].img,
        onClick = onClick,
      )
      HeroScoreBox(
        modifier =
          Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp, end = 16.dp).weight(1f),
        id = detailData.teams[1].id ?: "",
        teamName = detailData.teams[1].name,
        score = detailData.teams[1].score ?: 0,
        imageUrl = detailData.teams[1].img,
        onClick = onClick,
      )
    }
  }
}

@Composable
fun MapStatsCard(
  modifier: Modifier = Modifier,
  mapData: MatchInfo.MatchDetailData,
  toggleState: Boolean,
  onClick: (Boolean) -> Unit,
  onPlayerClick: (String) -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  CardView(
    modifier
      .fillMaxWidth()
      .animateContentSize()
      .clickable(interactionSource = interactionSource, indication = null) { onClick(!toggleState) }
      .testTag("matchDetails:map")
  ) {
    Row(
      modifier.fillMaxWidth().padding(Local16DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = mapData.map,
        style = VLRTheme.typography.titleSmall,
        color = VLRTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = modifier.weight(1f),
      )
      Icon(
        if (toggleState) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
        contentDescription = stringResource(R.string.expand),
        modifier = modifier.size(16.dp),
        tint = VLRTheme.colorScheme.primary,
      )
    }

    if (toggleState) {
      ScoreBox(mapData = mapData)
      StatViewPager(
        modifier.testTag("matchDetails:mapStats"),
        members = StableHolder(mapData.members),
        onPlayerClick,
      )
    }
  }
}

private fun String.toMatchTopic() = "match-$this"

@Composable
fun MapBox(modifier: Modifier = Modifier, matchInfo: MatchInfo, onPlayerClick: (String) -> Unit) {
  var overAllMapToggle by rememberSaveable { mutableStateOf(false) }
  var toggleStateMap by
    rememberSaveable(matchInfo, overAllMapToggle) {
      mutableStateOf(matchInfo.matchData.associate { it.map to false }.toMap())
    }
  EmphasisCardView(
    modifier =
      modifier
        .clickable { overAllMapToggle = overAllMapToggle.not() }
        .testTag("matchDetails:mapHeader")
  ) {
    Box(
      modifier = modifier.fillMaxWidth().padding(Local16DPPadding.current),
      contentAlignment = Alignment.CenterEnd,
    ) {
      Text(
        text = stringResource(R.string.maps),
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.titleSmall,
        color = VLRTheme.colorScheme.primary,
      )
      Icon(
        if (overAllMapToggle) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
        contentDescription = stringResource(R.string.expand),
        modifier = modifier.size(16.dp),
        tint = VLRTheme.colorScheme.primary,
      )
    }
  }
  AnimatedVisibility(overAllMapToggle, enter = expandVertically(), exit = shrinkVertically()) {
    Column {
      matchInfo.matchData.forEach { match ->
        MapStatsCard(
          modifier = modifier.animateContentSize(),
          mapData = match,
          toggleState = toggleStateMap[match.map] ?: false,
          onClick = {
            toggleStateMap = toggleStateMap.toMutableMap().apply { set(match.map, it) }.toMap()
          },
          onPlayerClick,
        )
      }
    }
  }
}

@Composable
fun MatchLiveUi(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition()
  val color by
    infiniteTransition.animateColor(
      initialValue = VLRTheme.colorScheme.primary,
      targetValue = Color.Transparent,
      animationSpec =
        infiniteRepeatable(animation = tween(1000, easing = Ease), repeatMode = RepeatMode.Reverse),
    )

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Canvas(
      modifier = modifier.size(18.dp).padding(Local2DPPadding.current),
      onDraw = { drawCircle(color = color) },
    )
    Text(
      text = stringResource(id = R.string.live),
      color = color,
      modifier = modifier.padding(Local4DPPadding.current),
    )
  }
}

@Composable
fun MatchStatusUi(modifier: Modifier, state: String, date: String?) {
  ProvideTextStyle(value = VLRTheme.typography.labelLarge) {
    if (state.equals(stringResource(id = R.string.live), ignoreCase = true)) MatchLiveUi(modifier)
    else {
      Text(
        text = state.uppercase() + " " + date?.timeDiff,
        modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current),
        textAlign = TextAlign.Center,
        color = VLRTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
fun HeroScoreBox(
  modifier: Modifier = Modifier,
  id: String,
  teamName: String,
  score: Int,
  imageUrl: String,
  onClick: (String) -> Unit = {},
) {
  DynamicTheme(model = imageUrl) {
    ElevatedCard(modifier = modifier.clickable { onClick(id) }) {
      Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
          model = imageUrl,
          contentDescription = teamName,
          contentScale = ContentScale.Fit,
          modifier = Modifier.padding(16.dp).aspectRatio(1f).alpha(0.3f).align(Alignment.Center),
        )
        Column(
          modifier = Modifier.fillMaxSize().align(Alignment.Center),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = teamName,
            style = VLRTheme.typography.titleSmall,
            color = VLRTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(Local8DPPadding.current),
          )
          Text(
            text = score.toString(),
            style = VLRTheme.typography.displayMedium,
            color = VLRTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Local8DPPadding.current),
          )
        }
      }
    }
  }
}

@Preview
@Composable
private fun HeroScoreBoxPreview() {
  val context = LocalContext.current
  VLRTheme() {
    CompositionLocalProvider(LocalColorExtractor provides ColorExtractor(context)) {
      PaddingLocalCompositions {
        Row(modifier = Modifier.fillMaxWidth()) {
          HeroScoreBox(
            modifier =
              Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp, end = 8.dp).weight(1f),
            id = "123",
            teamName = "Team 1",
            score = 16,
            imageUrl = "https://static.hltv.org/images/team/logo/6665",
          )
          HeroScoreBox(
            modifier =
              Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp, end = 16.dp).weight(1f),
            id = "123",
            teamName = "Team 2",
            score = 16,
            imageUrl = "https://static.hltv.org/images/team/logo/6665",
          )
        }
      }
    }
  }
}
