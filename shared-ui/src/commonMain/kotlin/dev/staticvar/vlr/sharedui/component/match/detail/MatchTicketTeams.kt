/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.match.MatchSharedContent
import dev.staticvar.vlr.sharedui.component.match.matchSharedBounds
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerContent

@Composable
internal fun MatchTicketTeams(matchId: String, teams: List<TeamPreview>, onTeamSelected: ((String) -> Unit)?) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    MatchTicketTeam(matchId, teams.getOrNull(0), Modifier.weight(1f), onTeamSelected)
    Box(
      Modifier.weight(0.9f).height(64.dp),
      contentAlignment = Alignment.Center,
    ) {
      SpoilerContent {
        Row(verticalAlignment = Alignment.CenterVertically) {
          MatchTicketScore(matchId, teams.getOrNull(0))
          Text(":", style = Prism.typography.headline, color = Prism.color.contentTertiary)
          MatchTicketScore(matchId, teams.getOrNull(1))
        }
      }
    }
    MatchTicketTeam(matchId, teams.getOrNull(1), Modifier.weight(1f), onTeamSelected)
  }
}

@Composable
private fun MatchTicketScore(matchId: String, team: TeamPreview?) {
  val winner = team?.isWinner == true && !LocalSpoilerMode.current.enabled
  Text(
    text = team?.score?.toString() ?: "–",
    modifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.TeamScore, team?.id),
    style = Prism.typography.display.copy(fontSize = 44.sp),
    color = if (winner) Prism.color.accent else Prism.color.contentPrimary,
    maxLines = 1,
  )
}

@Composable
private fun MatchTicketTeam(
  matchId: String,
  team: TeamPreview?,
  modifier: Modifier,
  onTeamSelected: ((String) -> Unit)?,
) {
  val teamId = team?.id?.takeIf(String::isNotBlank)
  val clickModifier = if (teamId != null && onTeamSelected != null) {
    Modifier.clickable(role = Role.Button) { onTeamSelected(teamId) }
  } else {
    Modifier
  }
  Column(
    modifier.then(clickModifier),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    SharedNetworkIcon(
      imageUrl = team?.img,
      contentDescription = team?.name ?: "TBD",
      size = PrismIconSize.Size64,
      style = PrismIconStyle.Plain,
      tint = PrismIconTint.None,
    )
    Text(
      text = team?.name?.takeIf(String::isNotBlank) ?: "TBD",
      modifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.TeamName, team?.id),
      style = Prism.typography.bodyLarge,
      color = if (team?.isWinner == true && !LocalSpoilerMode.current.enabled) Prism.color.accent else Prism.color.contentPrimary,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    team?.region?.takeIf(String::isNotBlank)?.let { region ->
      Text(region, style = Prism.typography.caption, color = Prism.color.contentSecondary, textAlign = TextAlign.Center)
    }
  }
}
