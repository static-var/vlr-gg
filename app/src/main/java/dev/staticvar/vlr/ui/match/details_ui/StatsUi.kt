package dev.staticvar.vlr.ui.match.details_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local4DP_2DPPadding
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.StableHolder

@Composable
fun StatViewPager(
  modifier: Modifier = Modifier,
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>,
  onClick: (String) -> Unit
) {
  val pagerState = rememberPagerState(pageCount = { 3 })
  Column(
    modifier
      .fillMaxWidth()
      .padding(Local4DPPadding.current)
  ) {
    ProvideTextStyle(value = VLRTheme.typography.labelMedium) {
      HorizontalPager(modifier = modifier, state = pagerState) { page ->
        when (page) {
          0 -> StatKDA(members = members, modifier = modifier, onClick = onClick)
          1 -> StatCombat(members = members, modifier = modifier, onClick = onClick)
          2 -> StatFirstBlood(members = members, modifier = modifier, onClick = onClick)
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
fun StatKDA(
  modifier: Modifier = Modifier,
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>,
  onClick: (String) -> Unit
) {
  val teamAndMember = remember(members) { members.item.groupBy { it.team } }

  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "KDA statistics",
      modifier = modifier
        .fillMaxWidth()
        .padding(Local4DPPadding.current)
    )
    Row(modifier = modifier.fillMaxWidth()) {
      Text(text = "Players", modifier = modifier.weight(2f), textAlign = TextAlign.Center)
      Text(text = "Rating", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Kills", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Deaths", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "Assists", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
      Text(text = "+/-", modifier = modifier.weight(1f), textAlign = TextAlign.Center)
    }
    teamAndMember.forEach { (team, member) ->
      TeamName(team = team)
      member.forEach { player ->
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          PlayerNameAndAgentDetailWithHyperlink(
            modifier = modifier.weight(2f),
            name = player.name,
            img = player.agents.getOrNull(0)?.img,
            id = player.playerId,
            onClick = onClick
          )
          Text(
            text = player.rating.toString(),
            modifier = modifier.weight(1f),
            textAlign = TextAlign.Center
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
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>,
  onClick: (String) -> Unit
) {
  val teamAndMember = remember(members) { members.item.groupBy { it.team } }
  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "Combat statistics",
      modifier = modifier
        .fillMaxWidth()
        .padding(Local4DPPadding.current)
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
          PlayerNameAndAgentDetailWithHyperlink(
            modifier = modifier,
            name = player.name,
            img = player.agents.getOrNull(0)?.img,
            id = player.playerId,
            onClick = onClick
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
  members: StableHolder<List<MatchInfo.MatchDetailData.Member>>,
  onClick: (String) -> Unit
) {
  val teamAndMember = remember(members) { members.item.groupBy { it.team } }
  Column(modifier = modifier.fillMaxWidth()) {
    StatTitle(
      text = "First Kill/Death statistics",
      modifier = modifier
        .fillMaxWidth()
        .padding(Local4DPPadding.current)
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
          PlayerNameAndAgentDetailWithHyperlink(
            modifier = modifier,
            name = player.name,
            img = player.agents.getOrNull(0)?.img,
            id = player.playerId,
            onClick = onClick
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
fun RowScope.PlayerNameAndAgentDetailWithHyperlink(
  modifier: Modifier = Modifier,
  name: String,
  img: String?,
  id: String,
  onClick: (String) -> Unit
) {
  Row(
    modifier
      .weight(1.5f)
      .clickable(id.isNotEmpty()) {
        onClick(id)
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlideImage(
      imageModel = { img },
      modifier = modifier
        .padding(Local4DP_2DPPadding.current)
        .size(24.dp),
      imageOptions = ImageOptions(
        contentScale = ContentScale.Fit,
      )
    )
    Text(
      text = name,
      modifier = modifier.padding(Local2DPPadding.current),
      textAlign = TextAlign.Start,
      textDecoration = if (id.isNotEmpty()) TextDecoration.Underline else TextDecoration.None
    )
  }
}

@Composable
fun TeamName(team: String, modifier: Modifier = Modifier) {
  Text(
    text = team,
    modifier = modifier
      .fillMaxWidth()
      .padding(Local4DPPadding.current),
    textAlign = TextAlign.Center,
    style = VLRTheme.typography.bodySmall,
    color = VLRTheme.colorScheme.primary,
  )
}

@Composable
fun StatTitle(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier
      .fillMaxWidth()
      .padding(Local4DPPadding.current),
    textAlign = TextAlign.Center,
    style = VLRTheme.typography.bodyMedium,
    color = VLRTheme.colorScheme.primary,
  )
}
