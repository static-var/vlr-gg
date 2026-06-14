/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.table.PrismTable
import dev.staticvar.designsystem.component.table.PrismTableColumn
import dev.staticvar.designsystem.component.table.PrismTableOptions
import dev.staticvar.designsystem.component.table.PrismTableRow
import dev.staticvar.vlr.domain.model.MapData

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

/**
 * Dense player stat table for one selected map.
 *
 * Uses [PrismTable] so sticky first-column behavior, scrolling, and table colors remain owned by
 * the design system.
 */
@Composable
public fun MatchDetailPlayerStatsTable(map: MapData, modifier: Modifier = Modifier) {
  MatchDetailPlayerStatsTable(
    rows = remember(map) { map.toPlayerStatsRows() },
    includeMapName = false,
    modifier = modifier,
  )
}

/**
 * Dense player stat table for aggregate all-map breakdowns.
 */
@Composable
public fun MatchDetailAllMapPlayerStatsTable(maps: List<MapData>, modifier: Modifier = Modifier) {
  MatchDetailPlayerStatsTable(
    rows = remember(maps) { maps.toAllMapPlayerStatsRows() },
    includeMapName = false,
    modifier = modifier,
  )
}

@Composable
private fun MatchDetailPlayerStatsTable(
  rows: List<MatchDetailPlayerStatsRow>,
  includeMapName: Boolean,
  modifier: Modifier = Modifier,
) {
  PrismTable(
    columns = remember(includeMapName) { playerStatsColumns(includeMapName = includeMapName) },
    rows = remember(rows, includeMapName) { rows.map { row -> row.toPrismTableRow(includeMapName = includeMapName) } },
    modifier = modifier,
    options = PrismTableOptions(
      stickyFirstColumn = true,
      rowHeight = 44.dp,
      headerHeight = 40.dp,
      bodyMaxLines = 2,
      bodyOverflow = TextOverflow.Clip,
      cellPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ),
  )
}

private fun playerStatsColumns(includeMapName: Boolean): List<PrismTableColumn> {
  val baseColumns = listOf(
    PrismTableColumn(key = MatchDetailStatsTableColumns.Player, title = "Player", width = 196.dp),
    PrismTableColumn(key = MatchDetailStatsTableColumns.Agent, title = "Agent", width = 148.dp),
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
    listOf(PrismTableColumn(key = MatchDetailStatsTableColumns.Map, title = "Map", width = 132.dp)) + baseColumns
  } else {
    baseColumns
  }
}

private fun MatchDetailPlayerStatsRow.toPrismTableRow(includeMapName: Boolean): PrismTableRow {
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
