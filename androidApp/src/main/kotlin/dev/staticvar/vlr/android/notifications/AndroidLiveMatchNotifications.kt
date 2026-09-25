/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dev.staticvar.vlr.android.BuildConfig
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.core.notifications.DismissedLiveUpdateProvider
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Handles incoming match updates and manages their Android notifications. */
internal class AndroidLiveMatchNotifications(
  context: Context,
  json: Json,
  private val preferences: LiveMatchNotificationPreferencesRepository,
  private val spoilerPreferences: SpoilerPreferencesRepository,
  private val logoScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
  internal val logoCache: LiveMatchLogoCache = LiveMatchLogoCache(context.applicationContext),
  private val teamLogos: TeamLogoDirectory = TeamLogoDirectory(context, json),
) : DismissedLiveUpdateProvider {
  override val platform: PushPlatform = PushPlatform.Android
  private val appContext = context.applicationContext
  private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
  private val parser = LiveMatchUpdateParser(json)
  private val stateStore = LiveMatchNotificationStateStore(appContext)
  private val renderer = LiveMatchNotificationRenderer(appContext, logoCache)
  private val lock = Any()
  private val dismissed = MutableStateFlow(stateStore.dismissedMatchIds())
  override val dismissedMatchIds = dismissed.asStateFlow()
  private val lastHandledUpdates = mutableMapOf<String, LiveMatchUpdate>()

  override fun restoreDismissedMatch(matchId: String): Boolean = synchronized(lock) {
    if (!canRequestStart() || !stateStore.restore(matchId)) return@synchronized false
    dismissed.value = stateStore.dismissedMatchIds()
    true
  }

  override fun keepMatchDismissed(matchId: String) {
    synchronized(lock) {
      stateStore.rejectRestore(matchId)
      dismissed.value = stateStore.dismissedMatchIds()
    }
  }

  override fun canRequestStart(): Boolean = AndroidLiveNotificationAvailability.isAvailable(appContext) &&
    preferences.preferences.value.enabled && canPostNotifications()

  override fun observedMatchIds(): List<String> = synchronized(lock) {
    stateStore.trackedMatchIds().toList()
  }

  /**
   * Posts eligible updates that pass ordering and dismissal checks.
   * Records the update after posting succeeds so a failed notification can be retried.
   */
  fun handle(data: Map<String, String>) {
    synchronized(lock) {
      if (!AndroidLiveNotificationAvailability.isAvailable(appContext)) {
        LiveNotificationDiagnostics.skipped("device_unavailable")
        return
      }
      if (!preferences.preferences.value.enabled) {
        LiveNotificationDiagnostics.skipped("preference_disabled")
        return
      }
      if (!canPostNotifications()) {
        LiveNotificationDiagnostics.skipped("notification_permission_or_channel_blocked")
        return
      }
      val update = parser.parse(data) ?: return
      stateStore.rejectionReason(update)?.let { reason ->
        LiveNotificationDiagnostics.skipped(reason, update.matchId)
        return
      }

      lastHandledUpdates[update.matchId] = update

      val generation = UUID.randomUUID().toString()
      val notification = try {
        renderer.build(update.withTeamLogos(), spoilerPreferences.enabled.value, generation = generation)
      } catch (error: Exception) {
        if (error is CancellationException) throw error
        LiveNotificationDiagnostics.failed("render", error)
        return
      }
      try {
        notificationManager.notify(notificationTag(update.matchId), NotificationId, notification)
      } catch (error: Exception) {
        if (error is CancellationException) throw error
        LiveNotificationDiagnostics.failed("post", error)
        return
      }
      stateStore.record(update, generation)
      dismissed.value = stateStore.dismissedMatchIds()
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 36) {
        runCatching {
          val posted = notificationManager.activeNotifications.firstOrNull {
            it.tag == notificationTag(update.matchId) && it.id == NotificationId
          }
          Log.d(
            "LiveMatchNotifications",
            "sdk=${Build.VERSION.SDK_INT}, promotionAllowed=${notificationManager.canPostPromotedNotifications()}, " +
              "promotable=${notification.hasPromotableCharacteristics()}, " +
              "immediatePostedPromotedSnapshot=${posted?.notification?.let { it.flags and Notification.FLAG_PROMOTED_ONGOING != 0 }}",
          )
        }
      }

      if (!update.terminal) {
        logoScope.launch {
          val directoryChanged = teamLogos.refresh()
          val teams = update.withTeamLogos().teams
          val t1Url = teams.getOrNull(0)?.imageUrl
          val t2Url = teams.getOrNull(1)?.imageUrl
          val needsLogos = listOf(t1Url, t2Url).any { !it.isNullOrBlank() && !logoCache.hasLogo(it) }
          val loaded = needsLogos && logoCache.loadAndCacheLogos(t1Url, t2Url)
          if (directoryChanged || loaded) {
            repostIfCurrent(update.matchId, update.observedAt)
          }
        }
      } else {
        lastHandledUpdates.remove(update.matchId)
      }
    }
  }

  private fun repostIfCurrent(matchId: String, observedAt: Long) {
    synchronized(lock) {
      if (!AndroidLiveNotificationAvailability.isAvailable(appContext)) return
      if (!preferences.preferences.value.enabled) return
      if (matchId in stateStore.dismissedMatchIds()) return
      val currentUpdate = lastHandledUpdates[matchId] ?: return
      if (currentUpdate.observedAt != observedAt || currentUpdate.terminal) return

      val newGeneration = UUID.randomUUID().toString()
      val notification = try {
        renderer.build(currentUpdate.withTeamLogos(), spoilerPreferences.enabled.value, generation = newGeneration)
      } catch (_: Exception) {
        return
      }
      try {
        notificationManager.notify(notificationTag(matchId), NotificationId, notification)
        stateStore.record(currentUpdate, newGeneration)
      } catch (_: Exception) {
      }
    }
  }

  private fun LiveMatchUpdate.withTeamLogos(): LiveMatchUpdate =
    copy(teams = teams.map { team -> teamLogos.logoUrl(team.id)?.let { team.copy(imageUrl = it) } ?: team })

  fun cancelAll() {
    synchronized(lock) {
      lastHandledUpdates.clear()
      stateStore.trackedMatchIds().forEach { notificationManager.cancel(notificationTag(it), NotificationId) }
    }
  }

  /**
   * Remembers the dismissal and cancels the visible match notification.
   * Later updates for that match remain suppressed while the saved state is retained.
   */
  fun dismiss(matchId: String) {
    synchronized(lock) {
      if (!matchId.isValidMatchId()) return
      lastHandledUpdates.remove(matchId)
      stateStore.dismiss(matchId)
      dismissed.value = stateStore.dismissedMatchIds()
      notificationManager.cancel(notificationTag(matchId), NotificationId)
    }
  }

  /**
   * Handles the notification's delete intent, which Android also sends when a live notification
   * times out. A timeout means updates paused, not that the user dismissed the match, so it is not
   * remembered and the next update posts the notification again. Callbacks from older posts
   * cannot dismiss a replacement notification.
   */
  fun onNotificationDeleted(matchId: String, generation: String) {
    synchronized(lock) {
      if (!matchId.isValidMatchId() || !stateStore.shouldRememberDeletion(matchId, generation)) return
      dismiss(matchId)
    }
  }

  private fun canPostNotifications(): Boolean {
    if (!notificationManager.areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT >= 33 &&
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false
    if (Build.VERSION.SDK_INT >= 26) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          ChannelId,
          appContext.getString(R.string.live_match_notification_channel_name),
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = appContext.getString(R.string.live_match_notification_channel_description)
        },
      )
      if (notificationManager.getNotificationChannel(ChannelId)?.importance == NotificationManager.IMPORTANCE_NONE) return false
    }
    return true
  }

  /** Identifies the notification channel and each match notification. */
  private companion object {
    const val ChannelId = "live_matches"
    const val NotificationId = 1

    fun notificationTag(matchId: String): String = "live-match-$matchId"
  }
}

