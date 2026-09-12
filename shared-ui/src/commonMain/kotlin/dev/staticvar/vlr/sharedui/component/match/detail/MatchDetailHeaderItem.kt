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
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.component.common.DetailStatItem
import dev.staticvar.vlr.sharedui.component.common.DetailStatStrip
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.formatMatchPreviewTime
import dev.staticvar.vlr.sharedui.component.match.MatchSharedContent
import dev.staticvar.vlr.sharedui.component.match.matchFavoriteReasonLabels
import dev.staticvar.vlr.sharedui.component.match.matchSharedBounds
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode

/**
 * Match detail summary card for event metadata, current score, participating teams, and optional
 * caller-provided actions.
 */
@Composable
public fun MatchDetailHeaderItem(
  match: MatchDetails,
  modifier: Modifier = Modifier,
  onEventSelected: ((String) -> Unit)? = null,
  onTeamSelected: ((String) -> Unit)? = null,
  actions: (@Composable () -> Unit)? = null,
) {
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val favoriteLabels = matchFavoriteReasonLabels(match.favoriteReasons)
  val canShowVeto = !spoilersHidden && match.bans.any(String::isNotBlank)
  var showVeto by remember(match.id, spoilersHidden) { mutableStateOf(false) }
  MatchDetailHeaderContent(
    matchId = match.id,
    eventId = match.event.id,
    eventName = match.event.name,
    series = match.matchDetailMeta(),
    time = formatMatchPreviewTime(match.event.date),
    status = match.event.status,
    teams = match.teams.take(2).map { team ->
      TeamPreview(team.id, team.name, team.region, team.img, team.score, team.isWinner, team.isFavorite)
    },
    isFavorite = match.isFavorite,
    modifier = modifier,
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    stats = {
      DetailStatStrip(
        items = buildList {
          add(DetailStatItem(value = if (spoilersHidden) "Hidden" else match.matchDetailMapCountStat(), label = "Maps"))
          if (favoriteLabels.isNotEmpty()) {
            add(DetailStatItem(value = favoriteLabels.joinToString(" · "), label = "Favorite via", valueMaxLines = 4))
          }
          add(
            DetailStatItem(
              value = if (spoilersHidden) "Hidden" else match.matchDetailVetoStat(),
              label = if (canShowVeto) "Map veto ↗" else "Map veto",
              onClick = if (canShowVeto) { { showVeto = true } } else null,
            ),
          )
        },
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
      )
    },
    actions = actions,
  )
  MatchDetailVetoSheet(entries = match.bans.filter(String::isNotBlank), visible = showVeto && canShowVeto, onDismissRequest = { showVeto = false })
}

@Composable
public fun MatchDetailPreviewHeaderItem(match: MatchPreview, modifier: Modifier = Modifier) {
  MatchDetailHeaderContent(
    matchId = match.id,
    eventId = match.eventId,
    eventName = match.event,
    series = match.series,
    time = formatMatchPreviewTime(match.time),
    status = match.status.name.lowercase(),
    teams = listOf(match.team1, match.team2),
    isFavorite = match.isFavorite,
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
  status: String?,
  teams: List<TeamPreview>,
  isFavorite: Boolean,
  modifier: Modifier = Modifier,
  onEventSelected: ((String) -> Unit)? = null,
  onTeamSelected: ((String) -> Unit)? = null,
  stats: (@Composable () -> Unit)? = null,
  actions: (@Composable () -> Unit)? = null,
) {
  FavoriteTicketCardBox(
    selected = isFavorite,
    modifier = modifier.fillMaxWidth(),
    favoriteModifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.Favorite),
  ) {
    PrismCard(
      modifier = Modifier.fillMaxWidth().matchSharedBounds(matchId, MatchSharedContent.Card),
      style = PrismCardStyle.Outlined,
    ) {
      val eventClickModifier = eventId.takeIf(String::isNotBlank)
        ?.let { eventId ->
          onEventSelected?.let { eventSelected ->
            Modifier.clickable(role = Role.Button) { eventSelected(eventId) }
          }
        }
        ?: Modifier

      PrismHeader(
        text = eventName.ifBlank { "match" },
        modifier = eventClickModifier.fillMaxWidth().matchSharedBounds(matchId, MatchSharedContent.Event),
      )
      if (series.isNotBlank()) {
        Text(
          text = series,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs).matchSharedBounds(matchId, MatchSharedContent.Series),
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        PrismTag(
          text = status.matchDetailStatusLabel,
          style = status.matchDetailStatusTagStyle,
          modifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.Status),
        )
        if (time != null) {
          Text(
            text = time,
            modifier = Modifier.weight(1f).matchSharedBounds(matchId, MatchSharedContent.Time),
            style = Prism.typography.label,
            color = Prism.color.labelColor,
            textAlign = TextAlign.End,
          )
        }
      }
      Column(modifier = Modifier.padding(vertical = Prism.dimens.spacingS)) {
        teams.forEachIndexed { index, team ->
          if (index > 0) PrismDivider(style = PrismDividerStyle.Hairline)
          MatchDetailTeamScoreRow(matchId = matchId, team = team, onTeamSelected = onTeamSelected)
        }
      }
      stats?.invoke()
      if (actions != null) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM)) {
          actions()
        }
      }
    }
  }
}

@Composable
private fun MatchDetailTeamScoreRow(
  matchId: String,
  team: TeamPreview,
  modifier: Modifier = Modifier,
  onTeamSelected: ((String) -> Unit)? = null,
) {
  MatchDetailTeamScoreRow(
    teamName = team.name,
    score = team.score?.toString() ?: "-",
    modifier = modifier,
    logoSize = PrismIconSize.Size40,
    nameModifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.TeamName, team.id),
    scoreModifier = Modifier.matchSharedBounds(matchId, MatchSharedContent.TeamScore, team.id),
    region = team.region,
    imageUrl = team.img,
    isWinner = team.isWinner == true,
    onClick = team.id?.takeIf(String::isNotBlank)?.let { teamId ->
      onTeamSelected?.let { onClick -> { onClick(teamId) } }
    },
  )
}
