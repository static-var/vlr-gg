/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.di.DispatcherQualifiers
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun pushTokenRegistrationModule(): Module = module {
  single {
    PushTokenRegistrationUploader(
      preferences = get(),
      identityRepository = get(),
      dataSource = get(),
      appScope = get<CoroutineScope>(DispatcherQualifiers.AppScope),
    )
  }
}
