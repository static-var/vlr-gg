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
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.component.ticket.PrismTicket
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.model.VetoAction
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.TransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.currentTransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.formatMatchPreviewTime
import dev.staticvar.vlr.sharedui.component.common.transitionContentFade
import dev.staticvar.vlr.sharedui.component.match.MatchSharedContent
import dev.staticvar.vlr.sharedui.component.match.matchFavoriteReasonLabels
import dev.staticvar.vlr.sharedui.component.match.matchSharedBounds
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_favorite_via
import vlr.shared_ui.generated.resources.match_event_format
import vlr.shared_ui.generated.resources.match_event_map_veto
import vlr.shared_ui.generated.resources.match_event_match
import vlr.shared_ui.generated.resources.match_event_match_day
import vlr.shared_ui.generated.resources.match_event_tbd
import vlr.shared_ui.generated.resources.match_event_time_tba

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
  val vetoEntries = match.veto.filter { entry -> entry.map.isNotBlank() || entry.action != VetoAction.UNKNOWN }
  val fallbackVetoEntries = match.bans.filter(String::isNotBlank).takeIf { vetoEntries.isEmpty() }.orEmpty()
  val canShowVeto = !spoilersHidden && (vetoEntries.isNotEmpty() || fallbackVetoEntries.isNotEmpty())
  var showVeto by remember(match.id, spoilersHidden) { mutableStateOf(false) }
  MatchDetailHeaderContent(
    matchId = match.id,
    eventId = match.event.id,
    eventName = match.event.name,
    series = match.matchDetailMeta(),
    time = formatMatchPreviewTime(match.event.date),
    format = match.matchDetailFormatLabel(),
    statusLabel = match.event.status.matchDetailStatusLabel,
    statusStyle = match.event.status.matchDetailStatusTagStyle,
    teams = match.teams.take(2).map { team ->
      TeamPreview(team.id, team.name, team.region, team.img, team.score, team.isWinner, team.isFavorite)
    },
    isFavorite = match.isFavorite,
    favoriteLabels = matchFavoriteReasonLabels(match.favoriteReasons),
    modifier = modifier,
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    onVetoSelected = if (canShowVeto) {
      { showVeto = true }
    } else {
      null
    },
    actions = actions,
    favoriteAction = favoriteAction,
  )
  MatchDetailVetoSheet(
    entries = vetoEntries,
    fallbackEntries = fallbackVetoEntries,
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
    format = stringResource(Res.string.match_event_tbd),
    statusLabel = match.status.matchDetailStatusLabel,
    statusStyle = match.status.matchDetailStatusTagStyle,
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
  statusLabel: String,
  statusStyle: PrismTagStyle,
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
  val extraContentFade = currentTransitionContentFade()
  FavoriteTicketCardBox(
    selected = isFavorite,
    modifier = modifier.fillMaxWidth(),
    favoriteModifier = Modifier.transitionContentFade(extraContentFade),
  ) {
    PrismTicket(
      modifier = Modifier.fillMaxWidth().matchSharedBounds(matchId, MatchSharedContent.Card),
      animateSizeChanges = extraContentFade.isSettled,
      header = {
        MatchTicketStatus(series, statusLabel, statusStyle, favoriteAction, extraContentFade)
      },
      stub = {
        MatchTicketStub(time, format, onVetoSelected, actions, extraContentFade)
      },
    ) {
      MatchTicketEvent(matchId, eventId, eventName, onEventSelected, extraContentFade)
      MatchTicketTeams(matchId = matchId, teams = teams, onTeamSelected = onTeamSelected)
      if (favoriteLabels.isNotEmpty()) {
        Text(
          text = stringResource(Res.string.match_event_favorite_via, favoriteLabels.joinToString(" · ")),
          modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM)
            .transitionContentFade(extraContentFade),
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
  extraContentFade: TransitionContentFade,
) {
  val eventModifier = if (eventId.isNotBlank() && onEventSelected != null) {
    Modifier.clickable(enabled = extraContentFade.acceptsInput, role = Role.Button) { onEventSelected(eventId) }
  } else {
    Modifier
  }
  Text(
    text = eventName.ifBlank { stringResource(Res.string.match_event_match) },
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
  series: String,
  statusLabel: String,
  statusStyle: PrismTagStyle,
  favoriteAction: (@Composable () -> Unit)?,
  extraContentFade: TransitionContentFade,
) {
  Row(
    Modifier.fillMaxWidth().transitionContentFade(extraContentFade),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text(
        text = series,
        style = Prism.typography.bodySmall,
        color = Prism.color.contentSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      PrismTag(
        text = statusLabel,
        style = statusStyle,
      )
    }
    favoriteAction?.invoke()
  }
}

@Composable
private fun MatchTicketStub(
  time: String?,
  format: String,
  onVetoSelected: (() -> Unit)?,
  actions: (@Composable () -> Unit)?,
  extraContentFade: TransitionContentFade,
) {
  Row(
    Modifier.fillMaxWidth().transitionContentFade(extraContentFade),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text(
        stringResource(Res.string.match_event_match_day),
        style = Prism.typography.overline,
        color = Prism.color.contentSecondary,
      )
      Text(
        text = time ?: stringResource(Res.string.match_event_time_tba),
        style = Prism.typography.bodySmall,
      )
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text(
        stringResource(Res.string.match_event_format),
        style = Prism.typography.overline,
        color = Prism.color.contentSecondary,
      )
      Text(format, style = Prism.typography.bodySmall)
    }
  }
  if (actions != null) {
    Column(
      Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM).transitionContentFade(extraContentFade),
    ) { actions() }
  }
  if (onVetoSelected != null) {
    PrismButton(
      modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM)
        .transitionContentFade(extraContentFade),
      onClick = onVetoSelected,
      enabled = extraContentFade.acceptsInput,
      style = PrismButtonStyle.Alternate,
    ) {
      Text(stringResource(Res.string.match_event_map_veto))
    }
  }
}
