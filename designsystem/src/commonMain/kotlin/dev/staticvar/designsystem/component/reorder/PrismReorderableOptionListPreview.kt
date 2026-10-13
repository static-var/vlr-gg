/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.reorder

import androidx.compose.foundation.background
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
internal fun PrismReorderableOptionListPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var options by remember {
      mutableStateOf(
        listOf(
          PrismReorderableOption("summary", "Summary", "Key numbers at a glance", checked = true),
          PrismReorderableOption("schedule", "Schedule", "Every fixture by date", checked = false),
          PrismReorderableOption("archive", "Archive", "Nothing to show yet", checked = true, toggleEnabled = false),
        ),
      )
    }
    PrismReorderableOptionList(
      options = options,
      onCheckedChange = { key, checked ->
        options = options.map { if (it.key == key) it.copy(checked = checked) else it }
      },
      onMove = { key, index ->
        val moved = options.first { it.key == key }
        options = (options - moved).toMutableList().apply { add(index, moved) }
      },
      modifier = Modifier.background(Prism.color.background).padding(Prism.dimens.spacingM).fillMaxWidth(),
    )
  }
}
