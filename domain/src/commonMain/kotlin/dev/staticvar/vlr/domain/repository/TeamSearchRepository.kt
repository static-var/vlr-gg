/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.TeamSearchResult

interface TeamSearchRepository {
  suspend fun searchTeams(query: String): Result<List<TeamSearchResult>>
}