/** Holds a match snapshot received for a live notification. */
internal data class LiveMatchUpdate(
  val matchId: String,
  val observedAt: Long,
  val terminal: Boolean,
  val teams: List<LiveMatchTeam>,
  val currentMap: LiveMatchMap?,
  val totalMaps: Int? = null,
  val mapWinners: List<String?> = emptyList(),
)

/** Holds a team name, badge, and series score for a notification. */
internal data class LiveMatchTeam(
  val name: String,
  val imageUrl: String?,
  val score: Int?,
  val tag: String? = null,
  val id: String? = null,
) {
  val displayName: String get() = tag?.trim()?.takeIf(String::isNotEmpty) ?: name
}

/** Holds the current map name, round scores, and map number. */
internal data class LiveMatchMap(val name: String, val scores: List<Int?>, val number: Int? = null)

/** Parses and validates live match snapshots from Firebase messages. */
internal class LiveMatchUpdateParser(private val json: Json) {
  fun parse(data: Map<String, String>): LiveMatchUpdate? {
    if (data[PayloadTypeKey] != PayloadType) {
      LiveNotificationDiagnostics.skipped("unsupported_payload_type")
      return null
    }
    val encodedState = data[StateKey]
    val update = try {
      encodedState?.let { parseState(json.parseToJsonElement(it).jsonObject) }
    } catch (error: Exception) {
      if (error is CancellationException) throw error
      null
    }
    if (update == null) LiveNotificationDiagnostics.skipped("malformed_payload")
    return update
  }

