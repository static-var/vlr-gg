/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismFavoriteIconPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      PrismFavoriteIconPreviewRow()
      PrismFavoriteIconBarePreviewRow()
    }
  }
}

@Composable
private fun PrismFavoriteIconPreviewRow() {
  Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismFavoriteIcon(selected = false, size = PrismFavoriteIconSize.Small)
    PrismFavoriteIcon(selected = true, size = PrismFavoriteIconSize.Small)
    PrismFavoriteIcon(selected = false, size = PrismFavoriteIconSize.Medium)
    PrismFavoriteIcon(selected = true, size = PrismFavoriteIconSize.Medium)
    PrismFavoriteIcon(selected = false, size = PrismFavoriteIconSize.Large)
    PrismFavoriteIcon(selected = true, size = PrismFavoriteIconSize.Large)
  }
}

@Composable
private fun PrismFavoriteIconBarePreviewRow() {
  Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismFavoriteIcon(selected = false, size = PrismFavoriteIconSize.Small, style = PrismFavoriteIconStyle.Bare)
    PrismFavoriteIcon(selected = true, size = PrismFavoriteIconSize.Small, style = PrismFavoriteIconStyle.Bare)
    PrismFavoriteIcon(selected = false, size = PrismFavoriteIconSize.Medium, style = PrismFavoriteIconStyle.Bare)
    PrismFavoriteIcon(selected = true, size = PrismFavoriteIconSize.Medium, style = PrismFavoriteIconStyle.Bare)
    PrismFavoriteIcon(selected = false, size = PrismFavoriteIconSize.Large, style = PrismFavoriteIconStyle.Bare)
    PrismFavoriteIcon(selected = true, size = PrismFavoriteIconSize.Large, style = PrismFavoriteIconStyle.Bare)
  }
}
