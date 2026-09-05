/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.formatMatchPreviewTime

@Composable
public fun MatchPreviewItem(modifier: Modifier = Modifier, matchPreview: MatchPreview, onClick: (() -> Unit)? = null) {
  FavoriteTicketCardBox(selected = matchPreview.isFavorite, modifier = modifier) {
    PrismCard(
      modifier = Modifier.fillMaxWidth(),
      style = if (matchPreview.isFavorite) PrismCardStyle.Outlined else PrismCardStyle.Filled,
      onClick = onClick,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        PrismHeader(text = matchPreview.event, modifier = Modifier.weight(1f))
        val time = formatMatchPreviewTime(isoUtcTime = matchPreview.time)
        when (matchPreview.status) {
          MatchStatus.LIVE -> PrismTag(
            text = "LIVE",
            style = PrismTagStyle.Accent,
          )

          MatchStatus.UPCOMING,
          MatchStatus.COMPLETED,
          -> time?.let { formattedTime -> PrismTag(text = formattedTime) }

          MatchStatus.UNKNOWN -> Unit
        }
      }
      ScoreBox(team1 = matchPreview.team1, team2 = matchPreview.team2, state = matchPreview.status)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(Prism.dimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = matchPreview.series,
          style = Prism.typography.label,
          color = Prism.color.labelColor,
          modifier = Modifier.padding(Prism.dimens.spacingXs),
        )
      }
    }
  }
}

@Composable
private fun ScoreBox(modifier: Modifier = Modifier, team1: TeamPreview, team2: TeamPreview, state: MatchStatus) {
  val (actualTeam1, actualTeam2) = rememberTeamPreview(team1, team2, state)
  Column(modifier = modifier.fillMaxWidth()) {
    TeamScoreRow(actualTeam1, useAltColor = state != MatchStatus.UPCOMING)
    PrismDivider(style = PrismDividerStyle.Hairline)
    TeamScoreRow(actualTeam2, useAltColor = state == MatchStatus.LIVE)
  }
}

@Composable
private fun rememberTeamPreview(
  team1: TeamPreview,
  team2: TeamPreview,
  state: MatchStatus,
): Pair<TeamPreview, TeamPreview> = remember(team1, team2, state) {
  when (state) {
    MatchStatus.UPCOMING,
    MatchStatus.LIVE,
    -> team1 to team2

    MatchStatus.COMPLETED,
    MatchStatus.UNKNOWN,
    -> teamsByScore(team1 = team1, team2 = team2)
  }
}

private fun teamsByScore(team1: TeamPreview, team2: TeamPreview): Pair<TeamPreview, TeamPreview> =
  if (team2.scoreForOrdering() < team1.scoreForOrdering()) {
    team1 to team2
  } else {
    team2 to team1
  }

private fun TeamPreview.scoreForOrdering(): Int = score ?: Int.MIN_VALUE

@Composable
private fun TeamScoreRow(team: TeamPreview, useAltColor: Boolean, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = team.name,
      style = Prism.typography.headline,
      color = if (useAltColor) Prism.color.accent else Prism.color.labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = team.score?.toString() ?: "-",
      style = Prism.typography.headline,
      color = if (useAltColor) Prism.color.accent else Prism.color.labelColor,
      modifier = Modifier.padding(start = Prism.dimens.spacingXs),
    )
  }
}
