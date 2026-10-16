/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import kotlin.time.Instant

public data class MatchesUiState(
  val matches: List<MatchPreview> = emptyList(),
  val statusLists: MatchStatusLists = MatchStatusLists(),
  val selectedStatus: MatchStatusFilter = MatchStatusFilter.Live,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
)

public enum class MatchStatusFilter {
  Live,
  Upcoming,
  Completed,
}

public data class MatchStatusLists(
  val live: List<MatchPreview> = emptyList(),
  val upcoming: List<MatchPreview> = emptyList(),
  val completed: List<MatchPreview> = emptyList(),
) {
  public operator fun get(filter: MatchStatusFilter): List<MatchPreview> = when (filter) {
    MatchStatusFilter.Live -> live
    MatchStatusFilter.Upcoming -> upcoming
    MatchStatusFilter.Completed -> completed
  }
}

internal fun List<MatchPreview>.groupByStatus(nowEpochMillis: Long): MatchStatusLists {
  fun sortedWithStatus(status: MatchStatus, order: Comparator<Long?>): List<MatchPreview> =
    filter { it.status == status }
      .map { match -> match to match.epochMillis() }
      .sortedWith(compareBy(order) { (_, epochMillis) -> epochMillis })
      .map { (match, _) -> match }

  return MatchStatusLists(
    live = sortedWithStatus(MatchStatus.LIVE, compareBy<Long?> { it ?: Long.MAX_VALUE }),
    upcoming = sortedWithStatus(
      MatchStatus.UPCOMING,
      compareBy<Long?> { (it ?: Long.MAX_VALUE) < nowEpochMillis }.thenBy { it ?: Long.MAX_VALUE },
    ),
    completed = sortedWithStatus(MatchStatus.COMPLETED, compareByDescending<Long?> { it ?: Long.MIN_VALUE }),
  )
}

private fun MatchPreview.epochMillis(): Long? = time
  ?.takeIf(String::isNotBlank)
  ?.let { value -> runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull() }
