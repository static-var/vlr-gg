/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.usecase

import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.domain.model.RankingsQuery

public class RefreshRankingsUseCase(private val rankingsRepository: RankingsRepository) {
  public suspend operator fun invoke(query: RankingsQuery = RankingsQuery()): Result<Unit> =
    rankingsRepository.refreshRankings(query)
}
