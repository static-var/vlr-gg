package dev.unusedvariable.vlr.data

import com.dropbox.android.external.store4.*
import com.github.ajalt.timberkt.e
import com.skydoves.sandwich.getOrElse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import dev.unusedvariable.vlr.data.api.response.*
import dev.unusedvariable.vlr.data.api.service.VlrService
import dev.unusedvariable.vlr.data.dao.CompletedMatchDao
import dev.unusedvariable.vlr.data.dao.MatchDetailsDao
import dev.unusedvariable.vlr.data.dao.UpcomingMatchDao
import dev.unusedvariable.vlr.data.dao.VlrDao
import dev.unusedvariable.vlr.data.model.CompletedMatch
import dev.unusedvariable.vlr.data.model.MatchDetails
import dev.unusedvariable.vlr.data.model.UpcomingMatch
import dev.unusedvariable.vlr.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Singleton
class VlrRepository @Inject constructor(
    private val completedMatchDao: CompletedMatchDao,
    private val upcomingMatchDao: UpcomingMatchDao,
    private val matchDetailsDao: MatchDetailsDao,
    private val vlrService: VlrService,
    private val vlrDao: VlrDao
) {
    private val upcomingMatchStore =
        StoreBuilder.from(
            fetcher = Fetcher.of<String, List<UpcomingMatch>> {
                VlrScraper.getMatches(true) as List<UpcomingMatch>
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                reader = { upcomingMatchDao.getAll() },
                writer = { _, b ->
                    upcomingMatchDao.insertTransaction(b)
                        .also { TimeElapsed.start(Constants.KEY_UPCOMING, Duration.seconds(90)) }
                },
                deleteAll = { upcomingMatchDao.deleteAll() },
                delete = { key ->
                    TimeElapsed.reset(key)
                    upcomingMatchDao.deleteAll()
                }
            )
        ).build()

    private val completedMatchStore =
        StoreBuilder.from(
            fetcher = Fetcher.of<String, List<CompletedMatch>> {
                VlrScraper.getMatches(false) as List<CompletedMatch>
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                reader = { completedMatchDao.getAll() },
                writer = { _, b ->
                    completedMatchDao.insertTransaction(b)
                        .also { TimeElapsed.start(Constants.KEY_COMPLETED, Duration.seconds(90)) }
                },
                deleteAll = { completedMatchDao.deleteAll() },
                delete = { key ->
                    TimeElapsed.reset(key)
                    completedMatchDao.deleteAll()
                }
            )
        ).build()


    private fun matchDetails(url: String) =
        StoreBuilder.from(
            fetcher = Fetcher.of<String, List<MatchDetails>> {
                listOf(VlrScraper.matchDetailsJsoup(url))
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                reader = { matchDetailsDao.getMatch(url.split("/").last()) },
                writer = { _, b ->
                    matchDetailsDao.insert(b)
                    TimeElapsed.start(Constants.matchDetailKey(url), Duration.seconds(30))
                },
            )
        ).build()


    fun getUpcomingMatches() = flow<Operation<List<UpcomingMatch>>> {
        upcomingMatchStore.stream(
            StoreRequest.cached(
                Constants.KEY_UPCOMING,
                refresh = TimeElapsed.hasElapsed(Constants.KEY_UPCOMING)
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    response.value.takeIf { it.isNotEmpty() }?.let {
                        emit(Pass(it))
                    } ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                        TimeElapsed.reset(Constants.KEY_UPCOMING)
                    }
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }

        }
    }.flowOn(Dispatchers.IO)


    fun getCompletedMatches() = flow<Operation<List<CompletedMatch>>> {
        completedMatchStore.stream(
            StoreRequest.cached(
                Constants.KEY_COMPLETED,
                refresh = TimeElapsed.hasElapsed(Constants.KEY_COMPLETED)
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    response.value.takeIf { it.isNotEmpty() }?.let {
                        emit(Pass(it))
                    } ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                        TimeElapsed.reset(Constants.KEY_COMPLETED)
                    }
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }

        }
    }.flowOn(Dispatchers.IO)

    fun getMatchDetail(url: String) = flow<Operation<MatchDetails>> {
        matchDetails(url).stream(
            StoreRequest.cached(
                Constants.matchDetailKey(url),
                refresh = TimeElapsed.hasElapsed(Constants.matchDetailKey(url))
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    response.value.takeIf { it.isNotEmpty() }?.let {
                        emit(Pass(response.value[0]))
                    }
                        ?: emit(
                            Fail(
                                "unable to get data",
                                SocketTimeoutException()
                            )
                        ).also { TimeElapsed.reset(Constants.matchDetailKey(url)) }
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getFiveUpcomingMatches() = upcomingMatchDao.fiveUpcomingMatches()

    private val news = StoreBuilder.from(
        fetcher = Fetcher.of<String, List<NewsResponseItem>> {
            vlrService.getNews().getOrNull() ?: listOf()
        },
        sourceOfTruth = SourceOfTruth.Companion.of(
            reader = { vlrDao.getNews() },
            writer = { key, b ->
                vlrDao.insertAllNews(b)
                    .also { TimeElapsed.start(key, 90.seconds) }
            },
            deleteAll = { },
            delete = { key ->
                TimeElapsed.reset(key)
            }
        )
    ).build()

    fun getAllNews() = flow<Operation<List<NewsResponseItem>>> {
        news.stream(
            StoreRequest.cached(
                Constants.KEY_NEWS,
                refresh = TimeElapsed.hasElapsed(Constants.KEY_NEWS)
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    response.value.takeIf { it.isNotEmpty() }?.let {
                        emit(Pass(it))
                    } ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                        TimeElapsed.reset(Constants.KEY_NEWS)
                    }
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }
        }
    }.flowOn(Dispatchers.IO)

    private val allMatches = StoreBuilder.from(
        fetcher = Fetcher.of<String, List<MatchPreviewInfo>> {
             vlrService.getAllMatches().getOrElse { listOf() }
        },
        sourceOfTruth = SourceOfTruth.Companion.of(
            reader = { vlrDao.getAllMatchesPreview() },
            writer = { key, b ->
                e { "$b" }
                vlrDao.insertAllMatches(b)
                    .also { TimeElapsed.start(key, 30.seconds) }
            },
            deleteAll = { },
            delete = { key ->
                TimeElapsed.reset(key)
            }
        )
    ).build()

    fun getAllMatchesPreview() = flow<Operation<List<MatchPreviewInfo>>> {
        allMatches.stream(
            StoreRequest.cached(
                Constants.KEY_MATCH_ALL,
                refresh = TimeElapsed.hasElapsed(Constants.KEY_MATCH_ALL)
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    response.value.takeIf { it.isNotEmpty() }?.let {
                        emit(Pass(it))
                    } ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                        TimeElapsed.reset(Constants.KEY_MATCH_ALL)
                    }
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }
        }
    }.flowOn(Dispatchers.IO)

    private val tournamentInfo: Store<String, List<TournamentPreview>> = StoreBuilder.from(
        fetcher = Fetcher.of<String, List<TournamentPreview>> {
            vlrService.getTournamentInfo().getOrThrow()
        },
        sourceOfTruth = SourceOfTruth.Companion.of(
            reader = { vlrDao.getTournaments() },
            writer = { key, b ->
                e {"$b"}
                vlrDao.insertAllTournamentInfo(b)
                    .also { TimeElapsed.start(key, 90.seconds) }
            },
            deleteAll = { },
            delete = { key ->
                TimeElapsed.reset(key)
            }
        )
    ).build()

    fun getTournaments() = flow<Operation<List<TournamentPreview>>> {
        tournamentInfo.stream(
            StoreRequest.cached(
                Constants.KEY_TOURNAMENT_ALL,
                refresh = TimeElapsed.hasElapsed(Constants.KEY_TOURNAMENT_ALL)
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    response.value.takeIf { it.isNotEmpty() }?.let {
                        emit(Pass(it))
                    } ?: emit(Fail("Unable to Parse", exception = SocketTimeoutException())).also {
                        TimeElapsed.reset(Constants.KEY_TOURNAMENT_ALL)
                    }
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }
        }
    }.flowOn(Dispatchers.IO)


    private fun newMatchDetails(url: String) =
        StoreBuilder.from(
            fetcher = Fetcher.of<String, MatchInfo> {
                vlrService.getMatchDetails(url).getOrNull() ?: MatchInfo()
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                reader = { vlrDao.getMatchById(url) },
                writer = { key, b ->
                    vlrDao.insertMatchInfo(b)
                    TimeElapsed.start(key, 30.seconds)
                },
            )
        ).build()

    fun getMatchInfo(url: String) = flow<Operation<MatchInfo>> {
        newMatchDetails(url).stream(
            StoreRequest.cached(
                Constants.matchDetailKey(url),
                refresh = TimeElapsed.hasElapsed(Constants.matchDetailKey(url))
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    response.value
                    if (response.value.id == 0L) {
                        emit(
                            Fail(
                                "unable to get data",
                                SocketTimeoutException()
                            )
                        ).also { TimeElapsed.reset(Constants.matchDetailKey(url)) }
                    } else
                        emit(Pass(response.value))
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }
        }
    }.flowOn(Dispatchers.IO)


    private fun tournamentInfo(url: String) =
        StoreBuilder.from(
            fetcher = Fetcher.of<String, TournamentDetails> {
                vlrService.getTournamentDetails(url).getOrThrow().also { e { "From API $it" } }
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                reader = { vlrDao.getTournamentById(url) },
                writer = { key, b ->
                    e { "Writing in DB $b" }
                    vlrDao.insertTournamentDetails(b)
                    TimeElapsed.start(key, 120.seconds)
                },
            )
        ).build()

    fun getTournamentInfo(url: String) = flow<Operation<TournamentDetails>> {
        tournamentInfo(url).stream(
            StoreRequest.cached(
                Constants.tournamentDetailKey(url),
                refresh = TimeElapsed.hasElapsed(Constants.tournamentDetailKey(url))
            )
        ).collect { response ->
            when (response) {
                is StoreResponse.Loading -> emit(Waiting())
                is StoreResponse.Data -> {
                    e { response.value.toString() }
                    if (response.value.id.isEmpty()) {
                        emit(
                            Fail(
                                "unable to get data",
                                SocketTimeoutException()
                            )
                        ).also { TimeElapsed.reset(Constants.tournamentDetailKey(url)) }
                    } else
                        emit(Pass(response.value))
                }
                is StoreResponse.Error.Exception,
                is StoreResponse.Error.Message -> emit(Fail(response.errorMessageOrNull() ?: ""))
            }
        }
    }.flowOn(Dispatchers.IO)
}