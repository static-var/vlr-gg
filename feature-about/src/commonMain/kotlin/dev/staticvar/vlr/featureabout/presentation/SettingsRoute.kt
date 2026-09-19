/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
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
import dev.staticvar.designsystem.component.selection.PrismSwitch
import dev.staticvar.designsystem.component.slider.PrismSlider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.AppearanceMode
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.MascotPreference
import dev.staticvar.vlr.core.settings.MascotVisitFrequency
import dev.staticvar.vlr.core.settings.ThemeFamily
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.about_val_esports
import vlr.feature_about.generated.resources.appearance
import vlr.feature_about.generated.resources.auto_cleanup
import vlr.feature_about.generated.resources.card_visits_are_off_win_celebrations_stay_on
import vlr.feature_about.generated.resources.chance_on_each_new_screen_with_room_for_a_mascot
import vlr.feature_about.generated.resources.companion
import vlr.feature_about.generated.resources.dark
import vlr.feature_about.generated.resources.deleted_cached_items
import vlr.feature_about.generated.resources.experimental
import vlr.feature_about.generated.resources.flavour
import vlr.feature_about.generated.resources.flavour_frappe
import vlr.feature_about.generated.resources.flavour_latte
import vlr.feature_about.generated.resources.flavour_macchiato
import vlr.feature_about.generated.resources.flavour_mocha
import vlr.feature_about.generated.resources.frequently
import vlr.feature_about.generated.resources.how_val_esports_handles_your_data
import vlr.feature_about.generated.resources.light
import vlr.feature_about.generated.resources.lynx_cat
import vlr.feature_about.generated.resources.make_it_yours
import vlr.feature_about.generated.resources.mascot
import vlr.feature_about.generated.resources.mascot_description
import vlr.feature_about.generated.resources.mascot_frequency_accessibility
import vlr.feature_about.generated.resources.mode
import vlr.feature_about.generated.resources.none
import vlr.feature_about.generated.resources.off
import vlr.feature_about.generated.resources.privacy_policy
import vlr.feature_about.generated.resources.remove_cached_items_that_haven_t_been_refreshed_in_30_days
import vlr.feature_about.generated.resources.rosie_dog
import vlr.feature_about.generated.resources.settings
import vlr.feature_about.generated.resources.sometimes
import vlr.feature_about.generated.resources.surprise_visits
import vlr.feature_about.generated.resources.terms_of_service
import vlr.feature_about.generated.resources.the_latest_features_and_improvements
import vlr.feature_about.generated.resources.the_project_the_people_and_the_data
import vlr.feature_about.generated.resources.theme
import vlr.feature_about.generated.resources.theme_brutalist
import vlr.feature_about.generated.resources.theme_catppuccin
import vlr.feature_about.generated.resources.theme_console
import vlr.feature_about.generated.resources.using_the_app_and_its_content
import vlr.feature_about.generated.resources.val_esports
import vlr.feature_about.generated.resources.what_s_new
import vlr.feature_about.generated.resources.yes

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
  autoCleanupEnabled: Boolean,
  deletedCacheRecords: Long,
  onAutoCleanupChanged: (Boolean) -> Unit,
  onAbout: () -> Unit,
  onWhatsNew: () -> Unit,
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val uriHandler = LocalUriHandler.current
  Column(modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM)) {
    PrismScreenTitleBar(
      title = stringResource(Res.string.settings),
      subtitle = stringResource(Res.string.make_it_yours),
      onBackPress = onBack,
    )
    Column(
      modifier = Modifier.weight(1f).cardMascotViewport().verticalScroll(rememberScrollState())
        .padding(bottom = Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      AppearanceSettingsCard(
        isDark = isDark,
        family = family,
        catppuccinFlavour = catppuccinFlavour,
        onModeSelected = onModeSelected,
        onFamilySelected = onFamilySelected,
        onFlavourSelected = onFlavourSelected,
      )
      AppIconSettingsCard()
      CleanupSettingsCard(
        autoCleanupEnabled = autoCleanupEnabled,
        deletedCacheRecords = deletedCacheRecords,
        onAutoCleanupChanged = onAutoCleanupChanged,
      )
      MascotSettingsCard(
        mascot = mascot,
        mascotVisitFrequency = mascotVisitFrequency,
        onMascotSelected = onMascotSelected,
        onMascotVisitFrequencySelected = onMascotVisitFrequencySelected,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.what_s_new),
        description = stringResource(Res.string.the_latest_features_and_improvements),
        onClick = onWhatsNew,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.about_val_esports),
        description = stringResource(Res.string.the_project_the_people_and_the_data),
        onClick = onAbout,
      )
      SettingsLinkCard(
        title = stringResource(Res.string.privacy_policy),
        description = stringResource(Res.string.how_val_esports_handles_your_data),
        onClick = { uriHandler.openUri(AppWebsite.Privacy) },
      )
      SettingsLinkCard(
        title = stringResource(Res.string.terms_of_service),
        description = stringResource(Res.string.using_the_app_and_its_content),
        onClick = { uriHandler.openUri(AppWebsite.Terms) },
      )
      SettingsVersionFooter()
    }
  }
}

