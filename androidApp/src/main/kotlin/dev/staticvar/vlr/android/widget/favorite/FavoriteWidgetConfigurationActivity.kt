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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.prism.typography.prismTypography
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.core.settings.AppearanceRepository
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.ThemeFamily
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
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
) {
  Surface(modifier = Modifier.fillMaxSize()) {
    if (picker == null) {
      Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Text(
          text = stringResource(
            when (picker.kind) {
              WidgetKind.Team -> R.string.widget_team_choose_team
              WidgetKind.Event -> R.string.widget_event_choose_event
            },
          ),
          style = MaterialTheme.prismTypography.cardTitle,
          modifier = Modifier.padding(24.dp).semantics { heading() },
        )
        if (picker.choices.isEmpty()) {
          Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
              .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Text(
              text = stringResource(
                when (picker.kind) {
                  WidgetKind.Team -> R.string.widget_favorite_picker_empty_teams
                  WidgetKind.Event -> R.string.widget_favorite_picker_empty_events
                },
              ),
              style = MaterialTheme.prismTypography.bodyLarge,
            )
            Button(onClick = onOpenApp) {
              Text(stringResource(R.string.widget_favorite_open_app))
            }
          }
        } else {
          LazyColumn(modifier = Modifier.weight(1f)) {
            items(picker.choices, key = { it.id }) { choice ->
              Text(
                text = choice.name,
                style = MaterialTheme.prismTypography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
                  .clickable(enabled = !saving, role = Role.Button) { onSelect(choice.id) }
                  .padding(horizontal = 24.dp, vertical = 20.dp),
              )
            }
          }
        }
      }
    }
  }
}
