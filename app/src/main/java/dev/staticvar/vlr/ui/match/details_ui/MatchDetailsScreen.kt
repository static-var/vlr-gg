package dev.staticvar.vlr.ui.match.details_ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.SetStatusBarColor
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun NewMatchDetails(viewModel: VlrViewModel, id: String) {

  SetStatusBarColor()
  val modifier: Modifier = Modifier

  val details by remember(viewModel) { viewModel.getMatchDetails(id) }.collectAsState(Waiting())
  val trackerString = id.toMatchTopic()
  val isTracked by
    remember { viewModel.isTopicTracked(trackerString) }.collectAsStateWithLifecycle(null)

  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshMatchInfo(id) }
      .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh = rememberSwipeRefreshState(isRefreshing = updateState.get() ?: false)
  val rememberListState = rememberLazyListState()

  val context = LocalContext.current

  Column(
    modifier = modifier.fillMaxSize().testTag("matchDetails:root"),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    details
      .onPass {
        data?.let { matchInfo ->
          SwipeRefresh(
            state = swipeRefresh,
            onRefresh = { triggerRefresh = triggerRefresh.not() },
            indicator = { _, _ -> }
          ) {
            LazyColumn(modifier = modifier.fillMaxSize(), state = rememberListState) {
              item { Spacer(modifier = modifier.statusBarsPadding()) }
              item {
                AnimatedVisibility(
                  visible = updateState.get() == true || swipeRefresh.isSwipeInProgress
                ) {
                  LinearProgressIndicator(
                    modifier
                      .fillMaxWidth()
                      .padding(Local16DPPadding.current)
                      .animateContentSize()
                      .testTag("common:loader")
                  )
                }
              }
              updateState.getError()?.let {
                item { ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString()) }
              }
              item {
                MatchOverallAndEventOverview(
                  modifier = modifier,
                  detailData = matchInfo,
                  isTracked = isTracked ?: false,
                  viewModel.action.team
                ) {
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
                }
              }
              item { VideoReferenceUi(videos = matchInfo.videos) }
              if (matchInfo.matchData.isNotEmpty()) {
                item { MapBox(modifier, matchInfo) }
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
            }
              ?: LinearProgressIndicator(modifier.animateContentSize())
          }
      }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun MatchOverallAndEventOverview(
  modifier: Modifier = Modifier,
  detailData: MatchInfo,
  isTracked: Boolean,
  onClick: (String) -> Unit,
  onSubButton: suspend () -> Unit
) {
  val scope = rememberCoroutineScope()
  val notificationPermission =
    rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

  CardView(
    modifier.fillMaxWidth().aspectRatio(1.3f),
  ) {
    detailData.event.status?.let {
      MatchStatusUi(modifier = modifier, state = it, date = detailData.event.date)
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
      Row(
        modifier = modifier.size(width = maxWidth, height = maxHeight),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
      ) {
        Spacer(modifier = Modifier.weight(0.1f))
        Box(
          modifier =
            modifier.weight(1f).padding(Local8DPPadding.current).clickable {
              detailData.teams[0].id?.let(onClick)
            },
          contentAlignment = Alignment.TopEnd
        ) {
          detailData.teams[0].id?.let {
            Icon(
              Icons.Outlined.OpenInNew,
              contentDescription = stringResource(R.string.open_match_content_description),
              modifier = modifier.size(24.dp).padding(Local2DPPadding.current)
            )
          }
          GlideImage(
            imageModel = detailData.teams[0].img,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = modifier.alpha(0.2f).aspectRatio(1f, true).align(Alignment.TopCenter),
            circularReveal = CircularReveal(400),
          )
        }
        Spacer(modifier = Modifier.weight(0.2f))
        Box(
          modifier =
            modifier.weight(1f).padding(Local8DPPadding.current).clickable {
              detailData.teams[1].id?.let(onClick)
            },
          contentAlignment = Alignment.TopEnd
        ) {
          detailData.teams[1].id?.let {
            Icon(
              Icons.Outlined.OpenInNew,
              contentDescription = stringResource(R.string.open_match_content_description),
              modifier = modifier.size(24.dp).padding(Local2DPPadding.current)
            )
          }
          GlideImage(
            imageModel = detailData.teams[1].img,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterEnd,
            modifier = modifier.alpha(0.2f).aspectRatio(1f, true).align(Alignment.TopCenter),
            circularReveal = CircularReveal(400)
          )
        }
        Spacer(modifier = Modifier.weight(0.1f))
      }
      Column(
        modifier =
          modifier.size(width = maxWidth, height = maxHeight).padding(Local8DPPadding.current),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = detailData.teams[0].name,
            style = VLRTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(Local16DPPadding.current).weight(1f),
            maxLines = 2,
            color = VLRTheme.colorScheme.primary,
          )
          Text(
            text = detailData.teams[1].name,
            style = VLRTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(Local16DPPadding.current).weight(1f),
            maxLines = 2,
            color = VLRTheme.colorScheme.primary,
          )
        }
        Row(modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = detailData.teams[0].score?.toString() ?: "-",
            style = VLRTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = modifier.weight(1f),
            color = VLRTheme.colorScheme.primary,
          )
          Text(
            text = detailData.teams[1].score?.toString() ?: "-",
            style = VLRTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = modifier.weight(1f),
            color = VLRTheme.colorScheme.primary,
          )
        }
        var dialogOpen by remember { mutableStateOf(false) }
        Row(
          modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Button(
            onClick = { dialogOpen = true },
            modifier = modifier.weight(1f).testTag("details:more_info"),
          ) {
            Text(text = stringResource(R.string.more_info))
          }
          detailData.event.date?.let {
            if (!it.hasElapsed) {
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
                modifier = Modifier.weight(1f)
              ) {
                if (processingTopicSubscription) {
                  LinearProgressIndicator()
                } else if (isTracked) Text(text = stringResource(R.string.unsubscribe))
                else Text(text = stringResource(R.string.get_notified))
              }
            }
          }
        }
        val context = LocalContext.current
        Button(
          onClick = { (Constants.VLR_BASE + detailData.id).openAsCustomTab(context) },
          modifier = modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.view_at_vlr))
        }

        MatchMoreDetailsDialog(
          detailData = detailData,
          open = dialogOpen,
          onDismiss = { dialogOpen = it }
        )
      }
    }
  }
}

