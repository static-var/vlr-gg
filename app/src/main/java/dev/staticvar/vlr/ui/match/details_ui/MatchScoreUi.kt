package dev.staticvar.vlr.ui.match.details_ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local8DP_4DPPadding
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun ScoreBox(modifier: Modifier = Modifier, mapData: MatchInfo.MatchDetailData) {
  Column(modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current)) {
    Row(
      modifier = modifier.fillMaxWidth().padding(Local2DPPadding.current),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val annotatedTeam1String = buildAnnotatedString {
        appendInlineContent(id = "team1Color")
        withStyle(style = SpanStyle(VLRTheme.colorScheme.primary)) { append(mapData.teams[0].name) }
      }
      val inlineTeam1ContentMap =
        mapOf(
          "team1Color" to
            InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)) {
              val color = VLRTheme.colorScheme.primary
              Canvas(
                modifier = modifier.size(18.dp).padding(Local2DPPadding.current),
                onDraw = { drawCircle(color = color) }
              )
            }
        )
      val annotatedTeam2String = buildAnnotatedString {
        appendInlineContent(id = "team2Color")
        withStyle(style = SpanStyle(VLRTheme.colorScheme.tertiary)) {
          append(mapData.teams[1].name)
        }
      }
      val inlineTeam2ContentMap =
        mapOf(
          "team2Color" to
            InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)) {
              val color = VLRTheme.colorScheme.tertiary
              Canvas(
                modifier = modifier.size(18.dp).padding(Local2DPPadding.current),
                onDraw = { drawCircle(color = color) }
              )
            }
        )
      Text(
        text = annotatedTeam1String,
        inlineContent = inlineTeam1ContentMap,
        modifier = modifier.weight(1f),
        textAlign = TextAlign.Center,
      )
      Text(
        text = annotatedTeam2String,
        inlineContent = inlineTeam2ContentMap,
        modifier = modifier.weight(1f),
        textAlign = TextAlign.Center,
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
        textAlign = TextAlign.Center,
        color = VLRTheme.colorScheme.primary
      )
      Text(
        text = mapData.teams[1].score.toString(),
        modifier = modifier.weight(1f),
        textAlign = TextAlign.Center,
        color = VLRTheme.colorScheme.tertiary
      )
    }
    val rounds by
      produceState(initialValue = listOf(), mapData) {
        value =
          mapData.rounds.filter { it.winType != MatchInfo.MatchDetailData.Rounds.WinType.NotPlayed }
      }
    if (rounds.isNotEmpty()) RoundByRoundRow(modifier = modifier, rounds = rounds)
  }
}

@Composable
fun RoundByRoundRow(modifier: Modifier, rounds: List<MatchInfo.MatchDetailData.Rounds>) {
  LazyRow(modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
    items(rounds, key = { item -> item.roundNo }) { RoundBox(modifier = modifier, round = it) }
  }
}

@Composable
fun RoundBox(modifier: Modifier, round: MatchInfo.MatchDetailData.Rounds) {
  val drawable =
    painterResource(
      id =
        when (round.winType) {
          MatchInfo.MatchDetailData.Rounds.WinType.Elimination -> R.drawable.elim
          MatchInfo.MatchDetailData.Rounds.WinType.SpikeExploded -> R.drawable.boom
          MatchInfo.MatchDetailData.Rounds.WinType.Defused -> R.drawable.defuse
          else -> R.drawable.time
        }
    )
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = round.roundNo.toString(),
      color =
        if (round.winner == MatchInfo.MatchDetailData.Rounds.Winner.TEAM1)
          VLRTheme.colorScheme.primary
        else VLRTheme.colorScheme.tertiary
    )
    Image(
      painter = drawable,
      contentDescription = round.winner.name,
      modifier =
        modifier
          .size(24.dp)
          .background(
            if (round.winner == MatchInfo.MatchDetailData.Rounds.Winner.TEAM1)
              VLRTheme.colorScheme.primary
            else VLRTheme.colorScheme.tertiary
          ),
    )
  }
}
