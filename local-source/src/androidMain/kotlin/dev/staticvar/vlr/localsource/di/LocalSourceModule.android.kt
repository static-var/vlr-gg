/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.di

import dev.staticvar.vlr.localsource.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module for database driver factory.
 */
actual val platformLocalSourceModule = module {
  single { DatabaseDriverFactory(androidContext()) }
}
