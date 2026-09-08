/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.usecase

import dev.staticvar.vlr.domain.repository.MatchRepository

public class SetMatchFavoriteUseCase(private val matchRepository: MatchRepository) {
  public suspend operator fun invoke(matchId: String, isFavorite: Boolean): Result<Unit> =
    if (isFavorite) matchRepository.addToFavorites(matchId) else matchRepository.removeFromFavorites(matchId)
}
