package dev.staticvar.vlr.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.circular.CircularRevealPlugin
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.PlayerData
import dev.staticvar.vlr.data.api.response.Team
import dev.staticvar.vlr.ui.Action
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local4DP_2DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.match.details_ui.StatTitle
import dev.staticvar.vlr.ui.scrim.NavigationBarSpacer
import dev.staticvar.vlr.ui.scrim.NavigationBarType
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting

@Composable
fun PlayerDetailsScreen(viewModel: VlrViewModel, id: String) {

  val playerDetails by
  remember(viewModel) { viewModel.getPlayerDetails(id) }.collectAsState(initial = Waiting())

  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }

  val updateState by
  remember(triggerRefresh) { viewModel.refreshPlayerDetails(id) }
    .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(
      refreshing = updateState.get() ?: false,
      { triggerRefresh = triggerRefresh.not() })

  val modifier: Modifier = Modifier

  val progressBarVisibility by remember(updateState.get(), swipeRefresh.progress) {
    mutableStateOf(
      updateState.get() == true || swipeRefresh.progress != 0f
    )
  }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    playerDetails
      .onPass {
        data?.let {
          AnimatedVisibility(visible = progressBarVisibility) {
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

          Box(
            modifier = Modifier
              .pullRefresh(swipeRefresh)
              .fillMaxSize(),
          ) {
            LazyColumn(modifier = modifier.fillMaxSize()) {
              item {
                StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT)
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

              if (data.previousTeams.isNotEmpty()) {
                item {
                  Text(
                    text = stringResource(R.string.previous_team),
                    modifier =
                    modifier
                      .padding(Local16DPPadding.current)
                      .testTag("playerDetail:teams"),
                    style = VLRTheme.typography.titleMedium,
                    color = VLRTheme.colorScheme.primary
                  )
                }
                items(data.previousTeams) { PreviousTeam(team = it, action = viewModel.action) }
              }
              item { NavigationBarSpacer(navigationBarType = NavigationBarType.TRANSPARENT) }
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
      Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        GlideImage(
          imageModel = { playerData.img },
          modifier =
          modifier
            .size(120.dp)
            .padding(Local8DPPadding.current)
            .background(VLRTheme.colorScheme.primary, shape)
            .clip(shape),
          loading = {
            CircularProgressIndicator(
              modifier = Modifier
                .align(Alignment.Center)
                .testTag("player:img"),
              color = VLRTheme.colorScheme.onPrimary
            )
          }
        )
        if (playerData.currentTeam?.img != null)
          GlideImage(
            imageModel = { playerData.currentTeam.img },
            modifier =
            modifier
              .size(120.dp)
              .padding(Local8DPPadding.current)
              .background(VLRTheme.colorScheme.primary, shape)
              .padding(Local8DPPadding.current)
              .clip(shape),
            loading = {
              CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = VLRTheme.colorScheme.onPrimary
              )
            }
          )
      }

      Row(
        modifier.fillMaxWidth(),
      ) {
        Column(
          modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          if (playerData.alias.isNotEmpty()) {
            Text(
              text = playerData.alias,
              modifier.padding(Local4DPPadding.current),
              style = VLRTheme.typography.titleMedium,
              maxLines = 1
            )
            Text(
              text = playerData.name,
              modifier.padding(Local4DPPadding.current),
              style = VLRTheme.typography.labelLarge,
              maxLines = 1
            )
          } else
            Text(
              text = playerData.name,
              modifier.padding(Local4DPPadding.current),
              style = VLRTheme.typography.titleMedium,
              maxLines = 1
            )
        }

        Column(
          modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = playerData.currentTeam?.name ?: "-",
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = VLRTheme.typography.titleMedium,
            modifier = modifier.padding(Local4DPPadding.current),
          )
        }
      }

      val playerSalaryString = buildAnnotatedString {
        withStyle(style = SpanStyle(VLRTheme.colorScheme.primary)) {
          append(stringResource(id = R.string.earnings))
        }
        withStyle(style = SpanStyle(fontSize = 20.sp)) { append(" $") }
        withStyle(style = SpanStyle(VLRTheme.colorScheme.primary)) {
          append(playerData.earnings.toString())
        }
      }

      Text(
        text = playerSalaryString,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier.padding(Local4DPPadding.current)
      )
    }
  }
}

@Composable
fun AgentStatViewPager(
  modifier: Modifier = Modifier,
  members: StableHolder<List<PlayerData.Agent>>
) {
  val pagerState = rememberPagerState(pageCount = { 4 })
  CardView(modifier.fillMaxWidth()) {
    ProvideTextStyle(value = VLRTheme.typography.labelMedium) {
      HorizontalPager(modifier = modifier, state = pagerState) { page ->
        when (page) {
          0 -> AgentStatOverall(members = members, modifier = modifier)
          1 -> AgentStatKDA(members = members, modifier = modifier)
          2 -> AgentStatCombat(members = members, modifier = modifier)
          3 -> AgentStatFirstBlood(members = members, modifier = modifier)
        }
      }
    }
    Row(
      Modifier
        .fillMaxWidth()
        .align(Alignment.CenterHorizontally),
      horizontalArrangement = Arrangement.Center
    ) {
      repeat(3) { iteration ->
        val color =
          if (pagerState.currentPage == iteration) VLRTheme.colorScheme.onPrimaryContainer else VLRTheme.colorScheme.primary
        Box(
          modifier = Modifier
            .size(20.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
        )
      }
    }
  }
}

@Composable
fun AgentStatKDA(modifier: Modifier = Modifier, members: StableHolder<List<PlayerData.Agent>>) {

  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "KDA statistics",
      modifier = modifier
        .fillMaxWidth()
        .padding(Local4DPPadding.current)
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
      modifier = modifier
        .fillMaxWidth()
        .padding(Local4DPPadding.current)
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
      modifier = modifier
        .fillMaxWidth()
        .padding(Local4DPPadding.current)
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
      modifier = modifier
        .fillMaxWidth()
        .padding(Local4DPPadding.current)
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
      imageModel = { img },
      modifier = modifier
        .padding(Local4DP_2DPPadding.current)
        .size(24.dp),
      imageOptions = ImageOptions(contentScale = ContentScale.Fit)
    )
    Text(
      text = name.replaceFirstChar { it.uppercase() },
      modifier = modifier.padding(Local2DPPadding.current),
      textAlign = TextAlign.Start,
      maxLines = 1
    )
  }
}

@Composable
fun PreviousTeam(modifier: Modifier = Modifier, team: Team, action: Action) {
  val imageComponent = rememberImageComponent { add(CircularRevealPlugin()) }
  CardView(
    modifier = modifier
      .clickable { if (team.id != null) action.team(team.id) }
      .height(120.dp)
  ) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
      Text(
        text = team.name,
        style = VLRTheme.typography.titleMedium,
        modifier = modifier
          .padding(start = 24.dp, end = 24.dp)
          .align(Alignment.CenterStart),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = VLRTheme.colorScheme.primary,
      )
      GlideImage(
        imageModel = { team.img },
        imageOptions =
        ImageOptions(contentScale = ContentScale.Fit, alignment = Alignment.CenterEnd),
        modifier = modifier
          .align(Alignment.CenterEnd)
          .padding(24.dp)
          .size(120.dp),
        component = imageComponent
      )
    }
  }
}
