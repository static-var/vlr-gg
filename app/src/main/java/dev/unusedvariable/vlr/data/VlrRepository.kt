package dev.unusedvariable.vlr.data

import com.dropbox.android.external.store4.*
import com.github.ajalt.timberkt.e
import dev.unusedvariable.vlr.data.api.response.*
import dev.unusedvariable.vlr.data.dao.VlrDao
import dev.unusedvariable.vlr.utils.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
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
              fetcher =
                  Fetcher.of<String, List<NewsResponseItem>> { ktorHttpClient.get(path = "news/") },
              sourceOfTruth =
                  SourceOfTruth.Companion.of(
                      reader = { vlrDao.getNews() },
                      writer = { key, b ->
                        vlrDao.insertAllNews(b).also { TimeElapsed.start(key, 90.seconds) }
                      },
                      deleteAll = {},
                      delete = { key -> TimeElapsed.reset(key) }))
          .build()

  fun getAllNews() =
      flow<Operation<List<NewsResponseItem>>> {
            news.stream(
                    StoreRequest.cached(
                        Constants.KEY_NEWS, refresh = TimeElapsed.hasElapsed(Constants.KEY_NEWS)))
                .collect { response ->
                  when (response) {
                    is StoreResponse.Loading -> emit(Waiting())
                    is StoreResponse.Data -> {
                      response.value.takeIf { it.isNotEmpty() }?.let { emit(Pass(it)) }
                          ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException()))
                              .also { TimeElapsed.reset(Constants.KEY_NEWS) }
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
                  Fetcher.of<String, List<MatchPreviewInfo>> {
                    ktorHttpClient.get(path = "matches/")
                  },
              sourceOfTruth =
                  SourceOfTruth.Companion.of(
                      reader = { vlrDao.getAllMatchesPreview() },
                      writer = { key, b ->
                        vlrDao.deleteAllMatchPreview()
                        vlrDao.insertAllMatches(b).also { TimeElapsed.start(key, 30.seconds) }
                      },
                      deleteAll = {},
                      delete = { key -> TimeElapsed.reset(key) }))
          .build()

  fun getAllMatchesPreview() =
      flow<Operation<List<MatchPreviewInfo>>> {
            allMatches.stream(
                    StoreRequest.cached(
                        Constants.KEY_MATCH_ALL,
                        refresh = TimeElapsed.hasElapsed(Constants.KEY_MATCH_ALL)))
                .collect { response ->
                  when (response) {
                    is StoreResponse.Loading -> emit(Waiting())
                    is StoreResponse.Data -> {
                      response.value.takeIf { it.isNotEmpty() }?.let { emit(Pass(it)) }
                          ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException()))
                              .also { TimeElapsed.reset(Constants.KEY_MATCH_ALL) }
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
                  Fetcher.of<String, List<TournamentPreview>> {
                    ktorHttpClient.get(path = "events/")
                  },
              sourceOfTruth =
                  SourceOfTruth.Companion.of(
                      reader = { vlrDao.getTournaments() },
                      writer = { key, b ->
                        vlrDao.deleteAllTournamentPreview()
                        vlrDao.insertAllTournamentInfo(b).also {
                          TimeElapsed.start(key, 90.seconds)
                        }
                      },
                      deleteAll = {},
                      delete = { key -> TimeElapsed.reset(key) }))
          .build()

  fun getTournaments() =
      flow<Operation<List<TournamentPreview>>> {
            tournamentInfo.stream(
                    StoreRequest.cached(
                        Constants.KEY_TOURNAMENT_ALL,
                        refresh = TimeElapsed.hasElapsed(Constants.KEY_TOURNAMENT_ALL)))
                .collect { response ->
                  when (response) {
                    is StoreResponse.Loading -> emit(Waiting())
                    is StoreResponse.Data -> {
                      response.value.takeIf { it.isNotEmpty() }?.let { emit(Pass(it)) }
                          ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException()))
                              .also { TimeElapsed.reset(Constants.KEY_TOURNAMENT_ALL) }
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
                  ))
          .build()

  fun getMatchInfo(url: String) =
      flow<Operation<MatchInfo>> {
            newMatchDetails(url)
                .stream(
                    StoreRequest.cached(
                        Constants.matchDetailKey(url),
                        refresh = TimeElapsed.hasElapsed(Constants.matchDetailKey(url))))
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
                  Fetcher.of<String, TournamentDetails> {
                    ktorHttpClient.get(path = "events/$url")
                  },
              sourceOfTruth =
                  SourceOfTruth.Companion.of(
                      reader = { vlrDao.getTournamentById(url) },
                      writer = { key, b ->
                        vlrDao.insertTournamentDetails(b)
                        TimeElapsed.start(key, 120.seconds)
                      },
                  ))
          .build()

  fun getTournamentInfo(url: String) =
      flow<Operation<TournamentDetails>> {
            tournamentInfo(url)
                .stream(
                    StoreRequest.cached(
                        Constants.tournamentDetailKey(url),
                        refresh = TimeElapsed.hasElapsed(Constants.tournamentDetailKey(url))))
                .collect { response ->
                  when (response) {
                    is StoreResponse.Loading -> emit(Waiting())
                    is StoreResponse.Data -> {
                      e { response.value.toString() }
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
}
