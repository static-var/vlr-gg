/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PredictionWarning
import dev.staticvar.vlr.domain.model.PredictionWarningCode
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.match.EventDto
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchPredictionDto
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.PredictionProbabilitiesDto
import dev.staticvar.vlr.remotesource.match.PredictionSourceDto
import dev.staticvar.vlr.remotesource.match.PredictionTeamDto
import dev.staticvar.vlr.remotesource.match.PredictionWarningDto
import dev.staticvar.vlr.remotesource.match.TeamDto
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class MatchPredictionPersistenceTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = object : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var predictions: TestMatchPredictionDataSource
  private lateinit var repository: MatchRepositoryImpl
  private var detailsResult: Result<MatchDetailsDto> = Result.failure(IllegalStateException("No details fixture"))
  private var listResult: Result<List<MatchPreviewDto>> = Result.success(emptyList())
  private val matches = object : MatchDataSource {
    override suspend fun list() = listResult
    override suspend fun details(id: String) = detailsResult
  }

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    predictions = TestMatchPredictionDataSource()
    repository = newRepository()
    database.matchesQueries.insertMatch(match())
  }

  @AfterTest
  fun teardown() = driver.close()

  @Test
  fun aggregateEmitsPredictionAndRestoresSourceAndAllWarningCodes() = runTest(dispatcher) {
    predictions.result = Result.success(prediction())
    repository.getMatchDetails("match").test {
      assertNull(requireNotNull(awaitItem()).prediction)

      assertTrue(repository.refreshMatchPrediction("match").isSuccess)

      val saved = requireNotNull(requireNotNull(awaitItem()).prediction)
      assertEquals("120", saved.teamAId)
      assertEquals("1034", saved.teamBId)
      assertEquals(0.6, saved.teamAProbability)
      assertEquals(0.4, saved.teamBProbability)
      assertEquals(PredictionSource.MODEL, saved.source)
      assertEquals(
        listOf(
          PredictionWarning(PredictionWarningCode.LOW_COVERAGE),
          PredictionWarning(PredictionWarningCode.UNKNOWN, upstreamCode = "roster_transition_detected"),
        ),
        saved.warnings,
      )
      assertEquals(saved, newRepository().getMatchDetails("match").first()?.prediction)
      cancelAndIgnoreRemainingEvents()
    }
    assertEquals(listOf("120" to "1034"), predictions.requests)
  }

  @Test
  fun failedRefreshRetainsSavedEloEstimate() = runTest(dispatcher) {
    predictions.result = Result.success(prediction(source = "elo"))
    assertTrue(repository.refreshMatchPrediction("match").isSuccess)
    val saved = requireNotNull(repository.getMatchDetails("match").first()).prediction
    assertEquals(PredictionSource.ELO, requireNotNull(saved).source)
    assertEquals(
      listOf(PredictionWarning(PredictionWarningCode.ELO_FALLBACK, reason = "model_timeout")),
      saved.warnings,
    )
    predictions.result = Result.failure(IllegalStateException("Offline"))

    assertTrue(repository.refreshMatchPrediction("match").isFailure)

    assertEquals(saved, newRepository().getMatchDetails("match").first()?.prediction)
  }

  @Test
  fun detailsRefreshPreservesPregameSnapshotAndStopsRequestsWhenMatchStarts() = runTest(dispatcher) {
    predictions.result = Result.success(prediction())
    assertTrue(repository.refreshMatchPrediction("match").isSuccess)
    val saved = repository.getMatchDetails("match").first()?.prediction
    for (status in listOf(MatchStatus.LIVE, MatchStatus.COMPLETED)) {
      detailsResult = Result.success(MatchDetailsDto(
        event = EventDto(id = "event", name = "Updated event", status = status),
        teams = listOf(TeamDto(id = "120", name = "Alpha", score = 1), TeamDto(id = "1034", name = "Beta", score = 0)),
      ))

      assertTrue(repository.refreshMatchDetails("match").isSuccess)
      assertTrue(repository.refreshMatchPrediction("match").isSuccess)

      val details = requireNotNull(repository.getMatchDetails("match").first())
      assertEquals("Updated event", details.event.name)
      assertEquals(saved, details.prediction)
      assertEquals(1, predictions.requests.size)
    }
  }

  @Test
  fun swappedTeamsInvalidateSnapshotAndLateResponseCannotRestoreIt() = runTest(dispatcher) {
    predictions.result = Result.success(prediction())
    assertTrue(repository.refreshMatchPrediction("match").isSuccess)
    val pending = CompletableDeferred<Result<MatchPredictionDto>>()
    predictions.pending = pending
    val refresh = async { repository.refreshMatchPrediction("match") }
    runCurrent()
    assertEquals(2, predictions.requests.size)
    listResult = Result.success(listOf(MatchPreviewDto(
      id = "match", event = "Event", series = "Bo3", status = MatchStatus.UPCOMING,
      team1 = TeamDto(id = "1034", name = "Beta"), team2 = TeamDto(id = "120", name = "Alpha"),
      time = "2099-01-01T12:00:00Z",
    )))

    assertTrue(repository.refreshMatches().isSuccess)
    assertNull(database.matchPredictionsQueries.getMatchPrediction("match").executeAsOneOrNull())
    pending.complete(Result.success(prediction()))
    assertTrue(refresh.await().isSuccess)

    assertNull(repository.getMatchDetails("match").first()?.prediction)
    assertNull(database.matchPredictionsQueries.getMatchPrediction("match").executeAsOneOrNull())
  }

  @Test
  fun matchStartingDuringRequestDoesNotSaveLateEstimate() = runTest(dispatcher) {
    val pending = CompletableDeferred<Result<MatchPredictionDto>>()
    predictions.pending = pending
    val refresh = async { repository.refreshMatchPrediction("match") }
    runCurrent()
    assertEquals(1, predictions.requests.size)
    detailsResult = Result.success(MatchDetailsDto(
      event = EventDto(id = "event", status = MatchStatus.LIVE),
      teams = listOf(TeamDto(id = "120", name = "Alpha"), TeamDto(id = "1034", name = "Beta")),
    ))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    pending.complete(Result.success(prediction()))

    assertTrue(refresh.await().isSuccess)

    assertNull(repository.getMatchDetails("match").first()?.prediction)
  }

  @Test
  fun overdueUpcomingMatchDoesNotRequestNewPrediction() = runTest(dispatcher) {
    database.matchesQueries.insertMatch(match().copy(time = "2000-01-01T12:00:00Z"))
    predictions.result = Result.success(prediction())

    assertTrue(repository.refreshMatchPrediction("match").isSuccess)

    assertTrue(predictions.requests.isEmpty())
    assertNull(repository.getMatchDetails("match").first()?.prediction)
  }

  @Test
  fun partialDetailsPreserveOverdueScheduleAndPregamePredictionWithoutAnotherRequest() = runTest(dispatcher) {
    predictions.result = Result.success(prediction())
    assertTrue(repository.refreshMatchPrediction("match").isSuccess)
    val saved = requireNotNull(repository.getMatchDetails("match").first()?.prediction)
    val overdueTime = "2000-01-01T12:00:00Z"
    driver.execute(null, "UPDATE matches SET time = '$overdueTime' WHERE id = 'match'", 0)
    detailsResult = Result.success(MatchDetailsDto(
      event = EventDto(id = "event", status = MatchStatus.UPCOMING),
      teams = listOf(TeamDto(id = "120", name = "Alpha"), TeamDto(id = "1034", name = "Beta")),
    ))

    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    assertTrue(repository.refreshMatchPrediction("match").isSuccess)

    assertEquals(overdueTime, database.matchesQueries.getMatchWithFavoriteStatus("match").executeAsOne().time)
    assertEquals(saved, repository.getMatchDetails("match").first()?.prediction)
    assertEquals(listOf("120" to "1034"), predictions.requests)
  }

  @Test
  fun partialListPreservesOverdueScheduleAndPregamePredictionWithoutAnotherRequest() = runTest(dispatcher) {
    predictions.result = Result.success(prediction())
    assertTrue(repository.refreshMatchPrediction("match").isSuccess)
    val saved = requireNotNull(repository.getMatchDetails("match").first()?.prediction)
    val overdueTime = "2000-01-01T12:00:00Z"
    driver.execute(null, "UPDATE matches SET time = '$overdueTime' WHERE id = 'match'", 0)
    listResult = Result.success(listOf(MatchPreviewDto(
      id = "match", event = "Event", series = "Bo3", status = MatchStatus.UPCOMING,
      team1 = TeamDto(id = "120", name = "Alpha"), team2 = TeamDto(id = "1034", name = "Beta"),
      time = null,
    )))

    assertTrue(repository.refreshMatches().isSuccess)
    assertTrue(repository.refreshMatchPrediction("match").isSuccess)

    assertEquals(overdueTime, database.matchesQueries.getMatchWithFavoriteStatus("match").executeAsOne().time)
    assertEquals(saved, repository.getMatchDetails("match").first()?.prediction)
    assertEquals(listOf("120" to "1034"), predictions.requests)
  }

  private fun newRepository() = MatchRepositoryImpl(matches, database, dispatchers, predictions, Json)

  private fun prediction(source: String = "model") = MatchPredictionDto(
    teamA = PredictionTeamDto("120"),
    teamB = PredictionTeamDto("1034"),
    match = PredictionProbabilitiesDto(
      teamA = 0.6,
      teamB = 0.4,
      source = PredictionSourceDto(kind = source, rating = if (source == "elo") "series" else null),
      warnings = if (source == "elo") listOf(PredictionWarningDto("elo_fallback", reason = "model_timeout")) else listOf(
        PredictionWarningDto("low_coverage"),
        PredictionWarningDto("unknown", upstreamCode = "roster_transition_detected"),
      ),
    ),
  )

  private fun match() = Matches(
    id = "match", event_id = null, event_name = "Event", event_logo_url = "", series = "Bo3", stage = "",
    status = "UPCOMING", time = "2099-01-01T12:00:00Z", eta = null, note = "", patch = null,
    team1_id = "120", team1_name = "Alpha", team1_logo_url = "", team1_score = null,
    team2_id = "1034", team2_name = "Beta", team2_logo_url = "", team2_score = null,
    map_count = 0, last_updated = 0,
  )
}
