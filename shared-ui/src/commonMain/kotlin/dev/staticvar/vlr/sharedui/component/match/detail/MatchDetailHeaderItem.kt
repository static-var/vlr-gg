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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.sharedui.component.common.DetailStatItem
import dev.staticvar.vlr.sharedui.component.common.DetailStatStrip
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox

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
  FavoriteTicketCardBox(selected = match.isFavorite, modifier = modifier.fillMaxWidth()) {
    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
      val eventClickModifier = match.event.id.takeIf(String::isNotBlank)
        ?.let { eventId ->
          onEventSelected?.let { eventSelected ->
            Modifier.clickable(role = Role.Button) { eventSelected(eventId) }
          }
        }
        ?: Modifier

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        PrismHeader(
          text = match.event.name.ifBlank { "match" },
          modifier = eventClickModifier,
        )
        PrismTag(text = match.event.status.matchDetailStatusLabel, style = match.event.status.matchDetailStatusTagStyle)
      }

      match.matchDetailMeta().takeIf(String::isNotBlank)?.let { meta ->
        Text(
          text = meta,
          modifier = Modifier.padding(top = Prism.dimens.spacingS),
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Column(modifier = Modifier.padding(top = Prism.dimens.spacingM)) {
        match.teams.take(2).forEachIndexed { index, team ->
          if (index > 0) {
            PrismDivider(style = PrismDividerStyle.Hairline)
          }
          MatchDetailTeamScoreRow(team = team, onTeamSelected = onTeamSelected)
        }
      }

      DetailStatStrip(
        items = listOf(
          DetailStatItem(value = match.matchDetailMapCountStat(), label = "Maps"),
          DetailStatItem(value = match.matchDetailPatchStat(), label = "Patch"),
          DetailStatItem(value = match.matchDetailBanStat(), label = "Bans"),
        ),
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
      )
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
  team: TeamDetails,
  modifier: Modifier = Modifier,
  onTeamSelected: ((String) -> Unit)? = null,
) {
  MatchDetailTeamScoreRow(
    teamName = team.name,
    score = team.score?.toString() ?: "-",
    modifier = modifier,
    region = team.region,
    imageUrl = team.img,
    isWinner = team.isWinner == true,
    onClick = team.id?.takeIf(String::isNotBlank)?.let { teamId ->
      onTeamSelected?.let { onClick -> { onClick(teamId) } }
    },
  )
}
