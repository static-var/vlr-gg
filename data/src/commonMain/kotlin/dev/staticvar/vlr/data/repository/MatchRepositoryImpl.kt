/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.MatchBans
import dev.staticvar.vlr.data.MatchMapPlayerStats
import dev.staticvar.vlr.data.MatchMapRounds
import dev.staticvar.vlr.data.MatchMaps
import dev.staticvar.vlr.data.MatchPreviousEncounters
import dev.staticvar.vlr.data.MatchVideos
import dev.staticvar.vlr.data.Matches
import dev.staticvar.vlr.data.mapper.aggregateMatchDetails
import dev.staticvar.vlr.data.mapper.toBanEntities
import dev.staticvar.vlr.data.mapper.toDomain
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.data.mapper.toMapEntities
import dev.staticvar.vlr.data.mapper.toMatchEntity
import dev.staticvar.vlr.data.mapper.toPlayerStatEntities
import dev.staticvar.vlr.data.mapper.toPreviousEncounterEntities
import dev.staticvar.vlr.data.mapper.toRoundEntities
import dev.staticvar.vlr.data.mapper.toVideoEntities
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.localsource.database.GetMatchWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetMatchesWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of MatchRepository.
 * Coordinates between remote API and local database.
 */
internal class MatchRepositoryImpl(
  private val matchDataSource: MatchDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : MatchRepository {

  private val matchesQueries = database.matchesQueries

  override fun getMatches(): Flow<List<MatchPreview>> = matchesQueries
    .getMatchesWithFavoriteStatus()
    .asFlow()
    .mapToList(dispatchers.io)
    .combine(observeFavoriteReasons()) { matches, reasonsByMatch ->
      matches.map { match ->
        val reasons = reasonsByMatch[match.id].orEmpty()
        match.toDomain().copy(
          isFavorite = reasons.isNotEmpty(),
          isDirectFavorite = reasons.any { it.source == MatchFavoriteSource.MATCH },
          favoriteReasons = reasons,
        )
      }
    }

  override fun getMatchDetails(matchId: String): Flow<MatchDetails?> {
    val matchFlow = matchesQueries
      .getMatchWithFavoriteStatus(matchId)
      .asFlow()
      .mapToOneOrNull(dispatchers.io)

    val mapsFlow = matchesQueries
      .getMatchMaps(matchId)
      .asFlow()
      .mapToList(dispatchers.io)

    val roundsFlow = matchesQueries
      .getMatchRounds(matchId)
      .asFlow()
      .mapToList(dispatchers.io)

    val playerStatsFlow = matchesQueries
      .getMatchPlayerStats(matchId)
      .asFlow()
      .mapToList(dispatchers.io)

    val bansFlow = matchesQueries
      .getMatchBans(matchId)
      .asFlow()
      .mapToList(dispatchers.io)

    val videosFlow = matchesQueries
      .getMatchVideos(matchId)
      .asFlow()
      .mapToList(dispatchers.io)

    val previousFlow = matchesQueries
      .getPreviousEncounters(matchId)
      .asFlow()
      .mapToList(dispatchers.io)
      .map { encounters -> encounters.map { it.toDomainEncounter() } }

    val coreFlow =
      matchFlow.combine(mapsFlow) { match, maps ->
        MatchDetailSlices(match = match, maps = maps)
      }.combine(roundsFlow) { slices, rounds ->
        slices.copy(rounds = rounds)
      }.combine(playerStatsFlow) { slices, playerStats ->
        slices.copy(playerStats = playerStats)
      }.combine(bansFlow) { slices, bans ->
        slices.copy(bans = bans)
      }.combine(videosFlow) { slices, videos ->
        slices.copy(videos = videos)
      }

    return coreFlow.combine(previousFlow) { slices, previousEncounters ->
      slices.match?.let { match ->
        aggregateMatchDetails(
          match = match,
          maps = slices.maps,
          rounds = slices.rounds,
          playerStats = slices.playerStats,
          bans = slices.bans,
          videos = slices.videos,
          previousEncounters = previousEncounters,
        )
      }
    }.combine(observeFavoriteReasons()) { match, reasonsByMatch ->
      match?.let {
        val reasons = reasonsByMatch[it.id].orEmpty()
        it.copy(
          isFavorite = reasons.isNotEmpty(),
          isDirectFavorite = reasons.any { reason -> reason.source == MatchFavoriteSource.MATCH },
          favoriteReasons = reasons,
        )
      }
    }
  }

  private fun observeFavoriteReasons(): Flow<Map<String, List<MatchFavoriteReason>>> = matchesQueries
    .getMatchFavoriteReasons()
    .asFlow()
    .mapToList(dispatchers.io)
    .map { rows ->
      rows.groupBy { it.match_id }.mapValues { (_, reasons) ->
        reasons.map { reason ->
          MatchFavoriteReason(
            source = MatchFavoriteSource.valueOf(reason.source),
            id = reason.entity_id,
            name = reason.entity_name,
          )
        }.sortedWith(compareBy({ it.source.ordinal }, { it.name }, { it.id }))
      }
    }

  override suspend fun addToFavorites(matchId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      matchesQueries.addFavoriteMatch(matchId)
      Unit
    }
  }

  override suspend fun removeFromFavorites(matchId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      matchesQueries.removeFavoriteMatch(matchId)
      Unit
    }
  }

  override suspend fun refreshMatches(): Result<Unit> = withContext(dispatchers.io) {
    matchDataSource.list().mapCatching { dtos ->
      database.transaction {
        val existing = matchesQueries
          .getMatchesWithFavoriteStatus()
          .executeAsList()
          .associateBy { it.id }
        val remoteIds = mutableSetOf<String>()

        dtos.forEach { dto ->
          val entity = dto.toEntity()
          if (entity.id.isBlank()) return@forEach
          remoteIds += entity.id
          upsertMatch(mergeMatchListEntity(entity, existing[entity.id]))
        }

        val staleIds = existing.keys - remoteIds
        staleIds.forEach { id -> matchesQueries.deleteMatchById(id) }
      }
    }
  }

  /**
   * Refreshes detailed match data from remote and stores in database.
   * Called when user views match details.
   */
  override suspend fun refreshMatchDetails(matchId: String): Result<Unit> = withContext(dispatchers.io) {
    matchDataSource.details(matchId).mapCatching { dto ->
      database.transaction {
        // Delete existing related data
        matchesQueries.deleteMatchMaps(matchId)
        matchesQueries.deleteMatchRounds(matchId)
        matchesQueries.deleteMatchPlayerStats(matchId)
        matchesQueries.deleteMatchBans(matchId)
        matchesQueries.deleteMatchVideos(matchId)
        matchesQueries.deletePreviousEncounters(matchId)

        // Insert updated match
        val cachedMatch = matchesQueries.getMatchWithFavoriteStatus(matchId).executeAsOneOrNull()
        val remoteMatch = dto.toMatchEntity(cachedStatus = cachedMatch?.status).copy(id = matchId)
        val matchEntity = remoteMatch.copy(
          event_id = remoteMatch.event_id ?: cachedMatch?.event_id,
          team1_id = remoteMatch.team1_id.ifBlank { cachedMatch?.team1_id.orEmpty() },
          team2_id = remoteMatch.team2_id.ifBlank { cachedMatch?.team2_id.orEmpty() },
        )
        upsertMatch(matchEntity)

        // Insert related data
        dto.toMapEntities(matchId).forEach { map ->
          matchesQueries.insertMatchMap(
            match_id = map.match_id,
            map_name = map.map_name,
            team1_score = map.team1_score,
            team2_score = map.team2_score,
            duration = map.duration,
            stats_url = map.stats_url,
          )
        }

        dto.toRoundEntities(matchId).forEach { round ->
          matchesQueries.insertMatchRound(
            match_id = round.match_id,
            map_name = round.map_name,
            round_number = round.round_number,
            round_score = round.round_score,
            winner = round.winner,
            side = round.side,
            win_type = round.win_type,
          )
        }

        dto.toPlayerStatEntities(matchId).forEach { stats ->
          matchesQueries.insertPlayerStats(
            match_id = stats.match_id,
            map_name = stats.map_name,
            player_id = stats.player_id,
            player_name = stats.player_name,
            team_id = stats.team_id,
            agent_name = stats.agent_name,
            agent_image_url = stats.agent_image_url,
            rating = stats.rating,
            acs = stats.acs,
            kills = stats.kills,
            deaths = stats.deaths,
            assists = stats.assists,
            kast_percent = stats.kast_percent,
            adr = stats.adr,
            hs_percent = stats.hs_percent,
            first_kills = stats.first_kills,
            first_deaths = stats.first_deaths,
            first_kills_diff = stats.first_kills_diff,
          )
        }

        dto.toBanEntities(matchId).forEach { ban ->
          matchesQueries.insertMatchBan(
            match_id = ban.match_id,
            ban_type = ban.ban_type,
            ban_value = ban.ban_value,
          )
        }

        dto.toVideoEntities(matchId).forEach { video ->
          matchesQueries.insertMatchVideo(
            match_id = video.match_id,
            video_type = video.video_type,
            name = video.name,
            url = video.url,
          )
        }

        dto.toPreviousEncounterEntities(matchId).forEach { encounter ->
          ensurePreviousEncounterMatchExists(matchEntity, encounter)
          matchesQueries.insertPreviousEncounter(
            match_id = encounter.match_id,
            previous_match_id = encounter.previous_match_id,
            team1_name = encounter.team1_name,
            team1_score = encounter.team1_score,
            team2_name = encounter.team2_name,
            team2_score = encounter.team2_score,
          )
        }
      }
    }
  }

  private fun ensurePreviousEncounterMatchExists(parentMatch: Matches, encounter: MatchPreviousEncounters) {
    matchesQueries.insertMatchIfMissing(
      id = encounter.previous_match_id,
      event_id = parentMatch.event_id,
      event_name = parentMatch.event_name,
      event_logo_url = parentMatch.event_logo_url,
      series = parentMatch.series,
      stage = parentMatch.stage,
      status = "COMPLETED",
      time = parentMatch.time,
      eta = null,
      note = "",
      patch = parentMatch.patch,
      team1_id = "",
      team1_name = encounter.team1_name,
      team1_logo_url = "",
      team1_score = encounter.team1_score,
      team2_id = "",
      team2_name = encounter.team2_name,
      team2_logo_url = "",
      team2_score = encounter.team2_score,
      map_count = 0,
      last_updated = parentMatch.last_updated,
    )
  }

  private fun MatchPreviousEncounters.toDomainEncounter(): PreviousEncounter = PreviousEncounter(
    id = previous_match_id,
    teams = listOf(
      TeamPreview(
        id = null,
        name = team1_name,
        region = "",
        img = "",
        score = team1_score?.toInt(),
        isWinner = determineWinner(team1_score, team2_score, true),
        isFavorite = false,
      ),
      TeamPreview(
        id = null,
        name = team2_name,
        region = "",
        img = "",
        score = team2_score?.toInt(),
        isWinner = determineWinner(team1_score, team2_score, false),
        isFavorite = false,
      ),
    ),
  )

  private fun mergeMatchListEntity(entity: Matches, current: GetMatchesWithFavoriteStatus?): Matches {
    if (current == null) return entity
    return entity.copy(
      event_id = entity.event_id ?: current.event_id,
      event_logo_url = entity.event_logo_url.ifBlank { current.event_logo_url },
      stage = entity.stage.ifBlank { current.stage },
      status = entity.status.ifBlank { current.status },
      eta = entity.eta ?: current.eta,
      note = entity.note.ifBlank { current.note },
      patch = entity.patch ?: current.patch,
      team1_id = entity.team1_id.ifBlank { current.team1_id },
      team1_name = entity.team1_name.ifBlank { current.team1_name },
      team1_logo_url = entity.team1_logo_url.ifBlank { current.team1_logo_url },
      team1_score = entity.team1_score ?: current.team1_score,
      team2_id = entity.team2_id.ifBlank { current.team2_id },
      team2_name = entity.team2_name.ifBlank { current.team2_name },
      team2_logo_url = entity.team2_logo_url.ifBlank { current.team2_logo_url },
      team2_score = entity.team2_score ?: current.team2_score,
      map_count = entity.map_count.takeIf { it > 0 } ?: current.map_count,
      last_updated = entity.last_updated,
    )
  }

  private fun upsertMatch(entity: Matches) {
    matchesQueries.updateMatch(
      event_id = entity.event_id,
      event_name = entity.event_name,
      event_logo_url = entity.event_logo_url,
      series = entity.series,
      stage = entity.stage,
      status = entity.status,
      time = entity.time,
      eta = entity.eta,
      note = entity.note,
      patch = entity.patch,
      team1_id = entity.team1_id,
      team1_name = entity.team1_name,
      team1_logo_url = entity.team1_logo_url,
      team1_score = entity.team1_score,
      team2_id = entity.team2_id,
      team2_name = entity.team2_name,
      team2_logo_url = entity.team2_logo_url,
      team2_score = entity.team2_score,
      map_count = entity.map_count,
      last_updated = entity.last_updated,
      id = entity.id,
    )
    matchesQueries.insertMatchIfMissing(
      id = entity.id,
      event_id = entity.event_id,
      event_name = entity.event_name,
      event_logo_url = entity.event_logo_url,
      series = entity.series,
      stage = entity.stage,
      status = entity.status,
      time = entity.time,
      eta = entity.eta,
      note = entity.note,
      patch = entity.patch,
      team1_id = entity.team1_id,
      team1_name = entity.team1_name,
      team1_logo_url = entity.team1_logo_url,
      team1_score = entity.team1_score,
      team2_id = entity.team2_id,
      team2_name = entity.team2_name,
      team2_logo_url = entity.team2_logo_url,
      team2_score = entity.team2_score,
      map_count = entity.map_count,
      last_updated = entity.last_updated,
    )
  }

  private fun determineWinner(team1Score: Long?, team2Score: Long?, isTeam1: Boolean): Boolean? {
    if (team1Score == null || team2Score == null) return null
    return if (isTeam1) team1Score > team2Score else team2Score > team1Score
  }

  private data class MatchDetailSlices(
    val match: GetMatchWithFavoriteStatus?,
    val maps: List<MatchMaps> = emptyList(),
    val rounds: List<MatchMapRounds> = emptyList(),
    val playerStats: List<MatchMapPlayerStats> = emptyList(),
    val bans: List<MatchBans> = emptyList(),
    val videos: List<MatchVideos> = emptyList(),
  )
}
