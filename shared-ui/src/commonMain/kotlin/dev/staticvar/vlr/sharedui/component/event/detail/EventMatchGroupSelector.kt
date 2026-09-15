/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.dropdown.PrismDropdown
import dev.staticvar.designsystem.component.dropdown.PrismDropdownOption
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_group_matches_by
import vlr.shared_ui.generated.resources.match_event_matches
import vlr.shared_ui.generated.resources.match_event_schedule

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
  val groupTabs =
    remember(groupNames) {
      groupNames.map { groupName -> PrismTab(id = groupName, label = groupName) }
    }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismSectionTitle(
      title = stringResource(Res.string.match_event_matches),
      preLabel = stringResource(Res.string.match_event_schedule),
      trailing = {
        PrismDropdown(
          options = groupingOptions,
          selectedOptionId = grouping.name,
          onOptionSelected = { option -> onGroupingSelected(EventMatchGrouping.valueOf(option.id)) },
          label = stringResource(Res.string.match_event_group_matches_by),
          onExpandedChange = onMenuExpandedChange,
        )
      },
    )
    if (groupTabs.isNotEmpty()) {
      PrismTabs(
        tabs = groupTabs,
        selectedTabId = selectedGroupName ?: groupTabs.first().id,
        onTabSelected = { tab -> onGroupSelected(tab.id) },
        modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      )
    }
  }
}

@Composable
private fun EventMatchGrouping.toDropdownOption(): PrismDropdownOption = PrismDropdownOption(id = name, label = label)
