/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.CogSolid
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismFabSheetPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  var expanded by remember { mutableStateOf(false) }
  PrismTheme(variant = variant) {
    Box(Modifier.fillMaxSize().background(Prism.color.background)) {
      PrismFabSheet(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        icon = LineAwesomeIcons.CogSolid,
        contentDescription = "Customize match details",
        sheetTitle = "Match details",
        header = { Text("Match details", style = Prism.typography.cardTitle) },
        footer = { PrismButton(onClick = { expanded = false }) { Text("Done") } },
      ) {
        Text("Choose the sections you want to see.", style = Prism.typography.bodyLarge)
      }
    }
  }
}
