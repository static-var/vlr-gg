/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.usecase

import dev.staticvar.vlr.domain.repository.TeamRepository

public class RefreshTeamDetailsUseCase(private val teamRepository: TeamRepository) {
  public suspend operator fun invoke(teamId: String): Result<Unit> = teamRepository.refreshTeamDetails(teamId)
}
