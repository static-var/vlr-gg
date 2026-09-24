/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import dev.staticvar.vlr.domain.model.TeamSearchResult
import dev.staticvar.vlr.domain.repository.TeamSearchRepository
import dev.staticvar.vlr.remotesource.common.SearchCategory
import dev.staticvar.vlr.remotesource.search.SearchDataSource

internal class TeamSearchRepositoryImpl(private val searchDataSource: SearchDataSource) : TeamSearchRepository {
  override suspend fun searchTeams(query: String): Result<List<TeamSearchResult>> =
    searchDataSource.search(SearchCategory.TEAM, query).map { results ->
      results.filter { it.category == SearchCategory.TEAM }.map { team ->
        TeamSearchResult(
          teamId = team.id,
          teamName = team.name,
          teamLogo = team.img,
          shortName = team.shortName,
        )
      }
    }
}
