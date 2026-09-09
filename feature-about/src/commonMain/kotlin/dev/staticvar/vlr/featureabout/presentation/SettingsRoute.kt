/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.dropdown.PrismDropdown
import dev.staticvar.designsystem.component.dropdown.PrismDropdownOption
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.slider.PrismSlider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.AppearanceMode
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.MascotPreference
import dev.staticvar.vlr.core.settings.MascotVisitFrequency
import dev.staticvar.vlr.core.settings.ThemeFamily
import kotlin.math.roundToInt

/** Preferences shared by Android and iOS. Changes apply immediately. */
@Composable
public fun SettingsRoute(
  isDark: Boolean,
  family: ThemeFamily,
  catppuccinFlavour: CatppuccinFlavour,
  mascot: MascotPreference,
  mascotVisitFrequency: MascotVisitFrequency,
  onModeSelected: (AppearanceMode) -> Unit,
  onFamilySelected: (ThemeFamily) -> Unit,
  onFlavourSelected: (CatppuccinFlavour) -> Unit,
  onMascotSelected: (MascotPreference) -> Unit,
  onMascotVisitFrequencySelected: (MascotVisitFrequency) -> Unit,
  onAbout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM)) {
    PrismScreenTitleBar(title = "Settings", subtitle = "Your look. Your companion.")
    Column(
      modifier = Modifier.weight(1f).cardMascotViewport().verticalScroll(rememberScrollState())
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
      PrismCard(
        modifier = Modifier.fillMaxWidth().cardMascotEligible(topClearance = Prism.dimens.spacingM),
        style = PrismCardStyle.Outlined,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
          PrismSectionTitle(title = "Mascot")
          Text(
            "A little company while you browse, with a celebration for big wins. Pick a companion or turn mascots off.",
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
          if (mascot != MascotPreference.Off) {
            MascotVisitFrequencySlider(mascotVisitFrequency, onMascotVisitFrequencySelected)
          }
        }
      }
      PrismCard(
        modifier = Modifier.fillMaxWidth().cardMascotEligible(topClearance = Prism.dimens.spacingM),
        style = PrismCardStyle.Outlined,
        onClick = onAbout,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
            Text("About VLR", style = Prism.typography.sectionTitle, color = Prism.color.contentPrimary)
            Text("The project, the people, and the data.", style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
          }
          Text("→", style = Prism.typography.sectionTitle, color = Prism.color.accent)
        }
      }
      Spacer(Modifier.height(80.dp))
      Text(
        text = "VLR\n${appVersionText()}",
        modifier = Modifier.fillMaxWidth(),
        style = Prism.typography.caption,
        color = Prism.color.captionColor,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
internal expect fun appVersionText(): String

@Composable
private fun MascotVisitFrequencySlider(
  frequency: MascotVisitFrequency,
  onSelected: (MascotVisitFrequency) -> Unit,
) {
  val options = MascotVisitFrequency.entries
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text("Surprise visits", style = Prism.typography.bodyLarge, color = Prism.color.contentPrimary)
    PrismSlider(
      value = options.indexOf(frequency).toFloat(),
      onValueChange = { onSelected(options[it.roundToInt().coerceIn(options.indices)]) },
      valueRange = 0f..3f,
      steps = 2,
      stopLabels = options.map { it.label },
      modifier = Modifier.fillMaxWidth().semantics {
        contentDescription = "Surprise visits"
        stateDescription = frequency.accessibilityLabel
      },
    )
    Text(
      text = if (frequency == MascotVisitFrequency.None) {
        "Card visits are off. Win celebrations stay on."
      } else {
        "Chance on each new screen with room for a mascot."
      },
      style = Prism.typography.bodySmall,
      color = Prism.color.bodyColor,
    )
  }
}

private val MascotVisitFrequency.label: String
  get() = when (this) {
    MascotVisitFrequency.None -> "None"
    MascotVisitFrequency.Sometimes -> "Sometimes"
    MascotVisitFrequency.Frequently -> "Frequently"
    MascotVisitFrequency.Yes -> "YES"
  }

private val MascotVisitFrequency.accessibilityLabel: String
  get() = "$label, $probabilityPercent percent chance per screen"

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
