package dev.staticvar.vlr.data

import com.google.firebase.messaging.FirebaseMessaging
import dev.staticvar.vlr.data.dao.EventFavDao
import dev.staticvar.vlr.data.dao.MatchFavDao
import dev.staticvar.vlr.data.dao.TeamFavDao
import dev.staticvar.vlr.data.model.EventFav
import dev.staticvar.vlr.data.model.MatchFav
import dev.staticvar.vlr.data.model.TeamFav
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class FavoriteTopicType(val prefix: String) {
  MATCH("match"),
  EVENT("event"),
  TEAM("team"),
}

data class FavoriteTopic(val type: FavoriteTopicType, val id: String) {
  val firebaseName: String = "${type.prefix}-$id"
}

@Singleton
class FavoriteTopicCoordinator
@Inject
constructor(
  private val matchFavDao: MatchFavDao,
  private val eventFavDao: EventFavDao,
  private val teamFavDao: TeamFavDao,
  private val topicSubscriber: FirebaseTopicSubscriber,
) {
  private val topicMutex = Mutex()

  suspend fun restoreIfFavorite(topic: FavoriteTopic) {
    if (topic.id.isBlank()) return
    topicMutex.withLock {
      if (topic.isFavorite()) {
        topicSubscriber.subscribe(topic.firebaseName)
      }
    }
  }

  suspend fun setFavorite(topic: FavoriteTopic, favorite: Boolean) {
    require(topic.id.isNotBlank()) { "Favorite topic ID cannot be blank" }
    topicMutex.withLock {
      withContext(NonCancellable) {
        if (favorite) {
          topicSubscriber.subscribe(topic.firebaseName)
          topic.addFavorite()
        } else {
          topicSubscriber.unsubscribe(topic.firebaseName)
          topic.removeFavorite()
        }
      }
    }
  }

  private suspend fun FavoriteTopic.isFavorite(): Boolean =
    when (type) {
      FavoriteTopicType.MATCH -> matchFavDao.isFavorite(id)
      FavoriteTopicType.EVENT -> eventFavDao.isFavorite(id)
      FavoriteTopicType.TEAM -> teamFavDao.isFavorite(id)
    }

  private suspend fun FavoriteTopic.addFavorite() {
    when (type) {
      FavoriteTopicType.MATCH -> matchFavDao.addFavMatch(MatchFav(id))
      FavoriteTopicType.EVENT -> eventFavDao.addFavEvent(EventFav(id))
      FavoriteTopicType.TEAM -> teamFavDao.addFavTeam(TeamFav(id))
    }
  }

  private suspend fun FavoriteTopic.removeFavorite() {
    when (type) {
      FavoriteTopicType.MATCH -> matchFavDao.deleteFavMatch(id)
      FavoriteTopicType.EVENT -> eventFavDao.deleteFavEvent(id)
      FavoriteTopicType.TEAM -> teamFavDao.deleteFavTeam(id)
    }
  }
}

@Singleton
class FirebaseTopicSubscriber @Inject constructor() {
  suspend fun subscribe(topic: String) {
    FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
  }

  suspend fun unsubscribe(topic: String) {
    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
  }
}
