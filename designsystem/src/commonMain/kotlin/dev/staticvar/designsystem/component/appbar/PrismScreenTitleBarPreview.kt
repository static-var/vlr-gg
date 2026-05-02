/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.BarsSolid
import compose.icons.lineawesomeicons.EllipsisHSolid
import compose.icons.lineawesomeicons.SearchSolid
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismScreenTitleBarPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      PrismScreenTitleBar(
        title = "Match Center",
        subtitle = "2 live · 4 upcoming",
        preLabel = "VCT Pacific",
        navigationSlot = {
          PrismIconButton(
            icon = LineAwesomeIcons.BarsSolid,
            contentDescription = "Open menu",
            onClick = {},
            size = PrismIconButtonSize.Small,
          )
        },
        actions = {
          PrismIconButton(
            icon = LineAwesomeIcons.SearchSolid,
            contentDescription = "Search",
            onClick = {},
            size = PrismIconButtonSize.Small,
          )
          PrismIconButton(
            icon = LineAwesomeIcons.EllipsisHSolid,
            contentDescription = "More actions",
            onClick = {},
            size = PrismIconButtonSize.Small,
          )
        },
      )

      PrismScreenTitleBar(
        title = "Standings",
        subtitle = "Updated 5m ago",
      )
    }
  }
}
