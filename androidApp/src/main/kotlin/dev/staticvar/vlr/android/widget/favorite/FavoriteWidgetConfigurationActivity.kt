/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.selection.PrismRadioButton
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.core.settings.AppearanceRepository
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.ThemeFamily
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin

class FavoriteWidgetConfigurationActivity : ComponentActivity() {
  private var picker by mutableStateOf<FavoritePicker?>(null)
  private var saving by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    setResult(RESULT_CANCELED, result)
    super.onCreate(savedInstanceState)
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }
    enableEdgeToEdge()
    val runtime = FavoriteWidgetRuntime.get()
    val appearance = getKoin().get<AppearanceRepository>()
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        FavoriteWidgetRuntime.invalidations.collect {
          picker = runtime.picker(appWidgetId)
          if (picker == null) finish()
        }
      }
    }
    setContent {
      val settings by appearance.settings.collectAsState()
      val isDark = settings.isDark(isSystemInDarkTheme())
      val family = when (settings.family) {
        ThemeFamily.Brutalist -> PrismThemeFamily.Brutalist
        ThemeFamily.Catppuccin -> PrismThemeFamily.Catppuccin
        ThemeFamily.Console -> PrismThemeFamily.Console
      }
      val flavour = when (settings.catppuccinFlavour) {
        CatppuccinFlavour.Latte -> PrismCatppuccinFlavour.Latte
        CatppuccinFlavour.Frappe -> PrismCatppuccinFlavour.Frappe
        CatppuccinFlavour.Macchiato -> PrismCatppuccinFlavour.Macchiato
        CatppuccinFlavour.Mocha -> PrismCatppuccinFlavour.Mocha
      }
      SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
          isAppearanceLightStatusBars = !isDark
          isAppearanceLightNavigationBars = !isDark
        }
      }
      PrismTheme(
        variant = if (isDark) PrismVariant.Dark else PrismVariant.Light,
        family = family,
        catppuccinFlavour = flavour,
      ) {
        FavoritePickerContent(
          picker = picker,
          saving = saving,
          onSelect = { entityId ->
            if (!saving) {
              saving = true
              lifecycleScope.launch {
                try {
                  when (runtime.configure(appWidgetId, entityId)) {
                    ConfigurationResult.Saved -> {
                      setResult(RESULT_OK, result)
                      finish()
                    }

                    ConfigurationResult.FavoritesChanged -> {
                      picker = runtime.picker(appWidgetId)
                      if (picker == null) finish()
                    }

                    ConfigurationResult.InvalidWidget -> finish()
                  }
                } finally {
                  saving = false
                }
              }
            }
          },
          onOpenApp = {
            startActivity(
              Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
          },
          onClose = ::finish,
        )
      }
    }
  }
}

@Composable
private fun FavoritePickerContent(
  picker: FavoritePicker?,
  saving: Boolean,
  onSelect: (String) -> Unit,
  onOpenApp: () -> Unit,
  onClose: () -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize(), color = Prism.color.background) {
    if (picker == null) {
      PrismFullscreenLoader(Modifier.fillMaxSize())
      return@Surface
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Prism.dimens.spacingM)) {
      PrismScreenTitleBar(
        title = stringResource(
          when (picker.kind) {
            WidgetKind.Team -> R.string.widget_team_choose_team
            WidgetKind.Event -> R.string.widget_event_choose_event
          },
        ),
        onBackPress = onClose,
      )
      if (picker.choices.isEmpty()) {
        Column(
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = Prism.dimens.spacingM),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          Text(
            text = stringResource(
              when (picker.kind) {
                WidgetKind.Team -> R.string.widget_favorite_picker_empty_teams
                WidgetKind.Event -> R.string.widget_favorite_picker_empty_events
              },
            ),
            style = Prism.typography.bodyLarge,
            color = Prism.color.contentPrimary,
          )
          PrismButton(onClick = onOpenApp) { Text(stringResource(R.string.widget_favorite_open_app)) }
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f).selectableGroup(),
          contentPadding = PaddingValues(bottom = Prism.dimens.spacingL),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          item {
            Text(
              text = stringResource(
                when (picker.kind) {
                  WidgetKind.Team -> R.string.widget_team_picker_explanation
                  WidgetKind.Event -> R.string.widget_event_picker_explanation
                },
              ),
              style = Prism.typography.bodySmall,
              color = Prism.color.bodyColor,
              modifier = Modifier.padding(bottom = Prism.dimens.spacingS),
            )
          }
          items(picker.choices, key = { it.id }) { choice ->
            FavoriteChoiceRow(choice, choice.id == picker.selected, enabled = !saving) { onSelect(choice.id) }
          }
        }
      }
    }
  }
}

@Composable
private fun FavoriteChoiceRow(choice: FavoriteChoice, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
  PrismCard(
    modifier = Modifier.fillMaxWidth().semantics {
      this.selected = selected
      role = Role.RadioButton
    },
    style = if (selected) PrismCardStyle.Filled else PrismCardStyle.Outlined,
    onClick = onSelect,
    enabled = enabled,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      SharedNetworkIcon(imageUrl = choice.logo, contentDescription = choice.name, size = PrismIconSize.Size40)
      Spacer(Modifier.width(Prism.dimens.spacingM))
      Column(Modifier.weight(1f)) {
        Text(choice.name, style = Prism.typography.bodyLarge, color = Prism.color.contentPrimary)
        choice.tag?.let { Text(it, style = Prism.typography.bodySmall, color = Prism.color.bodyColor) }
      }
      Spacer(Modifier.width(Prism.dimens.spacingS))
      PrismRadioButton(selected = selected, onClick = onSelect, enabled = enabled, modifier = Modifier.clearAndSetSemantics {})
    }
  }
}
