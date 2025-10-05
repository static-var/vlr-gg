package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.mapper.aggregateMatchDetails
import dev.staticvar.vlr.data.mapper.toBanEntities
import dev.staticvar.vlr.data.mapper.toMapEntities
import dev.staticvar.vlr.data.mapper.toMatchEntity
import dev.staticvar.vlr.data.mapper.toPlayerStatEntities
import dev.staticvar.vlr.data.mapper.toPreviousEncounterEntities
import dev.staticvar.vlr.data.mapper.toRoundEntities
import dev.staticvar.vlr.data.mapper.toVideoEntities
import dev.staticvar.vlr.data.mapper.toDomain
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of MatchRepository.
 * Coordinates between remote API and local database.
 */
internal class MatchRepositoryImpl(
  private val matchDataSource: MatchDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider
) : MatchRepository {

  private val matchesQueries = database.matchesQueries

  override fun getMatches(): Flow<List<MatchPreview>> =
    matchesQueries
      .getMatchesWithFavoriteStatus()
      .asFlow()
      .mapToList(dispatchers.io)
      .map { matches -> matches.map { it.toDomain() } }

  override suspend fun getMatchDetails(matchId: String): Result<MatchDetails> =
    withContext(dispatchers.io) {
      runCatching {
        val match = matchesQueries.getMatchWithFavoriteStatus(matchId).executeAsOne()
        val maps = matchesQueries.getMatchMaps(matchId).executeAsList()
        val rounds = matchesQueries.getMatchRounds(matchId).executeAsList()
        val playerStats = matchesQueries.getMatchPlayerStats(matchId).executeAsList()
        val bans = matchesQueries.getMatchBans(matchId).executeAsList()
        val videos = matchesQueries.getMatchVideos(matchId).executeAsList()
        val previousEncountersDb = matchesQueries.getPreviousEncounters(matchId).executeAsList()
        
        val previousEncounters = previousEncountersDb.map { encounter ->
          PreviousEncounter(
            id = encounter.previous_match_id,
            teams = listOf(
              TeamPreview(
                id = null,
                name = encounter.team1_name,
                region = "",
                img = "",
                score = encounter.team1_score?.toInt(),
                isWinner = run {
                  val t1 = encounter.team1_score
                  val t2 = encounter.team2_score
                  t1 != null && t2 != null && t1 > t2
                },
                isFavorite = false
              ),
              TeamPreview(
                id = null,
                name = encounter.team2_name,
                region = "",
                img = "",
                score = encounter.team2_score?.toInt(),
                isWinner = run {
                  val t1 = encounter.team1_score
                  val t2 = encounter.team2_score
                  t2 != null && t1 != null && t2 > t1
                },
                isFavorite = false
              )
            )
          )
        }

        aggregateMatchDetails(
          match = match,
          maps = maps,
          rounds = rounds,
          playerStats = playerStats,
          bans = bans,
          videos = videos,
          previousEncounters = previousEncounters
        )
      }
    }

  override fun getFavoriteMatches(): Flow<List<MatchPreview>> =
    matchesQueries
      .getAllFavoriteMatches()
      .asFlow()
      .mapToList(dispatchers.io)
      .map { matches -> matches.map { it.toDomain() } }

  override suspend fun addToFavorites(matchId: String): Result<Unit> =
    withContext(dispatchers.io) {
      runCatching {
        matchesQueries.addFavoriteMatch(matchId)
        Unit
      }
    }

  override suspend fun removeFromFavorites(matchId: String): Result<Unit> =
    withContext(dispatchers.io) {
      runCatching {
        matchesQueries.removeFavoriteMatch(matchId)
        Unit
      }
    }

  override suspend fun refreshMatches(): Result<Unit> =
    withContext(dispatchers.io) {
      matchDataSource.list().mapCatching { dtos ->
        database.transaction {
          matchesQueries.deleteAllMatches()
          dtos.forEach { dto ->
            val entity = dto.toEntity()
            matchesQueries.insertMatch(entity)
          }
        }
      }
    }

  /**
   * Refreshes detailed match data from remote and stores in database.
   * Called when user views match details.
   */
  suspend fun refreshMatchDetails(matchId: String): Result<Unit> =
    withContext(dispatchers.io) {
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
          val matchEntity = dto.toMatchEntity()
          matchesQueries.insertMatch(matchEntity)

          // Insert related data
          dto.toMapEntities(matchId).forEach { map ->
            matchesQueries.insertMatchMap(
              match_id = map.match_id,
              map_name = map.map_name,
              team1_score = map.team1_score,
              team2_score = map.team2_score,
              duration = map.duration,
              stats_url = map.stats_url
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
              win_type = round.win_type
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
              first_kills_diff = stats.first_kills_diff
            )
          }

          dto.toBanEntities(matchId).forEach { ban ->
            matchesQueries.insertMatchBan(
              match_id = ban.match_id,
              ban_type = ban.ban_type,
              ban_value = ban.ban_value
            )
          }

          dto.toVideoEntities(matchId).forEach { video ->
            matchesQueries.insertMatchVideo(
              match_id = video.match_id,
              video_type = video.video_type,
              name = video.name,
              url = video.url
            )
          }

          dto.toPreviousEncounterEntities(matchId).forEach { encounter ->
            matchesQueries.insertPreviousEncounter(
              match_id = encounter.match_id,
              previous_match_id = encounter.previous_match_id,
              team1_name = encounter.team1_name,
              team1_score = encounter.team1_score,
              team2_name = encounter.team2_name,
              team2_score = encounter.team2_score
            )
          }
        }
      }
    }
}
