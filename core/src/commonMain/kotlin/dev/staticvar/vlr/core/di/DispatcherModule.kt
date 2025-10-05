package dev.staticvar.vlr.core.di

import dev.staticvar.vlr.core.coroutines.AppScope
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.coroutines.StandardDispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * Koin module providing coroutine dispatchers and application-level scope.
 */
fun dispatcherModule() =
  module {
    // Dispatcher provider
    single<DispatcherProvider> { StandardDispatcherProvider() }
    
    // Individual dispatchers (qualified)
    single<CoroutineDispatcher>(DispatcherQualifiers.Default) { get<DispatcherProvider>().default }
    single<CoroutineDispatcher>(DispatcherQualifiers.Io) { get<DispatcherProvider>().io }
    single<CoroutineDispatcher>(DispatcherQualifiers.Main) { get<DispatcherProvider>().main }
    
    // Application-level scope
    single<CoroutineScope>(DispatcherQualifiers.AppScope) { AppScope(get()) }
  }
