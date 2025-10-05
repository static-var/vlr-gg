package dev.staticvar.vlr.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual class StandardDispatcherProvider actual constructor() : DispatcherProvider {
  actual override val default: CoroutineDispatcher = Dispatchers.Default
  actual override val io: CoroutineDispatcher = Dispatchers.Default // iOS doesn't have IO dispatcher
  actual override val main: CoroutineDispatcher = Dispatchers.Main
}
