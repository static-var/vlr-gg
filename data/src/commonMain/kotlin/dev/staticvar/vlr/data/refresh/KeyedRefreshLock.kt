/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.refresh

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class KeyedRefreshLock {
  private val entriesMutex = Mutex()
  private val entries = mutableMapOf<String, Entry>()

  suspend fun <T> withLock(key: String, action: suspend () -> T): T {
    val entry = entriesMutex.withLock {
      entries.getOrPut(key) { Entry() }.also { it.users += 1 }
    }
    try {
      return entry.mutex.withLock { action() }
    } finally {
      withContext(NonCancellable) {
        entriesMutex.withLock {
          entry.users -= 1
          if (entry.users == 0) entries.remove(key)
        }
      }
    }
  }

  private class Entry {
    val mutex = Mutex()
    var users = 0
  }
}
