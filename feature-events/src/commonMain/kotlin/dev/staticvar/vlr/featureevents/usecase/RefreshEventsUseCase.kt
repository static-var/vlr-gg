/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.usecase

import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.usecase.InitialFavoriteProfilesRefresh
import kotlinx.coroutines.CancellationException

public class RefreshEventsUseCase(
  private val eventRepository: EventRepository,
  private val initialFavoriteProfilesRefresh: InitialFavoriteProfilesRefresh,
) {
  public suspend operator fun invoke(): Result<Unit> {
    val profiles = initialRefreshResult { initialFavoriteProfilesRefresh.awaitInitialRefresh() }
    val events = eventRepository.refreshEvents().propagateCancellation()
    return if (profiles.isFailure) profiles else events
  }
}

private suspend fun initialRefreshResult(refresh: suspend () -> Result<Unit>): Result<Unit> = try {
  refresh().propagateCancellation()
} catch (cancellation: CancellationException) {
  throw cancellation
} catch (error: Exception) {
  Result.failure(error)
}

private fun Result<Unit>.propagateCancellation(): Result<Unit> = also { result ->
  val error = result.exceptionOrNull()
  if (error is CancellationException) throw error
}
