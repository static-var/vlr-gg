package dev.staticvar.vlr.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import dev.staticvar.vlr.BuildConfig
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.db.VlrDB
import dev.staticvar.vlr.data.db.VlrTypeConverter
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.Waiting
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
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
class VlrRepositoryTest {

  private lateinit var vlrDao: VlrDao
  private lateinit var db: VlrDB

  private val testDispatcher = StandardTestDispatcher(name = "Test dispatcher")

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
  }

  private val apiEngine = VlrMockEngine()

  private var vlrHttpClient =
    HttpClient(apiEngine.get()) {
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

  private var httpClient =
    HttpClient(apiEngine.get()) {
      defaultRequest {
        host = Constants.BASE_URL
        url { protocol = URLProtocol.HTTPS }
      }

      install(ContentNegotiation) { json(json = json) }
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
    repository = VlrRepository(vlrDao, vlrHttpClient, httpClient, testDispatcher, json)
  }

  @After
  fun tearDown() {
    db.close()
  }

  private lateinit var repository: VlrRepository

  @Test
  fun `test if mergeNews returns correct data when api responds with error and db has no data`() =
    runTest(testDispatcher) {
      apiEngine.nextResponseWithServerError()
      val flow = repository.mergeNews().testIn(this)
      assertThat(flow.awaitItem()).isInstanceOf(Waiting::class.java)

//      val failedResponse = flow.awaitItem()
//      assertThat(failedResponse).isInstanceOf(Fail::class.java)
//      assertThat(failedResponse.dataOrNull()).isNull()
      flow.cancelAndIgnoreRemainingEvents()
    }

  @Test
  fun `test if mergeNews returns correct data when api responds with error and db has data`() =
    runTest(testDispatcher) {
      launch { vlrDao.insertAllNews(listOf(NewsResponseItem())) }
      apiEngine.nextResponseWithServerError()
      val flow = repository.mergeNews().testIn(this)
      val fail = flow.awaitItem()
      assertThat(fail).isInstanceOf(Waiting::class.java)
      println(fail)

      val emptyData = flow.awaitItem()
      assertThat(emptyData.dataOrNull()).isNotNull()
      assertThat(emptyData.dataOrNull()?.size).isEqualTo(1)

      println(flow.awaitItem())
//
//      val failedResponse = flow.awaitItem()
//      assertThat(failedResponse).isInstanceOf(Fail::class.java)
//      assertThat(failedResponse.dataOrNull()).isNull()

      flow.cancelAndIgnoreRemainingEvents()
    }

  @Test
  fun `test if mergeNews returns correct data when api responds correctly and db has no data`() =
    runTest(testDispatcher) {
      val flow = repository.mergeNews().testIn(this)
      val waiting = flow.awaitItem()
      assertThat(waiting).isInstanceOf(Waiting::class.java)

//      val passData = flow.awaitItem()
//      assertThat(passData).isInstanceOf(Pass::class.java)
//      assertThat(passData.dataOrNull()).isNotEmpty()
      flow.cancelAndIgnoreRemainingEvents()
    }
}
