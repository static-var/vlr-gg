package dev.staticvar.vlr.workers

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import dev.staticvar.vlr.data.FavoriteTopic
import dev.staticvar.vlr.data.FavoriteTopicCoordinator
import dev.staticvar.vlr.data.FavoriteTopicType
import dev.staticvar.vlr.data.dao.EventFavDao
import dev.staticvar.vlr.data.dao.MatchFavDao
import dev.staticvar.vlr.data.dao.TeamFavDao
import dev.staticvar.vlr.data.model.MatchFav
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class FirebaseTopicSyncWorkerTest {
  private lateinit var context: Context
  private lateinit var matchFavDao: MatchFavDao
  private lateinit var eventFavDao: EventFavDao
  private lateinit var teamFavDao: TeamFavDao
  private lateinit var topicCoordinator: FavoriteTopicCoordinator

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    matchFavDao = mockk()
    eventFavDao = mockk()
    teamFavDao = mockk()
    topicCoordinator = mockk()
    every { eventFavDao.getFavoriteEvents() } returns flowOf(emptyList())
    every { teamFavDao.getFavoriteTeams() } returns flowOf(emptyList())
    coEvery { topicCoordinator.restoreIfFavorite(any()) } returns Unit
  }

  @Test
  fun `snapshot topics are restored through the coordinator`() = runTest {
    every { matchFavDao.getFavoriteMatches() } returns flowOf(listOf(MatchFav("123")))

    val result = newWorker().doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    coVerify(exactly = 1) {
      topicCoordinator.restoreIfFavorite(FavoriteTopic(FavoriteTopicType.MATCH, "123"))
    }
  }

  private fun newWorker(): FirebaseTopicSyncWorker {
    val workerParameters = mockk<WorkerParameters>(relaxed = true)
    every { workerParameters.inputData } returns workDataOf(INPUT_INSTALLATION_ID to "installation")
    return FirebaseTopicSyncWorker(
      appContext = context,
      workerParams = workerParameters,
      matchFavDao = matchFavDao,
      eventFavDao = eventFavDao,
      teamFavDao = teamFavDao,
      topicCoordinator = topicCoordinator,
    )
  }
}
