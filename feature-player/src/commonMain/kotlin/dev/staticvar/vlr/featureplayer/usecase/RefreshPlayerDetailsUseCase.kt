/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.usecase

import dev.staticvar.vlr.domain.repository.PlayerRepository

public class RefreshPlayerDetailsUseCase(private val playerRepository: PlayerRepository) {
  public suspend operator fun invoke(playerId: String): Result<Unit> = playerRepository.refreshPlayerDetails(playerId)
}
