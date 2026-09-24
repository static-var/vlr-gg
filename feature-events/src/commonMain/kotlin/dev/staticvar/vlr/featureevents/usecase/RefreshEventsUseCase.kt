/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.usecase

import dev.staticvar.vlr.domain.repository.EventRepository
import kotlinx.coroutines.CancellationException

public class RefreshEventsUseCase(
  private val eventRepository: EventRepository,
) {
  public suspend operator fun invoke(): Result<Unit> = eventRepository.refreshEvents().propagateCancellation()
}

private fun Result<Unit>.propagateCancellation(): Result<Unit> = also { result ->
  val error = result.exceptionOrNull()
  if (error is CancellationException) throw error
}
