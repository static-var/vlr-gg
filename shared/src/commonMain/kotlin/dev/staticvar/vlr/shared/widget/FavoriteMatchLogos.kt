/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.remotesource.common.ApiPaths
import dev.staticvar.vlr.shared.widget.favorite.MatchDetails
import dev.staticvar.vlr.shared.widget.favorite.RemoteTeam
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
 * logo, so each shown match is looked up once in match details and remembered by id.
 */
internal class FavoriteMatchLogos(private val client: HttpClient, private val json: Json) {
  private val lock = Mutex()
  private val known = mutableMapOf<String, TeamLogos>()

  suspend fun remember(matches: List<UpcomingWidgetMatch>) {
    lock.withLock {
      matches.forEach { match ->
        val logos = TeamLogos(match.team1Logo, match.team2Logo)
        if (logos.any) known[match.id] = logos
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

  private suspend fun lookup(match: UpcomingWidgetMatch): TeamLogos? {
    lock.withLock { known[match.id] }?.let { return it }
    val details = try {
      WidgetWireDecoders.details(json.parseToJsonElement(client.get(ApiPaths.match(match.id.encodeURLPathPart())).bodyAsText()))
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      return null
    }
    val logos = TeamLogos(details.logo(match.team1Id, match.team1), details.logo(match.team2Id, match.team2))
    if (!logos.any) return null
    lock.withLock { known[match.id] = logos }
    return logos
  }
}

private data class TeamLogos(val team1: String?, val team2: String?) {
  val any: Boolean get() = team1 != null || team2 != null
}

private fun UpcomingWidgetMatch.with(logos: TeamLogos) =
  copy(team1Logo = logos.team1 ?: team1Logo, team2Logo = logos.team2 ?: team2Logo)

private fun shown(state: FavoriteMatchState): List<UpcomingWidgetMatch> = when (state) {
  is FavoriteMatchState.Live -> listOf(state.match)
  is FavoriteMatchState.Upcoming -> listOf(state.match)
  is FavoriteMatchState.Completed -> listOfNotNull(state.match, state.next)
  FavoriteMatchState.NoMatches, FavoriteMatchState.NoFavorites, FavoriteMatchState.NeedsApp -> emptyList()
}

/** The team's logo, matched by id when the feed gave one and by name otherwise. */
private fun MatchDetails.logo(id: String?, name: String): String? {
  val team = teams.firstOrNull { it.matches(id, name) } ?: return null
  return team.logo?.takeIf(String::isNotBlank)
}

private fun RemoteTeam.matches(id: String?, name: String): Boolean =
  if (id != null && this.id != null) this.id == id else this.name.trim().equals(name.trim(), ignoreCase = true)
