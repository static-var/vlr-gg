/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.usecase

import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow

public class ObservePlayerDetailsUseCase(private val playerRepository: PlayerRepository) {
  public operator fun invoke(playerId: String): Flow<PlayerInfo?> = playerRepository.getPlayerDetails(playerId)
}
