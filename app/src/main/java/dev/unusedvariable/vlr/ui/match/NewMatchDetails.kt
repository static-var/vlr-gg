package dev.unusedvariable.vlr.ui.match

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.statusBarsPadding
import com.skydoves.landscapist.glide.GlideImage
import dev.unusedvariable.vlr.data.api.response.MatchInfo
import dev.unusedvariable.vlr.ui.CARD_ALPHA
import dev.unusedvariable.vlr.ui.COLOR_ALPHA
import dev.unusedvariable.vlr.ui.VlrViewModel
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import dev.unusedvariable.vlr.utils.*

@Composable
fun NewMatchDetails(viewModel: VlrViewModel, id: String) {
  val details by remember(viewModel) { viewModel.getMatchInfo(id) }.collectAsState(Waiting())

  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
    Box(modifier = Modifier.statusBarsPadding())

    details
        .onPass {
          e { data.prettyPrint() }
          data?.let { matchInfo ->
            var position by remember { mutableStateOf(0) }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
              item { MatchOverallAndEventOverview(detailData = matchInfo) }
              item { VideoReferenceUi(videos = matchInfo.videos) }
              if (matchInfo.matchData.isNotEmpty()) {
                val maps = matchInfo.matchData.filter { it.map != "All Maps" }
                item { ShowMatchStatsTab(mapData = maps, position) { position = it } }
                if (maps[position].map != "TBD") {
                  item { ScoreBox(mapData = maps[position]) }
                  item { StatsHeaderBox() }
                  val teamGroupedPlayers = maps[position].members.groupBy { it.team }
                  teamGroupedPlayers.keys.forEach {
                    item {
                      Text(
                          text = it,
                          modifier =
                              Modifier.fillMaxWidth()
                                  .padding(horizontal = 8.dp)
                                  .background(
                                      VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA)),
                          textAlign = TextAlign.Center)
                    }
                    teamGroupedPlayers[it]?.let { list ->
                      items(list) { member -> StatsRow(member = member) }
                    }
                  }
                } else {
                  item {
                    Text(
                        text = "Map is yet to be played or is under progress.",
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .background(VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA))
                                .padding(32.dp),
                        textAlign = TextAlign.Center,
                        style = VLRTheme.typography.bodyLarge)
                  }
                }

                if (matchInfo.head2head.isNotEmpty()) {
                  item {
                    Card(
                        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                        contentColor = VLRTheme.colorScheme.onPrimaryContainer,
                        containerColor = VLRTheme.colorScheme.primaryContainer) {
                      Text(
                          text = "Previous Encounters",
                          modifier = Modifier.fillMaxWidth().padding(12.dp),
                          textAlign = TextAlign.Center,
                          style = VLRTheme.typography.titleSmall)
                    }
                  }
                  items(matchInfo.head2head) {
                    PreviousEncounter(
                        previousEncounter = it, onClick = { viewModel.action.match(it) })
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
fun MatchOverallAndEventOverview(detailData: MatchInfo) {
  OutlinedCard(
      Modifier.fillMaxWidth().padding(8.dp).aspectRatio(1.6f),
      contentColor = VLRTheme.colorScheme.onPrimaryContainer,
      containerColor = VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA),
      border = BorderStroke(1.dp, VLRTheme.colorScheme.primaryContainer)) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      Row(
          modifier = Modifier.size(width = maxWidth, height = maxHeight),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.weight(0.6f).alpha(0.2f).padding(16.dp)) {
          GlideImage(
              imageModel = detailData.teams[0].img,
              contentScale = ContentScale.Fit,
              alignment = Alignment.CenterStart)
        }
        Box(modifier = Modifier.weight(0.6f).alpha(0.2f).padding(16.dp)) {
          GlideImage(
              imageModel = detailData.teams[1].img,
              contentScale = ContentScale.Fit,
              alignment = Alignment.CenterEnd)
        }
      }
      Column(
          modifier = Modifier.size(width = maxWidth, height = maxHeight).padding(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = detailData.teams[0].name,
              style = VLRTheme.typography.titleSmall,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp).weight(1f))
          Text(
              text = detailData.teams[1].name,
              style = VLRTheme.typography.titleSmall,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp).weight(1f))
        }
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = detailData.teams[0].score?.toString() ?: "-",
              style = VLRTheme.typography.titleSmall,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp).weight(1f))
          Text(
              text = detailData.teams[1].score?.toString() ?: "-",
              style = VLRTheme.typography.titleSmall,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp).weight(1f))
        }
        Text(
            text = detailData.bans.joinToString { it },
            style = VLRTheme.typography.labelMedium,
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center)
        var dialogOpen by remember { mutableStateOf(false) }
        FilledTonalButton(onClick = { dialogOpen = true }, modifier = Modifier.fillMaxWidth()) {
          Text(text = "More info")
        }
        MatchMoreDetailsDialog(
            detailData = detailData, open = dialogOpen, onDismiss = { dialogOpen = it })
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
      modifier =
          Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp))) {
    mapData.forEachIndexed { index, matchDetailData ->
      Tab(selected = index == tabIndex, onClick = { onTabChange(index) }) {
        Text(text = matchDetailData.map, modifier = Modifier.padding(16.dp))
      }
    }
  }
}

