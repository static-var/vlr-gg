package dev.staticvar.vlr.data

import com.dropbox.android.external.store4.*
import dev.staticvar.vlr.data.api.response.*
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.model.TopicTracker
import dev.staticvar.vlr.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class VlrRepository
@Inject
constructor(private val vlrDao: VlrDao, private val ktorHttpClient: HttpClient) {
  fun getFiveUpcomingMatches() = vlrDao.getAllMatchesPreviewNoFlow()

  private val news =
    StoreBuilder.from(
        fetcher = Fetcher.of<String, List<NewsResponseItem>> { ktorHttpClient.get(path = "news/") },
        sourceOfTruth =
          SourceOfTruth.Companion.of(
            reader = { vlrDao.getNews() },
            writer = { key, b ->
              vlrDao.insertAllNews(b).also { TimeElapsed.start(key, 180.seconds) }
            },
            deleteAll = {},
            delete = { key -> TimeElapsed.reset(key) }
          )
      )
      .build()

  fun getAllNews() =
    flow<Operation<List<NewsResponseItem>>> {
        news.stream(
            StoreRequest.cached(
              Constants.KEY_NEWS,
              refresh = TimeElapsed.hasElapsed(Constants.KEY_NEWS)
            )
          )
          .collect { response ->
            when (response) {
              is StoreResponse.Loading -> emit(Waiting())
              is StoreResponse.Data -> {
                response.value.takeIf { it.isNotEmpty() }?.let { emit(Pass(it)) }
                  ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                    TimeElapsed.reset(Constants.KEY_NEWS)
                  }
              }
              is StoreResponse.Error.Exception, is StoreResponse.Error.Message ->
                emit(Fail(response.errorMessageOrNull() ?: ""))
            }
          }
      }
      .flowOn(Dispatchers.IO)

  private val allMatches =
    StoreBuilder.from(
        fetcher =
          Fetcher.of<String, List<MatchPreviewInfo>> { ktorHttpClient.get(path = "matches/") },
        sourceOfTruth =
          SourceOfTruth.Companion.of(
            reader = { vlrDao.getAllMatchesPreview() },
            writer = { key, b ->
              vlrDao.deleteAllMatchPreview()
              vlrDao.insertAllMatches(b).also { TimeElapsed.start(key, 45.seconds) }
            },
            deleteAll = {},
            delete = { key -> TimeElapsed.reset(key) }
          )
      )
      .build()

  fun getAllMatchesPreview() =
    flow<Operation<List<MatchPreviewInfo>>> {
        allMatches.stream(
            StoreRequest.cached(
              Constants.KEY_MATCH_ALL,
              refresh = TimeElapsed.hasElapsed(Constants.KEY_MATCH_ALL)
            )
          )
          .collect { response ->
            when (response) {
              is StoreResponse.Loading -> emit(Waiting())
              is StoreResponse.Data -> {
                response.value.takeIf { it.isNotEmpty() }?.let { emit(Pass(it)) }
                  ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                    TimeElapsed.reset(Constants.KEY_MATCH_ALL)
                  }
              }
              is StoreResponse.Error.Exception, is StoreResponse.Error.Message ->
                emit(Fail(response.errorMessageOrNull() ?: ""))
            }
          }
      }
      .flowOn(Dispatchers.IO)

  private val tournamentInfo: Store<String, List<TournamentPreview>> =
    StoreBuilder.from(
        fetcher =
          Fetcher.of<String, List<TournamentPreview>> { ktorHttpClient.get(path = "events/") },
        sourceOfTruth =
          SourceOfTruth.Companion.of(
            reader = { vlrDao.getTournaments() },
            writer = { key, b ->
              vlrDao.deleteAllTournamentPreview()
              vlrDao.insertAllTournamentInfo(b).also { TimeElapsed.start(key, 120.seconds) }
            },
            deleteAll = {},
            delete = { key -> TimeElapsed.reset(key) }
          )
      )
      .build()

  fun getTournaments() =
    flow<Operation<List<TournamentPreview>>> {
        tournamentInfo.stream(
            StoreRequest.cached(
              Constants.KEY_TOURNAMENT_ALL,
              refresh = TimeElapsed.hasElapsed(Constants.KEY_TOURNAMENT_ALL)
            )
          )
          .collect { response ->
            when (response) {
              is StoreResponse.Loading -> emit(Waiting())
              is StoreResponse.Data -> {
                response.value.takeIf { it.isNotEmpty() }?.let { emit(Pass(it)) }
                  ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                    TimeElapsed.reset(Constants.KEY_TOURNAMENT_ALL)
                  }
              }
              is StoreResponse.Error.Exception, is StoreResponse.Error.Message ->
                emit(Fail(response.errorMessageOrNull() ?: ""))
            }
          }
      }
      .flowOn(Dispatchers.IO)

  private fun newMatchDetails(url: String) =
    StoreBuilder.from(
        fetcher = Fetcher.of<String, MatchInfo> { ktorHttpClient.get(path = "matches/$url") },
        sourceOfTruth =
          SourceOfTruth.Companion.of(
            reader = { vlrDao.getMatchById(url) },
            writer = { key, b ->
              b.id = url
              vlrDao.insertMatchInfo(b)
              TimeElapsed.start(key, 30.seconds)
            },
          )
      )
      .build()

  fun getMatchInfo(url: String) =
    flow<Operation<MatchInfo>> {
        newMatchDetails(url)
          .stream(
            StoreRequest.cached(
              Constants.matchDetailKey(url),
              refresh = TimeElapsed.hasElapsed(Constants.matchDetailKey(url))
            )
          )
          .collect { response ->
            when (response) {
              is StoreResponse.Loading -> emit(Waiting())
              is StoreResponse.Data -> {
                if (response.value.id == "") {
                  emit(Fail("unable to get data", SocketTimeoutException())).also {
                    TimeElapsed.reset(Constants.matchDetailKey(url))
                  }
                } else emit(Pass(response.value))
              }
              is StoreResponse.Error.Exception, is StoreResponse.Error.Message ->
                emit(Fail(response.errorMessageOrNull() ?: ""))
            }
          }
      }
      .flowOn(Dispatchers.IO)

  private fun tournamentInfo(url: String) =
    StoreBuilder.from(
        fetcher =
          Fetcher.of<String, TournamentDetails> { ktorHttpClient.get(path = "events/$url") },
        sourceOfTruth =
          SourceOfTruth.Companion.of(
            reader = { vlrDao.getTournamentById(url) },
            writer = { key, b ->
              vlrDao.insertTournamentDetails(b)
              TimeElapsed.start(key, 120.seconds)
            },
          )
      )
      .build()

  fun getTournamentInfo(url: String) =
    flow<Operation<TournamentDetails>> {
        tournamentInfo(url)
          .stream(
            StoreRequest.cached(
              Constants.tournamentDetailKey(url),
              refresh = TimeElapsed.hasElapsed(Constants.tournamentDetailKey(url))
            )
          )
          .collect { response ->
            when (response) {
              is StoreResponse.Loading -> emit(Waiting())
              is StoreResponse.Data -> {
                if (response.value.id.isEmpty()) {
                  emit(Fail("unable to get data", SocketTimeoutException())).also {
                    TimeElapsed.reset(Constants.tournamentDetailKey(url))
                  }
                } else emit(Pass(response.value))
              }
              is StoreResponse.Error.Exception, is StoreResponse.Error.Message ->
                emit(Fail(response.errorMessageOrNull() ?: ""))
            }
          }
      }
      .flowOn(Dispatchers.IO)

  fun trackTopic(topic: String) = vlrDao.insertTopicTracker(TopicTracker(topic))

  fun isTopicTracked(topic: String) =
    flow { emitAll(vlrDao.isTopicSubscribed(topic)) }.flowOn(Dispatchers.IO)

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
