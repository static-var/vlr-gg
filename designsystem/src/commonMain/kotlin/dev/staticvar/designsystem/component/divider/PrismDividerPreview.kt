/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.divider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
internal fun PrismDividerPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(text = "Hairline", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismDivider(style = PrismDividerStyle.Hairline)

      Text(text = "Default", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismDivider(style = PrismDividerStyle.Default)

      Text(text = "Strong", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismDivider(style = PrismDividerStyle.Strong)

      Text(text = "Vertical", style = Prism.typography.label, color = Prism.color.labelColor)
      Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        listOf(PrismDividerStyle.Hairline, PrismDividerStyle.Default, PrismDividerStyle.Strong).forEach { style ->
          Text(text = style.toString(), style = Prism.typography.label, color = Prism.color.labelColor)
          PrismVerticalDivider(style = style)
        }
      }
    }
  }
}
