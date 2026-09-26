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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import dev.staticvar.designsystem.component.ticket.PrismTicketStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.CurrentMatchMap
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.TransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.transitionContentFade
import dev.staticvar.vlr.sharedui.component.match.MatchSharedContent
import dev.staticvar.vlr.sharedui.component.match.matchSharedBounds
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerContent
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_match_score
import vlr.shared_ui.generated.resources.match_event_tbd

@Composable
internal fun MatchTicketTeams(
  matchId: String,
  teams: List<TeamPreview>,
  onTeamSelected: ((String) -> Unit)?,
  extraContentFade: TransitionContentFade,
  currentMap: CurrentMatchMap? = null,
) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    MatchTicketTeam(matchId, teams.getOrNull(0), Modifier.weight(1f), onTeamSelected, extraContentFade)
    Box(
      Modifier.weight(0.9f).heightIn(min = 64.dp),
      contentAlignment = Alignment.Center,
    ) {
      SpoilerContent {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            MatchTicketScore(matchId, teams.getOrNull(0))
            Text(
              ":",
              modifier = Modifier.transitionContentFade(extraContentFade),
              style = Prism.typography.headline,
              color = Prism.color.contentTertiary,
            )
            MatchTicketScore(matchId, teams.getOrNull(1))
          }
          Text(
            text = stringResource(Res.string.match_event_match_score),
            modifier = Modifier.transitionContentFade(extraContentFade),
            style = Prism.typography.caption,
            color = Prism.color.contentSecondary,
            textAlign = TextAlign.Center,
          )
          currentMap?.let { map ->
            Column(
              modifier = Modifier.padding(top = Prism.dimens.spacingS).transitionContentFade(extraContentFade),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "${map.team1Score?.toString() ?: "–"} : ${map.team2Score?.toString() ?: "–"}",
                style = Prism.typography.headline,
                maxLines = 1,
              )
              Text(
                text = map.name,
                style = Prism.typography.caption,
                color = Prism.color.contentSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
    MatchTicketTeam(matchId, teams.getOrNull(1), Modifier.weight(1f), onTeamSelected, extraContentFade)
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
  extraContentFade: TransitionContentFade,
) {
  val teamId = team?.id?.takeIf(String::isNotBlank)
  val clickModifier = if (teamId != null && onTeamSelected != null) {
    Modifier.clickable(enabled = extraContentFade.acceptsInput, role = Role.Button) { onTeamSelected(teamId) }
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
      contentDescription = team?.name ?: stringResource(Res.string.match_event_tbd),
      size = PrismIconSize.Size64,
      style = PrismIconStyle.Plain,
      parentBackground = PrismTicketStyle.Standard.containerColor,
      tint = PrismIconTint.None,
      modifier = Modifier.transitionContentFade(extraContentFade),
    )
    Text(
      text = team?.name?.takeIf(String::isNotBlank) ?: stringResource(Res.string.match_event_tbd),
      modifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.TeamName, team?.id),
      style = Prism.typography.bodyLarge,
      color = if (team?.isWinner == true &&
        !LocalSpoilerMode.current.enabled
      ) {
        Prism.color.accent
      } else {
        Prism.color.contentPrimary
      },
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    team?.region?.takeIf(String::isNotBlank)?.let { region ->
      Text(
        region,
        modifier = Modifier.transitionContentFade(extraContentFade),
        style = Prism.typography.caption,
        color = Prism.color.contentSecondary,
        textAlign = TextAlign.Center,
      )
    }
  }
}
