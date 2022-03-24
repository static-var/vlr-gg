package dev.staticvar.vlr.ui.match

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun NewMatchDetails(viewModel: VlrViewModel, id: String) {
  val details by remember(viewModel) { viewModel.getMatchInfo(id) }.collectAsState(Waiting())
  val trackerString = id.toMatchTopic()
  val isTracked by remember { viewModel.isTopicTracked(trackerString) }.collectAsState(null)
  var streamAndVodsCard by remember { mutableStateOf(false) }

  val primaryContainer = VLRTheme.colorScheme.tintedBackground
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    details
      .onPass {
        data?.let { matchInfo ->
          var position by remember { mutableStateOf(0) }

          LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { Spacer(modifier = Modifier.statusBarsPadding()) }
            item {
              MatchOverallAndEventOverview(
                detailData = matchInfo,
                isTracked = isTracked ?: false,
                viewModel.action.team
              ) {
                when (isTracked) {
                  true -> {
                    Firebase.messaging.unsubscribeFromTopic(trackerString).await()
                    viewModel.removeTopic(trackerString)
                    i { "You have unsubscribed from $trackerString" }
                  }
                  false -> {
                    Firebase.messaging.subscribeToTopic(trackerString).await()
                    viewModel.trackTopic(trackerString)
                    i { "You will be notified when the match starts $trackerString" }
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
              val maps = matchInfo.matchData.filter { it.map != "All Maps" }
              item { ShowMatchStatsTab(mapData = maps, position) { position = it } }
              if (!maps[position].map.equals("TBD", false)) {
                item { ScoreBox(mapData = maps[position]) }
                item { StatsHeaderBox() }
                val teamGroupedPlayers = maps[position].members.groupBy { it.team }
                teamGroupedPlayers.keys.forEach {
                  item {
                    Text(
                      text = it,
                      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                      textAlign = TextAlign.Center
                    )
                  }
                  teamGroupedPlayers[it]?.let { list ->
                    items(list) { member -> StatsRow(member = member) }
                  }
                }
              } else {
                item {
                  Text(
                    text = stringResource(R.string.map_tbp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(32.dp),
                    textAlign = TextAlign.Center,
                    style = VLRTheme.typography.bodyLarge
                  )
                }
              }

              if (matchInfo.head2head.isNotEmpty()) {
                item {
                  EmphasisCardView() {
                    Text(
                      text = stringResource(R.string.previous_encounter),
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      textAlign = TextAlign.Center,
                      style = VLRTheme.typography.titleSmall,
                      color = VLRTheme.colorScheme.primary,
                    )
                  }
                }
                items(matchInfo.head2head) {
                  PreviousEncounter(
                    previousEncounter = it,
                    onClick = { viewModel.action.match(it) }
                  )
                }
              }
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
          }
        }
      }
      .onWaiting { LinearProgressIndicator() }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun MatchOverallAndEventOverview(
  detailData: MatchInfo,
  isTracked: Boolean,
  onClick: (String) -> Unit,
  onSubButton: suspend () -> Unit
) {
  val scope = rememberCoroutineScope()
  CardView(
    Modifier.fillMaxWidth().aspectRatio(1.8f),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.size(width = maxWidth, height = maxHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Box(
          modifier =
            Modifier.weight(0.6f).padding(8.dp).clickable { detailData.teams[0].id?.let(onClick) }
        ) {
          detailData.teams[0].id?.let {
            Row(
              Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.Top
            ) {
              Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = stringResource(R.string.open_match_content_description),
                modifier = Modifier.size(24.dp).padding(2.dp)
              )
            }
          }
          GlideImage(
            imageModel = detailData.teams[0].img,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = Modifier.alpha(0.2f),
            circularReveal = CircularReveal(1000),
          )
        }
        Box(
          modifier =
            Modifier.weight(0.6f).padding(8.dp).clickable { detailData.teams[1].id?.let(onClick) }
        ) {
          detailData.teams[1].id?.let {
            Row(
              Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.Top
            ) {
              Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = stringResource(R.string.open_match_content_description),
                modifier = Modifier.size(24.dp).padding(2.dp)
              )
            }
          }
          GlideImage(
            imageModel = detailData.teams[1].img,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.alpha(0.2f),
            circularReveal = CircularReveal(1000)
          )
        }
      }
      Column(
        modifier = Modifier.size(width = maxWidth, height = maxHeight).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = detailData.teams[0].name,
            style = VLRTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp).weight(1f),
            maxLines = 2,
            color = VLRTheme.colorScheme.primary,
          )
          Text(
            text = detailData.teams[1].name,
            style = VLRTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp).weight(1f),
            maxLines = 2,
            color = VLRTheme.colorScheme.primary,
          )
        }
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = detailData.teams[0].score?.toString() ?: "-",
            style = VLRTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
            color = VLRTheme.colorScheme.primary,
          )
          Text(
            text = detailData.teams[1].score?.toString() ?: "-",
            style = VLRTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
            color = VLRTheme.colorScheme.primary,
          )
        }
        var dialogOpen by remember { mutableStateOf(false) }
        Row(
          Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Button(
            onClick = { dialogOpen = true },
            modifier = Modifier.weight(1f),
          ) { Text(text = stringResource(R.string.more_info)) }
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
fun ShowMatchStatsTab(
  mapData: List<MatchInfo.MatchDetailData>,
  tabIndex: Int,
  onTabChange: (Int) -> Unit
) {
  ScrollableTabRow(
    selectedTabIndex = tabIndex,
    containerColor = VLRTheme.colorScheme.primaryContainer,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)),
    indicator = { indicators -> VLRTabIndicator(indicators, tabIndex) }
  ) {
    mapData.forEachIndexed { index, matchDetailData ->
      Tab(selected = index == tabIndex, onClick = { onTabChange(index) }) {
        Text(text = matchDetailData.map, modifier = Modifier.padding(16.dp))
      }
    }
  }
}

