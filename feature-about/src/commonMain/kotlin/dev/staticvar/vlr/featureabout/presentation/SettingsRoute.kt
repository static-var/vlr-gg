/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.dropdown.PrismDropdown
import dev.staticvar.designsystem.component.dropdown.PrismDropdownOption
import dev.staticvar.designsystem.component.selection.PrismSegmentedButtonOption
import dev.staticvar.designsystem.component.selection.PrismSegmentedButtons
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.AppearanceMode
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.ThemeFamily

/** Appearance controls shared by Android, iOS, and desktop. Changes apply immediately. */
@Composable
public fun SettingsRoute(
  isDark: Boolean,
  family: ThemeFamily,
  catppuccinFlavour: CatppuccinFlavour,
  onModeSelected: (AppearanceMode) -> Unit,
  onFamilySelected: (ThemeFamily) -> Unit,
  onFlavourSelected: (CatppuccinFlavour) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
      .verticalScroll(rememberScrollState())
      .padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = "Settings",
      subtitle = "Make VLR feel right for you",
      onBackPress = onBack,
    )
    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
        PrismSectionTitle(title = "Theme", preLabel = "color")
        Text(
          "Choose the palette that feels right for you.",
          style = Prism.typography.bodySmall,
          color = Prism.color.bodyColor,
        )
        PrismSegmentedButtons(
          options = familyOptions,
          selectedOptionId = family.name,
          onOptionSelected = { onFamilySelected(ThemeFamily.valueOf(it.id)) },
        )
      }
    }
    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
        if (family == ThemeFamily.Brutalist) {
          PrismSectionTitle(title = "Appearance", preLabel = "display")
          Text("Choose a light or dark look.", style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
          PrismSegmentedButtons(
            options = modeOptions,
            selectedOptionId = if (isDark) AppearanceMode.Dark.name else AppearanceMode.Light.name,
            onOptionSelected = { onModeSelected(AppearanceMode.valueOf(it.id)) },
          )
        } else {
          PrismSectionTitle(title = "Flavour", preLabel = "catppuccin")
          Text(
            "Latte is light. Frappé, Macchiato, and Mocha offer three shades of dark.",
            style = Prism.typography.bodySmall,
            color = Prism.color.bodyColor,
          )
          PrismDropdown(
            modifier = Modifier.fillMaxWidth(),
            options = flavourOptions,
            selectedOptionId = catppuccinFlavour.name,
            onOptionSelected = { onFlavourSelected(CatppuccinFlavour.valueOf(it.id)) },
          )
        }
      }
    }
  }
}

private val modeOptions = listOf(
  PrismSegmentedButtonOption(AppearanceMode.Light.name, "Light"),
  PrismSegmentedButtonOption(AppearanceMode.Dark.name, "Dark"),
)

private val familyOptions = listOf(
  PrismSegmentedButtonOption(ThemeFamily.Brutalist.name, "Brutalist"),
  PrismSegmentedButtonOption(ThemeFamily.Catppuccin.name, "Catppuccin"),
)

private val flavourOptions = listOf(
  PrismDropdownOption(CatppuccinFlavour.Latte.name, "Latte"),
  PrismDropdownOption(CatppuccinFlavour.Frappe.name, "Frappé"),
  PrismDropdownOption(CatppuccinFlavour.Macchiato.name, "Macchiato"),
  PrismDropdownOption(CatppuccinFlavour.Mocha.name, "Mocha"),
)
