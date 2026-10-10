/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.chip.PrismChip
import dev.staticvar.designsystem.component.chip.PrismChipGroup
import dev.staticvar.designsystem.component.dropdown.PrismDropdown
import dev.staticvar.designsystem.component.dropdown.PrismDropdownOption
import dev.staticvar.designsystem.component.dropdown.PrismDropdownStyle
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_group_by
import vlr.shared_ui.generated.resources.match_event_group_matches_by

/**
 * Match grouping controls for event detail schedules.
 */
@Composable
public fun EventMatchGroupSelector(
  grouping: EventMatchGrouping,
  groupNames: List<String>,
  selectedGroupName: String?,
  onGroupingSelected: (EventMatchGrouping) -> Unit,
  onGroupSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onMenuExpandedChange: (Boolean) -> Unit = {},
) {
  val groupingOptions = EventMatchGrouping.entries.map { groupingOption -> groupingOption.toDropdownOption() }
  val groupChips =
    remember(groupNames) {
      groupNames.map { groupName -> PrismChip(id = groupName, label = groupName) }
    }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(Res.string.match_event_group_by),
        modifier = Modifier.weight(1f),
        style = Prism.typography.bodySmall,
        color = Prism.color.labelColor,
      )
      PrismDropdown(
        options = groupingOptions,
        selectedOptionId = grouping.name,
        onOptionSelected = { option -> onGroupingSelected(EventMatchGrouping.valueOf(option.id)) },
        label = stringResource(Res.string.match_event_group_matches_by),
        style = PrismDropdownStyle.Compact,
        onExpandedChange = onMenuExpandedChange,
      )
    }
    if (groupChips.isNotEmpty()) {
      key(grouping) {
        PrismChipGroup(
          chips = groupChips,
          selectedChipId = selectedGroupName ?: groupChips.first().id,
          onChipSelected = { chip -> onGroupSelected(chip.id) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun EventMatchGrouping.toDropdownOption(): PrismDropdownOption = PrismDropdownOption(id = name, label = label)