@Composable
private fun AppearanceSettingsCard(
  isDark: Boolean,
  family: ThemeFamily,
  catppuccinFlavour: CatppuccinFlavour,
  onModeSelected: (AppearanceMode) -> Unit,
  onFamilySelected: (ThemeFamily) -> Unit,
  onFlavourSelected: (CatppuccinFlavour) -> Unit,
) {
  PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      PrismSectionTitle(title = stringResource(Res.string.appearance))
      PrismDropdown(
        label = stringResource(Res.string.theme),
        modifier = Modifier.fillMaxWidth(),
        options = familyOptions(),
        selectedOptionId = family.name,
        onOptionSelected = { onFamilySelected(ThemeFamily.valueOf(it.id)) },
      )
      when (family) {
        ThemeFamily.Brutalist, ThemeFamily.Console -> PrismDropdown(
          label = stringResource(Res.string.mode),
          modifier = Modifier.fillMaxWidth(),
          options = modeOptions(),
          selectedOptionId = if (isDark) AppearanceMode.Dark.name else AppearanceMode.Light.name,
          onOptionSelected = { onModeSelected(AppearanceMode.valueOf(it.id)) },
        )
        ThemeFamily.Catppuccin -> PrismDropdown(
          label = stringResource(Res.string.flavour),
          modifier = Modifier.fillMaxWidth(),
          options = flavourOptions(),
          selectedOptionId = catppuccinFlavour.name,
          onOptionSelected = { onFlavourSelected(CatppuccinFlavour.valueOf(it.id)) },
        )
      }
    }
  }
}

@Composable
private fun CleanupSettingsCard(
  autoCleanupEnabled: Boolean,
  deletedCacheRecords: Long,
  onAutoCleanupChanged: (Boolean) -> Unit,
) {
  val autoCleanupLabel = stringResource(Res.string.auto_cleanup)
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      PrismSectionTitle(title = stringResource(Res.string.experimental))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        Text(
          text = stringResource(Res.string.auto_cleanup),
          modifier = Modifier.weight(1f),
          style = Prism.typography.bodyLarge,
          color = Prism.color.contentPrimary,
        )
        PrismSwitch(
          checked = autoCleanupEnabled,
          onCheckedChange = onAutoCleanupChanged,
          modifier = Modifier.semantics { contentDescription = autoCleanupLabel },
        )
      }
      Text(
        text = stringResource(Res.string.remove_cached_items_that_haven_t_been_refreshed_in_30_days),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
      Text(
        text = pluralStringResource(
          Res.plurals.deleted_cached_items,
          deletedCacheRecords.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
          deletedCacheRecords.toString(),
        ),
        style = Prism.typography.caption,
        color = Prism.color.captionColor,
      )
    }
  }
}

@Composable
private fun MascotSettingsCard(
  mascot: MascotPreference,
  mascotVisitFrequency: MascotVisitFrequency,
  onMascotSelected: (MascotPreference) -> Unit,
  onMascotVisitFrequencySelected: (MascotVisitFrequency) -> Unit,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth().cardMascotEligible(topClearance = Prism.dimens.spacingM),
    style = PrismCardStyle.Outlined,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      PrismSectionTitle(title = stringResource(Res.string.mascot))
      Text(
        stringResource(Res.string.mascot_description),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
      PrismDropdown(
        label = stringResource(Res.string.companion),
        modifier = Modifier.fillMaxWidth(),
        options = mascotOptions(),
        selectedOptionId = mascot.name,
        onOptionSelected = { onMascotSelected(MascotPreference.valueOf(it.id)) },
      )
      if (mascot != MascotPreference.Off) {
        MascotVisitFrequencySlider(mascotVisitFrequency, onMascotVisitFrequencySelected)
      }
    }
  }
}