@Composable
fun ScoreBox(mapData: MatchInfo.MatchDetailData) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(2.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = mapData.teams[0].name,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      Text(
        text = mapData.teams[1].name,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(2.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = mapData.teams[0].score.toString(),
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      Text(
        text = mapData.teams[1].score.toString(),
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun StatsHeaderBox() {
  Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    Text(
      text = stringResource(R.string.agent),
      modifier = Modifier.weight(AGENT_IMG).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = stringResource(R.string.player),
      modifier = Modifier.weight(NAME).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = stringResource(R.string.acs),
      modifier = Modifier.weight(ACS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = stringResource(R.string.adr),
      modifier = Modifier.weight(ADR).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = stringResource(R.string.kill),
      modifier = Modifier.weight(KILLS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = stringResource(R.string.death),
      modifier = Modifier.weight(DEATHS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = stringResource(R.string.assist),
      modifier = Modifier.weight(ASSISTS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = stringResource(R.string.headshot_percentage),
      modifier = Modifier.weight(HSP).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
fun StatsRow(member: MatchInfo.MatchDetailData.Member) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp).animateContentSize(tween(400)),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    GlideImage(
      imageModel = member.agents.getOrNull(0)?.img,
      modifier = Modifier.weight(AGENT_IMG).padding(horizontal = 1.dp),
      alignment = Alignment.Center,
      contentScale = ContentScale.Fit,
      circularReveal = CircularReveal(1000)
    )
    Text(
      text = member.name,
      modifier = Modifier.weight(NAME).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Start
    )
    Text(
      text = member.acs.toString(),
      modifier = Modifier.weight(ACS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = member.adr.toString(),
      modifier = Modifier.weight(ADR).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = member.kills.toString(),
      modifier = Modifier.weight(KILLS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = member.deaths.toString(),
      modifier = Modifier.weight(DEATHS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = member.assists.toString(),
      modifier = Modifier.weight(ASSISTS).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = member.hsPercent.toString(),
      modifier = Modifier.weight(HSP).padding(horizontal = 1.dp),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
  }
}

const val AGENT_IMG = 0.15f
const val NAME = 0.44f
const val ACS = 0.15f
const val ADR = 0.15f
const val KILLS = 0.12f
const val DEATHS = 0.12f
const val ASSISTS = 0.12f
const val HSP = 0.15f

@Composable
fun VideoReferenceUi(videos: MatchInfo.Videos, expand: Boolean, onClick: (Boolean) -> Unit) {
  val intent by remember { mutableStateOf(Intent(Intent.ACTION_VIEW)) }
  val context = LocalContext.current
  if (videos.streams.isEmpty() && videos.vods.isEmpty()) return

  EmphasisCardView(
    Modifier.animateContentSize(tween(500)),
  ) {
    if (expand) {
      Row(
        Modifier.fillMaxWidth().padding(8.dp).clickable { onClick(false) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.streams_and_vods),
          style = VLRTheme.typography.titleSmall,
          color = VLRTheme.colorScheme.primary,
        )
        Icon(
          Icons.Outlined.ArrowUpward,
          contentDescription = stringResource(R.string.expand),
          modifier = Modifier.size(16.dp),
          tint = VLRTheme.colorScheme.primary,
        )
      }
      if (videos.streams.isNotEmpty()) {
        Text(text = "Stream", modifier = Modifier.fillMaxWidth().padding(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
          items(videos.streams) { stream ->
            Button(
              onClick = {
                intent.data = Uri.parse(stream.url)
                context.startActivity(intent)
              },
              modifier = Modifier.padding(horizontal = 4.dp)
            ) { Text(text = stream.name) }
          }
        }
      }

      if (videos.vods.isNotEmpty()) {
        Text(text = stringResource(R.string.vods), modifier = Modifier.fillMaxWidth().padding(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
          items(videos.vods) { stream ->
            Button(
              onClick = {
                intent.data = Uri.parse(stream.url)
                context.startActivity(intent)
              },
              modifier = Modifier.padding(horizontal = 4.dp)
            ) { Text(text = stream.name) }
          }
        }
      }
    } else {
      Row(
        Modifier.fillMaxWidth().padding(8.dp).clickable { onClick(true) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.streams_and_vods),
          style = VLRTheme.typography.titleSmall,
          color = VLRTheme.colorScheme.primary,
        )
        Icon(
          Icons.Outlined.ArrowDownward,
          contentDescription = stringResource(R.string.expand),
          modifier = Modifier.size(16.dp),
          tint = VLRTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
fun MatchMoreDetailsDialog(detailData: MatchInfo, open: Boolean, onDismiss: (Boolean) -> Unit) {
  if (open)
    AlertDialog(
      onDismissRequest = { onDismiss(false) },
      title = {
        Text(
          text = stringResource(R.string.event_info),
          modifier = Modifier.padding(8.dp),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.titleMedium
        )
      },
      text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
          detailData.bans.takeIf { it.isNotEmpty() }?.let {
            Text(
              text = detailData.bans.joinToString { it },
              modifier = Modifier.padding(8.dp),
              textAlign = TextAlign.Center
            )
          }
          detailData.event.patch?.let {
            Text(
              text = it,
              modifier = Modifier.padding(4.dp).fillMaxWidth(),
              textAlign = TextAlign.Center
            )
          }
          Text(
            text = detailData.event.date?.readableDateAndTime ?: "",
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Text(
            text = detailData.event.stage,
            modifier = Modifier.padding(4.dp).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Text(
            text = detailData.event.series,
            modifier = Modifier.padding(4.dp).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
        }
      },
      confirmButton = {}
    )
}

@Composable
fun PreviousEncounter(previousEncounter: MatchInfo.Head2head, onClick: (String) -> Unit) {
  CardView(modifier = Modifier.clickable { onClick(previousEncounter.id) }) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End
    ) {
      Icon(
        Icons.Outlined.OpenInNew,
        contentDescription = stringResource(R.string.open_match_content_description),
        modifier = Modifier.size(24.dp).padding(2.dp)
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = previousEncounter.teams[0].name,
        modifier = Modifier.weight(1f).padding(2.dp),
        textAlign = TextAlign.Center
      )
      Text(
        text = previousEncounter.teams[1].name,
        modifier = Modifier.weight(1f).padding(2.dp),
        textAlign = TextAlign.Center
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = previousEncounter.teams[0].score?.toString() ?: "",
        modifier = Modifier.weight(1f).padding(2.dp),
        textAlign = TextAlign.Center
      )
      Text(
        text = previousEncounter.teams[1].score?.toString() ?: "",
        modifier = Modifier.weight(1f).padding(2.dp),
        textAlign = TextAlign.Center
      )
    }
  }
}

fun String.toMatchTopic() = "match-$this"
