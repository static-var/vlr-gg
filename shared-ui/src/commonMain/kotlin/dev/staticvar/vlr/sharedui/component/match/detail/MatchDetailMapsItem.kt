/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MapData

/**
 * Reusable match-detail maps section.
 *
 * Keeps the section title and map dropdown in one left/right header row, while
 * [MatchDetailMapBreakdown] swaps the content for the selected dropdown value.
 */
@Composable
public fun MatchDetailMapsItem(
  maps: List<MapData>,
  selectedMapIndex: Int?,
  onMapSelected: (Int?) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onPlayerSelected: ((String) -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismSectionTitle(
      title = "Maps",
      preLabel = "breakdown",
      trailing = {
        MatchDetailMapsSelectorTrailing(
          maps = maps,
          selectedMapIndex = selectedMapIndex,
          onMapSelected = onMapSelected,
          enabled = enabled,
        )
      },
    )
    MatchDetailMapBreakdown(
      maps = maps,
      selectedMapIndex = selectedMapIndex,
      onPlayerSelected = onPlayerSelected,
    )
  }
}

@Composable
private fun RowScope.MatchDetailMapsSelectorTrailing(
  maps: List<MapData>,
  selectedMapIndex: Int?,
  onMapSelected: (Int?) -> Unit,
  enabled: Boolean,
) {
  MatchDetailMapSelector(
    maps = maps,
    selectedMapIndex = selectedMapIndex,
    onMapSelected = onMapSelected,
    enabled = enabled,
  )
}
