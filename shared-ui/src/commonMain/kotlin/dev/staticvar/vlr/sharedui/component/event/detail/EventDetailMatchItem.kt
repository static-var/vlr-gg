/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
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
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam

/**
 * Event match row with two score lines and schedule metadata.
 */
@Composable
public fun EventDetailMatchItem(match: EventMatch, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  PrismCard(modifier = modifier.fillMaxWidth(), style = PrismCardStyle.Filled, onClick = onClick) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = match.eventMatchTitle(),
        modifier = Modifier.weight(1f),
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      PrismTag(text = match.status.ifBlank { "Unknown" }, style = match.status.eventMatchStatusTagStyle)
    }

    Text(
      text = listOf(match.stage, match.round).filter(String::isNotBlank).joinToString(separator = " • "),
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    match.teams.take(2).forEachIndexed { index, team ->
      if (index > 0) {
        PrismDivider(style = PrismDividerStyle.Hairline)
      }
      EventMatchTeamScoreRow(team = team)
    }

    match.eventMatchSchedule().takeIf(String::isNotBlank)?.let { schedule ->
      Text(
        text = schedule,
        modifier = Modifier.padding(top = Prism.dimens.spacingXs),
        style = Prism.typography.label,
        color = Prism.color.bodyColor,
      )
    }
  }
}

@Composable
private fun EventMatchTeamScoreRow(team: EventMatchTeam, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Text(
      text = team.name.ifBlank { "TBD" },
      modifier = Modifier.weight(1f),
      style = Prism.typography.headline,
      color = Prism.color.bodyColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = team.score?.toString() ?: "-",
      style = Prism.typography.headline,
      color = Prism.color.accent,
    )
  }
}
