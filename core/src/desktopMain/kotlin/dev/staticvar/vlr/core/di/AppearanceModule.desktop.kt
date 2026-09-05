/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module

public actual val platformAppearanceModule: Module = module {
  single<Settings> { PreferencesSettings(Preferences.userRoot().node("dev/staticvar/vlr/appearance")) }
}
