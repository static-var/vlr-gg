package dev.staticvar.vlr.data

import dev.staticvar.vlr.data.api.response.*
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.model.TopicTracker
import dev.staticvar.vlr.di.IoDispatcher
import dev.staticvar.vlr.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class VlrRepository
@Inject
constructor(
  private val vlrDao: VlrDao,
  private val ktorHttpClient: HttpClient,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  fun getFiveUpcomingMatches() = vlrDao.getAllMatchesPreviewNoFlow()

  private fun getNews() =
    flow<Operation<List<NewsResponseItem>>> {
      if (TimeElapsed.hasElapsed(Constants.KEY_NEWS)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get<List<NewsResponseItem>>(path = "news/").also {
              vlrDao.deleteAndInsertNews(it)
              TimeElapsed.start(Constants.KEY_NEWS, 180.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  private fun getNewsFromDb() =
    vlrDao.getNews().map { if (it.isEmpty()) Waiting() else Pass(it) }.distinctUntilChanged()

  fun mergeNews() = merge(getNewsFromDb(), getNews()).flowOn(ioDispatcher)

  private fun getMatchesFromServer() =
    flow<Operation<List<MatchPreviewInfo>>> {
      if (TimeElapsed.hasElapsed(Constants.KEY_MATCH_ALL)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get<List<MatchPreviewInfo>>(path = "matches/").also {
              vlrDao.deleteAndInsertMatchPreviewInfo(it)
              TimeElapsed.start(Constants.KEY_MATCH_ALL, 30.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  private fun getMatchesFromDb() =
    vlrDao
      .getAllMatchesPreview()
      .map { if (it.isEmpty()) Waiting() else Pass(it) }
      .distinctUntilChanged()

  fun mergeMatches() = merge(getMatchesFromDb(), getMatchesFromServer()).flowOn(ioDispatcher)

  private fun getEventsFromServer() =
    flow<Operation<List<TournamentPreview>>> {
      if (TimeElapsed.hasElapsed(Constants.KEY_TOURNAMENT_ALL)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get<List<TournamentPreview>>(path = "events/").also {
              vlrDao.deleteAndInsertTournamentPreview(it)
              TimeElapsed.start(Constants.KEY_TOURNAMENT_ALL, 60.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  private fun getEventsFromDb() =
    vlrDao.getTournaments().map { if (it.isEmpty()) Waiting() else Pass(it) }.distinctUntilChanged()

  fun mergeEvents() = merge(getEventsFromDb(), getEventsFromServer()).flowOn(ioDispatcher)

  private fun getMatchDetailsFromServer(url: String) =
    flow<Operation<MatchInfo>> {
      if (TimeElapsed.hasElapsed(url)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get<MatchInfo>(path = "matches/$url").also {
              it.id = url
              vlrDao.insertMatchInfo(it)
              TimeElapsed.start(url, 30.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  private fun getMatchDetailsFromDb(url: String) =
    vlrDao.getMatchById(url).map { if (it == null) Waiting() else Pass(it) }

  fun mergeMatchDetails(url: String) =
    merge(getMatchDetailsFromDb(url), getMatchDetailsFromServer(url)).flowOn(ioDispatcher)

  private fun getEventDetailsFromServer(url: String) =
    flow<Operation<TournamentDetails>> {
      if (TimeElapsed.hasElapsed(url)) {
        val response =
          kotlin.runCatching {
            ktorHttpClient.get<TournamentDetails>(path = "events/$url").also {
              vlrDao.insertTournamentDetails(it)
              TimeElapsed.start(url, 30.seconds)
            }
          }
        if (response.isFailure) emit(Fail(response.exceptionOrNull().toString()))
      }
    }

  private fun getEventDetailsFromDb(url: String) =
    vlrDao.getTournamentById(url).map { if (it == null) Waiting() else Pass(it) }

  fun mergeEventDetails(url: String) =
    merge(getEventDetailsFromDb(url), getEventDetailsFromServer(url)).flowOn(ioDispatcher)

  fun trackTopic(topic: String) = vlrDao.insertTopicTracker(TopicTracker(topic))

  fun isTopicTracked(topic: String) = vlrDao.isTopicSubscribed(topic).flowOn(ioDispatcher)

  fun removeTopic(topic: String) = vlrDao.deleteTopic(topic)

  fun getLatestAppVersion() = flow {
    emit(
      HttpClient(Android)
        .request<HttpStatement>("https://raw.githubusercontent.com/static-var/vlr-gg/trunk/version")
        .execute()
        .readText()
    )
  }

  fun getApkUrl() =
    flow<String?> {
      emit(
        HttpClient(Android)
          .request<HttpStatement>("https://github.com/static-var/vlr-gg/releases/latest")
          .execute()
          .readText()
          .lines()
          .find { it.contains(".apk") }
          ?.substringAfter("\"")
          ?.substringBefore("\"")
          ?.prependIndent("https://github.com")
      )
    }

  fun downloadApkWithProgress(url: String) =
    flow<Pair<Int, ByteArray>> {
      HttpClient(Android).request<HttpStatement>(url).execute {
        var offset = 0
        val byteBufferSize = 1024 * 100
        val channel = it.receive<ByteReadChannel>()
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

  fun getTeamDetails(id: String) =
    flow<Operation<TeamDetails>> {
      emit(Waiting())
      try {
        ktorHttpClient.get<TeamDetails>("team/$id").also { emit(Pass(it)) }
      } catch (e: Exception) {
        emit(Fail("Error", e))
      }
    }
}
