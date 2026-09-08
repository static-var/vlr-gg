/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.dropdown.PrismDropdown
import dev.staticvar.designsystem.component.dropdown.PrismDropdownOption
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.AppearanceMode
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.MascotPreference
import dev.staticvar.vlr.core.settings.ThemeFamily

/** Preferences shared by Android, iOS, and desktop. Changes apply immediately. */
@Composable
public fun SettingsRoute(
  isDark: Boolean,
  family: ThemeFamily,
  catppuccinFlavour: CatppuccinFlavour,
  mascot: MascotPreference,
  onModeSelected: (AppearanceMode) -> Unit,
  onFamilySelected: (ThemeFamily) -> Unit,
  onFlavourSelected: (CatppuccinFlavour) -> Unit,
  onMascotSelected: (MascotPreference) -> Unit,
  onAbout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM)) {
    PrismScreenTitleBar(title = "Settings", subtitle = "Your look. Your companion.")
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        .padding(bottom = Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
          PrismSectionTitle(title = "Appearance")
          PrismDropdown(
            label = "Theme",
            modifier = Modifier.fillMaxWidth(),
            options = familyOptions,
            selectedOptionId = family.name,
            onOptionSelected = { onFamilySelected(ThemeFamily.valueOf(it.id)) },
          )
          when (family) {
            ThemeFamily.Brutalist, ThemeFamily.Console -> PrismDropdown(
              label = "Mode",
              modifier = Modifier.fillMaxWidth(),
              options = modeOptions,
              selectedOptionId = if (isDark) AppearanceMode.Dark.name else AppearanceMode.Light.name,
              onOptionSelected = { onModeSelected(AppearanceMode.valueOf(it.id)) },
            )
            ThemeFamily.Catppuccin -> PrismDropdown(
              label = "Flavour",
              modifier = Modifier.fillMaxWidth(),
              options = flavourOptions,
              selectedOptionId = catppuccinFlavour.name,
              onOptionSelected = { onFlavourSelected(CatppuccinFlavour.valueOf(it.id)) },
            )
          }
        }
      }
      PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
          PrismSectionTitle(title = "Mascot")
          Text(
            "A little company for the big moments. Pick who peeks in during matches and events, or turn them off.",
            style = Prism.typography.bodySmall,
            color = Prism.color.bodyColor,
          )
          PrismDropdown(
            label = "Companion",
            modifier = Modifier.fillMaxWidth(),
            options = mascotOptions,
            selectedOptionId = mascot.name,
            onOptionSelected = { onMascotSelected(MascotPreference.valueOf(it.id)) },
          )
        }
      }
      PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined, onClick = onAbout) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
            Text("About VLR", style = Prism.typography.sectionTitle, color = Prism.color.contentPrimary)
            Text("The project, the people, and the data.", style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
          }
          Text("→", style = Prism.typography.sectionTitle, color = Prism.color.accent)
        }
      }
    }
  }
}

private val modeOptions = listOf(
  PrismDropdownOption(AppearanceMode.Light.name, "Light"),
  PrismDropdownOption(AppearanceMode.Dark.name, "Dark"),
)

private val familyOptions = listOf(
  PrismDropdownOption(ThemeFamily.Brutalist.name, "Brutalist"),
  PrismDropdownOption(ThemeFamily.Catppuccin.name, "Catppuccin"),
  PrismDropdownOption(ThemeFamily.Console.name, "Console"),
)

private val flavourOptions = listOf(
  PrismDropdownOption(CatppuccinFlavour.Latte.name, "Latte"),
  PrismDropdownOption(CatppuccinFlavour.Frappe.name, "Frappé"),
  PrismDropdownOption(CatppuccinFlavour.Macchiato.name, "Macchiato"),
  PrismDropdownOption(CatppuccinFlavour.Mocha.name, "Mocha"),
)

private val mascotOptions = listOf(
  PrismDropdownOption(MascotPreference.Lynx.name, "Lynx · Cat"),
  PrismDropdownOption(MascotPreference.Rosie.name, "Rosie · Dog"),
  PrismDropdownOption(MascotPreference.Off.name, "Off"),
)
