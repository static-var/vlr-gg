/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.usecase

import dev.staticvar.vlr.domain.repository.MatchRepository
import kotlinx.coroutines.CancellationException

public class RefreshMatchesUseCase(
  private val matchRepository: MatchRepository,
) {
  public suspend operator fun invoke(): Result<Unit> = matchRepository.refreshMatches().propagateCancellation()
}

private fun Result<Unit>.propagateCancellation(): Result<Unit> = also { result ->
  val error = result.exceptionOrNull()
  if (error is CancellationException) throw error
}
