/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.di.DispatcherQualifiers
import dev.staticvar.vlr.domain.repository.FavoriteSyncStatus
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
      syncState = get(),
      tokenPreferences = get(),
      dataSource = get(),
      appScope = get<CoroutineScope>(DispatcherQualifiers.AppScope),
    )
  }
  single<FavoriteSyncStatus> { get<FavoriteLiveUpdateCoordinator>() }
  single {
    val favoriteSync: FavoriteLiveUpdateCoordinator = get()
    PushTokenRegistrationUploader(
      preferences = get(),
      identityRepository = get(),
      dataSource = get(),
      appScope = get<CoroutineScope>(DispatcherQualifiers.AppScope),
      onCurrentTokenDeleted = favoriteSync::reconcile,
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
