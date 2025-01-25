package dev.staticvar.vlr.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOr
import com.google.common.truth.Truth.assertThat
import dev.staticvar.vlr.data.NewsArticle
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.data.api.response.TournamentDetails
import dev.staticvar.vlr.data.api.response.TournamentPreview
import dev.staticvar.vlr.utils.Pass
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class VlrViewModelTest {
  private val vlrRepository = mockk<VlrRepository>()

  private val viewModel = VlrViewModel(vlrRepository)

  private val testDispatcher = StandardTestDispatcher(name = "Test dispatcher")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun cleanup() {
    Dispatchers.resetMain()
  }

  @Test
  fun `test if refreshNews emits correct loading states if api is successful`() = runTest {
    every { vlrRepository.updateLatestNews() } returns
      flow {
        emit(Ok(true))
        emit(Ok(false))
      }

    viewModel.refreshNews().test {
      skipItems(1)
      assertThat(awaitItem().get()).isTrue()
      assertThat(awaitItem().get()).isFalse()
    }
  }

  @Test
  fun `test if refreshNews emits correct error if api is unsuccessful`() = runTest {
    val exception = IllegalStateException()
    every { vlrRepository.updateLatestNews() } returns flow { emit(Err(exception)) }

    viewModel.refreshNews().test {
      assertThat(awaitItem().get()).isFalse()
      assertThat(awaitItem().getError()).isEqualTo(exception)
    }
  }

  @Test
  fun `test if refreshMatches emits correct loading states if api is successful`() = runTest {
    every { vlrRepository.updateLatestMatches() } returns
      flow {
        emit(Ok(true))
        emit(Ok(false))
      }

    viewModel.refreshMatches().test {
      skipItems(1)
      assertThat(awaitItem().get()).isTrue()
      assertThat(awaitItem().get()).isFalse()
    }
  }

  @Test
  fun `test if refreshMatches emits correct error if api is unsuccessful`() = runTest {
    val exception = IllegalStateException()
    every { vlrRepository.updateLatestMatches() } returns flow { emit(Err(exception)) }

    viewModel.refreshMatches().test {
      assertThat(awaitItem().get()).isFalse()
      assertThat(awaitItem().getError()).isEqualTo(exception)
    }
  }

  @Test
  fun `test if refreshEvents emits correct loading states if api is successful`() = runTest {
    every { vlrRepository.updateLatestEvents() } returns
      flow {
        emit(Ok(true))
        emit(Ok(false))
      }

    viewModel.refreshEvents().test {
      skipItems(1)
      assertThat(awaitItem().get()).isTrue()
      assertThat(awaitItem().get()).isFalse()
    }
  }

  @Test
  fun `test if refreshEvents emits correct error if api is unsuccessful`() = runTest {
    val exception = IllegalStateException()
    every { vlrRepository.updateLatestEvents() } returns flow { emit(Err(exception)) }

    viewModel.refreshEvents().test {
      assertThat(awaitItem().get()).isFalse()
      assertThat(awaitItem().getError()).isEqualTo(exception)
    }
  }

  @Test
  fun `test if refreshMatchInfo emits correct loading states if api is successful`() = runTest {
    every { vlrRepository.updateLatestMatchDetails("800") } returns
      flow {
        emit(Ok(true))
        emit(Ok(false))
      }

    viewModel.refreshMatchInfo("800").test {
      skipItems(1)
      assertThat(awaitItem().get()).isTrue()
      assertThat(awaitItem().get()).isFalse()
    }
  }

  @Test
  fun `test if refreshMatchInfo emits correct error if api is unsuccessful`() = runTest {
    val exception = IllegalStateException()
    every { vlrRepository.updateLatestMatchDetails("800") } returns flow { emit(Err(exception)) }

    viewModel.refreshMatchInfo("800").test {
      assertThat(awaitItem().get()).isFalse()
      assertThat(awaitItem().getError()).isEqualTo(exception)
    }
  }

  @Test
  fun `test if refreshEventDetails emits correct loading states if api is successful`() = runTest {
    every { vlrRepository.updateLatestEventDetails("800") } returns
      flow {
        emit(Ok(true))
        emit(Ok(false))
      }

    viewModel.refreshEventDetails("800").test {
      skipItems(1)
      assertThat(awaitItem().get()).isTrue()
      assertThat(awaitItem().get()).isFalse()
    }
  }

  @Test
  fun `test if refreshEventDetails emits correct error if api is unsuccessful`() = runTest {
    val exception = IllegalStateException()
    every { vlrRepository.updateLatestEventDetails("800") } returns flow { emit(Err(exception)) }

    viewModel.refreshEventDetails("800").test {
      assertThat(awaitItem().get()).isFalse()
      assertThat(awaitItem().getError()).isEqualTo(exception)
    }
  }

  @Test
  fun `test if getNews emits data from db`() = runTest {
    val data = listOf(NewsResponseItem())
    every { vlrRepository.getNewsFromDb() } returns flow { emit(Pass(data)) }
    viewModel.getNews().test {
      skipItems(1)

      val response = awaitItem()
      assertThat(response).isInstanceOf(Pass::class.java)
      assertThat(response.dataOrNull()).isNotEmpty()
    }
  }

  @Test
  fun `test if getMatches emits data from db`() = runTest {
    val data = listOf(MatchPreviewInfo())
    every { vlrRepository.getMatchesFromDb() } returns flow { emit(Pass(data)) }
    viewModel.getMatches().test {
      skipItems(1)

      val response = awaitItem()
      assertThat(response).isInstanceOf(Pass::class.java)
      assertThat(response.dataOrNull()).isNotEmpty()
    }
  }

  @Test
  fun `test if getEvents emits data from db`() = runTest {
    val data = listOf(TournamentPreview())
    every { vlrRepository.getEventsFromDb() } returns flow { emit(Pass(data)) }
    viewModel.getEvents().test {
      skipItems(1)
      val response = awaitItem()
      assertThat(response).isInstanceOf(Pass::class.java)
      assertThat(response.dataOrNull()).isNotEmpty()
    }
  }

  @Test
  fun `test if getMatchDetails emits data from db`() = runTest {
    val data = MatchInfo()
    every { vlrRepository.getMatchDetailsFromDb("800") } returns flow { emit(Pass(data)) }
    viewModel.getMatchDetails("800").test {
      skipItems(1)
      val response = awaitItem()
      assertThat(response).isInstanceOf(Pass::class.java)
      assertThat(response.dataOrNull()).isNotNull()
    }
  }

  @Test
  fun `test if getEventDetails emits data from db`() = runTest {
    val data = TournamentDetails()
    every { vlrRepository.getEventDetailsFromDb("800") } returns flow { emit(Pass(data)) }
    viewModel.getEventDetails("800").test {
      skipItems(1)
      val response = awaitItem()
      assertThat(response).isInstanceOf(Pass::class.java)
      assertThat(response.dataOrNull()).isNotNull()
    }
  }

  @Test
  fun `test if getTeamDetails emits data from db`() = runTest {
    val data = TeamDetails()
    every { vlrRepository.getTeamDetailsFromDb("800") } returns flow { emit(Pass(data)) }
    viewModel.getTeamDetails("800").test {
      skipItems(1)
      val response = awaitItem()
      assertThat(response).isInstanceOf(Pass::class.java)
      assertThat(response.dataOrNull()).isNotNull()
    }
  }

//  @Test
//  fun `test if isTopicTracked calls db`() =
//    runTest() {
//      val id = "800"
//      every { vlrRepository.isTopicTracked(id) } returns flowOf(true)
//      runBlocking { viewModel.isTopicTracked(id) }
//      verify { vlrRepository.isTopicTracked(id) }
//    }

  @Test
  fun `test if parseNews emits data from repository`() =
    runTest() {
      val id = "800"
      val data = NewsArticle()
      every { vlrRepository.parseNews(id) } returns flowOf(Ok(NewsArticle()))
      viewModel.parseNews(id).test {
        skipItems(1)
        assertThat(awaitItem()?.getOr(NewsArticle())).isEqualTo(data)
      }
      verify { vlrRepository.parseNews(id) }
    }
}