  private fun parseState(root: JsonObject): LiveMatchUpdate? {
    val matchId = root["match_id"]?.jsonPrimitive?.contentOrNull ?: return null
    val observedAt = root["observed_at"]?.jsonPrimitive?.longOrNull ?: return null
    val terminal = root["terminal"]?.jsonPrimitive?.booleanOrNull ?: return null
    val teams = root["teams"]?.jsonArray?.map { teamElement ->
      val team = teamElement.jsonObject
      LiveMatchTeam(
        name = team["name"]?.jsonPrimitive?.contentOrNull ?: return null,
        imageUrl = team["img"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull,
        score = (team.nullableScore("score") ?: return null).value,
        tag = (team["tag"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull,
        id = team["id"].teamIdOrNull(),
      )
    } ?: return null
    val currentMap = root["current_map"]?.takeUnless { it is JsonNull }?.let { mapElement ->
      val map = mapElement.jsonObject
      LiveMatchMap(
        name = map["name"]?.jsonPrimitive?.contentOrNull ?: return null,
        scores = map["scores"]?.jsonArray?.map { scoreElement ->
          (scoreElement.nullableScore() ?: return null).value
        } ?: return null,
        number = map.optionalInt("number"),
      )
    }
    return LiveMatchUpdate(
      matchId, observedAt, terminal, teams, currentMap, root.optionalInt("total_maps"),
      (root["map_winners"] as? JsonArray)?.take(MaxVisibleMapSegments)?.map { it.teamIdOrNull() }.orEmpty(),
    )
      .takeIf(LiveMatchUpdate::isValid)
  }

  private fun JsonElement?.teamIdOrNull(): String? =
    (this as? JsonPrimitive)?.contentOrNull?.toLongOrNull()?.takeIf { it > 0 }?.toString()

  private fun JsonObject.optionalInt(key: String): Int? =
    (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

  private fun JsonObject.nullableScore(key: String): ScoreResult? =
    if (containsKey(key)) getValue(key).nullableScore() else ScoreResult(null)

  private fun kotlinx.serialization.json.JsonElement.nullableScore(): ScoreResult? = when (this) {
    JsonNull -> ScoreResult(null)
    else -> jsonPrimitive.intOrNull?.takeIf { it >= 0 }?.let(::ScoreResult)
  }

  /** Distinguishes a valid missing score from an invalid score. */
  private data class ScoreResult(val value: Int?)

  /** Defines the expected Firebase payload type and field names. */
  private companion object {
    const val PayloadTypeKey = "type"
    const val PayloadType = "match-live-v1"
    const val StateKey = "state"
  }
}

private fun LiveMatchUpdate.isValid(): Boolean =
  matchId.isValidMatchId() &&
    observedAt >= 0 &&
    teams.size == 2 &&
    teams.all { it.name.isNotBlank() } &&
    (currentMap == null || (currentMap.name.isNotBlank() && currentMap.scores.size == 2))

private fun String.isValidMatchId(): Boolean =
  length in 1..10 && all { it in '0'..'9' } && toLongOrNull()?.let { it > 0 } == true

/** Stores update timestamps and dismissals to reject stale or finished match updates. */
internal class LiveMatchNotificationStateStore(
  context: Context,
  private val nowMillis: () -> Long = System::currentTimeMillis,
  private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) {
  private val storage = context.getSharedPreferences(StorageName, Context.MODE_PRIVATE)

  init {
    pruneStale()
  }

  /**
   * Rejects dismissed, finished, and older match updates.
   * Allows a final update with the same timestamp as the last live update.
   */
  fun shouldAccept(update: LiveMatchUpdate): Boolean = rejectionReason(update) == null

  /** Reports the saved-state rule that rejects an update without exposing its contents. */
  fun rejectionReason(update: LiveMatchUpdate): String? {
    if (storage.getBoolean(update.key(DismissedSuffix), false)) return "match_dismissed"
    if (storage.getBoolean(update.key(TerminalSuffix), false)) return "match_terminal"
    val lastObservedAt = storage.getLong(update.key(ObservedAtSuffix), -1)
    return if (update.observedAt > lastObservedAt || ((update.terminal || storage.getBoolean(update.key(RestoringSuffix), false)) && update.observedAt == lastObservedAt)) {
      null
    } else {
      "duplicate_or_out_of_order"
    }
  }

  fun record(update: LiveMatchUpdate, generation: String = UUID.randomUUID().toString()) {
    pruneStale()
    storage.edit()
      .remove(update.key(RestoringSuffix))
      .putString(update.key(GenerationSuffix), generation)
      .putLong(update.key(ObservedAtSuffix), update.observedAt)
      .putBoolean(update.key(TerminalSuffix), update.terminal)
      .putLong(update.key(RecordedAtSuffix), nowMillis())
      .putLong(update.key(RecordedElapsedRealtimeSuffix), elapsedRealtimeMillis())
      .putStringSet(TrackedMatchesKey, trackedMatchIds() + update.matchId)
      .apply()
  }

  /**
   * Saves a dismissal even if no update has been recorded for the match.
   * Refreshes its retention timestamp so later updates stay suppressed.
   */
  fun dismiss(matchId: String) {
    pruneStale()
    storage.edit()
      .remove(matchId.key(RestoringSuffix))
      .putBoolean(matchId.key(DismissedSuffix), true)
      .putLong(matchId.key(RecordedAtSuffix), nowMillis())
      .remove(matchId.key(RecordedElapsedRealtimeSuffix))
      .putStringSet(TrackedMatchesKey, trackedMatchIds() + matchId)
      .apply()
  }

  /** Returns dismissed live matches that can still be explicitly followed again. */
  fun dismissedMatchIds(): Set<String> = trackedMatchIds().filterTo(mutableSetOf()) {
    storage.getBoolean(it.key(DismissedSuffix), false) && !storage.getBoolean(it.key(TerminalSuffix), false)
  }

  /** Clears dismissal while retaining the timestamp and terminal safeguards. */
  fun restore(matchId: String): Boolean {
    if (matchId !in dismissedMatchIds()) return false
    storage.edit()
      .putBoolean(matchId.key(DismissedSuffix), false)
      .putBoolean(matchId.key(RestoringSuffix), true)
      .apply()
    return true
  }

  /** Leaves a notification delivered meanwhile intact if the start request was rejected. */
  fun rejectRestore(matchId: String) {
    if (storage.getBoolean(matchId.key(RestoringSuffix), false)) dismiss(matchId)
  }

  /** Only a timely deletion of the current notification counts as a user dismissal. */
  fun shouldRememberDeletion(matchId: String, generation: String): Boolean {
    if (storage.getString(matchId.key(GenerationSuffix), null) != generation) return false
    if (storage.getBoolean(matchId.key(TerminalSuffix), false)) return true
    val recordedAt = storage.getLong(matchId.key(RecordedElapsedRealtimeSuffix), -1)
    if (recordedAt < 0) return false
    val elapsed = elapsedRealtimeMillis()
    return elapsed >= recordedAt && elapsed - recordedAt < LiveNotificationTimeoutMillis - TimeoutGraceMillis
  }

  fun trackedMatchIds(): Set<String> = storage.getStringSet(TrackedMatchesKey, emptySet()).orEmpty().toSet()

  private fun LiveMatchUpdate.key(suffix: String): String = matchId.key(suffix)

  private fun String.key(suffix: String): String = "match.$this.$suffix"

  private fun pruneStale() {
    val stale = trackedMatchIds().filterTo(mutableSetOf()) { matchId ->
      storage.getLong(matchId.key(RecordedAtSuffix), Long.MIN_VALUE) < nowMillis() - RetentionMillis
    }
    if (stale.isEmpty()) return
    storage.edit().apply {
      stale.forEach { matchId ->
        remove(matchId.key(GenerationSuffix))
        remove(matchId.key(ObservedAtSuffix))
        remove(matchId.key(TerminalSuffix))
        remove(matchId.key(DismissedSuffix))
        remove(matchId.key(RestoringSuffix))
        remove(matchId.key(RecordedAtSuffix))
        remove(matchId.key(RecordedElapsedRealtimeSuffix))
      }
      putStringSet(TrackedMatchesKey, trackedMatchIds() - stale)
    }.apply()
  }

  /** Defines storage keys and the retention period for match notification state. */
  internal companion object {
    const val StorageName = "live_match_notifications"
    private const val TrackedMatchesKey = "tracked_matches"
    private const val GenerationSuffix = "generation"
    private const val ObservedAtSuffix = "observed_at"
    private const val TerminalSuffix = "terminal"
    private const val DismissedSuffix = "dismissed"
    private const val RestoringSuffix = "restoring"
    private const val RecordedAtSuffix = "recorded_at"
    private const val RecordedElapsedRealtimeSuffix = "recorded_elapsed_realtime"
    internal const val RetentionMillis = 24 * 60 * 60 * 1_000L
    private const val TimeoutGraceMillis = 5_000L
  }
}

/** Builds live and final match notifications while respecting hidden scores. */
internal class LiveMatchNotificationRenderer(
  private val context: Context,
  private val logoCache: LiveMatchLogoCache = LiveMatchLogoCache(context.applicationContext),
) {
  private val night: Boolean
    get() = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

  private val colors: LiveMatchTeamColors
    get() = if (night) {
      LiveMatchTeamColors(Color.rgb(207, 178, 255), Color.rgb(100, 218, 199), Color.rgb(158, 158, 166))
    } else {
      LiveMatchTeamColors(Color.rgb(103, 58, 183), Color.rgb(0, 105, 92), Color.rgb(117, 117, 125))
    }

  /**
   * Builds a live or final notification with match actions and optional hidden scores.
   * Uses the native progress style when the Android version supports it.
   */
  fun build(
    update: LiveMatchUpdate,
    scoresHidden: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
    generation: String = UUID.randomUUID().toString(),
  ): Notification {
    val openMatch = PendingIntent.getActivity(
      context,
      update.matchId.hashCode(),
      Intent(Intent.ACTION_VIEW, android.net.Uri.parse("vlr://match/${update.matchId}"), context, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val builder = if (sdkInt >= 26) Notification.Builder(context, ChannelId) else Notification.Builder(context)
    builder
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setColor(context.getColor(R.color.widget_preview_accent))
      .setContentIntent(openMatch)
      .setDeleteIntent(dismissIntent(update.matchId, LiveMatchNotificationReceiver.ActionDismiss, generation))
      .setOnlyAlertOnce(true)
      .setShowWhen(false)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .setCategory(Notification.CATEGORY_EVENT)
      .addAction(
        Notification.Action.Builder(
          null,
          context.getString(R.string.live_match_notification_open_match),
          openMatch,
        ).build(),
      )

    val (t1, t2) = update.teams
    val largeLogo = logoCache.getCompositeIcon(t1.imageUrl, t2.imageUrl, night) ?: logoCache.getStartIcon(t1.imageUrl, night)
    if (largeLogo != null) {
      builder.setLargeIcon(Icon.createWithBitmap(largeLogo))
    }

    if (update.terminal) {
      applyFinal(builder, update, scoresHidden)
      return builder.setOngoing(false).setAutoCancel(true).build()
    }

    applyLive(builder, update, scoresHidden, sdkInt)
    builder
      .setOngoing(true)
      .setAutoCancel(false)
      .setTimeoutAfter(LiveNotificationTimeoutMillis)
      .addAction(
        Notification.Action.Builder(
          null,
          context.getString(R.string.live_match_notification_unpin),
          dismissIntent(update.matchId, LiveMatchNotificationReceiver.ActionUnpin),
        ).build(),
      )
    if (sdkInt >= 36) {
      applyChip(builder, update, scoresHidden)
      if (sdkInt >= 37) {
        Api37Notification.applyPromotedOngoing(builder)
      } else {
        builder.addExtras(Bundle().apply { putBoolean(PromotedOngoingExtra, true) })
      }
    }
    return builder.build()
  }

  private fun applyLive(
    builder: Notification.Builder,
    update: LiveMatchUpdate,
    scoresHidden: Boolean,
    sdkInt: Int,
  ) {
    val (t1, t2) = update.teams
    val map = update.currentMap
    val r1 = map?.scores?.getOrNull(0)
    val r2 = map?.scores?.getOrNull(1)

    val title = liveTitle(t1.displayName, t2.displayName, r1, r2, scoresHidden, map != null)
    val contentText = map?.name ?: context.getString(R.string.widget_live)
    val subText = liveSubText(update.totalMaps, map?.number, t1.score, t2.score, scoresHidden)

    builder
      .setContentTitle(title)
      .setContentText(contentText)
      .setSubText(subText)

    val progress = update.mapProgress()
    if (sdkInt >= 36 && progress != null && !scoresHidden) {
      Api36Notification.applyProgressStyle(
        builder = builder,
        progress = progress,
        colors = colors,
        currentMapScores = map?.scores.orEmpty(),
        context = context,
      )
    } else {
      builder.setStyle(Notification.BigTextStyle().bigText(contentText))
    }
  }

  private fun liveTitle(
    t1Name: String,
    t2Name: String,
    r1: Int?,
    r2: Int?,
    scoresHidden: Boolean,
    hasMap: Boolean,
  ): String = if (scoresHidden || !hasMap) {
    context.getString(R.string.widget_match_teams, t1Name, t2Name)
  } else {
    matchupScore(t1Name, t2Name, r1, r2)
  }

  private fun score(value: Int?): String = value?.toString() ?: context.getString(R.string.widget_score_unavailable)

  private fun matchupScore(first: String, second: String, firstScore: Int?, secondScore: Int?): String =
    context.getString(R.string.live_match_notification_matchup_score, first, score(firstScore), score(secondScore), second)

  private fun liveSubText(
    totalMaps: Int?,
    mapNumber: Int?,
    score1: Int?,
    score2: Int?,
    scoresHidden: Boolean,
  ): String {
    if (scoresHidden) return context.getString(R.string.widget_scores_hidden)
    val series = context.getString(R.string.widget_live_score, score(score1), score(score2))
    return if (totalMaps != null && mapNumber != null) {
      context.getString(R.string.live_match_notification_map_series, mapNumber, totalMaps, series)
    } else if (totalMaps != null) {
      context.getString(R.string.live_match_notification_series, series)
    } else {
      series
    }
  }

  private fun applyFinal(builder: Notification.Builder, update: LiveMatchUpdate, scoresHidden: Boolean) {
    val (first, second) = update.teams
    val matchup = if (scoresHidden) {
      context.getString(R.string.widget_match_teams, first.displayName, second.displayName)
    } else {
      matchupScore(first.displayName, second.displayName, first.score, second.score)
    }
    builder.setContentTitle(context.getString(R.string.live_match_notification_final))
      .setContentText(matchup)
      .setSubText(null)
      .setStyle(Notification.BigTextStyle().bigText(matchup))
  }

  /**
   * Fills the status-bar chip, which shows only the small icon and a short text.
   *
   * With a leader, the chip shows the leader's logo silhouette and the score leader-first (`[NRG] 10–5`). Without a
   * usable logo it names the leader instead (`NRG 10–5`), dropping the name when the text would be too long for the
   * chip, which hides overlong text entirely. Ties, hidden scores and the pre-round state keep the app icon.
   */
  private fun applyChip(builder: Notification.Builder, update: LiveMatchUpdate, scoresHidden: Boolean) {
    val scores = chipScores(update, scoresHidden)
    if (scores == null) {
      builder.setShortCriticalText(context.getString(R.string.widget_live))
      return
    }
    val (s1, s2) = scores
    if (s1 == null || s2 == null) {
      builder.setShortCriticalText("${score(s1)}–${score(s2)}")
      return
    }
    if (s1 == s2) {
      builder.setShortCriticalText("$s1–$s2")
      return
    }
    val leader = update.teams[if (s1 > s2) 0 else 1]
    val score = "${maxOf(s1, s2)}–${minOf(s1, s2)}"
    val icon = logoCache.getChipIcon(leader.imageUrl)
    if (icon != null) {
      builder.setSmallIcon(Icon.createWithBitmap(icon)).setShortCriticalText(score)
    } else {
      builder.setShortCriticalText("${leader.displayName} $score".takeIf { it.length <= ChipTextLimit } ?: score)
    }
  }

  /** Returns the scores the chip shows: the series between maps, otherwise the current map; null before play. */
  private fun chipScores(update: LiveMatchUpdate, scoresHidden: Boolean): Pair<Int?, Int?>? {
    if (scoresHidden) return null
    val map = update.currentMap ?: return null
    val r1 = map.scores.getOrNull(0)
    val r2 = map.scores.getOrNull(1)
    val s1 = update.teams.getOrNull(0)?.score
    val s2 = update.teams.getOrNull(1)?.score

    // Before the first round: round scores are null or both 0, and no map has been won yet
    val isFirstRound = (r1 == null || r1 == 0) && (r2 == null || r2 == 0)
    if (isFirstRound && s1 == 0 && s2 == 0 && (map.number ?: 1) <= 1) {
      return null
    }

    // Between maps: current map is finished
    val mapIndex = (map.number ?: 1) - 1
    val hasWinnerInWinners = update.mapWinners.getOrNull(mapIndex) != null
    val hasWinningScore = r1 != null && r2 != null && maxOf(r1, r2) >= 13 && kotlin.math.abs(r1 - r2) >= 2
    if (hasWinnerInWinners || hasWinningScore) {
      return s1 to s2
    }

    // During a map: current-map round score
    return r1 to r2
  }

  private fun dismissIntent(matchId: String, action: String, generation: String? = null): PendingIntent = PendingIntent.getBroadcast(
    context,
    31 * matchId.hashCode() + action.hashCode(),
    Intent(context, LiveMatchNotificationReceiver::class.java)
      .setAction(action)
      .setData(android.net.Uri.parse("vlr-live-notification://action/$matchId/${generation.orEmpty()}"))
      .putExtra(LiveMatchNotificationReceiver.ExtraMatchId, matchId)
      .putExtra(LiveMatchNotificationReceiver.ExtraGeneration, generation),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
  )

  /** Defines the live notification channel and promotion flag. */
  private companion object {
    const val ChannelId = "live_matches"
    const val PromotedOngoingExtra = "android.requestPromotedOngoing"
  }
}

/** Removes a live notification that stops receiving updates, such as during a server outage. */
// Longest chip text seen fully on API 36 ("NRG 12–10"); longer text is not shortened but hidden.
private const val ChipTextLimit = 9

internal const val LiveNotificationTimeoutMillis = 5 * 60 * 1_000L

/** Describes the active map position within a match series. */
internal data class LiveMatchMapProgress(
  val currentMapNumber: Int,
  val totalMaps: Int,
  val winnerTeamIndices: List<Int?> = List(totalMaps) { null },
)

private const val MaxVisibleMapSegments = 9

/**
 * Returns map progress only for an active match with usable map numbers.
 * Omits progress when the map metadata is missing or outside the supported range.
 */
internal fun LiveMatchUpdate.mapProgress(): LiveMatchMapProgress? {
  if (terminal) return null
  val maximumMaps = totalMaps?.takeIf { it in 1..MaxVisibleMapSegments } ?: return null
  val activeMap = currentMap?.number?.takeIf { it in 1..maximumMaps } ?: return null
  val winners = List(maximumMaps) { mapIndex ->
    mapWinners.getOrNull(mapIndex)?.takeIf { mapIndex + 1 <= activeMap }?.let { winnerId ->
      teams.indices.filter { teams[it].id == winnerId }.singleOrNull()
    }
  }
  return LiveMatchMapProgress(activeMap, maximumMaps, winners)
}

/** Applies the map progress notification style available on Android API 36. */
@RequiresApi(36)
private object Api36Notification {
  const val MapSegmentLength = 100

  fun applyProgressStyle(
    builder: Notification.Builder,
    progress: LiveMatchMapProgress,
    colors: LiveMatchTeamColors,
    currentMapScores: List<Int?>,
    context: Context,
  ) {
    val total = progress.totalMaps
    val current = progress.currentMapNumber
    val first = currentMapScores.getOrNull(0)
    val second = currentMapScores.getOrNull(1)
    val completed = progress.winnerTeamIndices[current - 1] != null ||
      (first != null && second != null && maxOf(first, second) >= 13 && kotlin.math.abs(first - second) >= 2)
    // Round count is an estimate until the map has a winner; overtime must remain inside its segment.
    val roundsProgress = if (completed) {
      MapSegmentLength
    } else {
      (((first ?: 0) + (second ?: 0)) * MapSegmentLength / 24).coerceIn(0, MapSegmentLength - 1)
    }
    val progressValue = (current - 1) * MapSegmentLength + roundsProgress

    val style = Notification.ProgressStyle()
      .setStyledByProgress(false)
      .setProgressSegments(List(total) { index ->
        Notification.ProgressStyle.Segment(MapSegmentLength)
          .setColor(colors.winner(progress.winnerTeamIndices[index]))
      })
      .setProgressPoints((1 until total).take(4).map { index ->
        Notification.ProgressStyle.Point(index * MapSegmentLength)
          .setColor(colors.neutral)
      })
      .setProgress(progressValue)
      .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_live_tracker))

    builder.setStyle(style)
  }
}

/** Applies promoted notifications on Android API 37. */
@RequiresApi(37)
private object Api37Notification {
  fun applyPromotedOngoing(builder: Notification.Builder) {
    builder.setRequestPromotedOngoing(true)
  }
}

/** Keeps each team's text and map wins recognizable in either system theme. */
private data class LiveMatchTeamColors(val first: Int, val second: Int, val neutral: Int) {
  fun team(index: Int): Int = if (index == 0) first else second
  fun winner(index: Int?): Int = index?.let(::team) ?: neutral
}
