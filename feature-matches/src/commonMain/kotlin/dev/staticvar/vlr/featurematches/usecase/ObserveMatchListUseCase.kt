/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.usecase

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow

public class ObserveMatchListUseCase(private val matchRepository: MatchRepository) {
  public operator fun invoke(): Flow<List<MatchPreview>> = matchRepository.getMatches()
}