@Composable
private fun SettingsLinkCard(title: String, description: String, onClick: () -> Unit) {
  PrismCard(
    modifier = Modifier.fillMaxWidth().cardMascotEligible(topClearance = Prism.dimens.spacingM),
    style = PrismCardStyle.Outlined,
    onClick = onClick,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        Text(title, style = Prism.typography.sectionTitle, color = Prism.color.contentPrimary)
        Text(description, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
      }
      Text("→", style = Prism.typography.sectionTitle, color = Prism.color.accent)
    }
  }
}

@Composable
private fun SettingsVersionFooter() {
  Spacer(Modifier.height(80.dp))
  Text(
    text = "${stringResource(Res.string.val_esports)}\n${appVersionText()}",
    modifier = Modifier.fillMaxWidth(),
    style = Prism.typography.caption,
    color = Prism.color.captionColor,
    textAlign = TextAlign.Center,
  )
}

@Composable
internal expect fun appVersionText(): String

@Composable
private fun MascotVisitFrequencySlider(
  frequency: MascotVisitFrequency,
  onSelected: (MascotVisitFrequency) -> Unit,
) {
  val options = MascotVisitFrequency.entries
  val surpriseVisitsLabel = stringResource(Res.string.surprise_visits)
  val frequencyDescription = frequency.accessibilityLabel
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(stringResource(Res.string.surprise_visits), style = Prism.typography.bodyLarge, color = Prism.color.contentPrimary)
    PrismSlider(
      value = options.indexOf(frequency).toFloat(),
      onValueChange = { onSelected(options[it.roundToInt().coerceIn(options.indices)]) },
      valueRange = 0f..3f,
      steps = 2,
      stopLabels = options.map { it.label },
      modifier = Modifier.fillMaxWidth().semantics {
        contentDescription = surpriseVisitsLabel
        stateDescription = frequencyDescription
      },
    )
    Text(
      text = if (frequency == MascotVisitFrequency.None) {
        stringResource(Res.string.card_visits_are_off_win_celebrations_stay_on)
      } else {
        stringResource(Res.string.chance_on_each_new_screen_with_room_for_a_mascot)
      },
      style = Prism.typography.bodySmall,
      color = Prism.color.bodyColor,
    )
  }
}

private val MascotVisitFrequency.label: String
  @Composable get() = when (this) {
    MascotVisitFrequency.None -> stringResource(Res.string.none)
    MascotVisitFrequency.Sometimes -> stringResource(Res.string.sometimes)
    MascotVisitFrequency.Frequently -> stringResource(Res.string.frequently)
    MascotVisitFrequency.Yes -> stringResource(Res.string.yes)
  }

private val MascotVisitFrequency.accessibilityLabel: String
  @Composable get() = stringResource(Res.string.mascot_frequency_accessibility, label, probabilityPercent)

@Composable
private fun modeOptions(): List<PrismDropdownOption> = listOf(
  PrismDropdownOption(AppearanceMode.Light.name, stringResource(Res.string.light)),
  PrismDropdownOption(AppearanceMode.Dark.name, stringResource(Res.string.dark)),
)

@Composable
private fun familyOptions(): List<PrismDropdownOption> = listOf(
  PrismDropdownOption(ThemeFamily.Brutalist.name, stringResource(Res.string.theme_brutalist)),
  PrismDropdownOption(ThemeFamily.Catppuccin.name, stringResource(Res.string.theme_catppuccin)),
  PrismDropdownOption(ThemeFamily.Console.name, stringResource(Res.string.theme_console)),
)

@Composable
private fun flavourOptions(): List<PrismDropdownOption> = listOf(
  PrismDropdownOption(CatppuccinFlavour.Latte.name, stringResource(Res.string.flavour_latte)),
  PrismDropdownOption(CatppuccinFlavour.Frappe.name, stringResource(Res.string.flavour_frappe)),
  PrismDropdownOption(CatppuccinFlavour.Macchiato.name, stringResource(Res.string.flavour_macchiato)),
  PrismDropdownOption(CatppuccinFlavour.Mocha.name, stringResource(Res.string.flavour_mocha)),
)

@Composable
private fun mascotOptions(): List<PrismDropdownOption> = listOf(
  PrismDropdownOption(MascotPreference.Lynx.name, stringResource(Res.string.lynx_cat)),
  PrismDropdownOption(MascotPreference.Rosie.name, stringResource(Res.string.rosie_dog)),
  PrismDropdownOption(MascotPreference.Off.name, stringResource(Res.string.off)),
)
