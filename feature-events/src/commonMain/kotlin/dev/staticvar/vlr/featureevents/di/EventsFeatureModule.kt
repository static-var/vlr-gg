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
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val NavigationEntryScopeQualifier: String = "navigation-entry"

public fun eventsFeatureModule(): Module = module {
  factory { ObserveEventListUseCase(eventRepository = get()) }
  factory { RefreshEventsUseCase(eventRepository = get()) }
  factory { ObserveEventDetailsUseCase(eventRepository = get()) }
  factory { RefreshEventDetailsUseCase(eventRepository = get()) }
  scope(named(NavigationEntryScopeQualifier)) {
    scoped {
      EventsViewModel(
        observeEventListUseCase = get(),
        refreshEventsUseCase = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
    scoped {
      EventDetailsViewModel(
        observeEventDetailsUseCase = get(),
        refreshEventDetailsUseCase = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
  }
}
