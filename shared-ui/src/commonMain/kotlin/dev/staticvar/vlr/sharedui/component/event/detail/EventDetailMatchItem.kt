/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.match.MatchFavoriteReasons
import dev.staticvar.vlr.sharedui.spoilers.SpoilerScore

/**
 * Event match row with two score lines and schedule metadata.
 */
@Composable
public fun EventDetailMatchItem(
  match: EventMatch,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  favoriteReasons: List<MatchFavoriteReason> = emptyList(),
) {
  FavoriteTicketCardBox(selected = favoriteReasons.isNotEmpty(), modifier = modifier.fillMaxWidth()) {
    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Filled, onClick = onClick) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        Text(
          text = listOf(match.stage, match.round).filter(String::isNotBlank)
            .joinToString(separator = " • ").ifBlank { "Match" },
          modifier = Modifier.weight(1f),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        PrismTag(text = match.status.ifBlank { "Unknown" }, style = match.status.eventMatchStatusTagStyle)
      }

      Column(
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      ) {
        if (match.teams.isEmpty()) {
          Text(text = "Match TBD", style = Prism.typography.bodyLarge, color = Prism.color.bodyColor)
        }
        match.teams.take(2).forEach { team ->
          EventMatchTeamScoreRow(team = team)
        }
      }

      MatchFavoriteReasons(favoriteReasons, modifier = Modifier.padding(top = Prism.dimens.spacingXs))

      match.eventMatchSchedule().takeIf(String::isNotBlank)?.let { schedule ->
        Text(
          text = schedule,
          modifier = Modifier.padding(top = Prism.dimens.spacingS),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun EventMatchTeamScoreRow(team: EventMatchTeam, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Text(
      text = team.name.ifBlank { "TBD" },
      modifier = Modifier.weight(1f),
      style = Prism.typography.bodyLarge,
      color = Prism.color.titleColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    SpoilerScore(
      text = team.score?.toString() ?: "-",
      style = Prism.typography.bodyLarge,
      color = Prism.color.accent,
    )
  }
}
