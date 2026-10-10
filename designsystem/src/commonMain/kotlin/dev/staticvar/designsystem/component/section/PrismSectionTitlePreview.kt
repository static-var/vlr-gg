/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant

@Preview(name = "Section titles - Light", group = "Prism/Section title", showBackground = true)
@Composable
internal fun PrismSectionTitleLightPreview() {
  PrismSectionTitlePreviewContent(variant = PrismVariant.Light)
}

@Preview(name = "Section titles - Dark", group = "Prism/Section title", showBackground = true)
@Composable
internal fun PrismSectionTitleDarkPreview() {
  PrismSectionTitlePreviewContent(variant = PrismVariant.Dark)
}

@Preview(name = "Console section titles - Light", group = "Prism/Section title", showBackground = true)
@Composable
internal fun PrismSectionTitleConsoleLightPreview() {
  PrismSectionTitlePreviewContent(PrismVariant.Light, PrismThemeFamily.Console)
}

@Preview(name = "Console section titles - Dark", group = "Prism/Section title", showBackground = true)
@Composable
internal fun PrismSectionTitleConsoleDarkPreview() {
  PrismSectionTitlePreviewContent(PrismVariant.Dark, PrismThemeFamily.Console)
}

@Preview(name = "Catppuccin section titles - Latte", group = "Prism/Section title", showBackground = true)
@Composable
internal fun PrismSectionTitleCatppuccinLightPreview() {
  PrismSectionTitlePreviewContent(PrismVariant.Light, PrismThemeFamily.Catppuccin)
}

@Preview(name = "Catppuccin section titles - Frappe", group = "Prism/Section title", showBackground = true)
@Composable
internal fun PrismSectionTitleCatppuccinDarkPreview() {
  PrismSectionTitlePreviewContent(PrismVariant.Dark, PrismThemeFamily.Catppuccin)
}

@Composable
private fun PrismSectionTitlePreviewContent(
  variant: PrismVariant,
  family: PrismThemeFamily = PrismThemeFamily.Brutalist,
) {
  PrismTheme(variant = variant, family = family) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingL),
    ) {
      PrismSectionTitle(title = "Maps", prefix = "02", trailing = { Text("3 PLAYED") })
      PrismSectionTitle(title = "Win probability", trailing = { Text("PRE-MATCH") })
      PrismSectionTitle(title = "Head to head", trailing = { PrismTag(text = "PRX leads 3–2") })
      PrismSectionTitle(title = "Streams & VODs")
      PrismSectionTitle(
        title = "A section title long enough to need truncation",
        prefix = "LR1",
        trailing = { Text("12") },
      )
      PrismSectionTitle(title = "Teams", style = PrismSectionTitleStyle.Subtle, trailing = { Text("4") })
    }
  }
}
