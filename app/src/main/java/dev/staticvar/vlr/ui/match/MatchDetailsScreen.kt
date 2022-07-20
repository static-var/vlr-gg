package dev.staticvar.vlr.ui.match

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun NewMatchDetails(viewModel: VlrViewModel, id: String) {
  val details by remember(viewModel) { viewModel.getMatchDetails(id) }.collectAsState(Waiting())
  val trackerString = id.toMatchTopic()
  val isTracked by remember { viewModel.isTopicTracked(trackerString) }.collectAsStateWithLifecycle(null)
  var streamAndVodsCard by remember { mutableStateOf(false) }

  val primaryContainer = VLRTheme.colorScheme.surface.copy(0.2f)
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }

  val modifier: Modifier = Modifier

  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshMatchInfo(id) }.collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh = rememberSwipeRefreshState(isRefreshing = updateState.get() ?: false)

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    details
      .onPass {
        data?.let { matchInfo ->
          val maps by
            remember(matchInfo) {
              mutableStateOf(matchInfo.matchData.filter { it.map != "All Maps" })
            }
          var toggleStateMap by
            remember(maps, matchInfo) { mutableStateOf(maps.associate { it.map to false }.toMap()) }

          val rememberListState = rememberLazyListState()
          var upcomingMatchToggle by remember { mutableStateOf(false) }
          var overAllMapToggle by remember { mutableStateOf(false) }

          SwipeRefresh(
            state = swipeRefresh,
            onRefresh = { triggerRefresh = triggerRefresh.not() },
            indicator = { _, _ -> }
          ) {
            LazyColumn(modifier = modifier.fillMaxSize().testTag("matchDetails:root"), state = rememberListState) {
              item { Spacer(modifier = modifier.statusBarsPadding()) }
              item {
                AnimatedVisibility(
                  visible = updateState.get() == true || swipeRefresh.isSwipeInProgress
                ) {
                  LinearProgressIndicator(
                    modifier.fillMaxWidth().padding(Local16DPPadding.current).animateContentSize().testTag("common:loader")
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
              item {
                VideoReferenceUi(
                  videos = matchInfo.videos,
                  expand = streamAndVodsCard,
                  onClick = { streamAndVodsCard = it }
                )
              }
              if (matchInfo.matchData.isNotEmpty()) {
                item {
                  EmphasisCardView(
                    modifier = modifier.clickable { overAllMapToggle = overAllMapToggle.not() }.testTag("matchDetails:mapHeader")
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
                        if (overAllMapToggle) Icons.Outlined.ArrowUpward
                        else Icons.Outlined.ArrowDownward,
                        contentDescription = stringResource(R.string.expand),
                        modifier = modifier.size(16.dp),
                        tint = VLRTheme.colorScheme.primary,
                      )
                    }
                  }
                }
                if (overAllMapToggle)
                  maps.forEach { match ->
                    item {
                      MapStatsCard(
                        mapData = match,
                        toggleState = toggleStateMap[match.map] ?: false,
                        onClick = {
                          toggleStateMap =
                            toggleStateMap.toMutableMap().apply { set(match.map, it) }.toMap()
                        }
                      )
                    }
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
                  EmphasisCardView(
                    modifier =
                      modifier.clickable { upcomingMatchToggle = upcomingMatchToggle.not() }
                  ) {
                    Box(
                      modifier = modifier.fillMaxWidth().padding(Local16DPPadding.current),
                      contentAlignment = Alignment.CenterEnd
                    ) {
                      Text(
                        text = stringResource(R.string.previous_encounter),
                        modifier = modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = VLRTheme.typography.titleSmall,
                        color = VLRTheme.colorScheme.primary,
                      )
                      Icon(
                        if (upcomingMatchToggle) Icons.Outlined.ArrowUpward
                        else Icons.Outlined.ArrowDownward,
                        contentDescription = stringResource(R.string.expand),
                        modifier = modifier.size(16.dp),
                        tint = VLRTheme.colorScheme.primary,
                      )
                    }
                  }
                }
                if (upcomingMatchToggle)
                  items(matchInfo.head2head, key = { item -> item.id }) {
                    PreviousEncounter(
                      modifier = modifier,
                      previousEncounter = it,
                      onClick = { viewModel.action.match(it) }
                    )
                  }
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
  CardView(
    modifier.fillMaxWidth().aspectRatio(1.8f),
  ) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
      Row(
        modifier = modifier.size(width = maxWidth, height = maxHeight),
        verticalAlignment = Alignment.CenterVertically,
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
            modifier = modifier.alpha(0.2f),
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
            modifier = modifier.alpha(0.2f),
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
                  if (!processingTopicSubscription) {
                    processingTopicSubscription = true
                    scope.launch(Dispatchers.IO) {
                      onSubButton()
                      processingTopicSubscription = false
                    }
                  }
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
fun ScoreBox(modifier: Modifier = Modifier, mapData: MatchInfo.MatchDetailData) {
  Column(modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current)) {
    Row(
      modifier = modifier.fillMaxWidth().padding(Local2DPPadding.current),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = mapData.teams[0].name,
        modifier = modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      Text(
        text = mapData.teams[1].name,
        modifier = modifier.weight(1f),
        textAlign = TextAlign.Center
      )
    }
    Row(
      modifier = modifier.fillMaxWidth().padding(Local2DPPadding.current),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = mapData.teams[0].score.toString(),
        modifier = modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      Text(
        text = mapData.teams[1].score.toString(),
        modifier = modifier.weight(1f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun VideoReferenceUi(
  modifier: Modifier = Modifier,
  videos: MatchInfo.Videos,
  expand: Boolean,
  onClick: (Boolean) -> Unit
) {
  val intent by remember { mutableStateOf(Intent(Intent.ACTION_VIEW)) }
  val context = LocalContext.current
  if (videos.streams.isEmpty() && videos.vods.isEmpty()) return

  EmphasisCardView(
    modifier.animateContentSize(tween(500)),
  ) {
    if (expand) {
      Row(
        modifier.fillMaxWidth().padding(Local16DPPadding.current).clickable { onClick(false) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.streams_and_vods),
          style = VLRTheme.typography.titleSmall,
          color = VLRTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = modifier.weight(1f)
        )
        Icon(
          Icons.Outlined.ArrowUpward,
          contentDescription = stringResource(R.string.expand),
          modifier = modifier.size(16.dp),
          tint = VLRTheme.colorScheme.primary,
        )
      }
      if (videos.streams.isNotEmpty()) {
        Text(text = "Stream", modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current))
        LazyRow(modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)) {
          items(videos.streams) { stream ->
            Button(
              onClick = {
                intent.data = Uri.parse(stream.url)
                context.startActivity(intent)
              },
              modifier = modifier.padding(Local4DPPadding.current)
            ) { Text(text = stream.name) }
          }
        }
      }

      if (videos.vods.isNotEmpty()) {
        Text(
          text = stringResource(R.string.vods),
          modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current)
        )
        LazyRow(modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)) {
          items(videos.vods) { stream ->
            Button(
              onClick = {
                intent.data = Uri.parse(stream.url)
                context.startActivity(intent)
              },
              modifier = modifier.padding(Local4DPPadding.current)
            ) { Text(text = stream.name) }
          }
        }
      }
    } else {
      Row(
        modifier.fillMaxWidth().padding(Local16DPPadding.current).clickable { onClick(true) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.streams_and_vods),
          style = VLRTheme.typography.titleSmall,
          color = VLRTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = modifier.weight(1f)
        )
        Icon(
          Icons.Outlined.ArrowDownward,
          contentDescription = stringResource(R.string.expand),
          modifier = modifier.size(16.dp),
          tint = VLRTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
fun MatchMoreDetailsDialog(
  modifier: Modifier = Modifier,
  detailData: MatchInfo,
  open: Boolean,
  onDismiss: (Boolean) -> Unit
) {
  if (open)
    AlertDialog(
      onDismissRequest = { onDismiss(false) },
      title = {
        Text(
          text = stringResource(R.string.event_info),
          modifier = modifier.padding(Local8DPPadding.current),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.titleMedium
        )
      },
      text = {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
          detailData.bans
            .takeIf { it.isNotEmpty() }
            ?.let {
              Text(
                text = detailData.bans.joinToString { it },
                modifier = modifier.padding(Local8DPPadding.current),
                textAlign = TextAlign.Center
              )
            }
          detailData.event.patch?.let {
            Text(
              text = it,
              modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
              textAlign = TextAlign.Center
            )
          }
          Text(
            text = detailData.event.date?.readableDateAndTimeWithZone ?: "",
            modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Text(
            text = detailData.event.stage,
            modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Text(
            text = detailData.event.series,
            modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
        }
      },
      confirmButton = {}
    )
}

@Composable
fun PreviousEncounter(
  modifier: Modifier = Modifier,
  previousEncounter: MatchInfo.Head2head,
  onClick: (String) -> Unit
) {
  CardView(modifier = modifier.clickable { onClick(previousEncounter.id) }) {
    Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End
    ) {
      Icon(
        Icons.Outlined.OpenInNew,
        contentDescription = stringResource(R.string.open_match_content_description),
        modifier = modifier.size(24.dp).padding(Local2DPPadding.current)
      )
    }
    Row(
      modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = previousEncounter.teams[0].name,
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
      Text(
        text = previousEncounter.teams[1].name,
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
    }
    Row(
      modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = previousEncounter.teams[0].score?.toString() ?: "",
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
      Text(
        text = previousEncounter.teams[1].score?.toString() ?: "",
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
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
  CardView(modifier.fillMaxWidth().animateContentSize().clickable { onClick(!toggleState) }.testTag("matchDetails:map")) {
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
      StatViewPager(modifier.testTag("matchDetails:mapStats"), members = StableHolder(mapData.members))
    }
  }
}

@Composable
fun StatViewPager(
  modifier: Modifier = Modifier,
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>
) {
  val pagerState = rememberPagerState()
  Column(modifier.fillMaxWidth()) {
    ProvideTextStyle(value = VLRTheme.typography.labelMedium) {
      HorizontalPager(count = 3, modifier = modifier, state = pagerState) { page ->
        when (page) {
          0 -> StatKDA(members = members, modifier = modifier)
          1 -> StatCombat(members = members, modifier = modifier)
          2 -> StatFirstBlood(members = members, modifier = modifier)
        }
      }
    }
    HorizontalPagerIndicator(
      pagerState = pagerState,
      modifier = modifier.padding(Local4DPPadding.current).align(Alignment.CenterHorizontally),
      activeColor = VLRTheme.colorScheme.onPrimaryContainer,
      inactiveColor = VLRTheme.colorScheme.primary,
    )
  }
}

@Composable
fun StatKDA(
  modifier: Modifier = Modifier,
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>
) {
  val teamAndMember = remember(members) { members.item.groupBy { it.team } }

  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "KDA statistics",
      modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Players", modifier = modifier.weight(1.5f), textAlign = TextAlign.Center)
      Text(text = "Kills", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Deaths", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Assists", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "+/-", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    teamAndMember.forEach { (team, member) ->
      TeamName(team = team)
      member.forEach { player ->
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          PlayerNameAndAgentDetail(
            modifier = modifier,
            name = player.name,
            img = player.agents.getOrNull(0)?.img
          )
          Text(
            text = player.kills.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = player.deaths.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = player.assists.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = (player.kills - player.deaths).toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
fun StatCombat(
  modifier: Modifier = Modifier,
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>
) {
  val teamAndMember = remember(members) { members.item.groupBy { it.team } }
  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "Combat statistics",
      modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Players", modifier = modifier.weight(1.5f), textAlign = TextAlign.Center)
      Text(text = "ACS", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "ADR", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "KAST", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "HS%", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    teamAndMember.forEach { (team, member) ->
      TeamName(team = team)
      member.forEach { player ->
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          PlayerNameAndAgentDetail(
            modifier = modifier,
            name = player.name,
            img = player.agents.getOrNull(0)?.img
          )
          Text(
            text = player.acs.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = player.adr.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = player.kast.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = player.hsPercent.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
fun StatFirstBlood(
  modifier: Modifier = Modifier,
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>
) {
  val teamAndMember = remember(members) { members.item.groupBy { it.team } }
  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "First Kill/Death statistics",
      modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Players", modifier = modifier.weight(1.5f), textAlign = TextAlign.Center)
      Text(text = "FK", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "FD", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "+/-", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    teamAndMember.forEach { (team, member) ->
      TeamName(team = team)
      member.forEach { player ->
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          PlayerNameAndAgentDetail(
            modifier = modifier,
            name = player.name,
            img = player.agents.getOrNull(0)?.img
          )
          Text(
            text = player.firstKills.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = player.firstDeaths.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
          Text(
            text = player.firstKillsDiff.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
fun RowScope.PlayerNameAndAgentDetail(modifier: Modifier = Modifier, name: String, img: String?) {
  Row(modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
    GlideImage(
      imageModel = img,
      modifier = modifier.padding(Local4DP_2DPPadding.current).size(24.dp),
      contentScale = ContentScale.Fit,
    )
    Text(
      text = name,
      modifier = modifier.padding(Local2DPPadding.current),
      textAlign = TextAlign.Start
    )
  }
}

@Composable
fun TeamName(team: String, modifier: Modifier = Modifier) {
  Text(
    text = team,
    modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current),
    textAlign = TextAlign.Center,
    style = VLRTheme.typography.bodySmall,
    color = VLRTheme.colorScheme.primary,
  )
}

@Composable
fun StatTitle(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current),
    textAlign = TextAlign.Center,
    style = VLRTheme.typography.bodyMedium,
    color = VLRTheme.colorScheme.primary,
  )
}

fun String.toMatchTopic() = "match-$this"
