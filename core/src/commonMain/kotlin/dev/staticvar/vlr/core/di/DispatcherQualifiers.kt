package dev.staticvar.vlr.core.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/**
 * Koin qualifiers for coroutine dispatchers and scopes.
 */
object DispatcherQualifiers {
  val Default: Qualifier = named("dispatcher.default")
  val Io: Qualifier = named("dispatcher.io")
  val Main: Qualifier = named("dispatcher.main")
  val AppScope: Qualifier = named("scope.app")
}
