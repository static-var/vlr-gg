/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.search

import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class PublishSearchFavoritesTest {
  @Test
  fun loadsEachMissingTeamOnceAndPublishesArrivingAliases() = runTest {
    val snapshots = MutableStateFlow(
      DirectFavoriteSnapshot(
        teams = listOf(DirectFavorite.Team("prx", "Paper Rex", "", unresolvedTeamId = "prx")),
        matches = listOf(DirectFavorite.Match(
          "match", "Paper Rex vs Fnatic", "", unresolvedTeamIds = listOf("prx", "fnc"),
        )),
      ),
    )
    val refreshed = mutableListOf<String>()
    val publications = mutableListOf<List<SearchFavorite>>()
    backgroundScope.launch {
      publishSearchFavorites(
        snapshots,
        refreshTeam = { teamId -> refreshed += teamId; Result.success(Unit) },
        publish = { publications += Json.decodeFromString<List<SearchFavorite>>(it) },
      )
    }

    runCurrent()
    assertEquals(setOf("prx", "fnc"), refreshed.toSet())
    assertEquals(2, refreshed.size)
    assertEquals(1, publications.size)

    snapshots.value = DirectFavoriteSnapshot(
      teams = listOf(DirectFavorite.Team("prx", "Paper Rex", "", shortName = "PRX")),
      matches = listOf(DirectFavorite.Match(
        "match", "Paper Rex vs Fnatic", "", teamShortNames = listOf("PRX", "FNC"),
      )),
    )
    runCurrent()
    assertEquals(2, publications.size)
    assertEquals(listOf("PRX"), publications.last().single { it.id == "team:prx" }.aliases)
    assertEquals(listOf("PRX", "FNC"), publications.last().single { it.id == "match:match" }.aliases)
  }

  @Test
  fun failedRefreshRetriesAndRemovingFavoriteResetsTracking() = runTest {
    val missing = DirectFavoriteSnapshot(
      teams = listOf(DirectFavorite.Team("prx", "Paper Rex", "", unresolvedTeamId = "prx")),
    )
    val snapshots = MutableStateFlow(missing)
    val publications = mutableListOf<List<SearchFavorite>>()
    var refreshes = 0
    backgroundScope.launch {
      publishSearchFavorites(
        snapshots,
        refreshTeam = {
          refreshes++
          if (refreshes == 1) Result.failure(IllegalStateException("offline"))
          else {
            if (refreshes == 2) {
              snapshots.value = DirectFavoriteSnapshot(
                teams = listOf(DirectFavorite.Team("prx", "Paper Rex", "", shortName = "PRX")),
              )
            }
            Result.success(Unit)
          }
        },
        publish = { publications += Json.decodeFromString<List<SearchFavorite>>(it) },
      )
    }

    runCurrent()
    assertEquals(1, refreshes)
    advanceTimeBy(30_000)
    runCurrent()
    assertEquals(2, refreshes)
    assertEquals(listOf("PRX"), publications.last().single().aliases)

    snapshots.value = DirectFavoriteSnapshot()
    runCurrent()
    snapshots.value = missing
    runCurrent()
    assertEquals(3, refreshes)
  }
}
