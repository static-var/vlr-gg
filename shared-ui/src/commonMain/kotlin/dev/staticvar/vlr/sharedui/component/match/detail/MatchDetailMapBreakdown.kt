/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenNotice

/**
 * Compact map breakdown content controlled by [MatchDetailMapSelector].
 *
 * A `null` selected index renders the aggregate all-maps state when multiple maps exist. Otherwise
 * the selected map renders score metadata and the full player stat table. Spoiler mode replaces
 * the breakdown with a hidden-results notice.
 */
@Composable
public fun MatchDetailMapBreakdown(
  maps: List<MapData>,
  selectedMapIndex: Int?,
  modifier: Modifier = Modifier,
  onPlayerSelected: ((String) -> Unit)? = null,
) {
  if (LocalSpoilerMode.current.enabled) {
    SpoilerHiddenNotice(modifier = modifier)
    return
  }
  val selectedMap = maps.resolveSelectedMap(selectedMapIndex)

  PrismSurface(
    modifier = modifier.fillMaxWidth(),
    color = Prism.color.surfaceVariant,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(Prism.dimens.spacingS),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      when {
        maps.isEmpty() -> EmptyMapBreakdown()

        selectedMap == null -> AllMapsBreakdown(maps = maps, onPlayerSelected = onPlayerSelected)

        else -> SingleMapBreakdown(
          map = selectedMap,
          mapIndex = maps.indexOf(selectedMap),
          onPlayerSelected = onPlayerSelected,
        )
      }
    }
  }
}

@Composable
private fun EmptyMapBreakdown() {
  Text(
    text = "Map breakdown unavailable.",
    style = Prism.typography.bodySmall,
    color = Prism.color.labelColor,
  )
}

@Composable
private fun AllMapsBreakdown(maps: List<MapData>, onPlayerSelected: ((String) -> Unit)?) {
  MatchDetailMapBreakdownHeader(
    title = "Combined stats",
    subtitle = maps.matchDetailAllMapsMeta(),
    tag = "All maps",
    tagStyle = PrismTagStyle.Neutral,
  )
  MatchDetailAllMapPlayerStatsTable(maps = maps, onPlayerSelected = onPlayerSelected)
}

@Composable
private fun SingleMapBreakdown(map: MapData, mapIndex: Int, onPlayerSelected: ((String) -> Unit)?) {
  MatchDetailMapBreakdownHeader(
    title = map.matchDetailMapName(),
    subtitle = map.matchDetailMapMeta(index = mapIndex.takeIf { it >= 0 }),
    tag = map.matchDetailMapScoreLabel(),
    tagStyle = PrismTagStyle.Accent,
  )
  MatchDetailMapScoreLine(map = map)
  PrismDivider(style = PrismDividerStyle.Hairline)
  MatchDetailPlayerStatsTable(map = map, onPlayerSelected = onPlayerSelected)
}

@Composable
private fun MatchDetailMapBreakdownHeader(title: String, subtitle: String, tag: String, tagStyle: PrismTagStyle) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        modifier = Modifier.padding(top = Prism.dimens.spacingXs),
        style = Prism.typography.bodySmall,
        color = Prism.color.labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    PrismTag(text = tag, style = tagStyle)
  }
}

@Composable
private fun MatchDetailMapScoreLine(map: MapData) {
  val teams = map.teams.take(2)
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    teams.getOrNull(0)?.let { team ->
      TeamScoreText(team = team, modifier = Modifier.weight(1f))
    }
    Text(
      text = teams.map { team -> team.score?.toString() ?: "-" }.joinToString(separator = " : "),
      style = Prism.typography.headline,
      color = Prism.color.titleColor,
      maxLines = 1,
    )
    teams.getOrNull(1)?.let { team ->
      TeamScoreText(team = team, modifier = Modifier.weight(1f), alignEnd = true)
    }
  }
}

@Composable
private fun TeamScoreText(team: TeamDetails, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
  Text(
    text = team.name.ifBlank { "TBD" },
    modifier = modifier,
    style = Prism.typography.cardTitle,
    color = if (team.isWinner == true) Prism.color.accent else Prism.color.labelColor,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
  )
}
