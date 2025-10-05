package dev.staticvar.vlr.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Abstraction over coroutine dispatchers so platform specific implementations can swap in custom
 * dispatchers (e.g. Main on Native) without relying on expect/actual plumbing.
 */
interface DispatcherProvider {
  val default: CoroutineDispatcher
  val io: CoroutineDispatcher
  val main: CoroutineDispatcher
}
