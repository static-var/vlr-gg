package dev.staticvar.vlr.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.common.truth.Truth.assertThat
import dev.staticvar.vlr.BuildConfig
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.db.VlrDB
import dev.staticvar.vlr.data.db.VlrTypeConverter
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.Endpoints
import dev.staticvar.vlr.utils.Fail
import dev.staticvar.vlr.utils.Pass
import dev.staticvar.vlr.utils.TimeElapsed
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// import com.google.common.truth.Truth8.assertThat

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class VlrRepositoryTest {

  private lateinit var vlrDao: VlrDao
  private lateinit var db: VlrDB

  private val testDispatcher = StandardTestDispatcher(name = "Test dispatcher")

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
  }

  private val vlrMockEngine = VlrMockEngine()

  private var vlrHttpClient =
    HttpClient(vlrMockEngine.get()) {
      defaultRequest {
        host = Constants.BASE_URL
        url { protocol = URLProtocol.HTTPS }
      }

      install(DefaultRequest) {
        headers {
          append(HttpHeaders.Authorization, BuildConfig.TOKEN)
          append(Constants.APPLICATION_HEADER, BuildConfig.APPLICATION_ID)
        }
      }
      expectSuccess = true

      install(ContentNegotiation) {
        register(ContentType.Application.Json, KotlinxSerializationConverter(json))
      }
    }

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
      Room.inMemoryDatabaseBuilder(context, VlrDB::class.java)
        .addTypeConverter(VlrTypeConverter(json))
        .allowMainThreadQueries()
        .build()
    vlrDao = db.getVlrDao()
  }

  @After
  fun tearDown() {
    db.close()
  }

  private val repository: VlrRepository by lazy {
    VlrRepository(vlrDao, mockk(), mockk(), mockk(), vlrHttpClient, testDispatcher, json)
  }

  @Test
  fun `test if Ok(false) is returned when updateLatestNews api call is successful`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.NEWS) // Reset cache key for api calls
      repository.updateLatestNews().test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().get()).isFalse() // Api call complete, should return [Ok(false)]
        awaitComplete()
      }
    }

  @Test
  fun `test if Err(exception) is returned when updateLatestNews api responds with error`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.NEWS) // Reset cache key for api calls
      vlrMockEngine.nextResponseWithServerError() // Ensure the api call fails

      repository.updateLatestNews().test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().getError())
          .isNotNull() // Api call complete, should error [Err(ServerResponseException)]
        awaitComplete()
      }
    }

  @Test
  fun `test if getNewsFromDb gets data when api call is successful`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.NEWS) // Reset cache key for api calls

      repository.updateLatestNews().collect()

      repository.getNewsFromDb().test {
        val data = awaitItem()
        assertThat(data)
          .isInstanceOf(Pass::class.java) // Pass is a wrapper class around the actual data
        assertThat(data.data).isNotNull()
        assertThat(data.data?.size).isAtLeast(1) // Check if DAO data has at least 1 item
      }
    }

  @Test
  fun `test if getNewsFromDb has same data as before when api call fails`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.NEWS) // Reset cache key for api calls

      repository.getNewsFromDb().test {
        assertThat(awaitItem().data).isEmpty() // Check if DAO returns has 0 data

        vlrMockEngine.nextResponseWithServerError()
        repository.updateLatestNews().collect()

        expectNoEvents() // This will assert that flow does not emit anything after API call fails
      }
    }

  @Test
  fun `test if Ok(false) is returned when updateLatestMatches api call is successful`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.MATCHES_OVERVIEW) // Reset cache key for api calls
      repository.updateLatestMatches().test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().get()).isFalse() // Api call complete, should return [Ok(false)]
        awaitComplete()
      }
    }

  @Test
  fun `test if Err(exception) is returned when updateLatestMatches api responds with error`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.MATCHES_OVERVIEW) // Reset cache key for api calls
      vlrMockEngine.nextResponseWithServerError() // Ensure the api call fails

      repository.updateLatestMatches().test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().getError())
          .isNotNull() // Api call complete, should error [Err(ServerResponseException)]
        awaitComplete()
      }
    }

  @Test
  fun `test if getMatchesFromDb gets data when api call is successful`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.MATCHES_OVERVIEW) // Reset cache key for api calls

      repository.updateLatestMatches().collect()

      repository.getMatchesFromDb().test {
        val data = awaitItem()
        assertThat(data)
          .isInstanceOf(Pass::class.java) // Pass is a wrapper class around the actual data
        assertThat(data.data).isNotNull()
        assertThat(data.data?.size).isAtLeast(1) // Check if DAO data has at least 1 item
      }
    }

  @Test
  fun `test if getMatchesFromDb has same data as before when api call fails`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.MATCHES_OVERVIEW) // Reset cache key for api calls

      repository.getMatchesFromDb().test {
        assertThat(awaitItem().data).isEmpty() // Check if DAO returns has 0 data

        vlrMockEngine.nextResponseWithServerError()
        repository.updateLatestMatches().collect()

        expectNoEvents() // This will assert that flow does not emit anything after API call fails
      }
    }

  @Test
  fun `test if Ok(false) is returned when updateLatestEvents api call is successful`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.EVENTS_OVERVIEW) // Reset cache key for api calls
      repository.updateLatestEvents().test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().get()).isFalse() // Api call complete, should return [Ok(false)]
        awaitComplete()
      }
    }

  @Test
  fun `test if Err(exception) is returned when updateLatestEvents api responds with error`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.EVENTS_OVERVIEW) // Reset cache key for api calls
      vlrMockEngine.nextResponseWithServerError() // Ensure the api call fails

      repository.updateLatestEvents().test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().getError())
          .isNotNull() // Api call complete, should error [Err(ServerResponseException)]
        awaitComplete()
      }
    }

  @Test
  fun `test if getEventsFromDb gets data when api call is successful`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.EVENTS_OVERVIEW) // Reset cache key for api calls

      repository.updateLatestEvents().collect()

      repository.getEventsFromDb().test {
        val data = awaitItem()
        assertThat(data)
          .isInstanceOf(Pass::class.java) // Pass is a wrapper class around the actual data
        assertThat(data.data).isNotNull()
        assertThat(data.data?.size).isAtLeast(1) // Check if DAO data has at least 1 item
      }
    }

  @Test
  fun `test if getEventsFromDb has same data as before when api call fails`() =
    runTest(testDispatcher) {
      TimeElapsed.reset(Endpoints.EVENTS_OVERVIEW) // Reset cache key for api calls

      repository.getEventsFromDb().test {
        assertThat(awaitItem().data).isEmpty() // Check if DAO returns has 0 data

        vlrMockEngine.nextResponseWithServerError()
        repository.updateLatestEvents().collect()

        expectNoEvents() // This will assert that flow does not emit anything after API call fails
      }
    }

  @Test
  fun `test if Ok(false) is returned when updateLatestMatchDetails api call is successful`() =
    runTest(testDispatcher) {
      val matchId = "107742"
      TimeElapsed.reset(Endpoints.matchDetails(matchId)) // Reset cache key for api calls
      repository.updateLatestMatchDetails(matchId).test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().get()).isFalse() // Api call complete, should return [Ok(false)]
        awaitComplete()
      }
    }

  @Test
  fun `test if Err(exception) is returned when updateLatestMatchDetails api responds with error`() =
    runTest(testDispatcher) {
      val matchId = "107742"
      TimeElapsed.reset(Endpoints.matchDetails(matchId)) // Reset cache key for api calls
      vlrMockEngine.nextResponseWithServerError() // Ensure the api call fails

      repository.updateLatestMatchDetails(matchId).test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().getError())
          .isNotNull() // Api call complete, should error [Err(ServerResponseException)]
        awaitComplete()
      }
    }

  @Test
  fun `test if getMatchDetailsFromDb gets data when api call is successful`() =
    runTest(testDispatcher) {
      val matchId = "107742"
      TimeElapsed.reset(Endpoints.matchDetails(matchId)) // Reset cache key for api calls

      repository.updateLatestMatchDetails(matchId).collect()

      repository.getMatchDetailsFromDb(matchId).test {
        val data = awaitItem()
        assertThat(data)
          .isInstanceOf(Pass::class.java) // Pass is a wrapper class around the actual data
        assertThat(data.data).isNotNull() // Check if DAO data has returned !null data
      }
    }

  @Test
  fun `test if getMatchDetailsFromDb has same data as before when api call fails`() =
    runTest(testDispatcher) {
      val matchId = "107742"
      TimeElapsed.reset(Endpoints.matchDetails(matchId)) // Reset cache key for api calls

      repository.getMatchDetailsFromDb(matchId).test {
        assertThat(awaitItem().data).isNull() // Confirm if DAO has no record of it before

        vlrMockEngine.nextResponseWithServerError()
        repository.updateLatestMatchDetails(matchId).collect()

        expectNoEvents() // This will assert that flow does not emit anything after API call fails
      }
    }

  @Test
  fun `test if Ok(false) is returned when updateLatestEventDetails api call is successful`() =
    runTest(testDispatcher) {
      val eventId = "800"
      TimeElapsed.reset(Endpoints.eventDetails(eventId)) // Reset cache key for api calls
      repository.updateLatestEventDetails(eventId).test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().get()).isFalse() // Api call complete, should return [Ok(false)]
        awaitComplete()
      }
    }

  @Test
  fun `test if Err(exception) is returned when updateLatestEventDetails api responds with error`() =
    runTest(testDispatcher) {
      val eventId = "800"
      TimeElapsed.reset(Endpoints.eventDetails(eventId)) // Reset cache key for api calls
      vlrMockEngine.nextResponseWithServerError() // Ensure the api call fails

      repository.updateLatestEventDetails(eventId).test {
        assertThat(awaitItem().get()).isTrue() // Initial Loading, should return [Ok(true)]
        assertThat(awaitItem().getError())
          .isNotNull() // Api call complete, should error [Err(ServerResponseException)]
        awaitComplete()
      }
    }

  @Test
  fun `test if getEventDetailsFromDb gets data when api call is successful`() =
    runTest(testDispatcher) {
      val eventId = "800"
      TimeElapsed.reset(Endpoints.eventDetails(eventId)) // Reset cache key for api calls

      repository.updateLatestEventDetails(eventId).collect()

      repository.getEventDetailsFromDb(eventId).test {
        val data = awaitItem()
        assertThat(data)
          .isInstanceOf(Pass::class.java) // Pass is a wrapper class around the actual data
        assertThat(data.data).isNotNull() // Check if DAO data has returned !null data
      }
    }

  @Test
  fun `test if getEventDetailsFromDb has same data as before when api call fails`() =
    runTest(testDispatcher) {
      val eventId = "800"
      TimeElapsed.reset(Endpoints.eventDetails(eventId)) // Reset cache key for api calls

      repository.getEventDetailsFromDb(eventId).test {
        assertThat(awaitItem().data).isNull() // Confirm if DAO has no record of it before

        vlrMockEngine.nextResponseWithServerError()
        repository.updateLatestEventDetails(eventId).collect()

        expectNoEvents() // This will assert that flow does not emit anything after API call fails
      }
    }

