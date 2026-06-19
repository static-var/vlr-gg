/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.table.PrismTable
import dev.staticvar.designsystem.component.table.PrismTableColumn
import dev.staticvar.designsystem.component.table.PrismTableCellContentResolver
import dev.staticvar.designsystem.component.table.PrismTableCellStyle
import dev.staticvar.designsystem.component.table.PrismTableCellStyleResolver
import dev.staticvar.designsystem.component.table.PrismTableColorRole
import dev.staticvar.designsystem.component.table.PrismTableDefaults
import dev.staticvar.designsystem.component.table.PrismTableOptions
import dev.staticvar.designsystem.component.table.PrismTableRow
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

private object MatchDetailStatsTableColumns {
  const val Map = "map"
  const val Player = "player"
  const val Agent = "agent"
  const val Acs = "acs"
  const val Kills = "kills"
  const val Deaths = "deaths"
  const val Assists = "assists"
  const val Kast = "kast"
  const val Rating = "rating"
}

private object MatchDetailStatsTableDimensions {
  val RowHeight: Dp = 60.dp
  val HeaderHeight: Dp = 40.dp
  const val MaxVisibleRows: Int = 10
}

/**
 * Dense player stat table for one selected map.
 *
 * Uses [PrismTable] so sticky first-column behavior, scrolling, and table colors remain owned by
 * the design system.
 */
@Composable
public fun MatchDetailPlayerStatsTable(
  map: MapData,
  modifier: Modifier = Modifier,
  onPlayerSelected: ((String) -> Unit)? = null,
) {
  MatchDetailPlayerStatsTable(
    rows = remember(map) { map.toPlayerStatsRows() },
    includeMapName = false,
    modifier = modifier,
    onPlayerSelected = onPlayerSelected,
  )
}

/**
 * Dense player stat table for aggregate all-map breakdowns.
 */
@Composable
public fun MatchDetailAllMapPlayerStatsTable(
  maps: List<MapData>,
  modifier: Modifier = Modifier,
  onPlayerSelected: ((String) -> Unit)? = null,
) {
  MatchDetailPlayerStatsTable(
    rows = remember(maps) { maps.toAllMapPlayerStatsRows() },
    includeMapName = false,
    modifier = modifier,
    onPlayerSelected = onPlayerSelected,
  )
}

@Composable
private fun MatchDetailPlayerStatsTable(
  rows: List<MatchDetailPlayerStatsRow>,
  includeMapName: Boolean,
  modifier: Modifier = Modifier,
  onPlayerSelected: ((String) -> Unit)?,
) {
  val accentContentColor = Prism.color.accent
  PrismTable(
    columns = remember(includeMapName) { playerStatsColumns(includeMapName = includeMapName) },
    rows = remember(rows, includeMapName, onPlayerSelected) {
      rows.map { row ->
        row.toPrismTableRow(includeMapName = includeMapName, onPlayerSelected = onPlayerSelected)
      }
    },
    modifier = modifier.heightIn(max = matchDetailStatsTableMaxHeight(rows.size)),
    options = PrismTableOptions(
      stickyFirstColumn = true,
      rowHeight = MatchDetailStatsTableDimensions.RowHeight,
      headerHeight = MatchDetailStatsTableDimensions.HeaderHeight,
      bodyMaxLines = 2,
      bodyOverflow = TextOverflow.Clip,
      cellPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ),
    viewState = PrismTableDefaults.viewState(
      cellStyleResolver = PrismTableCellStyleResolver { context ->
        if (context.isHeader) {
          return@PrismTableCellStyleResolver PrismTableCellStyle(colorRole = PrismTableColorRole.Primary)
        }
        val rowTeamColorRole = rows.getOrNull(context.rowIndex)?.teamColorRole
        when (rowTeamColorRole) {
          MatchDetailPlayerStatsTeamColorRole.Accent ->
            PrismTableCellStyle(colorRole = PrismTableColorRole.Secondary, contentColor = accentContentColor)
          MatchDetailPlayerStatsTeamColorRole.Neutral, null -> PrismTableCellStyle(colorRole = PrismTableColorRole.Secondary)
        }
      },
    ),
    cellContentResolver = PrismTableCellContentResolver { context, value ->
      val row = rows.getOrNull(context.rowIndex)
      if (!context.isHeader && context.columnKey == MatchDetailStatsTableColumns.Player && row != null) {
        { MatchDetailPlayerCell(row = row, playerName = value) }
      } else {
        null
      }
    },
  )
}

