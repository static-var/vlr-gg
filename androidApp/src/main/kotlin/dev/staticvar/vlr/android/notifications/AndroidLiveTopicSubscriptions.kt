package dev.staticvar.vlr.android.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.domain.repository.FavoriteTopicMode
import dev.staticvar.vlr.domain.repository.FavoriteTopicOperation
import dev.staticvar.vlr.domain.repository.FavoriteTopicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Keeps Firebase live match topics in sync with favorites and notification settings. */
internal class AndroidLiveTopicSubscriptions(
  context: Context,
  private val topics: FavoriteTopicRepository,
  private val preferences: LiveMatchNotificationPreferencesRepository,
  scope: CoroutineScope,
) {
  private val appContext = context.applicationContext
  private val storage = context.getSharedPreferences("live_match_topics", Context.MODE_PRIVATE)
  private val notifications = context.getSystemService(NotificationManager::class.java)
  private val refreshes = Channel<Unit>(Channel.CONFLATED)

  init {
    scope.launch {
      combine(topics.observeChanges(), preferences.preferences) { _, _ -> Unit }
        .collect { refresh() }
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
    if (!AndroidLiveNotificationAvailability.supportsMatchAlerts(appContext)) return
    val transport = FirebaseLiveTopicSubscriptionTransport(FirebaseMessaging.getInstance())
    storage.getStringSet("topics", null)?.let { savedTopics ->
      migrateLegacyTopicSubscriptions(topics, savedTopics.toSet(), transport) {
        check(storage.edit().remove("topics").commit())
      }
    }
    if (subscriptionMode() == FavoriteTopicMode.Live && !storage.getBoolean("legacy_topics_cleared", false)) {
      val reminders = topics.reminderTopics()
      if (reminders.isNotEmpty()) {
        for (topic in reminders) {
          transport.unsubscribe(topic)
          topics.acknowledge(FavoriteTopicOperation.Unsubscribe(topic))
        }
        check(storage.edit().putBoolean("legacy_topics_cleared", true).commit())
      }
    }
    reconcileLiveTopicSubscriptions(
      topics = topics,
      mode = ::subscriptionMode,
      previousToken = storage.getString("token", null),
      transport = transport,
      saveToken = { token -> check(storage.edit().putString("token", token).commit()) },
    )
  }

  private fun subscriptionMode(): FavoriteTopicMode {
    if (!AndroidLiveNotificationAvailability.supportsMatchAlerts(appContext)) return FavoriteTopicMode.Disabled
    val liveSupported = AndroidLiveNotificationAvailability.usesLiveUpdates(
      appContext, preferences.preferences.value.enabled,
    )
    val channelId = if (liveSupported) "live_matches" else "match_alerts"
    val enabled = notifications.areNotificationsEnabled() &&
      (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        notifications.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE)
    return when {
      !enabled -> FavoriteTopicMode.Disabled
      liveSupported -> FavoriteTopicMode.Live
      else -> FavoriteTopicMode.TeamAlerts
    }
  }
}

/** Provides a push token and operations to change live match topic subscriptions. */
internal interface LiveTopicSubscriptionTransport {
  suspend fun currentToken(): String

  suspend fun subscribe(topic: String)

  suspend fun unsubscribe(topic: String)
}

internal suspend fun migrateLegacyTopicSubscriptions(
  topics: FavoriteTopicRepository,
  savedTopics: Set<String>,
  transport: LiveTopicSubscriptionTransport,
  clearLegacyTopics: () -> Unit,
) {
  for (topic in savedTopics) transport.unsubscribe(topic)
  topics.invalidateAcknowledgements()
  clearLegacyTopics()
}

internal suspend fun reconcileLiveTopicSubscriptions(
  topics: FavoriteTopicRepository,
  mode: () -> FavoriteTopicMode,
  previousToken: String?,
  transport: LiveTopicSubscriptionTransport,
  saveToken: (String) -> Unit,
) {
  val token = transport.currentToken()
  if (previousToken != token) {
    topics.invalidateAcknowledgements()
    saveToken(token)
  }
  while (true) {
    val operation = topics.nextOperation(mode()) ?: return
    when (operation) {
      is FavoriteTopicOperation.Subscribe -> transport.subscribe(operation.topic)
      is FavoriteTopicOperation.Unsubscribe -> transport.unsubscribe(operation.topic)
    }
    topics.acknowledge(operation)
  }
}

/** Runs live match topic subscription operations through Firebase Messaging. */
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

private suspend fun <T> Task<T>.awaitCompletion(): T = suspendCancellableCoroutine { continuation ->
  addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
  addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
  addOnCanceledListener { continuation.cancel() }
}
