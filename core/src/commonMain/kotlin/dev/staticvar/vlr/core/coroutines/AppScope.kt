/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Application-level CoroutineScope that survives across the entire app lifecycle.
 * Uses SupervisorJob so failures in one coroutine don't cancel others.
 */
class AppScope(private val dispatcherProvider: DispatcherProvider) : CoroutineScope {
  override val coroutineContext: CoroutineContext =
    SupervisorJob() + dispatcherProvider.default
}
