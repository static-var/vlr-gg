/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Provides platform-appropriate dispatchers for database operations.
 *
 * Note: Dispatchers.IO is available on:
 * - Android: Yes (native support)
 * - iOS: No (uses Dispatchers.Default)
 *
 * This object provides a unified API that works across all platforms.
 */
object DatabaseDispatchers {
  /**
   * Dispatcher for database operations.
   * Uses Dispatchers.IO on platforms that support it, falls back to Dispatchers.Default on others.
   */
  val database: CoroutineDispatcher
    get() = try {
      Dispatchers.IO
    } catch (e: NotImplementedError) {
      // iOS doesn't have Dispatchers.IO, use Default
      Dispatchers.Default
    }
}
