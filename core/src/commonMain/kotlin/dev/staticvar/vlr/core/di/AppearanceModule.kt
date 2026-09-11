/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.di

import dev.staticvar.vlr.core.settings.AppearanceRepository
import dev.staticvar.vlr.core.settings.MatchDetailsPreferencesRepository
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public expect val platformAppearanceModule: Module

public fun appearanceModule(): Module = module {
  single { AppearanceRepository(get()) }
  single { MatchDetailsPreferencesRepository(get()) }
  single { SpoilerPreferencesRepository(get()) }
}
