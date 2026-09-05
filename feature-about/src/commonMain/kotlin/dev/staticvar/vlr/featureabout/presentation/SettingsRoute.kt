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
import dev.staticvar.designsystem.component.navigation.PrismSegmentedFilterTab
import dev.staticvar.designsystem.component.navigation.PrismSegmentedFilterTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.AppearanceMode
import dev.staticvar.vlr.core.settings.ThemeFamily

/** Appearance controls shared by Android, iOS, and desktop. Changes apply immediately. */
@Composable
public fun SettingsRoute(
  isDark: Boolean,
  family: ThemeFamily,
  onModeSelected: (AppearanceMode) -> Unit,
  onFamilySelected: (ThemeFamily) -> Unit,
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
        PrismSectionTitle(title = "Appearance", preLabel = "display")
        Text("Choose a light or dark look.", style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
        PrismSegmentedFilterTabs(
          tabs = modeTabs,
          selectedTabId = if (isDark) AppearanceMode.Dark.name else AppearanceMode.Light.name,
          onTabSelected = { onModeSelected(AppearanceMode.valueOf(it.id)) },
        )
      }
    }
    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
        PrismSectionTitle(title = "Theme", preLabel = "color")
        Text(
          "Catppuccin uses Latte in light mode and Frappé in dark mode.",
          style = Prism.typography.bodySmall,
          color = Prism.color.bodyColor,
        )
        PrismSegmentedFilterTabs(
          tabs = familyTabs,
          selectedTabId = family.name,
          onTabSelected = { onFamilySelected(ThemeFamily.valueOf(it.id)) },
        )
      }
    }
  }
}

private val modeTabs = listOf(
  PrismSegmentedFilterTab(AppearanceMode.Light.name, "Light"),
  PrismSegmentedFilterTab(AppearanceMode.Dark.name, "Dark"),
)

private val familyTabs = listOf(
  PrismSegmentedFilterTab(ThemeFamily.Brutalist.name, "Brutalist"),
  PrismSegmentedFilterTab(ThemeFamily.Catppuccin.name, "Catppuccin"),
)
