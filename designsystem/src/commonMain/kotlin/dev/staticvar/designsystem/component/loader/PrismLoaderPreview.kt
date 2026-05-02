/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.loader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismLoaderPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text("PrismLoader", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
        PrismLoader(size = PrismLoaderSize.Small)
        PrismLoader(size = PrismLoaderSize.Medium, label = "SYNCING")
        PrismLoader(size = PrismLoaderSize.Large, label = "LOADING MATCH CENTER")
      }
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
        PrismLoader(size = PrismLoaderSize.Medium, label = "")
        PrismLoader(size = PrismLoaderSize.Large, label = "")
      }
    }
  }
}

@PrismPreview
@Composable
internal fun PrismFullscreenLoaderPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Box(
      modifier =
      Modifier.fillMaxWidth()
        .height(280.dp)
        .background(Prism.color.background),
    ) {
      PrismFullscreenLoader(
        modifier = Modifier.fillMaxSize(),
        label = "SYNCING",
        supportingText = "Fetching standings and match timeline",
      )
    }
  }
}
