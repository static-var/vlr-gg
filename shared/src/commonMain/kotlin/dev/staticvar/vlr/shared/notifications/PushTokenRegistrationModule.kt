/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.di.DispatcherQualifiers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun pushTokenRegistrationModule(): Module = module {
  single(createdAtStart = true) {
    LiveUpdateReconnectRecovery(
      networkMonitor = get(),
      tokenUploader = get(),
      favoriteSync = get(),
      appScope = get<CoroutineScope>(DispatcherQualifiers.AppScope),
    )
  }
  single {
    FavoriteLiveUpdateCoordinator(
      identityRepository = get(),
      favoritesRepository = get(),
      tokenPreferences = get(),
      dataSource = get(),
      appScope = get<CoroutineScope>(DispatcherQualifiers.AppScope),
    )
  }
  single {
    PushTokenRegistrationUploader(
      preferences = get(),
      identityRepository = get(),
      dataSource = get(),
      appScope = get<CoroutineScope>(DispatcherQualifiers.AppScope),
    )
  }
  single { LiveActivityStartLedger(get(), get()) }
  single {
    LiveActivityStartCoordinator(
      identity = get(),
      favorites = get(),
      schedule = get(),
      tokenPreferences = get(),
      favoriteSync = get(),
      dataSource = get(),
      ledger = get(),
      mainDispatcher = get<CoroutineDispatcher>(DispatcherQualifiers.Main),
      appScope = get<CoroutineScope>(DispatcherQualifiers.AppScope),
    )
  }
}
