package dev.staticvar.vlr.data

import com.google.common.truth.Truth.assertThat
import dev.staticvar.vlr.data.dao.EventFavDao
import dev.staticvar.vlr.data.dao.MatchFavDao
import dev.staticvar.vlr.data.dao.TeamFavDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

class FavoriteTopicCoordinatorTest {
  private val matchFavDao = mockk<MatchFavDao>()
  private val eventFavDao = mockk<EventFavDao>()
  private val teamFavDao = mockk<TeamFavDao>()
  private val topicSubscriber = mockk<FirebaseTopicSubscriber>()
  private val coordinator =
    FavoriteTopicCoordinator(matchFavDao, eventFavDao, teamFavDao, topicSubscriber)

  @Test
  fun `removed snapshot topic is not restored`() = runTest {
    val topic = FavoriteTopic(FavoriteTopicType.MATCH, "removed")
    coEvery { matchFavDao.isFavorite(topic.id) } returns false

    coordinator.restoreIfFavorite(topic)

    coVerify(exactly = 0) { topicSubscriber.subscribe(any()) }
  }

  @Test
  fun `cancelled unfavorite completes Firebase and Room transition`() = runTest {
    val topic = FavoriteTopic(FavoriteTopicType.MATCH, "123")
    val unsubscribeStarted = CompletableDeferred<Unit>()
    val finishUnsubscribe = CompletableDeferred<Unit>()
    coEvery { topicSubscriber.unsubscribe(topic.firebaseName) } coAnswers {
      unsubscribeStarted.complete(Unit)
      finishUnsubscribe.await()
    }
    coEvery { matchFavDao.deleteFavMatch(topic.id) } returns Unit

    val unfavorite = async { coordinator.setFavorite(topic, favorite = false) }
    unsubscribeStarted.await()
    unfavorite.cancel()
    finishUnsubscribe.complete(Unit)
    unfavorite.cancelAndJoin()

    coVerifyOrder {
      topicSubscriber.unsubscribe(topic.firebaseName)
      matchFavDao.deleteFavMatch(topic.id)
    }
  }

  @Test
  fun `unfavorite waits for in-flight restore and wins`() = runTest {
    val topic = FavoriteTopic(FavoriteTopicType.MATCH, "123")
    val restoreStarted = CompletableDeferred<Unit>()
    val finishRestore = CompletableDeferred<Unit>()
    coEvery { matchFavDao.isFavorite(topic.id) } returns true
    coEvery { topicSubscriber.subscribe(topic.firebaseName) } coAnswers {
      restoreStarted.complete(Unit)
      finishRestore.await()
    }
    coEvery { topicSubscriber.unsubscribe(topic.firebaseName) } returns Unit
    coEvery { matchFavDao.deleteFavMatch(topic.id) } returns Unit

    val restore = async { coordinator.restoreIfFavorite(topic) }
    restoreStarted.await()
    val unfavorite = async { coordinator.setFavorite(topic, favorite = false) }
    yield()
    assertThat(unfavorite.isCompleted).isFalse()
    finishRestore.complete(Unit)
    restore.await()
    unfavorite.await()

    coVerifyOrder {
      topicSubscriber.subscribe(topic.firebaseName)
      topicSubscriber.unsubscribe(topic.firebaseName)
      matchFavDao.deleteFavMatch(topic.id)
    }
  }
}
