package dev.staticvar.vlr.data

import dev.staticvar.vlr.data.api.response.*
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.model.TopicTracker
import dev.staticvar.vlr.di.IoDispatcher
import dev.staticvar.vlr.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class VlrRepository
@Inject
constructor(
  private val vlrDao: VlrDao,
  private val ktorHttpClient: HttpClient,
  private val simpleKtorHttpClient: HttpClient,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val json: Json
) {
  /** Upcoming matches Method returns all the upcoming matches we have stored in db */
  fun upcomingMatches() = vlrDao.getAllMatchesPreviewNoFlow()

  /**
   * Get news Method which calls API and requests for latest news article info This method won't
   * make the call again till 180 seconds
   */
  private fun getNews() =
    flow<Operation<List<NewsResponseItem>>> {
      if (TimeElapsed.hasElapsed(Constants.KEY_NEWS)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get(Endpoints.NEWS).body<List<NewsResponseItem>>().also {
              vlrDao.deleteAndInsertNews(it)
              TimeElapsed.start(Constants.KEY_NEWS, 180.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  /** Get news from db */
  private fun getNewsFromDb() =
    vlrDao.getNews().map { if (it.isEmpty()) Waiting() else Pass(it) }.distinctUntilChanged()

  /** Merge news Merges data from both the flows [getNews] & [getNewsFromDb] */
  fun mergeNews() = merge(getNewsFromDb(), getNews()).flowOn(ioDispatcher)

  /**
   * Get matches from server This method will request server to return the latest matches This call
   * is made once every 30 seconds
   */
  fun getMatchesFromServer() =
    flow<Operation<List<MatchPreviewInfo>>> {
      if (TimeElapsed.hasElapsed(Constants.KEY_MATCH_ALL)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get(Endpoints.MATCHES_OVERVIEW).body<List<MatchPreviewInfo>>().also {
              vlrDao.deleteAndInsertMatchPreviewInfo(it)
              TimeElapsed.start(Constants.KEY_MATCH_ALL, 30.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  /** Get matches from db */
  private fun getMatchesFromDb() =
    vlrDao
      .getAllMatchesPreview()
      .map { if (it.isEmpty()) Waiting() else Pass(it) }
      .distinctUntilChanged()

  /** Merge matches Merges data from both the flows [getMatchesFromServer] & [getMatchesFromDb] */
  fun mergeMatches() = merge(getMatchesFromDb(), getMatchesFromServer()).flowOn(ioDispatcher)

  /**
   * Get events from server This method will request server to return the latest events / tournament
   * related data This call is made once every 30 seconds
   */
  private fun getEventsFromServer() =
    flow<Operation<List<TournamentPreview>>> {
      if (TimeElapsed.hasElapsed(Constants.KEY_TOURNAMENT_ALL)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get(Endpoints.EVENTS_OVERVIEW).body<List<TournamentPreview>>().also {
              vlrDao.deleteAndInsertTournamentPreview(it)
              TimeElapsed.start(Constants.KEY_TOURNAMENT_ALL, 60.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  /** Get events from db */
  private fun getEventsFromDb() =
    vlrDao.getTournaments().map { if (it.isEmpty()) Waiting() else Pass(it) }.distinctUntilChanged()

  /** Merge events Merges data from both the flows [getEventsFromServer] & [getEventsFromDb] */
  fun mergeEvents() = merge(getEventsFromDb(), getEventsFromServer()).flowOn(ioDispatcher)

  /**
   * Get match details from server This will request server to return match data of a given match ID
   * Call will only be made once every 30 seconds for a particular match ID
   *
   * @param id
   */
  private fun getMatchDetailsFromServer(id: String) =
    flow<Operation<MatchInfo>> {
      if (TimeElapsed.hasElapsed(id)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get("api/v1/matches/$id").body<MatchInfo>().also {
              it.id = id
              vlrDao.insertMatchInfo(it)
              TimeElapsed.start(id, 30.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  /**
   * Get match details from db
   *
   * @param id
   */
  private fun getMatchDetailsFromDb(id: String) =
    vlrDao.getMatchById(id).map { if (it == null) Waiting() else Pass(it) }

  /**
   * Merge match details Merges data from both the flows [getMatchDetailsFromServer] &
   * [getMatchDetailsFromDb]
   * @param id
   */
  fun mergeMatchDetails(id: String) =
    merge(getMatchDetailsFromDb(id), getMatchDetailsFromServer(id)).flowOn(ioDispatcher)

  /**
   * Get event details from server Request for latest Event / Tournament details of a given event
   * ID.
   *
   * @param id
   */
  private fun getEventDetailsFromServer(id: String) =
    flow<Operation<TournamentDetails>> {
      if (TimeElapsed.hasElapsed(id)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get(Endpoints.eventDetails(id)).body<TournamentDetails>().also {
              vlrDao.insertTournamentDetails(it)
              TimeElapsed.start(id, 30.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  /**
   * Get event details from db
   *
   * @param id
   */
  private fun getEventDetailsFromDb(id: String) =
    vlrDao.getTournamentById(id).map { if (it == null) Waiting() else Pass(it) }

  /**
   * Merge event details Merges data from both the flows [getEventDetailsFromServer] &
   * [getEventDetailsFromDb]
   *
   * @param id
   */
  fun mergeEventDetails(id: String) =
    merge(getEventDetailsFromDb(id), getEventDetailsFromServer(id)).flowOn(ioDispatcher)

  /**
   * Add a match to be tracked in DB
   *
   * @param topic
   */
  fun trackTopic(topic: String) = vlrDao.insertTopicTracker(TopicTracker(topic))

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
  fun removeTopic(topic: String) = vlrDao.deleteTopic(topic)

  /** Get latest app version from github version file */
  fun getLatestAppVersion() = flow {
    emit(
      simpleKtorHttpClient
        .request(Endpoints.APK_VERSION_PAGE_LINK)
        .bodyAsText()
    )
  }

  /** Get url to the latest apk from github from the latest release page */
  fun getApkUrl() =
    flow<String?> {
      emit(
        HttpClient(OkHttp)
          .request(Endpoints.APK_DOWNLOAD_PAGE_LINK)
          .bodyAsText()
          .lines()
          .find { it.contains(".apk") }
          ?.substringAfter("\"")
          ?.substringBefore("\"")
          ?.prependIndent("https://github.com")
      )
    }

  /**
   * Download apk with progress This method will give % based update of download while downloading
   * the file Returns data in pair format where first parameter is the download progress and the
   * second param is the byte array
   *
   * @param url
   */
  fun downloadApkWithProgress(url: String) =
    flow<Pair<Int, ByteArray>> {
      simpleKtorHttpClient.prepareRequest(url).execute {
        var offset = 0
        val byteBufferSize = 1024 * 100
        val channel = it.bodyAsChannel()
        val contentLen = it.contentLength()?.toInt() ?: 0
        val data = ByteArray(contentLen)
        do {
          val currentRead = channel.readAvailable(data, offset, byteBufferSize)
          val progress =
            if (contentLen == 0) 0 else ((offset / contentLen.toDouble()) * 100).toInt()
          offset += currentRead
          emit(Pair(progress, ByteArray(0)))
        } while (currentRead >= 0)
        emit(Pair(100, data))
      }
    }

  /**
   * Get team details
   *
   * @param id
   */
  fun getTeamDetails(id: String) =
    flow<Operation<TeamDetails>> {
      emit(Waiting())
      try {
        ktorHttpClient.get(Endpoints.teamDetails(id)).body<TeamDetails>().also { emit(Pass(it)) }
      } catch (e: Exception) {
        emit(Fail("Error", e))
      }
    }

  /**
   * Parse news
   *
   * @param id
   */
  fun parseNews(id: String) = NewsParser.parser(id, json).flowOn(ioDispatcher)
}
