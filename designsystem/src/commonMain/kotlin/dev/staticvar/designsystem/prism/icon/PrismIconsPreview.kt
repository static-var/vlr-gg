/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.icon.PrismIcon
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismIconsPreview(@PreviewParameter(IconThemePreviewProvider::class) theme: IconThemePreviewCase) {
  PrismTheme(variant = theme.variant, family = theme.family, catppuccinFlavour = theme.flavour) {
    val icons = Prism.icons
    Column(
      modifier = Modifier.background(Prism.color.background).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(text = theme.label, color = Prism.color.contentPrimary)
      val actions = listOf(icons.refresh, icons.share, icons.preview, icons.back)
      IconPreviewRow("Actions", actions)
      Text(text = "Disabled actions", color = Prism.color.contentPrimary)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { icon ->
          PrismIconButton(icon = icon, contentDescription = icon.name, onClick = {}, enabled = false)
        }
      }
      val navigation = listOf(icons.home, icons.news, icons.matches, icons.events, icons.rankings, icons.settings)
      IconPreviewRow("Navigation", navigation.map { it.unselected })
      IconPreviewRow("Selected", navigation.map { it.selected })
    }
  }
}

@Composable
private fun IconPreviewRow(label: String, icons: List<ImageVector>) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = label, color = Prism.color.contentPrimary)
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
      icons.forEach { icon -> PrismIcon(imageVector = icon, contentDescription = icon.name, style = PrismIconStyle.Plain) }
    }
  }
}

internal data class IconThemePreviewCase(
  val family: PrismThemeFamily,
  val variant: PrismVariant,
  val flavour: PrismCatppuccinFlavour = PrismCatppuccinFlavour.Latte,
) {
  val label: String
    get() = if (family == PrismThemeFamily.Catppuccin) "${family.name} ${flavour.name}" else "${family.name} ${variant.name}"
}

internal class IconThemePreviewProvider : PreviewParameterProvider<IconThemePreviewCase> {
  override val values: Sequence<IconThemePreviewCase> = sequence {
    listOf(PrismThemeFamily.Brutalist, PrismThemeFamily.Console).forEach { family ->
      PrismVariant.entries.forEach { variant -> yield(IconThemePreviewCase(family, variant)) }
    }
    PrismCatppuccinFlavour.entries.forEach { flavour ->
      yield(
        IconThemePreviewCase(
          family = PrismThemeFamily.Catppuccin,
          variant = if (flavour.isDark) PrismVariant.Dark else PrismVariant.Light,
          flavour = flavour,
        ),
      )
    }
  }
}
