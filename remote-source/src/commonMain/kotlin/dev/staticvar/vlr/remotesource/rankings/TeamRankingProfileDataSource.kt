/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

interface TeamRankingProfileDataSource {
  suspend fun getProfile(teamId: String): Result<TeamRankingProfileDto>
}
