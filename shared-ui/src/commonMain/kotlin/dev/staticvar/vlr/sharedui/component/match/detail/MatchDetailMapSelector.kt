/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.dropdown.PrismDropdown
import dev.staticvar.designsystem.component.dropdown.PrismDropdownOption
import dev.staticvar.vlr.domain.model.MapData
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_group
import vlr.shared_ui.generated.resources.match_event_map_upper

/**
 * Dropdown selector for match detail map breakdowns.
 *
 * Adds an `All maps` option when at least two maps are available. The caller owns the selected map
 * index; `null` represents the aggregate all-maps state.
 */
@Composable
public fun MatchDetailMapSelector(
  maps: List<MapData>,
  selectedMapIndex: Int?,
  onMapSelected: (Int?) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onMenuExpandedChange: (Boolean) -> Unit = {},
) {
  val labels = matchFormattingLabels()
  val mapOptions = remember(maps, labels) { maps.matchDetailMapOptions(labels) }
  val dropdownOptions = remember(mapOptions) {
    mapOptions.map { option -> PrismDropdownOption(id = option.id, label = option.label) }
  }
  val selectedOptionId = maps.resolveSelectedMapOptionId(selectedMapIndex)
  val label = if (selectedOptionId ==
    AllMapsOptionId
  ) {
    stringResource(Res.string.match_event_group)
  } else {
    stringResource(Res.string.match_event_map_upper)
  }

  PrismDropdown(
    options = dropdownOptions,
    selectedOptionId = selectedOptionId,
    onOptionSelected = { option -> onMapSelected(option.id.toSelectedMapIndex()) },
    modifier = modifier,
    label = label,
    enabled = enabled && dropdownOptions.isNotEmpty(),
    onExpandedChange = onMenuExpandedChange,
  )
}

private fun String.toSelectedMapIndex(): Int? = if (this == AllMapsOptionId) null else toIntOrNull()
