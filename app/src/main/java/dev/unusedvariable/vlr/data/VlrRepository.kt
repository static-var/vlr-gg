package dev.unusedvariable.vlr.data

import com.dropbox.android.external.store4.*
import dev.unusedvariable.vlr.data.dao.CompletedMatchDao
import dev.unusedvariable.vlr.data.dao.MatchDetailsDao
import dev.unusedvariable.vlr.data.dao.UpcomingMatchDao
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

@Singleton
class VlrRepository @Inject constructor(
    private val completedMatchDao: CompletedMatchDao,
    private val upcomingMatchDao: UpcomingMatchDao,
    private val matchDetailsDao: MatchDetailsDao
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
}