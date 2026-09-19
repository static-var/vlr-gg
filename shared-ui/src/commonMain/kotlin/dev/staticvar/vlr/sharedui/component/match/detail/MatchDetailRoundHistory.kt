/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.RoundWinType
import dev.staticvar.vlr.domain.model.RoundWinner
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_tbd
import vlr.shared_ui.generated.resources.match_event_win_type_defused
import vlr.shared_ui.generated.resources.match_event_win_type_elimination
import vlr.shared_ui.generated.resources.match_event_win_type_not_played
import vlr.shared_ui.generated.resources.match_event_win_type_spike_exploded
import vlr.shared_ui.generated.resources.match_event_win_type_time_out
import vlr.shared_ui.generated.resources.round_defuse
import vlr.shared_ui.generated.resources.round_elimination
import vlr.shared_ui.generated.resources.round_explosion
import vlr.shared_ui.generated.resources.round_result_description
import vlr.shared_ui.generated.resources.round_timeout

@Composable
internal fun MatchDetailRoundHistory(map: MapData) {
  val rounds = remember(map.rounds) {
    map.rounds.filter { it.winType != RoundWinType.NOT_PLAYED && it.winner != RoundWinner.NOT_PLAYED }
      .sortedBy { it.roundNo }
  }
  if (rounds.isEmpty()) return
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      items(rounds, key = { it.roundNo }) { round ->
        val firstTeam = round.winner == RoundWinner.TEAM1
        val winningTeam = map.teams.getOrNull(if (firstTeam) 0 else 1)
        val tint = if (winningTeam?.isWinner == true) Prism.color.accent else Prism.color.labelColor
        val outcome = stringResource(
          when (round.winType) {
            RoundWinType.ELIMINATION -> Res.string.match_event_win_type_elimination
            RoundWinType.SPIKE_EXPLODED -> Res.string.match_event_win_type_spike_exploded
            RoundWinType.DEFUSED -> Res.string.match_event_win_type_defused
            RoundWinType.TIME_OUT -> Res.string.match_event_win_type_time_out
            RoundWinType.NOT_PLAYED -> Res.string.match_event_win_type_not_played
          },
        )
        val description = stringResource(
          Res.string.round_result_description,
          round.roundNo,
          map.teams.getOrNull(if (firstTeam) 0 else 1)?.name ?: stringResource(Res.string.match_event_tbd),
          outcome,
          round.score,
        )
        Column(
          modifier = Modifier.width(48.dp).clearAndSetSemantics { contentDescription = description },
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
        ) {
          Text(round.roundNo.toString(), style = Prism.typography.caption, color = tint)
          Image(
            painterResource(
              when (round.winType) {
                RoundWinType.ELIMINATION -> Res.drawable.round_elimination
                RoundWinType.SPIKE_EXPLODED -> Res.drawable.round_explosion
                RoundWinType.DEFUSED -> Res.drawable.round_defuse
                RoundWinType.TIME_OUT, RoundWinType.NOT_PLAYED -> Res.drawable.round_timeout
              },
            ),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(tint),
          )
          Text(round.score, style = Prism.typography.caption, color = tint, textAlign = TextAlign.Center)
        }
      }
    }
  }
}
