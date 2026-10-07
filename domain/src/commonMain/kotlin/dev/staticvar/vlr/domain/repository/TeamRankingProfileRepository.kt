/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

interface TeamRankingProfileRepository {
  suspend fun refreshProfile(teamId: String): Result<Unit>
}