@Composable
fun MapStatsCard(
  modifier: Modifier = Modifier,
  mapData: MatchInfo.MatchDetailData,
  toggleState: Boolean,
  onClick: (Boolean) -> Unit
) {
  CardView(
    modifier
      .fillMaxWidth()
      .animateContentSize()
      .clickable { onClick(!toggleState) }
      .testTag("matchDetails:map")
  ) {
    Row(
      modifier.fillMaxWidth().padding(Local16DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = mapData.map,
        style = VLRTheme.typography.titleSmall,
        color = VLRTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = modifier.weight(1f)
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
        members = StableHolder(mapData.members)
      )
    }
  }
}

private fun String.toMatchTopic() = "match-$this"

@Composable
fun MapBox(modifier: Modifier = Modifier, matchInfo: MatchInfo) {
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
      contentAlignment = Alignment.CenterEnd
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
  if (overAllMapToggle) {
    matchInfo.matchData.forEach { match ->
      MapStatsCard(
        modifier = modifier.animateContentSize(),
        mapData = match,
        toggleState = toggleStateMap[match.map] ?: false,
        onClick = {
          toggleStateMap = toggleStateMap.toMutableMap().apply { set(match.map, it) }.toMap()
        }
      )
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
        infiniteRepeatable(animation = tween(1000, easing = Ease), repeatMode = RepeatMode.Reverse)
    )

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Canvas(
      modifier = modifier.size(18.dp).padding(Local2DPPadding.current),
      onDraw = { drawCircle(color = color) }
    )
    Text(
      text = stringResource(id = R.string.live),
      color = color,
      modifier = modifier.padding(Local4DPPadding.current)
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
        color = VLRTheme.colorScheme.primary
      )
    }
  }
}
