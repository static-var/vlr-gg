package dev.staticvar.vlr.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.data.api.response.PlayerData
import dev.staticvar.vlr.data.api.response.RankPerRegion
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.data.api.response.TournamentDetails
import dev.staticvar.vlr.data.api.response.TournamentPreview
import dev.staticvar.vlr.data.dao.EventFavDao
import dev.staticvar.vlr.data.dao.MatchFavDao
import dev.staticvar.vlr.data.dao.TeamFavDao
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.model.EventFav
import dev.staticvar.vlr.data.model.MatchFav
import dev.staticvar.vlr.data.model.TeamFav
import dev.staticvar.vlr.data.model.TopicTracker
import dev.staticvar.vlr.di.IoDispatcher
import dev.staticvar.vlr.utils.Endpoints
import dev.staticvar.vlr.utils.Pass
import dev.staticvar.vlr.utils.TimeElapsed
import dev.staticvar.vlr.utils.runSuspendCatching
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Suppress("LongParameterList")
@Singleton
class VlrRepository
@Inject
constructor(
  private val vlrDao: VlrDao,
  private val matchFavDao: MatchFavDao,
  private val eventFavDao: EventFavDao,
  private val teamFavDao: TeamFavDao,
  private val ktorHttpClient: HttpClient,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val json: Json,
) {
  /** Upcoming matches Method returns all the upcoming matches we have stored in db */
  fun upcomingMatches() = vlrDao.getAllMatchesPreviewNoFlow()

  /**
   * Get news Method which calls API and requests for latest news article info This method won't
   * make the call again till 180 seconds
   */
  fun updateLatestNews() =
    flow<Result<Boolean, Throwable?>> {
      if (TimeElapsed.hasElapsed(Endpoints.NEWS)) {
        emit(Ok(true))
        val result = runSuspendCatching {
          ktorHttpClient.get(Endpoints.NEWS).body<List<NewsResponseItem>>()
        }
        result.get()?.let {
          vlrDao.deleteAndInsertNews(it)
          TimeElapsed.start(Endpoints.NEWS, 180.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /** Get news from db */
  fun getNewsFromDb() = vlrDao.getNews().map { Pass(it) }

  /**
   * Get matches from server This method will request server to return the latest matches This call
   * is made once every 30 seconds
   */
  fun updateLatestMatches() =
    flow<Result<Boolean, Throwable?>> {
      if (TimeElapsed.hasElapsed(Endpoints.MATCHES_OVERVIEW)) {
        emit(Ok(true))
        val result = runSuspendCatching {
          ktorHttpClient.get(Endpoints.MATCHES_OVERVIEW).body<List<MatchPreviewInfo>>()
        }
        result.get()?.let {
          vlrDao.deleteAndInsertMatchPreviewInfo(it)
          TimeElapsed.start(Endpoints.MATCHES_OVERVIEW, 30.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /** Get matches from db */
  fun getMatchesFromDb() = combine(
    vlrDao.getAllMatchesPreview(),
    matchFavDao.getFavoriteMatches(),
    teamFavDao.getFavoriteTeams()
  ) { matches, matchFavs, teamFavs ->
    matches.map {
      it.copy(
        markedFav = matchFavs.any { fav -> fav.id == it.id }
            || teamFavs.any { fav -> it.team1.id == fav.id || it.team2.id == fav.id }
      )
    }
  }.map { Pass(it) }

  /**
   * Get events from server This method will request server to return the latest events / tournament
   * related data This call is made once every 30 seconds
   */
  fun updateLatestEvents() =
    flow<Result<Boolean, Throwable?>> {
      if (TimeElapsed.hasElapsed(Endpoints.EVENTS_OVERVIEW)) {
        emit(Ok(true))
        val result = runSuspendCatching {
          ktorHttpClient.get(Endpoints.EVENTS_OVERVIEW).body<List<TournamentPreview>>()
        }
        result.get()?.let {
          vlrDao.deleteAndInsertTournamentPreview(it)
          TimeElapsed.start(Endpoints.EVENTS_OVERVIEW, 60.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /** Get events from db */
  fun getEventsFromDb() =
    vlrDao.getTournaments().combine(eventFavDao.getFavoriteEvents()) { tournaments, eventFavs ->
      tournaments.map { it.copy(markedFav = eventFavs.any { fav -> fav.id == it.id }) }
    }.map { Pass(it) }

  /**
   * Get events from server This method will request server to return the latest events / tournament
   * related data This call is made once every 30 seconds
   */
  fun updateLatestRanks() =
    flow<Result<Boolean, Throwable?>> {
      if (TimeElapsed.hasElapsed(Endpoints.RANK_OVERVIEW)) {
        emit(Ok(true))
        val result = runSuspendCatching {
          ktorHttpClient.get(Endpoints.RANK_OVERVIEW).body<List<RankPerRegion>>()
        }
        result.get()?.let { ranks ->
          ranks.forEach { perRegion -> perRegion.teams.forEach { it.region = perRegion.region } }
          val limitedRanks =
            ranks.map { RankPerRegion(it.region, it.teams.sortedBy { it.rank }.take(25)) }
          val teams = limitedRanks.flatMap { it.teams }
          vlrDao.insertTeamDetails(teams)
          TimeElapsed.start(Endpoints.RANK_OVERVIEW, 180.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /** Get events from db */
  fun getRanksFromDb() =
    vlrDao.getTeamDetailsInFlow().combine(teamFavDao.getFavoriteTeams()) { teams, teamFavs ->
      teams?.map { it.copy(markedFav = teamFavs.any { fav -> fav.id == it.id }) }
    }.map { Pass(it) }

  /**
   * Get match details from server This will request server to return match data of a given match ID
   * Call will only be made once every 30 seconds for a particular match ID
   *
   * @param id
   */
  fun updateLatestMatchDetails(id: String) =
    flow<Result<Boolean, Throwable?>> {
      val key = Endpoints.matchDetails(id)
      if (TimeElapsed.hasElapsed(key)) {
        emit(Ok(true))
        val result = runSuspendCatching { ktorHttpClient.get(key).body<MatchInfo>() }
        result.get()?.let {
          it.id = id
          vlrDao.insertMatchInfo(it)
          TimeElapsed.start(key, 30.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /**
   * Get match details from db
   *
   * @param id
   */
  fun getMatchDetailsFromDb(id: String) = combine(
    vlrDao.getMatchById(id),
    matchFavDao.getFavoriteMatches(),
    teamFavDao.getFavoriteTeams()
  ) { match, matchFavs, teamFavs ->
    match?.copy(
      markedFav = matchFavs.any { fav -> fav.id == match.id }
          || teamFavs.any { fav -> match.teams.any { it.id == fav.id } }
    )
  }.map { Pass(it) }

  /**
   * Get event details from server Request for latest Event / Tournament details of a given event
   * ID.
   *
   * @param id
   */
  fun updateLatestEventDetails(id: String) =
    flow<Result<Boolean, Throwable?>> {
      val key = Endpoints.eventDetails(id)
      if (TimeElapsed.hasElapsed(key)) {
        emit(Ok(true))
        val result = runSuspendCatching { ktorHttpClient.get(key).body<TournamentDetails>() }
        result.get()?.let {
          vlrDao.insertTournamentDetails(it)
          TimeElapsed.start(key, 30.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /**
   * Get event details from db
   *
   * @param id
   */
  fun getEventDetailsFromDb(id: String) =
    vlrDao.getTournamentById(id).combine(eventFavDao.getFavoriteEvents()) { tournament, eventFavs ->
      tournament?.copy(markedFav = eventFavs.any { fav -> fav.id == tournament.id })
    }.map { Pass(it) }

  /**
   * Add a match to be tracked in DB
   *
   * @param topic
   */
  suspend fun trackTopic(topic: String) =
    withContext(ioDispatcher) { vlrDao.insertTopicTracker(TopicTracker(topic)) }

  /**
   * Is match tracked
   *
   * @param topic
   */
  fun isTopicTracked(topic: String) = vlrDao.isTopicSubscribed(topic).flowOn(ioDispatcher)

  /**
   * Remove match from being tracked
   *
   * @param topic
   */
  suspend fun removeTopic(topic: String) = withContext(ioDispatcher) { vlrDao.deleteTopic(topic) }

  /**
   * Get team details
   *
   * @param id
   */
  fun getTeamDetails(id: String) =
    flow<Result<Boolean, Throwable?>> {
      val key = Endpoints.teamDetails(id)
      if (TimeElapsed.hasElapsed(key)) {
        emit(Ok(true))
        val result = runSuspendCatching { ktorHttpClient.get(key).body<TeamDetails>() }
        result.get()?.let {
          it.id = id
          vlrDao.insertTeamDetail(it)
          TimeElapsed.start(key, 180.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /**
   * Get event details from db
   *
   * @param id
   */
  fun getTeamDetailsFromDb(id: String) =
    vlrDao.getTeamDetailById(id).combine(teamFavDao.getFavoriteTeams()) { team, teamFavs ->
      team?.copy(markedFav = teamFavs.any { fav -> fav.id == team.id })
    }.map { Pass(it) }

  /**
   * Get team details
   *
   * @param id
   */
  fun getPlayerDetails(id: String) =
    flow<Result<Boolean, Throwable?>> {
      val key = Endpoints.playerDetails(id)
      if (TimeElapsed.hasElapsed(key)) {
        emit(Ok(true))
        val result = runSuspendCatching { ktorHttpClient.get(key).body<PlayerData>() }
        result.get()?.let {
          it.id = id
          vlrDao.upsertPlayer(it)
          TimeElapsed.start(key, 120.seconds)
          emit(Ok(false))
        }
          ?: emit(Err(result.getError()))
      }
    }

  /**
   * Get event details from db
   *
   * @param id
   */
  fun getPlayerDetailsFromDb(id: String) = vlrDao.getPlayerById(id).map { Pass(it) }

  /**
   * Parse news
   *
   * @param id
   */
  fun parseNews(id: String) = NewsParser.parser(id, json).flowOn(ioDispatcher)

  suspend fun deleteObsoleteRecords() {
    val time = System.currentTimeMillis() - DAY_15
    val matchInfoRecords = vlrDao.getObsoleteRecordFromMatchInfo(time).map { it.id }
    val teamDetailsRecords = vlrDao.getObsoleteRecordFromTeamDetails(time).map { it.id }
    val tournamentDetailsRecords = vlrDao.getObsoleteRecordFromTournamentDetails(time).map { it.id }
    val playerDataRecords = vlrDao.getObsoleteRecordFromPlayerData(time).map { it.id }

    println("Deleting ${matchInfoRecords.size} items from MatchInfo")
    println("Deleting ${teamDetailsRecords.size} items from TeamDetails")
    println("Deleting ${tournamentDetailsRecords.size} items from TournamentDetails")
    println("Deleting ${playerDataRecords.size} items from PlayerData")

    vlrDao.deleteMatchInfo(matchInfoRecords)
    vlrDao.deleteTeamDetails(teamDetailsRecords)
    vlrDao.deleteTournamentDetails(tournamentDetailsRecords)
    vlrDao.deletePlayerData(playerDataRecords)
  }

  suspend fun addFavoriteMatch(id: String) = matchFavDao.addFavMatch(MatchFav(id))
  suspend fun removeFavoriteMatch(id: String) = matchFavDao.deleteFavMatch(id)

  suspend fun addFavoriteEvent(id: String) = eventFavDao.addFavEvent(EventFav(id))
  suspend fun removeFavoriteEvent(id: String) = eventFavDao.deleteFavEvent(id)

  suspend fun addFavoriteTeam(id: String) = teamFavDao.addFavTeam(TeamFav(id))
  suspend fun removeFavoriteTeam(id: String) = teamFavDao.deleteFavTeam(id)
}

const val DAY_15: Long = 15 * 24 * 60 * 60 * 1000