//  @Test
//  fun `test if trackTopic inserts the data in db`() =
//    runTest(testDispatcher) {
//      val topic = "match/1100"
//      repository.trackTopic(topic)
//      repository.isTopicTracked(topic).test {
//        assertThat(awaitItem()).isTrue()
//        expectNoEvents()
//      }
//    }
//
//  @Test
//  fun `test if removeTopic removes the data from db`() =
//    runTest(testDispatcher) {
//      val topic = "match/1100"
//      repository.trackTopic(topic)
//      repository.isTopicTracked(topic).test {
//        assertThat(awaitItem()).isTrue()
//        expectNoEvents()
//      }
//      repository.removeTopic(topic)
//      repository.isTopicTracked(topic).test {
//        assertThat(awaitItem()).isFalse()
//        expectNoEvents()
//      }
//    }

  @Test
  fun `test if getTeamDetails gets data when api call is successful`() =
    runTest(testDispatcher) {
      val teamId = "2291"

      repository.getTeamDetails(teamId).test {
        skipItems(1) // Waiting state emits first so skip that
        val data = awaitItem()
        assertThat(data)
          .isInstanceOf(Pass::class.java) // Pass is a wrapper class around the actual data
        assertThat(data).isNotNull() // Check if data is not empty
        awaitComplete()
      }
    }

  @Test
  fun `test if getTeamDetails fails gracefully`() =
    runTest(testDispatcher) {
      val teamId = "2291"
      vlrMockEngine.nextResponseWithServerError()
      repository.getTeamDetails(teamId).test {
        skipItems(1) // Waiting state emits first so skip that
        assertThat(awaitItem()).isInstanceOf(Fail::class.java)
        awaitComplete()
      }
    }
}