internal fun matchDetailStatsTableMaxHeight(rowCount: Int): Dp {
  val visibleRows = rowCount.coerceIn(
    minimumValue = 0,
    maximumValue = MatchDetailStatsTableDimensions.MaxVisibleRows,
  )
  return MatchDetailStatsTableDimensions.HeaderHeight +
    (MatchDetailStatsTableDimensions.RowHeight * visibleRows.toFloat())
}

@Composable
private fun MatchDetailPlayerCell(row: MatchDetailPlayerStatsRow, playerName: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SharedNetworkIcon(
      imageUrl = row.teamLogoUrl,
      contentDescription = row.teamName.ifBlank { null },
      size = PrismIconSize.Small,
      style = PrismIconStyle.Bordered,
      tint = PrismIconTint.None,
    )
    Text(
      text = playerName,
      modifier = Modifier.weight(1f),
      style = Prism.typography.bodySmall,
      color = if (row.teamColorRole == MatchDetailPlayerStatsTeamColorRole.Accent) {
        Prism.color.accent
      } else {
        Prism.color.bodyColor
      },
      maxLines = 2,
      overflow = TextOverflow.Clip,
    )
  }
}

private fun playerStatsColumns(includeMapName: Boolean): List<PrismTableColumn> {
  val baseColumns = listOf(
    PrismTableColumn(key = MatchDetailStatsTableColumns.Player, title = "Player", width = 144.dp),
    PrismTableColumn(key = MatchDetailStatsTableColumns.Agent, title = "Agent", width = 116.dp),
    PrismTableColumn(key = MatchDetailStatsTableColumns.Acs, title = "ACS", width = 64.dp, textAlign = TextAlign.End),
    PrismTableColumn(key = MatchDetailStatsTableColumns.Kills, title = "K", width = 56.dp, textAlign = TextAlign.End),
    PrismTableColumn(key = MatchDetailStatsTableColumns.Deaths, title = "D", width = 56.dp, textAlign = TextAlign.End),
    PrismTableColumn(key = MatchDetailStatsTableColumns.Assists, title = "A", width = 56.dp, textAlign = TextAlign.End),
    PrismTableColumn(key = MatchDetailStatsTableColumns.Kast, title = "KAST", width = 72.dp, textAlign = TextAlign.End),
    PrismTableColumn(
      key = MatchDetailStatsTableColumns.Rating,
      title = "Rating",
      width = 76.dp,
      textAlign = TextAlign.End,
    ),
  )

  return if (includeMapName) {
    listOf(PrismTableColumn(key = MatchDetailStatsTableColumns.Map, title = "Map", width = 96.dp)) + baseColumns
  } else {
    baseColumns
  }
}

private fun MatchDetailPlayerStatsRow.toPrismTableRow(
  includeMapName: Boolean,
  onPlayerSelected: ((String) -> Unit)?,
): PrismTableRow {
  val cells = mutableMapOf(
    MatchDetailStatsTableColumns.Player to playerName,
    MatchDetailStatsTableColumns.Agent to agentNames,
    MatchDetailStatsTableColumns.Acs to acs,
    MatchDetailStatsTableColumns.Kills to kills,
    MatchDetailStatsTableColumns.Deaths to deaths,
    MatchDetailStatsTableColumns.Assists to assists,
    MatchDetailStatsTableColumns.Kast to kast,
    MatchDetailStatsTableColumns.Rating to rating,
  )
  if (includeMapName) {
    cells[MatchDetailStatsTableColumns.Map] = mapName.orEmpty()
  }

  return PrismTableRow(
    key = key,
    cells = cells,
    onClick = playerId?.let { id -> onPlayerSelected?.let { onClick -> { onClick(id) } } },
    cellTextAlignments = mapOf(
      MatchDetailStatsTableColumns.Acs to TextAlign.End,
      MatchDetailStatsTableColumns.Kills to TextAlign.End,
      MatchDetailStatsTableColumns.Deaths to TextAlign.End,
      MatchDetailStatsTableColumns.Assists to TextAlign.End,
      MatchDetailStatsTableColumns.Kast to TextAlign.End,
      MatchDetailStatsTableColumns.Rating to TextAlign.End,
    ),
  )
}
