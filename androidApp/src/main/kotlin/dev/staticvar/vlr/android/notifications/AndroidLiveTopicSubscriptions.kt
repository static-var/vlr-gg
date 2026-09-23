package dev.staticvar.vlr.android.notifications

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AndroidLiveTopicSubscriptions(
  context: Context,
  favorites: FavoritesRepository,
  private val preferences: LiveMatchNotificationPreferencesRepository,
  scope: CoroutineScope,
) {
  private val appContext = context.applicationContext
  private val storage = context.getSharedPreferences("live_match_topics", Context.MODE_PRIVATE)
  private val notifications = context.getSystemService(NotificationManager::class.java)
  private val refreshes = Channel<Unit>(Channel.CONFLATED)
  @Volatile private var favoritesSnapshot: DirectFavoriteSnapshot? = null

  init {
    scope.launch {
      combine(favorites.observeDirectFavorites(), preferences.preferences) { snapshot, _ -> snapshot }
        .collect { snapshot ->
          favoritesSnapshot = snapshot
          refresh()
        }
    }
    scope.launch {
      for (ignored in refreshes) {
        try {
          synchronize()
        } catch (cancelled: CancellationException) {
          throw cancelled
        } catch (error: Exception) {
          Log.w("LiveMatchTopics", "Could not synchronize live match subscriptions", error)
        }
      }
    }
  }

  fun refresh() {
    refreshes.trySend(Unit)
  }

  private suspend fun synchronize() {
    val snapshot = favoritesSnapshot ?: return
    if (!AndroidLiveNotificationAvailability.isAvailable(appContext)) return
    val enabled = preferences.preferences.value.enabled &&
      notifications.areNotificationsEnabled() &&
      notifications.getNotificationChannel("live_matches")?.importance != NotificationManager.IMPORTANCE_NONE
    val desired = if (enabled) snapshot.liveTopics() else emptySet()
    val previous = LiveTopicSubscriptionState(
      token = storage.getString("token", null),
      topics = storage.getStringSet("topics", emptySet()).orEmpty(),
    )
    if (desired.isEmpty() && previous.topics.isEmpty()) return

    reconcileLiveTopicSubscriptions(
      desired = desired,
      previous = previous,
      transport = FirebaseLiveTopicSubscriptionTransport(FirebaseMessaging.getInstance()),
    ) { state ->
      storage.edit()
        .putString("token", state.token)
        .putStringSet("topics", state.topics)
        .apply()
    }
  }
}

internal data class LiveTopicSubscriptionState(
  val token: String?,
  val topics: Set<String>,
)

internal interface LiveTopicSubscriptionTransport {
  suspend fun currentToken(): String

  suspend fun subscribe(topic: String)

  suspend fun unsubscribe(topic: String)
}

internal suspend fun reconcileLiveTopicSubscriptions(
  desired: Set<String>,
  previous: LiveTopicSubscriptionState,
  transport: LiveTopicSubscriptionTransport,
  save: (LiveTopicSubscriptionState) -> Unit,
) {
  val token = transport.currentToken()
  val current = previous.topics.toMutableSet()
  if (previous.token != token) {
    // Firebase restores subscriptions after token rotation. Remove interests that are no longer
    // desired, then rebuild the known set so every desired topic is subscribed on the new token.
    for (topic in current - desired) transport.unsubscribe(topic)
    current.clear()
    save(LiveTopicSubscriptionState(token, emptySet()))
  }
  for (topic in current - desired) {
    transport.unsubscribe(topic)
    current.remove(topic)
    save(LiveTopicSubscriptionState(token, current.toSet()))
  }
  for (topic in desired - current) {
    transport.subscribe(topic)
    current.add(topic)
    save(LiveTopicSubscriptionState(token, current.toSet()))
  }
}

private class FirebaseLiveTopicSubscriptionTransport(
  private val messaging: FirebaseMessaging,
) : LiveTopicSubscriptionTransport {
  override suspend fun currentToken(): String = messaging.token.awaitCompletion()

  override suspend fun subscribe(topic: String) {
    messaging.subscribeToTopic(topic).awaitCompletion()
  }

  override suspend fun unsubscribe(topic: String) {
    messaging.unsubscribeFromTopic(topic).awaitCompletion()
  }
}

internal fun DirectFavoriteSnapshot.liveTopics(): Set<String> = buildSet {
  teams.forEach { add("live-team-${it.id}") }
  matches.forEach { add("live-match-${it.id}") }
  events.forEach { add("live-event-${it.id}") }
  players.forEach { add("live-player-${it.id}") }
}

private suspend fun <T> Task<T>.awaitCompletion(): T = suspendCancellableCoroutine { continuation ->
  addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
  addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
  addOnCanceledListener { continuation.cancel() }
}
