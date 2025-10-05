package dev.staticvar.vlr.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-specific dispatcher provider.
 * Android/Desktop use Dispatchers.IO for IO operations.
 * iOS uses Dispatchers.Default (no IO dispatcher available).
 */
internal expect class StandardDispatcherProvider() : DispatcherProvider {
  override val default: CoroutineDispatcher
  override val io: CoroutineDispatcher
  override val main: CoroutineDispatcher
}