@Composable
fun ScoreBox(mapData: MatchInfo.MatchDetailData) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .background(VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA)),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
      Text(
          text = mapData.teams[0].name,
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center)
      Text(
          text = mapData.teams[1].name,
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
      Text(
          text = mapData.teams[0].score.toString(),
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center)
      Text(
          text = mapData.teams[1].score.toString(),
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center)
    }
  }
}

@Composable
fun StatsHeaderBox() {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp)
              .background(VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA))) {
    Text(
        text = "Agent",
        modifier = Modifier.weight(AGENT_IMG).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = "Player",
        modifier = Modifier.weight(NAME).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = "ACS",
        modifier = Modifier.weight(ACS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = "ADR",
        modifier = Modifier.weight(ADR).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = "K",
        modifier = Modifier.weight(KILLS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = "D",
        modifier = Modifier.weight(DEATHS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = "A",
        modifier = Modifier.weight(ASSISTS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = "HS%",
        modifier = Modifier.weight(HSP).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
  }
}

@Composable
fun StatsRow(member: MatchInfo.MatchDetailData.Member) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 2.dp)
              .background(VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA)),
      verticalAlignment = Alignment.CenterVertically) {
    GlideImage(
        imageModel = member.agents.getOrNull(0)?.img,
        modifier = Modifier.weight(AGENT_IMG).padding(horizontal = 1.dp),
        alignment = Alignment.Center,
        contentScale = ContentScale.Fit)
    Text(
        text = member.name,
        modifier = Modifier.weight(NAME).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Start)
    Text(
        text = member.acs.toString(),
        modifier = Modifier.weight(ACS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = member.adr.toString(),
        modifier = Modifier.weight(ADR).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = member.kills.toString(),
        modifier = Modifier.weight(KILLS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = member.deaths.toString(),
        modifier = Modifier.weight(DEATHS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = member.assists.toString(),
        modifier = Modifier.weight(ASSISTS).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Text(
        text = member.hsPercent.toString(),
        modifier = Modifier.weight(HSP).padding(horizontal = 1.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
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
fun VideoReferenceUi(videos: MatchInfo.Videos) {
  val intent by remember { mutableStateOf(Intent(Intent.ACTION_VIEW)) }
  val context = LocalContext.current
  if (videos.streams.isEmpty() && videos.vods.isEmpty()) return

  OutlinedCard(
      Modifier.fillMaxWidth().padding(8.dp),
      contentColor = VLRTheme.colorScheme.onPrimaryContainer,
      containerColor = VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA),
      border = BorderStroke(1.dp, VLRTheme.colorScheme.primaryContainer)) {
    if (videos.streams.isNotEmpty()) {
      Text(text = "Stream", modifier = Modifier.fillMaxWidth().padding(8.dp))
      LazyRow(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        items(videos.streams) { stream ->
          FilledTonalButton(
              onClick = {
                intent.data = Uri.parse(stream.url)
                context.startActivity(intent)
              },
              modifier = Modifier.padding(horizontal = 4.dp)) { Text(text = stream.name) }
        }
      }
    }

    if (videos.vods.isNotEmpty()) {
      Text(text = "Vods", modifier = Modifier.fillMaxWidth().padding(8.dp))
      LazyRow(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        items(videos.vods) { stream ->
          FilledTonalButton(
              onClick = {
                intent.data = Uri.parse(stream.url)
                context.startActivity(intent)
              },
              modifier = Modifier.padding(horizontal = 4.dp)) { Text(text = stream.name) }
        }
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
                text = "Event Info",
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center)
          },
          text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
              detailData.event.patch?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(4.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center)
              }
              Text(
                  text = detailData.event.date,
                  modifier = Modifier.padding(8.dp).fillMaxWidth(),
                  textAlign = TextAlign.Center)
              Text(
                  text = detailData.event.stage,
                  modifier = Modifier.padding(4.dp).fillMaxWidth(),
                  textAlign = TextAlign.Center)
              Text(
                  text = detailData.event.series,
                  modifier = Modifier.padding(4.dp).fillMaxWidth(),
                  textAlign = TextAlign.Center)
            }
          },
          confirmButton = {})
}

@Composable
fun PreviousEncounter(previousEncounter: MatchInfo.Head2head, onClick: (String) -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onClick(previousEncounter.id) },
      shape = RoundedCornerShape(16.dp),
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End) {
      Icon(
          Icons.Outlined.OpenInNew,
          contentDescription = "Open match",
          modifier = Modifier.size(24.dp).padding(2.dp))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
      Text(
          text = previousEncounter.teams[0].name,
          modifier = Modifier.weight(1f).padding(2.dp),
          textAlign = TextAlign.Center)
      Text(
          text = previousEncounter.teams[1].name,
          modifier = Modifier.weight(1f).padding(2.dp),
          textAlign = TextAlign.Center)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
      Text(
          text = previousEncounter.teams[0].score?.toString() ?: "",
          modifier = Modifier.weight(1f).padding(2.dp),
          textAlign = TextAlign.Center)
      Text(
          text = previousEncounter.teams[1].score?.toString() ?: "",
          modifier = Modifier.weight(1f).padding(2.dp),
          textAlign = TextAlign.Center)
    }
  }
}
