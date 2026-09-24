/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.usecase

import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Refreshes favorite schedules without requiring a screen or an active UI lifecycle. */
public class RefreshFavoriteMatches(
  private val favorites: FavoritesRepository,
  private val matches: MatchRepository,
  private val events: EventRepository,
  private val schedule: FavoriteScheduleRepository,
) {
  private val refreshMutex = Mutex()

  public suspend operator fun invoke(): Unit = refreshMutex.withLock {
    val selected = favorites.observeDirectFavorites().first()
    if (!selected.hasAny) return@withLock

    matches.refreshMatches().getOrThrow()
    refreshAll(selected.events.map { event -> { events.refreshEventDetails(event.id) } })

    val overview = matches.getMatches().first().associateBy { it.id }
    val candidates = (selected.matches.map { it.id } + schedule.observeMatches().first().map { it.id }).distinct()
    val detailIds = candidates.filter { id ->
      overview[id]?.status != MatchStatus.COMPLETED &&
        matches.getMatchDetails(id).first()?.event?.status?.uppercase() !in setOf("COMPLETED", "FINAL")
    }
    refreshAll(detailIds.map { id -> { matches.refreshMatchDetails(id) } })
  }
}

private suspend fun refreshAll(requests: List<suspend () -> Result<Unit>>) {
  val permits = Semaphore(4)
  coroutineScope {
    requests.map { request -> async { permits.withPermit { request().getOrThrow() } } }.awaitAll()
  }
}
