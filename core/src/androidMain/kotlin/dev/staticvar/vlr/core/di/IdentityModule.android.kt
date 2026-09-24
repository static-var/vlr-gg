/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.di

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

public actual val platformIdentityModule: Module = module {
  single(createdAtStart = true) {
    UserIdentityRepository(
      SharedPreferencesSettings(androidContext().getSharedPreferences("identity", Context.MODE_PRIVATE)),
    )
  }
}
