/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.di

import dev.staticvar.vlr.localsource.database.DatabaseDriverFactory
import dev.staticvar.vlr.localsource.database.VlrDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for local-source dependencies.
 */
val localSourceModule = module {
  single { get<DatabaseDriverFactory>().createDriver() }
  single { VlrDatabase(get()) }
}

/**
 * Platform-specific module for database driver factory.
 * Each platform must provide its own implementation.
 */
expect val platformLocalSourceModule: Module
