/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.remotesource.common.ApiPaths
import dev.staticvar.vlr.shared.widget.favorite.WidgetWireDecoders
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Adds team logos to the matches the one-match widget shows. The favorites feed carries team ids but no
 * logo, so each shown match is looked up once in match details and its logos are remembered by match id,
 * keyed by team identity rather than side, because the schedule and the feed can order the sides differently.
 */
internal class FavoriteMatchLogos(private val client: HttpClient, private val json: Json) {
  private val lock = Mutex()
  private val known = mutableMapOf<String, LogoIndex>()

  suspend fun remember(matches: List<UpcomingWidgetMatch>) {
    lock.withLock {
      matches.forEach { match ->
        val index = match.index()
        if (index.isNotEmpty()) known[match.id] = known[match.id].orEmpty() + index
      }
    }
  }

  suspend fun fill(snapshot: UpcomingMatchesSnapshot, nowEpochMillis: Long): UpcomingMatchesSnapshot {
    val missing = shown(favoriteMatchState(snapshot, nowEpochMillis)).filter { it.team1Logo == null || it.team2Logo == null }
    if (missing.isEmpty()) return snapshot
    val logos = missing.mapNotNull { match -> lookup(match)?.let { match.id to it } }.toMap()
    if (logos.isEmpty()) return snapshot
    fun List<UpcomingWidgetMatch>.filled() = map { match -> logos[match.id]?.let(match::with) ?: match }
    return snapshot.copy(matches = snapshot.matches.filled(), results = snapshot.results.filled())
  }

  private suspend fun lookup(match: UpcomingWidgetMatch): LogoIndex? {
    lock.withLock { known[match.id] }?.let { return it }
    val details = try {
      WidgetWireDecoders.details(json.parseToJsonElement(client.get(ApiPaths.match(match.id.encodeURLPathPart())).bodyAsText()))
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      return null
    }
    val index = buildMap {
      details.teams.forEach { team ->
        val logo = team.logo?.takeIf(String::isNotBlank) ?: return@forEach
        teamKeys(team.id, team.name).forEach { put(it, logo) }
      }
    }
    if (index.isEmpty()) return null
    lock.withLock { known[match.id] = index }
    return index
  }
}

/** Logo URL by team key, where a team has an id key when the feed gave one and always a name key. */
private typealias LogoIndex = Map<String, String>

private fun teamKeys(id: String?, name: String): List<String> =
  listOfNotNull(id?.takeIf(String::isNotBlank)?.let { "id:$it" }, "name:${name.trim().lowercase()}")

private fun LogoIndex.logo(id: String?, name: String): String? = teamKeys(id, name).firstNotNullOfOrNull(::get)

private fun UpcomingWidgetMatch.index(): LogoIndex = buildMap {
  team1Logo?.let { logo -> teamKeys(team1Id, team1).forEach { put(it, logo) } }
  team2Logo?.let { logo -> teamKeys(team2Id, team2).forEach { put(it, logo) } }
}

private fun UpcomingWidgetMatch.with(index: LogoIndex) =
  copy(team1Logo = index.logo(team1Id, team1) ?: team1Logo, team2Logo = index.logo(team2Id, team2) ?: team2Logo)

private fun shown(state: FavoriteMatchState): List<UpcomingWidgetMatch> = when (state) {
  is FavoriteMatchState.Live -> listOf(state.match)
  is FavoriteMatchState.Upcoming -> listOf(state.match)
  is FavoriteMatchState.Completed -> listOfNotNull(state.match, state.next)
  FavoriteMatchState.NoMatches, FavoriteMatchState.NoFavorites, FavoriteMatchState.NeedsApp -> emptyList()
}
