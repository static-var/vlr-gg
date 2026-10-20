/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
internal fun PrismVerticalBarNavPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedItemId by remember { mutableStateOf("home") }
    val icons = Prism.icons
    val items = remember(icons) {
      listOf(
        PrismBottomNavItem("home", "Home", icons.home.unselected, icons.home.selected),
        PrismBottomNavItem("matches", "Matches", icons.matches.unselected, icons.matches.selected),
        PrismBottomNavItem("events", "Events", icons.events.unselected, icons.events.selected),
        PrismBottomNavItem("rankings", "Ranking", icons.rankings.unselected, icons.rankings.selected),
      )
    }

    Column(
      modifier = Modifier.background(Prism.color.background).padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(
        text = "VerticalBarNav",
        style = Prism.typography.label,
        color = Prism.color.labelColor,
      )
      PrismVerticalBarNav(
        items = items,
        selectedItemId = selectedItemId,
        onItemSelected = { selectedItemId = it.id },
      )
    }
  }
}
