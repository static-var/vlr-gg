/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.di

import dev.staticvar.vlr.featureevents.presentation.EventDetailsViewModel
import dev.staticvar.vlr.featureevents.presentation.EventsViewModel
import dev.staticvar.vlr.featureevents.usecase.ObserveEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.ObserveEventListUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public fun eventsFeatureModule(): Module = module {
  factory { ObserveEventListUseCase(eventRepository = get()) }
  factory { RefreshEventsUseCase(eventRepository = get()) }
  factory { ObserveEventDetailsUseCase(eventRepository = get()) }
  factory { RefreshEventDetailsUseCase(eventRepository = get()) }
  viewModel {
    EventsViewModel(
      observeEventListUseCase = get(),
      refreshEventsUseCase = get(),
      networkMonitor = get(),
    )
  }
  viewModel { parameters ->
    EventDetailsViewModel(
      eventId = parameters.get(),
      observeEventDetailsUseCase = get(),
      refreshEventDetailsUseCase = get(),
      favoritesRepository = get(),
      eventRepository = get(),
      networkMonitor = get(),
    )
  }
}
