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
import androidx.compose.ui.Alignment
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
internal fun PrismRefreshButtonPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismRefreshButtonPreviewContent(variant, PrismThemeFamily.Brutalist)
}

@PrismPreview
@Composable
internal fun PrismRefreshButtonConsolePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismRefreshButtonPreviewContent(variant, PrismThemeFamily.Console)
}

@Composable
private fun PrismRefreshButtonPreviewContent(variant: PrismVariant, family: PrismThemeFamily) {
  PrismTheme(variant = variant, family = family) {
    Row(
      modifier = Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingL),
    ) {
      RefreshButtonState("Idle", isRefreshing = false, enabled = true)
      RefreshButtonState("Busy", isRefreshing = true, enabled = true)
      RefreshButtonState("Disabled", isRefreshing = false, enabled = false)
    }
  }
}

@Composable
private fun RefreshButtonState(label: String, isRefreshing: Boolean, enabled: Boolean) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismRefreshButton(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onClick = {},
      style = PrismIconButtonStyle.Bordered,
    )
    Text(label, style = Prism.typography.label, color = Prism.color.labelColor)
  }
}
