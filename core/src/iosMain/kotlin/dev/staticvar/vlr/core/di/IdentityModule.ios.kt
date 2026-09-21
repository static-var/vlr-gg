/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.di

import com.russhwolf.settings.NSUserDefaultsSettings
import dev.staticvar.vlr.core.identity.ICloudIdentitySync
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUbiquitousKeyValueStore

public actual val platformIdentityModule: Module = module {
  single {
    UserIdentityRepository(
      NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults),
      backupId = NSUbiquitousKeyValueStore.defaultStore.stringForKey(UserIdentityRepository.IdentityKey),
      allowDelayedRestore = true,
    )
  }
  single(createdAtStart = true) { ICloudIdentitySync(get()) } onClose { it?.close() }
}
