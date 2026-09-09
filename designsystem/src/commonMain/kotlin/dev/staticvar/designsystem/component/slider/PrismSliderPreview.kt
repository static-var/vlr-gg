/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.slider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismSliderPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismSliderPreviewContent(variant, PrismThemeFamily.Brutalist)
}

@PrismPreview
@Composable
internal fun PrismSliderConsolePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismSliderPreviewContent(variant, PrismThemeFamily.Console)
}

@PrismPreview
@Composable
internal fun PrismSliderCatppuccinPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismSliderPreviewContent(variant, PrismThemeFamily.Catppuccin)
}

@Composable
private fun PrismSliderPreviewContent(variant: PrismVariant, family: PrismThemeFamily) {
  PrismTheme(variant = variant, family = family) {
    var steppedValue by remember { mutableFloatStateOf(2f) }
    var continuousValue by remember { mutableFloatStateOf(0.42f) }
    var rtlValue by remember { mutableFloatStateOf(1f) }

    Column(
      modifier = Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Text("Four stops · ${steppedValue.toInt() + 1} of 4", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismSlider(
        value = steppedValue,
        onValueChange = { steppedValue = it },
        modifier = Modifier.fillMaxWidth(),
        valueRange = 0f..3f,
        steps = 2,
        stopLabels = listOf("None", "Sometimes", "Frequently", "YES"),
      )

      Text("Continuous · ${(continuousValue * 100).toInt()}%", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismSlider(
        value = continuousValue,
        onValueChange = { continuousValue = it },
        modifier = Modifier.fillMaxWidth(),
      )

      Text("Disabled", style = Prism.typography.label, color = Prism.color.labelColor)
      PrismSlider(
        value = 2f,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
        valueRange = 0f..3f,
        steps = 2,
        stopLabels = listOf("None", "Sometimes", "Frequently", "YES"),
      )

      Text("Four stops · RTL", style = Prism.typography.label, color = Prism.color.labelColor)
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PrismSlider(
          value = rtlValue,
          onValueChange = { rtlValue = it },
          modifier = Modifier.fillMaxWidth(),
          valueRange = 0f..3f,
          steps = 2,
          stopLabels = listOf("None", "Sometimes", "Frequently", "YES"),
        )
      }
    }
  }
}
