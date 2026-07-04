/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
        .background(Prism.color.background),
    ) {
      PrismScreenTitleBar(
        title = "Matches",
        subtitle = "Live and upcoming games",
        actions = {
          Text(
            text = "Filter",
            style = Prism.typography.button,
            color = Prism.color.accent,
          )
        },
        modifier = Modifier.fillMaxWidth(),
        onBackPress = {},
      )
      PrismScreenTitleBar(
        title = "Matches",
        subtitle = "Live and upcoming games",
        actions = {
          Text(
            text = "Filter",
            style = Prism.typography.button,
            color = Prism.color.accent,
          )
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
