/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.repository.FavoriteMatchFeed
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RefreshFavoriteWidgetSnapshotTest {
  @Test
  fun emptyWidgetSkipsFavoriteGetAndActiveWidgetExcludesResults() = runTest {
    val repository = RecordingFavorites()

    assertEquals(emptyList(), repository.fetchForWidget(hasFavorites = false))
    assertEquals(emptyList(), repository.requests)

    assertEquals(emptyList(), repository.fetchForWidget(hasFavorites = true))
    assertEquals(listOf(false), repository.requests)
  }
}

private class RecordingFavorites : FavoriteMatchesRepository {
  override val homeMatches: StateFlow<FavoriteMatchFeed?> = MutableStateFlow(null)
  val requests = mutableListOf<Boolean>()

  override suspend fun fetch(includeResults: Boolean): Result<List<MatchPreview>> {
    requests += includeResults
    return Result.success(emptyList())
  }
}
