/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import dev.staticvar.vlr.domain.model.TeamSearchResult
import dev.staticvar.vlr.remotesource.common.SearchCategory
import dev.staticvar.vlr.remotesource.search.SearchDataSource
import dev.staticvar.vlr.remotesource.search.SearchResultDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TeamSearchRepositoryTest {
  @Test
  fun teamSearchPreservesAliasMatchesAndDetailIdentity() = runTest {
    val source = object : SearchDataSource {
      override suspend fun search(category: SearchCategory, term: String): Result<List<SearchResultDto>> {
        assertEquals(SearchCategory.TEAM, category)
        assertEquals("prx", term)
        return Result.success(
          listOf(
            SearchResultDto(id = "624", name = "Paper Rex", img = "logo", shortName = "PRX", category = SearchCategory.TEAM),
            SearchResultDto(id = "12", name = "PRX event", category = SearchCategory.EVENT),
            SearchResultDto(id = "13", name = "PRX player", category = SearchCategory.PLAYER),
            SearchResultDto(id = "14", name = "PRX unknown"),
          ),
        )
      }
    }
    assertEquals(
      listOf(TeamSearchResult("624", "Paper Rex", "logo", "PRX")),
      TeamSearchRepositoryImpl(source).searchTeams("prx").getOrThrow(),
    )
  }
}
