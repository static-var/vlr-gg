/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
internal fun PrismSegmentedButtonsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedId by remember { mutableStateOf("catppuccin") }
    val options = remember {
      listOf(
        PrismSegmentedButtonOption("brutalist", "Brutalist"),
        PrismSegmentedButtonOption("catppuccin", "Catppuccin"),
      )
    }
    Column(
      modifier = Modifier.fillMaxWidth().background(Prism.color.background).padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      PrismSegmentedButtons(options, selectedId, { selectedId = it.id })
      PrismSegmentedButtons(options, "brutalist", {}, enabled = false)
      PrismSegmentedButtons(
        options = listOf(
          PrismSegmentedButtonOption("light", "Light"),
          PrismSegmentedButtonOption("dark", "Dark"),
          PrismSegmentedButtonOption("system", "System", enabled = false),
        ),
        selectedOptionId = "light",
        onOptionSelected = {},
      )
    }
  }
}
