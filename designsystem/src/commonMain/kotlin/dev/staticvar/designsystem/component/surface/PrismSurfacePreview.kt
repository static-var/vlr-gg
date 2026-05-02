/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismSurfacePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(
        "Automatic Content Colors",
        style = Prism.typography.headline,
        color = Prism.color.titleColor,
      )

      PrismSurfacePreviewSample(Prism.color.accent, "Accent Surface", "Content automatically uses light color")
      PrismSurfacePreviewSample(Prism.color.successContainer, "Success Container", "Content uses success color")
      PrismSurfacePreviewSample(Prism.color.warningContainer, "Warning Container", "Content uses warning color")
      PrismSurfacePreviewSample(Prism.color.dangerContainer, "Danger Container", "Content uses danger color")
      PrismSurfacePreviewSample(Prism.color.surfaceVariant, "Surface Variant", "Content uses primary text color")
    }
  }
}

@Composable
private fun PrismSurfacePreviewSample(color: Color, title: String, subtitle: String) {
  PrismSurface(
    color = color,
    shape = RoundedCornerShape(Prism.dimens.cornerM),
    modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      Text(title, style = Prism.typography.cardTitle)
      Text(subtitle, style = Prism.typography.bodySmall)
    }
  }
}
