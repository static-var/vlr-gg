package dev.staticvar.vlr.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.data.api.response.PlayerData
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.SetStatusBarColor
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.match.details_ui.StatTitle
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*

@Composable
fun PlayerDetailsScreen(viewModel: VlrViewModel, id: String) {
  SetStatusBarColor()

  val playerDetails by
    remember(viewModel) { viewModel.getPlayerDetails(id) }.collectAsState(initial = Waiting())

  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }

  val updateState by
    remember(triggerRefresh) { viewModel.refreshPlayerDetails(id) }
      .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh = rememberSwipeRefreshState(isRefreshing = updateState.get() ?: false)

  val modifier: Modifier = Modifier

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    playerDetails
      .onPass {
        data?.let {
          SwipeRefresh(
            state = swipeRefresh,
            onRefresh = { triggerRefresh = triggerRefresh.not() },
            indicator = { _, _ -> }
          ) {
            LazyColumn(modifier = modifier.fillMaxSize()) {
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

              item { PlayerHeaderUi(modifier = modifier, playerData = data) }

              item {
                AgentStatViewPager(
                  modifier.testTag("matchDetails:mapStats"),
                  members = StableHolder(data.agents)
                )
              }
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
      .onWaiting { LinearProgressIndicator(modifier) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun PlayerHeaderUi(modifier: Modifier, playerData: PlayerData) {
  val shape = remember { RoundedCornerShape(50) }
  CardView(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      GlideImage(
        imageModel = playerData.img,
        modifier =
          modifier
            .size(160.dp)
            .padding(Local8DPPadding.current)
            .background(VLRTheme.colorScheme.primary, shape)
            .clip(shape),
        loading = {
          CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = VLRTheme.colorScheme.onPrimary
          )
        }
      )
      if (playerData.alias.isNotEmpty()) {
        Text(
          text = playerData.alias,
          modifier.padding(Local4DPPadding.current),
          style = VLRTheme.typography.titleMedium
        )
        Text(
          text = playerData.name,
          modifier.padding(Local4DPPadding.current),
          style = VLRTheme.typography.labelLarge
        )
      } else
        Text(
          text = playerData.name,
          modifier.padding(Local4DPPadding.current),
          style = VLRTheme.typography.titleMedium
        )
      Text(
        text = playerData.country,
        modifier.padding(Local4DPPadding.current),
        style = VLRTheme.typography.labelLarge
      )
    }
  }
}

@Composable
fun AgentStatViewPager(
  modifier: Modifier = Modifier,
  members: StableHolder<List<PlayerData.Agent>>
) {
  val pagerState = rememberPagerState()
  CardView(modifier.fillMaxWidth()) {
    ProvideTextStyle(value = VLRTheme.typography.labelMedium) {
      HorizontalPager(count = 4, modifier = modifier, state = pagerState) { page ->
        when (page) {
          0 -> AgentStatOverall(members = members, modifier = modifier)
          1 -> AgentStatKDA(members = members, modifier = modifier)
          2 -> AgentStatCombat(members = members, modifier = modifier)
          3 -> AgentStatFirstBlood(members = members, modifier = modifier)
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
fun AgentStatKDA(modifier: Modifier = Modifier, members: StableHolder<List<PlayerData.Agent>>) {

  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "KDA statistics",
      modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Agent", modifier = modifier.weight(1.5f), textAlign = TextAlign.Center)
      Text(text = "Kills", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Deaths", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Assists", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "K:D", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    members.item.forEach { member ->
      Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NameAndAgentDetail(modifier = modifier, name = member.name, img = member.img)
        Text(
          text = member.k.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.d.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.a.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = (member.kd).toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun AgentStatCombat(modifier: Modifier = Modifier, members: StableHolder<List<PlayerData.Agent>>) {
  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "Combat statistics",
      modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Agent", modifier = modifier.weight(1.5f), textAlign = TextAlign.Center)
      Text(text = "ACS", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "ADR", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "KAST", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    members.item.forEach { member ->
      Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NameAndAgentDetail(modifier = modifier, name = member.name, img = member.img)
        Text(
          text = member.acs.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.adr.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.kast.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun AgentStatFirstBlood(
  modifier: Modifier = Modifier,
  members: StableHolder<List<PlayerData.Agent>>
) {
  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "First Kill/Death statistics",
      modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Agent", modifier = modifier.weight(1.5f), textAlign = TextAlign.Center)
      Text(text = "FK", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "FD", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "FKPR", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "FDPR", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    members.item.forEach { member ->
      Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NameAndAgentDetail(modifier = modifier, name = member.name, img = member.img)
        Text(
          text = member.fk.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.fd.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.fkpr.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.fdpr.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun AgentStatOverall(modifier: Modifier = Modifier, members: StableHolder<List<PlayerData.Agent>>) {

  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "Overall statistics",
      modifier = modifier.fillMaxWidth().padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Agent", modifier = modifier.weight(1.5f), textAlign = TextAlign.Center)
      Text(text = "Usage %", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Matches", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Rounds", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "KPR", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "APR", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    members.item.forEach { member ->
      Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NameAndAgentDetail(modifier = modifier, name = member.name, img = member.img)
        Text(
          text = member.percent.toString() + " %",
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.count.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.rounds.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.kpr.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = member.apr.toString(),
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun RowScope.NameAndAgentDetail(modifier: Modifier = Modifier, name: String, img: String?) {
  Row(modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
    GlideImage(
      imageModel = img,
      modifier = modifier.padding(Local4DP_2DPPadding.current).size(24.dp),
      contentScale = ContentScale.Fit,
    )
    Text(
      text = name.replaceFirstChar { it.uppercase() },
      modifier = modifier.padding(Local2DPPadding.current),
      textAlign = TextAlign.Start
    )
  }
}
