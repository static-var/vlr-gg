/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismButtonPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismButtonPreviewContent(variant, PrismThemeFamily.Brutalist)
}

@PrismPreview
@Composable
internal fun PrismButtonConsolePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismButtonPreviewContent(variant, PrismThemeFamily.Console)
}

@Composable
private fun PrismButtonPreviewContent(variant: PrismVariant, family: PrismThemeFamily) {
  PrismTheme(variant = variant, family = family) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("Primary", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismButton(onClick = {}, style = PrismButtonStyle.Primary) { Text("Primary Button") }

      Text("Alternate", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismButton(onClick = {}, style = PrismButtonStyle.Alternate) { Text("Alternate Button") }

      Text("Secondary", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismButton(onClick = {}, style = PrismButtonStyle.Secondary) {
        Text("Secondary Button")
      }

      Text("Tertiary", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismButton(onClick = {}, style = PrismButtonStyle.Tertiary) { Text("Tertiary Button") }

      Text("Disabled States", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismButton(onClick = {}, style = PrismButtonStyle.Primary, enabled = false) {
        Text("Disabled Primary")
      }
      PrismButton(onClick = {}, style = PrismButtonStyle.Alternate, enabled = false) {
        Text("Disabled Alternate")
      }
      PrismButton(onClick = {}, style = PrismButtonStyle.Secondary, enabled = false) {
        Text("Disabled Secondary")
      }
      PrismButton(onClick = {}, style = PrismButtonStyle.Tertiary, enabled = false) {
        Text("Disabled Tertiary")
      }
    }
  }
}
