/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import org.jetbrains.compose.resources.stringResource
import vlr.feature_events.generated.resources.Res
import vlr.feature_events.generated.resources.standings_column_losses
import vlr.feature_events.generated.resources.standings_column_team
import vlr.feature_events.generated.resources.standings_column_ties
import vlr.feature_events.generated.resources.standings_column_wins
import vlr.feature_events.generated.resources.standings_differences
import vlr.feature_events.generated.resources.standings_row_description

private val RankWidth = 20.dp

/**
 * One standings group as a compact table, in the order VLR ranks it.
 *
 * Map and round figures are the differences the API sends, never totals. The record only shows
 * ties when a team in the group has one.
 */
@Composable
internal fun EventStandingsTable(group: EventStandingsGroup, modifier: Modifier = Modifier) {
  val showTies = group.standings.any { it.ties != 0 }
  val recordHeader = listOfNotNull(
    stringResource(Res.string.standings_column_wins),
    stringResource(Res.string.standings_column_losses),
    stringResource(Res.string.standings_column_ties).takeIf { showTies },
  ).joinToString(separator = RecordSeparator)
  PrismCard(modifier = modifier.fillMaxWidth(), style = PrismCardStyle.Filled) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      group.name?.let { name ->
        Text(
          text = name,
          modifier = Modifier.semantics { heading() },
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      // Each row announces its full record, so the abbreviated header stays visual only.
      Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        Text(
          text = "#",
          modifier = Modifier.width(RankWidth),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
        )
        Text(
          stringResource(Res.string.standings_column_team),
          modifier = Modifier.weight(1f),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
        )
        Text(recordHeader, style = Prism.typography.caption, color = Prism.color.labelColor)
      }
      group.standings.forEachIndexed { index, standing ->
        PrismDivider(style = PrismDividerStyle.Hairline)
        EventStandingRow(position = index + 1, standing = standing, showTies = showTies)
      }
    }
  }
}

@Composable
private fun EventStandingRow(position: Int, standing: EventStanding, showTies: Boolean) {
  val description = stringResource(
    Res.string.standings_row_description,
    position,
    standing.teamName,
    standing.wins,
    standing.losses,
    standing.ties,
    standing.mapDifference.signed(),
    standing.roundDifference.signed(),
    standing.roundDelta.signed(),
  )
  Row(
    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Text(
      text = position.toString(),
      modifier = Modifier.width(RankWidth),
      style = Prism.typography.label,
      color = Prism.color.labelColor,
    )
    SharedNetworkIcon(
      imageUrl = standing.teamLogoUrl,
      contentDescription = null,
      size = PrismIconSize.Medium,
      style = PrismIconStyle.Plain,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = standing.teamName,
        style = Prism.typography.bodyLarge,
        color = Prism.color.titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = stringResource(
          Res.string.standings_differences,
          standing.mapDifference.signed(),
          standing.roundDifference.signed(),
          standing.roundDelta.signed(),
        ),
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Text(
      text = listOfNotNull(standing.wins, standing.losses, standing.ties.takeIf { showTies })
        .joinToString(separator = RecordSeparator),
      style = Prism.typography.numericSecondary,
      color = Prism.color.titleColor,
      textAlign = TextAlign.End,
    )
  }
}

private const val RecordSeparator = "–"

private fun Int.signed(): String = if (this > 0) "+$this" else toString()
