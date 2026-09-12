/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.ticket.PrismTicket
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.formatMatchPreviewTime
import dev.staticvar.vlr.sharedui.component.match.MatchSharedContent
import dev.staticvar.vlr.sharedui.component.match.matchFavoriteReasonLabels
import dev.staticvar.vlr.sharedui.component.match.matchSharedBounds
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode

/** Match ticket with event identity, opposing teams, and a schedule/action stub. */
@Composable
public fun MatchDetailHeaderItem(
  match: MatchDetails,
  modifier: Modifier = Modifier,
  onEventSelected: ((String) -> Unit)? = null,
  onTeamSelected: ((String) -> Unit)? = null,
  actions: (@Composable () -> Unit)? = null,
  favoriteAction: (@Composable () -> Unit)? = null,
) {
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val canShowVeto = !spoilersHidden && match.bans.any(String::isNotBlank)
  var showVeto by remember(match.id, spoilersHidden) { mutableStateOf(false) }
  MatchDetailHeaderContent(
    matchId = match.id,
    eventId = match.event.id,
    eventName = match.event.name,
    series = match.matchDetailMeta(),
    time = formatMatchPreviewTime(match.event.date),
    format = match.matchDetailFormatLabel(),
    status = match.event.status,
    teams = match.teams.take(2).map { team ->
      TeamPreview(team.id, team.name, team.region, team.img, team.score, team.isWinner, team.isFavorite)
    },
    isFavorite = match.isFavorite,
    favoriteLabels = matchFavoriteReasonLabels(match.favoriteReasons),
    modifier = modifier,
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    onVetoSelected = if (canShowVeto) { { showVeto = true } } else null,
    actions = actions,
    favoriteAction = favoriteAction,
  )
  MatchDetailVetoSheet(
    entries = match.bans.filter(String::isNotBlank),
    visible = showVeto && canShowVeto,
    onDismissRequest = { showVeto = false },
  )
}

@Composable
public fun MatchDetailPreviewHeaderItem(match: MatchPreview, modifier: Modifier = Modifier) {
  MatchDetailHeaderContent(
    matchId = match.id,
    eventId = match.eventId,
    eventName = match.event,
    series = match.series,
    time = formatMatchPreviewTime(match.time),
    format = "TBD",
    status = match.status.name.lowercase(),
    teams = listOf(match.team1, match.team2),
    isFavorite = match.isFavorite,
    favoriteLabels = matchFavoriteReasonLabels(match.favoriteReasons),
    modifier = modifier,
  )
}

@Composable
private fun MatchDetailHeaderContent(
  matchId: String,
  eventId: String,
  eventName: String,
  series: String,
  time: String?,
  format: String,
  status: String?,
  teams: List<TeamPreview>,
  isFavorite: Boolean,
  favoriteLabels: List<String>,
  modifier: Modifier = Modifier,
  onEventSelected: ((String) -> Unit)? = null,
  onTeamSelected: ((String) -> Unit)? = null,
  onVetoSelected: (() -> Unit)? = null,
  actions: (@Composable () -> Unit)? = null,
  favoriteAction: (@Composable () -> Unit)? = null,
) {
  FavoriteTicketCardBox(
    selected = isFavorite,
    modifier = modifier.fillMaxWidth(),
    favoriteModifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.Favorite),
  ) {
    PrismTicket(
      modifier = Modifier.fillMaxWidth().matchSharedBounds(matchId, MatchSharedContent.Card),
      header = {
        MatchTicketStatus(matchId, series, status, favoriteAction)
      },
      stub = {
        MatchTicketStub(matchId, time, format, onVetoSelected, actions)
      },
    ) {
      MatchTicketEvent(matchId, eventId, eventName, onEventSelected)
      MatchTicketTeams(matchId = matchId, teams = teams, onTeamSelected = onTeamSelected)
      if (favoriteLabels.isNotEmpty()) {
        Text(
          text = "Favorite via ${favoriteLabels.joinToString(" · ")}",
          modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
          style = Prism.typography.caption,
          color = Prism.color.contentSecondary,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun MatchTicketEvent(
  matchId: String,
  eventId: String,
  eventName: String,
  onEventSelected: ((String) -> Unit)?,
) {
  val eventModifier = if (eventId.isNotBlank() && onEventSelected != null) {
    Modifier.clickable(role = Role.Button) { onEventSelected(eventId) }
  } else {
    Modifier
  }
  Text(
    text = eventName.ifBlank { "Match" },
    modifier = Modifier.fillMaxWidth().padding(bottom = Prism.dimens.spacingM)
      .then(eventModifier).matchSharedBounds(matchId, MatchSharedContent.Event),
    style = Prism.typography.cardTitle,
    textAlign = TextAlign.Center,
    maxLines = 3,
    overflow = TextOverflow.Ellipsis,
  )
}

@Composable
private fun MatchTicketStatus(
  matchId: String,
  series: String,
  status: String?,
  favoriteAction: (@Composable () -> Unit)?,
) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text(
        text = series,
        modifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.Series),
        style = Prism.typography.bodySmall,
        color = Prism.color.contentSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      PrismTag(
        text = status.matchDetailStatusLabel,
        style = status.matchDetailStatusTagStyle,
        modifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.Status),
      )
    }
    favoriteAction?.invoke()
  }
}

@Composable
private fun MatchTicketStub(
  matchId: String,
  time: String?,
  format: String,
  onVetoSelected: (() -> Unit)?,
  actions: (@Composable () -> Unit)?,
) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text("MATCH DAY", style = Prism.typography.overline, color = Prism.color.contentSecondary)
      Text(
        text = time ?: "Time to be announced",
        modifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.Time),
        style = Prism.typography.bodySmall,
      )
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text("FORMAT", style = Prism.typography.overline, color = Prism.color.contentSecondary)
      Text(format, style = Prism.typography.bodySmall)
    }
  }
  if (actions != null) {
    Column(Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM)) { actions() }
  }
  if (onVetoSelected != null) {
    PrismButton(
      modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
      onClick = onVetoSelected,
      style = PrismButtonStyle.Alternate,
    ) {
      Text("Map veto")
    }
  }
}
