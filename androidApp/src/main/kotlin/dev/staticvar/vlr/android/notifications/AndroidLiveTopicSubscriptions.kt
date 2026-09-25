package dev.staticvar.vlr.android.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
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

/** Keeps Firebase favorite topics in sync with favorites and the active notification mode. */
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

  /**
   * Reconciles saved Firebase topics with the latest favorites and notification settings.
   * Saves each completed change so a later refresh can continue after a failure.
   */
  private suspend fun synchronize() {
    val snapshot = favoritesSnapshot ?: return
    if (!AndroidLiveNotificationAvailability.supportsMatchAlerts(appContext)) return
    val liveUpdatesEnabled = AndroidLiveNotificationAvailability.usesLiveUpdates(
      appContext, preferences.preferences.value.enabled,
    )
    val channelId = if (liveUpdatesEnabled) {
      "live_matches"
    } else {
      "match_alerts"
    }
    val enabled = notifications.areNotificationsEnabled() &&
      (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        notifications.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE)
    val desired = if (enabled) snapshot.notificationTopics(liveUpdatesEnabled) else emptySet()
    val previous = LiveTopicSubscriptionState(
      token = storage.getString("token", null),
      topics = storage.getStringSet("topics", emptySet()).orEmpty(),
    )
    if (desired.isEmpty() && previous.topics.isEmpty()) return
    val transport = FirebaseLiveTopicSubscriptionTransport(FirebaseMessaging.getInstance())
    if (liveUpdatesEnabled && snapshot.hasAny && !storage.getBoolean("legacy_topics_cleared", false)) {
      for (topic in snapshot.notificationTopics(liveUpdatesEnabled = false) - previous.topics) {
        transport.unsubscribe(topic)
      }
      storage.edit().putBoolean("legacy_topics_cleared", true).apply()
    }

    reconcileLiveTopicSubscriptions(
      desired = desired,
      previous = previous,
      transport = transport,
    ) { state ->
      storage.edit()
        .putString("token", state.token)
        .putStringSet("topics", state.topics)
        .apply()
    }
  }
}

/** Records the push token and its known favorite topic subscriptions. */
internal data class LiveTopicSubscriptionState(
  val token: String?,
  val topics: Set<String>,
)

/** Provides a push token and operations to change favorite topic subscriptions. */
internal interface LiveTopicSubscriptionTransport {
  suspend fun currentToken(): String

  suspend fun subscribe(topic: String)

  suspend fun unsubscribe(topic: String)
}

/**
 * Removes unwanted topics and subscribes to missing ones, saving completed changes.
 * After token rotation, rebuilds desired subscriptions and clears stale saved membership.
 */
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

/** Runs topic subscription operations through Firebase Messaging. */
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

/** Uses cached player team IDs for legacy reminders, which do not have player topics. */
internal fun DirectFavoriteSnapshot.notificationTopics(liveUpdatesEnabled: Boolean): Set<String> = buildSet {
  val prefix = if (liveUpdatesEnabled) "live-" else ""
  teams.forEach { add("${prefix}team-${it.id}") }
  matches.forEach { add("${prefix}match-${it.id}") }
  events.forEach { add("${prefix}event-${it.id}") }
  players.forEach { player ->
    if (liveUpdatesEnabled) {
      add("live-player-${player.id}")
    } else {
      player.currentTeamId?.takeIf(String::isNotBlank)?.let { add("team-$it") }
    }
  }
}

private suspend fun <T> Task<T>.awaitCompletion(): T = suspendCancellableCoroutine { continuation ->
  addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
  addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
  addOnCanceledListener { continuation.cancel() }
}
