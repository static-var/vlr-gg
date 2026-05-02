/*
 * Copyright (c) 2022 Shreyansh Lodha
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
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.SearchSolid
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismIconButtonPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
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
          icon = LineAwesomeIcons.SearchSolid,
          contentDescription = "Search small",
          onClick = {},
          size = PrismIconButtonSize.Small,
        )
        PrismIconButton(
          icon = LineAwesomeIcons.SearchSolid,
          contentDescription = "Search medium",
          onClick = {},
          size = PrismIconButtonSize.Medium,
        )
        PrismIconButton(
          icon = LineAwesomeIcons.SearchSolid,
          contentDescription = "Search large",
          onClick = {},
          size = PrismIconButtonSize.Large,
        )
        PrismIconButton(
          icon = LineAwesomeIcons.SearchSolid,
          contentDescription = "Search extra large",
          onClick = {},
          size = PrismIconButtonSize.XL,
        )
      }

      Text(text = "States", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismIconButton(
          icon = LineAwesomeIcons.SearchSolid,
          contentDescription = "Search default",
          onClick = {},
        )
        PrismIconButton(
          icon = LineAwesomeIcons.SearchSolid,
          contentDescription = "Search selected",
          onClick = {},
          selected = true,
        )
        PrismIconButton(
          icon = LineAwesomeIcons.SearchSolid,
          contentDescription = "Search disabled",
          onClick = {},
          enabled = false,
        )
      }
    }
  }
}
