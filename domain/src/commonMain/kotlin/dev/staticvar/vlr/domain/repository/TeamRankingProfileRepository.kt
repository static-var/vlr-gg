/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.TeamRankingProfile

interface TeamRankingProfileRepository {
  suspend fun getProfile(teamId: String): Result<TeamRankingProfile>
}
