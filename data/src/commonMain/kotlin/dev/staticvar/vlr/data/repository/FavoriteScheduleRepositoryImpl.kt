/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class FavoriteScheduleRepositoryImpl(
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : FavoriteScheduleRepository {
  override fun observeMatches(): Flow<List<FavoriteScheduledMatch>> = database.favoriteScheduleQueries
    .getFavoriteSchedule { id, event, team1, team2, time, status, score1, score2, formatSource, mapCount, stage ->
      val matchStatus = status.toMatchStatus()
      FavoriteScheduledMatch(
        id = id,
        event = event,
        team1 = team1,
        team2 = team2,
        time = time,
        status = matchStatus,
        score1 = score1?.toInt(),
        score2 = score2?.toInt(),
        format = formatSource.toBestOfFormat().ifEmpty { mapCount.toPlannedFormat(matchStatus) },
        stage = stage,
      )
    }
    .asFlow()
    .mapToList(dispatchers.io)
    .distinctUntilChanged()
}

private fun String.toMatchStatus(): MatchStatus = when (this) {
  "UPCOMING" -> MatchStatus.UPCOMING
  "LIVE" -> MatchStatus.LIVE
  "COMPLETED" -> MatchStatus.COMPLETED
  else -> MatchStatus.UNKNOWN
}

private val bestOfPattern = Regex("""(?i)\b(?:bo|best(?:\s*-\s*|\s+)of)\s*[-:]?\s*(\d+)\b""")

private fun String.toBestOfFormat(): String = bestOfPattern
  .find(this)
  ?.groupValues
  ?.get(1)
  ?.toIntOrNull()
  ?.takeIf { it > 0 && it % 2 == 1 }
  ?.let { "BO$it" }
  .orEmpty()

private fun Long?.toPlannedFormat(status: MatchStatus): String = this
  ?.toInt()
  .takeIf { status == MatchStatus.UPCOMING || status == MatchStatus.LIVE }
  ?.takeIf { it == 1 || it == 3 || it == 5 }
  ?.let { "BO$it" }
  .orEmpty()
