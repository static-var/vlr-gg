/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.MatchSummary
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PredictionWarning
import dev.staticvar.vlr.domain.model.PredictionWarningCode
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.match.EventDto
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchInsightsDto
import dev.staticvar.vlr.remotesource.match.MatchPredictionDto
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.MatchSummaryDto
import dev.staticvar.vlr.remotesource.match.PredictionSourceDto
import dev.staticvar.vlr.remotesource.match.PredictionWarningDto
import dev.staticvar.vlr.remotesource.match.TeamDto
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class MatchInsightsPersistenceTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = object : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var repository: MatchRepositoryImpl
  private var detailsResult: Result<MatchDetailsDto> = Result.failure(IllegalStateException("Offline"))
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
    repository = newRepository()
    database.matchesQueries.insertMatch(match())
  }

  @AfterTest
  fun teardown() = driver.close()

  @Test
  fun detailsAndInsightsEmitOneSnapshotAndRestoreOffline() = runTest(dispatcher) {
    detailsResult = Result.success(details(insights = MatchInsightsDto(
      prediction = prediction(),
      preMatchSummary = MatchSummaryDto("Alpha has the edge.", "2026-10-16T12:01:00Z"),
    )))
    repository.getMatchDetails("match").test {
      assertNull(requireNotNull(awaitItem()).insights)
      assertTrue(repository.refreshMatchDetails("match").isSuccess)
      val saved = requireNotNull(awaitItem())
      assertEquals("Updated details", saved.note)
      val insights = requireNotNull(saved.insights)
      val prediction = requireNotNull(insights.prediction)
      assertEquals("120", prediction.teamAId)
      assertEquals("1034", prediction.teamBId)
      assertEquals(0.6, prediction.teamAProbability)
      assertEquals(0.4, prediction.teamBProbability)
      assertEquals(PredictionSource.MODEL, prediction.source)
      assertEquals("2026-10-16T12:00:00Z", prediction.generatedAt)
      assertEquals(listOf(
        PredictionWarning(PredictionWarningCode.LOW_COVERAGE),
        PredictionWarning(PredictionWarningCode.UNKNOWN, upstreamCode = "roster_transition_detected"),
      ), prediction.warnings)
      assertEquals(MatchSummary("Alpha has the edge.", "2026-10-16T12:01:00Z"), insights.preMatchSummary)
      assertEquals(insights, newRepository().getMatchDetails("match").first()?.insights)
      detailsResult = Result.failure(IllegalStateException("Offline"))
      assertTrue(repository.refreshMatchDetails("match").isFailure)
      assertEquals(saved, repository.getMatchDetails("match").first())
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun liveAndCompletedDetailsReplaceEntireSnapshotIncludingMissingSections() = runTest(dispatcher) {
    detailsResult = Result.success(details(status = MatchStatus.LIVE, insights = MatchInsightsDto(
      prediction = prediction().copy(source = PredictionSourceDto("elo", rating = "series")),
      preMatchSummary = MatchSummaryDto("Pregame preview", "before"),
    )))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    assertEquals(PredictionSource.ELO, repository.getMatchDetails("match").first()?.insights?.prediction?.source)

    detailsResult = Result.success(details(status = MatchStatus.COMPLETED, insights = MatchInsightsDto(
      postMatchSummary = MatchSummaryDto("Beta won 2-1.", "after"),
    )))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    val recap = requireNotNull(repository.getMatchDetails("match").first()?.insights)
    assertNull(recap.prediction)
    assertNull(recap.preMatchSummary)
    assertEquals(MatchSummary("Beta won 2-1.", "after"), recap.postMatchSummary)

    detailsResult = Result.success(details(insights = null))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    assertNull(repository.getMatchDetails("match").first()?.insights)
    assertNull(database.matchInsightsQueries.getMatchInsights("match").executeAsOneOrNull())
  }

  @Test
  fun invalidPredictionDoesNotDiscardValidSummaryOrOtherDetails() = runTest(dispatcher) {
    val invalid = listOf(
      prediction().copy(teamAId = "other"),
      prediction().copy(teamBId = "120"),
      prediction().copy(teamA = -0.1, teamB = 1.1),
      prediction().copy(teamA = 0.9),
      prediction().copy(teamA = null),
      prediction().copy(source = PredictionSourceDto("future")),
    )
    for (prediction in invalid) {
      detailsResult = Result.success(details(insights = MatchInsightsDto(
        prediction = prediction,
        preMatchSummary = MatchSummaryDto("Preview", "before"),
      )))
      assertTrue(repository.refreshMatchDetails("match").isSuccess)
      val saved = requireNotNull(repository.getMatchDetails("match").first())
      assertEquals("Updated details", saved.note)
      assertNull(saved.insights?.prediction)
      assertEquals("Preview", saved.insights?.preMatchSummary?.text)
    }
  }

  @Test
  fun reversedPredictionAndListOrderRetainIdAssociation() = runTest(dispatcher) {
    detailsResult = Result.success(details(insights = MatchInsightsDto(
      prediction = prediction().copy(teamAId = "1034", teamBId = "120"),
      preMatchSummary = MatchSummaryDto("Preview", "before"),
    )))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    val saved = requireNotNull(repository.getMatchDetails("match").first()?.insights)
    assertEquals("1034", saved.prediction?.teamAId)
    assertEquals(0.6, saved.prediction?.teamAProbability)
    listResult = Result.success(listOf(preview(team1 = "1034", team2 = "120")))
    assertTrue(repository.refreshMatches().isSuccess)
    assertEquals(saved, repository.getMatchDetails("match").first()?.insights)
  }

  @Test
  fun changedListTeamPairRemovesAllCachedInsights() = runTest(dispatcher) {
    detailsResult = Result.success(details(insights = MatchInsightsDto(
      prediction = prediction(),
      postMatchSummary = MatchSummaryDto("Recap for original pair", "after"),
    )))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    listResult = Result.success(listOf(preview(team1 = "120", team2 = "new-team")))
    assertTrue(repository.refreshMatches().isSuccess)
    assertNull(repository.getMatchDetails("match").first()?.insights)
    assertNull(database.matchInsightsQueries.getMatchInsights("match").executeAsOneOrNull())
  }

  @Test
  fun partialDetailsUseCachedPairAndEmptyInsightsRemainValid() = runTest(dispatcher) {
    detailsResult = Result.success(details(insights = MatchInsightsDto(prediction = prediction())).copy(teams = emptyList()))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    assertEquals("120", repository.getMatchDetails("match").first()?.insights?.prediction?.teamAId)
    detailsResult = Result.success(details(insights = MatchInsightsDto()))
    assertTrue(repository.refreshMatchDetails("match").isSuccess)
    val empty = requireNotNull(repository.getMatchDetails("match").first()?.insights)
    assertNull(empty.prediction)
    assertNull(empty.preMatchSummary)
    assertNull(empty.postMatchSummary)
  }

  private fun newRepository() = MatchRepositoryImpl(matches, database, dispatchers, Json)

  private fun details(status: MatchStatus = MatchStatus.UPCOMING, insights: MatchInsightsDto?) = MatchDetailsDto(
    event = EventDto(id = "event", status = status),
    note = "Updated details",
    teams = listOf(TeamDto(id = "120", name = "Alpha"), TeamDto(id = "1034", name = "Beta")),
    insights = insights,
  )

  private fun preview(team1: String, team2: String) = MatchPreviewDto(
    id = "match", event = "Event", series = "Bo3", status = MatchStatus.UPCOMING,
    team1 = TeamDto(id = team1, name = "Alpha"), team2 = TeamDto(id = team2, name = "Beta"),
  )

  private fun prediction() = MatchPredictionDto(
    teamA = 0.6,
    teamB = 0.4,
    teamAId = "120",
    teamBId = "1034",
    source = PredictionSourceDto("model", modelVersion = "v1"),
    warnings = listOf(
      PredictionWarningDto("low_coverage"),
      PredictionWarningDto("unknown", upstreamCode = "roster_transition_detected"),
    ),
    generatedAt = "2026-10-16T12:00:00Z",
  )

  private fun match() = Matches(
    id = "match", event_id = null, event_name = "Event", event_logo_url = "", series = "Bo3", stage = "",
    status = "UPCOMING", time = "2099-01-01T12:00:00Z", eta = null, note = "", patch = null,
    team1_id = "120", team1_name = "Alpha", team1_logo_url = "", team1_score = null,
    team2_id = "1034", team2_name = "Beta", team2_logo_url = "", team2_score = null,
    map_count = 0, last_updated = 0,
  )
}
