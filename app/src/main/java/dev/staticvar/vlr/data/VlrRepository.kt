package dev.staticvar.vlr.data

import com.github.michaelbull.result.*
import dev.staticvar.vlr.data.api.response.*
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.model.TopicTracker
import dev.staticvar.vlr.di.IoDispatcher
import dev.staticvar.vlr.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
  fun getMatchesFromDb() = vlrDao.getAllMatchesPreview().map { Pass(it) }

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
  fun getEventsFromDb() = vlrDao.getTournaments().map { Pass(it) }

  /**
   * Get match details from server This will request server to return match data of a given match ID
   * Call will only be made once every 30 seconds for a particular match ID
   *
   * @param id
   */
  fun updateLatestMatchDetails(id: String) =
    flow<Result<Boolean, Throwable?>> {
      emit(Ok(true))
      val key = Endpoints.matchDetails(id)
      if (TimeElapsed.hasElapsed(key)) {
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
  fun getMatchDetailsFromDb(id: String) = vlrDao.getMatchById(id).map { Pass(it) }

  /**
   * Get event details from server Request for latest Event / Tournament details of a given event
   * ID.
   *
   * @param id
   */
  fun updateLatestEventDetails(id: String) =
    flow<Result<Boolean, Throwable?>> {
      emit(Ok(true))
      val key = Endpoints.eventDetails(id)
      if (TimeElapsed.hasElapsed(key)) {
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
  fun getEventDetailsFromDb(id: String) = vlrDao.getTournamentById(id).map { Pass(it) }

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

  /** Get latest app version from github version file */
  fun getLatestAppVersion() = flow {
    runSuspendCatching {
        simpleKtorHttpClient.request(Endpoints.APK_VERSION_PAGE_LINK).bodyAsText()
      }
      .also { emit(it.get()) }
  }

  /** Get url to the latest apk from github from the latest release page */
  fun getApkUrl() =
    flow<String?> {
      runSuspendCatching {
          simpleKtorHttpClient
            .request(Endpoints.APK_DOWNLOAD_PAGE_LINK)
            .bodyAsText()
            .lines()
            .find { it.contains(".apk") }
            ?.substringAfter("\"")
            ?.substringBefore("\"")
            ?.prependIndent("https://github.com")
        }
        .also { emit(it.get()) }
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
