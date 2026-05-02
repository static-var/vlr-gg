package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.Events
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.common.EventStatus
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.events.EventDataSource
import dev.staticvar.vlr.remotesource.events.EventDetailsDto
import dev.staticvar.vlr.remotesource.events.EventListDto
import dev.staticvar.vlr.remotesource.events.EventMatchDto
import dev.staticvar.vlr.remotesource.events.EventMatchTeamDto
import dev.staticvar.vlr.remotesource.events.EventPrizeDto
import dev.staticvar.vlr.remotesource.events.EventStandingsEntryDto
import dev.staticvar.vlr.remotesource.events.EventTeamDto
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: JdbcSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeEventDataSource
  private lateinit var repository: EventRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VlrDatabase.Schema.create(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakeEventDataSource()
    repository = EventRepositoryImpl(
      eventDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun refreshEvents_upsertsAndPrunes() = runTest(dispatcher) {
    insertEvent(
      id = "keep",
      name = "Existing Event",
      subtitle = "Existing subtitle",
      status = "ONGOING",
      prizes = "$1",
      dates = "Old date",
      region = "NA"
    )
    insertEvent(
      id = "stale",
      name = "Stale Event",
      subtitle = "Stale subtitle",
      status = "COMPLETED",
      prizes = "$2",
      dates = "2000-01-01",
      region = "EU"
    )

    dataSource.listResult = Result.success(
      listOf(
        EventListDto(
          id = "keep",
          title = "Updated Event",
          status = EventStatus.UPCOMING,
          prize = "$10",
          dates = "2025-01-01",
          location = "NA",
          img = "logo.png"
        ),
        EventListDto(
          id = "fresh",
          title = "Fresh Event",
          status = EventStatus.UPCOMING,
          prize = "$5",
          dates = "2025-02-01",
          location = "APAC",
          img = "fresh.png"
        )
      )
    )

    val result = repository.refreshEvents()
    assertTrue(result.isSuccess)

    val stored = database.eventsQueries.getEventsWithFavoriteStatus().executeAsList()
    assertEquals(setOf("keep", "fresh"), stored.map { it.id }.toSet())
    val keep = stored.first { it.id == "keep" }
    assertEquals("Existing subtitle", keep.subtitle)
    assertEquals("$10", keep.prizes)
    assertEquals("NA", keep.region)
  }

  @Test
  fun getEvents_emitsWithFavoriteFlag() = runTest(dispatcher) {
    insertEvent(
      id = "event1",
      name = "Event 1",
      subtitle = "",
      status = "UPCOMING",
      prizes = "$1",
      dates = "2025-01-01",
      region = "NA"
    )
    insertEvent(
      id = "event2",
      name = "Event 2",
      subtitle = "",
      status = "COMPLETED",
      prizes = "$2",
      dates = "2025-01-02",
      region = "EU"
    )

    assertTrue(repository.addToFavorites("event2").isSuccess)

    repository.getEvents().test {
      val emission = awaitItem()
      assertEquals(setOf("event1", "event2"), emission.map { it.id }.toSet())
      val favorite = emission.first { it.id == "event2" }
      assertTrue(favorite.isFavorite)
      cancelAndIgnoreRemainingEvents()
    }

    assertTrue(repository.removeFromFavorites("event2").isSuccess)

    repository.getEvents().test {
      val emission = awaitItem()
      val second = emission.first { it.id == "event2" }
      assertTrue(!second.isFavorite)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun eventDetails_flow_updatesAfterRefresh() = runTest(dispatcher) {
    dataSource.listResult = Result.success(
      listOf(
        EventListDto(
          id = "event1",
          title = "Event 1",
          status = EventStatus.UPCOMING,
          prize = "$1",
          dates = "2025-01-01",
          location = "NA",
          img = "event.png"
        )
      )
    )
    assertTrue(repository.refreshEvents().isSuccess)
    assertEquals(1, database.eventsQueries.getEventsWithFavoriteStatus().executeAsList().size)

    dataSource.detailResults["event1"] = Result.success(
      EventDetailsDto(
        id = "event1",
        title = "Event 1",
        subtitle = "Subtitle",
        dates = "2025-01-01",
        prize = "$1",
        location = "NA",
        status = EventStatus.ONGOING,
        img = "event.png",
        prizes = listOf(
          EventPrizeDto(position = "1st", prize = "$10")
        ),
        teams = listOf(
          EventTeamDto(name = "Team A", id = "ta", img = "a.png")
        ),
        matches = listOf(
          EventMatchDto(
            id = "match1",
            time = "10:00",
            date = "2025-01-02",
            eta = "2h",
            status = MatchStatus.COMPLETED,
            stage = "Group",
            round = "Upper",
            teams = listOf(
              EventMatchTeamDto(name = "Team A", region = "NA", score = 2),
              EventMatchTeamDto(name = "Team B", region = "EU", score = 1)
            )
          )
        ),
        standings = listOf(
          EventStandingsEntryDto(
            team = "Team A",
            country = "USA",
            wins = 3,
            losses = 0,
            ties = 0,
            mapDifference = 6,
            roundDifference = 20,
            roundDelta = 15
          )
        )
      )
    )
    val preparedDetail = requireNotNull(dataSource.detailResults["event1"]).getOrNull()
    requireNotNull(preparedDetail)
    assertEquals(1, preparedDetail.prizes.size)
    assertEquals(1, preparedDetail.teams.size)
    assertEquals(1, preparedDetail.standings.size)

    repository.getEventDetails("event1").test {
      val initial = awaitItem()
      assertEquals("$1", initial?.prize)

      val refreshResult = repository.refreshEventDetails("event1")
      assertTrue(refreshResult.isSuccess)
      advanceUntilIdle()
      assertEquals(1, database.eventsQueries.getEventMatches("event1").executeAsList().size)

      var updated: EventDetails? = null
      for (i in 0 until 5) {
        val details = awaitItem()
        if (
          details?.prizes?.isNotEmpty() == true &&
          details.teams.isNotEmpty() &&
          details.standings.isNotEmpty() &&
          details.matches.isNotEmpty()
        ) {
          updated = details
          cancelAndIgnoreRemainingEvents()
          break
        }
      }
      val details = requireNotNull(updated)
      assertEquals("Subtitle", details.subtitle)
      assertEquals(1, details.prizes.size)
      assertEquals(1, details.teams.size)
      assertEquals(1, details.standings.size)
      assertEquals(1, details.matches.size)
      val match = details.matches.first()
      assertEquals("match1", match.matchId)
      assertEquals("10:00", match.time)
      assertEquals("2025-01-02", match.date)
      assertEquals("2h", match.eta)
      assertEquals("completed", match.status)
      assertEquals("Upper", match.round)
      assertEquals("Group", match.stage)
      assertEquals(listOf("Team A", "Team B"), match.teams.map { it.name })
      assertEquals(listOf("NA", "EU"), match.teams.map { it.region })
      assertEquals(listOf(2, 1), match.teams.map { it.score })
    }
  }

  private fun insertEvent(
    id: String,
    name: String,
    subtitle: String,
    status: String?,
    prizes: String,
    dates: String,
    region: String?
  ) {
    eventsQueries().insertEvent(
      Events(
        id = id,
        name = name,
        subtitle = subtitle,
        status = status,
        prizes = prizes,
        dates = dates,
        region = region,
        logo_url = "$id.png",
        last_updated = 0
      )
    )
  }

  private fun eventsQueries() = database.eventsQueries


  private class TestDispatcherProvider(
    private val dispatcher: TestDispatcher
  ) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }

  private class FakeEventDataSource : EventDataSource {
    var listResult: Result<List<EventListDto>> = Result.success(emptyList())
    val detailResults: MutableMap<String, Result<EventDetailsDto>> = mutableMapOf()

    override suspend fun list(): Result<List<EventListDto>> = listResult

    override suspend fun details(id: String): Result<EventDetailsDto> =
      detailResults[id] ?: Result.failure(IllegalStateException("No details for $id"))
  }
}
