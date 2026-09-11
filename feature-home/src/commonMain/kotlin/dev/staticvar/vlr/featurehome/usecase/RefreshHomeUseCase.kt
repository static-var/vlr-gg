/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.usecase

import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

public class RefreshHomeUseCase(
  private val matchRepository: MatchRepository,
  private val eventRepository: EventRepository,
) {
  public suspend operator fun invoke(): Result<Unit> = coroutineScope {
    val matches = async { refreshResult { matchRepository.refreshMatches() } }
    val events = async { refreshResult { eventRepository.refreshEvents() } }
    val results = awaitAll(matches, events)
    results.firstOrNull { result -> result.isFailure } ?: Result.success(Unit)
  }
}

private suspend fun refreshResult(refresh: suspend () -> Result<Unit>): Result<Unit> = try {
  refresh().also { result ->
    val error = result.exceptionOrNull()
    if (error is CancellationException) throw error
  }
} catch (cancellation: CancellationException) {
  throw cancellation
} catch (error: Exception) {
  Result.failure(error)
}
