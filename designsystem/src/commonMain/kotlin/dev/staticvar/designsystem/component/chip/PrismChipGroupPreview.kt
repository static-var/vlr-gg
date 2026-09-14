/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismChipGroupPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedChipId by remember { mutableStateOf("completed") }
    var selectedChipIds by remember { mutableStateOf(setOf("completed", "live")) }
    val icons = Prism.icons
    val chips =
      remember(icons) {
        listOf(
          PrismChip(id = "all", label = "All"),
          PrismChip(
            id = "live",
            label = "Live",
            icon = icons.events.unselected,
            selectedIcon = icons.events.selected,
          ),
          PrismChip(
            id = "completed",
            label = "Completed",
            icon = icons.matches.unselected,
            selectedIcon = icons.matches.selected,
          ),
          PrismChip(
            id = "upcoming",
            label = "Upcoming",
            icon = icons.news.unselected,
            enabled = false,
          ),
        )
      }

    Column(
      modifier =
      Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("Single Select", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismChipGroup(
        chips = chips,
        selectedChipId = selectedChipId,
        onChipSelected = { selectedChipId = it.id },
      )

      Text("Multi Select", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismChipGroup(
        chips = chips,
        selectedChipIds = selectedChipIds,
        onSelectionChange = { selectedChipIds = it },
      )

      Text("Disabled", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismChipGroup(
        chips = chips,
        selectedChipId = "completed",
        onChipSelected = {},
        enabled = false,
      )
    }
  }
}
