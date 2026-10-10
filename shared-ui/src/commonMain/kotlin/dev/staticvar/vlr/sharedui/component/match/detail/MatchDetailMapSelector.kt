/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.chip.PrismChip
import dev.staticvar.designsystem.component.chip.PrismChipGroup
import dev.staticvar.vlr.domain.model.MapData

/**
 * Chip bar selector for match detail map breakdowns.
 *
 * Shows an `All maps` chip followed by one chip per map with its score. Nothing is shown for a
 * single map, because there is nothing to choose. The caller owns the selected map index; `null`
 * represents the aggregate all-maps state.
 */
@Composable
public fun MatchDetailMapSelector(
  maps: List<MapData>,
  selectedMapIndex: Int?,
  onMapSelected: (Int?) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  if (maps.size < 2) return
  val labels = matchFormattingLabels()
  val chips = remember(maps, labels) {
    maps.matchDetailMapOptions(labels).map { option ->
      PrismChip(id = option.id, label = option.label, supportingText = option.supportingText)
    }
  }

  PrismChipGroup(
    chips = chips,
    selectedChipId = maps.resolveSelectedMapOptionId(selectedMapIndex),
    onChipSelected = { chip -> onMapSelected(chip.id.toSelectedMapIndex()) },
    modifier = modifier,
    enabled = enabled,
  )
}

private fun String.toSelectedMapIndex(): Int? = if (this == AllMapsOptionId) null else toIntOrNull()
