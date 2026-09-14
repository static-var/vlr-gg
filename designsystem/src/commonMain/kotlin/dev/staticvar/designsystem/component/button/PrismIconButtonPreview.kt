/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
internal fun PrismIconButtonPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismIconButtonPreviewContent(variant, PrismThemeFamily.Brutalist)
}

@PrismPreview
@Composable
internal fun PrismIconButtonConsolePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismIconButtonPreviewContent(variant, PrismThemeFamily.Console)
}

@Composable
private fun PrismIconButtonPreviewContent(variant: PrismVariant, family: PrismThemeFamily) {
  PrismTheme(variant = variant, family = family) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(text = "Sizes", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismIconButton(
          icon = Prism.icons.preview,
          contentDescription = "Preview small",
          onClick = {},
          size = PrismIconButtonSize.Small,
        )
        PrismIconButton(
          icon = Prism.icons.preview,
          contentDescription = "Preview medium",
          onClick = {},
          size = PrismIconButtonSize.Medium,
        )
        PrismIconButton(
          icon = Prism.icons.preview,
          contentDescription = "Preview large",
          onClick = {},
          size = PrismIconButtonSize.Large,
        )
        PrismIconButton(
          icon = Prism.icons.preview,
          contentDescription = "Preview extra large",
          onClick = {},
          size = PrismIconButtonSize.XL,
        )
      }

      Text(text = "States", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismIconButton(
          icon = Prism.icons.preview,
          contentDescription = "Preview default",
          onClick = {},
        )
        PrismIconButton(
          icon = Prism.icons.preview,
          contentDescription = "Preview selected",
          onClick = {},
          selected = true,
        )
        PrismIconButton(
          icon = Prism.icons.preview,
          contentDescription = "Preview disabled",
          onClick = {},
          enabled = false,
        )
      }
    }
  }
}
