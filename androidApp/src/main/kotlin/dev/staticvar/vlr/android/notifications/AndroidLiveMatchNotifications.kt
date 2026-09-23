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
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.core.notifications.LiveUpdateStateProvider
import dev.staticvar.vlr.core.notifications.PushPlatform
import kotlinx.serialization.json.Json
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
) : LiveUpdateStateProvider {
  override val platform: PushPlatform = PushPlatform.Android
  private val appContext = context.applicationContext
  private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
  private val parser = LiveMatchUpdateParser(json)
  private val stateStore = LiveMatchNotificationStateStore(appContext)
  private val renderer = LiveMatchNotificationRenderer(appContext)
  private val lock = Any()

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
      if (!AndroidLiveNotificationAvailability.isAvailable(appContext) ||
        !preferences.preferences.value.enabled || !canPostNotifications()
      ) return
      val update = parser.parse(data) ?: return
      if (!stateStore.shouldAccept(update)) return

      val notification = try {
        renderer.build(update, spoilerPreferences.enabled.value)
      } catch (_: Exception) {
        return
      }
      try {
        notificationManager.notify(notificationTag(update.matchId), NotificationId, notification)
      } catch (_: Exception) {
        return
      }
      stateStore.record(update)
    }
  }

  fun cancelAll() {
    synchronized(lock) {
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
      stateStore.dismiss(matchId)
      notificationManager.cancel(notificationTag(matchId), NotificationId)
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
)

/** Holds a team name, badge, and series score for a notification. */
internal data class LiveMatchTeam(val name: String, val imageUrl: String?, val score: Int?, val tag: String? = null) {
  val displayName: String get() = tag?.trim()?.takeIf(String::isNotEmpty) ?: name
}

/** Holds the current map name, round scores, and map number. */
internal data class LiveMatchMap(val name: String, val scores: List<Int?>, val number: Int? = null)

/** Parses and validates live match snapshots from Firebase messages. */
internal class LiveMatchUpdateParser(private val json: Json) {
  fun parse(data: Map<String, String>): LiveMatchUpdate? {
    if (data[PayloadTypeKey] != PayloadType) return null
    val encodedState = data[StateKey] ?: return null
    return try {
      parseState(json.parseToJsonElement(encodedState).jsonObject)
    } catch (_: Exception) {
      null
    }
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
    return LiveMatchUpdate(matchId, observedAt, terminal, teams, currentMap, root.optionalInt("total_maps"))
      .takeIf(LiveMatchUpdate::isValid)
  }

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
) {
  private val storage = context.getSharedPreferences(StorageName, Context.MODE_PRIVATE)

  init {
    pruneStale()
  }

  /**
   * Rejects dismissed, finished, and older match updates.
   * Allows a final update with the same timestamp as the last live update.
   */
  fun shouldAccept(update: LiveMatchUpdate): Boolean {
    if (storage.getBoolean(update.key(DismissedSuffix), false) ||
      storage.getBoolean(update.key(TerminalSuffix), false)
    ) return false
    val lastObservedAt = storage.getLong(update.key(ObservedAtSuffix), -1)
    return update.observedAt > lastObservedAt || (update.terminal && update.observedAt == lastObservedAt)
  }

  fun record(update: LiveMatchUpdate) {
    pruneStale()
    storage.edit()
      .putLong(update.key(ObservedAtSuffix), update.observedAt)
      .putBoolean(update.key(TerminalSuffix), update.terminal)
      .putLong(update.key(RecordedAtSuffix), nowMillis())
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
      .putBoolean(matchId.key(DismissedSuffix), true)
      .putLong(matchId.key(RecordedAtSuffix), nowMillis())
      .putStringSet(TrackedMatchesKey, trackedMatchIds() + matchId)
      .apply()
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
        remove(matchId.key(ObservedAtSuffix))
        remove(matchId.key(TerminalSuffix))
        remove(matchId.key(DismissedSuffix))
        remove(matchId.key(RecordedAtSuffix))
      }
      putStringSet(TrackedMatchesKey, trackedMatchIds() - stale)
    }.apply()
  }

  /** Defines storage keys and the retention period for match notification state. */
  internal companion object {
    const val StorageName = "live_match_notifications"
    private const val TrackedMatchesKey = "tracked_matches"
    private const val ObservedAtSuffix = "observed_at"
    private const val TerminalSuffix = "terminal"
    private const val DismissedSuffix = "dismissed"
    private const val RecordedAtSuffix = "recorded_at"
    internal const val RetentionMillis = 24 * 60 * 60 * 1_000L
  }
}

/** Builds live and final match notifications while respecting hidden scores. */
internal class LiveMatchNotificationRenderer(private val context: Context) {
  /**
   * Builds a live or final notification with match actions and optional hidden scores.
   * Uses the native progress or metric style when the Android version supports it.
   */
  fun build(update: LiveMatchUpdate, scoresHidden: Boolean, sdkInt: Int = Build.VERSION.SDK_INT): Notification {
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
      .setDeleteIntent(dismissIntent(update.matchId, LiveMatchNotificationReceiver.ActionDismiss))
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
      builder.setShortCriticalText(shortCriticalText(update, scoresHidden))
      if (sdkInt >= 37) {
        Api37Notification.applyPromotedOngoing(builder)
      } else {
        builder.addExtras(Bundle().apply { putBoolean(PromotedOngoingExtra, true) })
      }
    }
    return builder.build()
  }

  private fun applyLive(builder: Notification.Builder, update: LiveMatchUpdate, scoresHidden: Boolean, sdkInt: Int) {
    val seriesScore = scorePair(update.teams.map(LiveMatchTeam::score), scoresHidden)
    val map = update.currentMap
    if (sdkInt >= 37 && map != null) {
      builder.setContentTitle(map.name)
        .setSubText(if (scoresHidden) context.getString(R.string.widget_scores_hidden) else seriesScore)
      Api37Notification.applyMetricStyle(
        builder,
        update,
        scoresHidden,
        context.getString(R.string.widget_score_unavailable),
      )
      return
    }

    val title = if (map == null) {
      context.getString(R.string.widget_live)
    } else {
      joinMetadata(map.name, scorePair(map.scores, scoresHidden))
    }
    val scoreLines = teamScoreLines(update, scoresHidden)
    builder.setContentTitle(title)
      .setContentText(scoreLines)
      .setSubText(if (scoresHidden) context.getString(R.string.widget_scores_hidden) else seriesScore)
    val progress = update.mapProgress()
    if (sdkInt >= 36 && progress != null && !scoresHidden) {
      Api36Notification.applyProgressStyle(builder, progress)
    } else {
      builder.setStyle(Notification.BigTextStyle().bigText(scoreLines))
    }
  }

  private fun applyFinal(builder: Notification.Builder, update: LiveMatchUpdate, scoresHidden: Boolean) {
    val teams = update.teams
    val matchSummary = if (scoresHidden) {
      context.getString(R.string.widget_match_teams, teams[0].displayName, teams[1].displayName)
    } else {
      "${teams[0].displayName} ${scorePair(teams.map(LiveMatchTeam::score), false)} ${teams[1].displayName}"
    }
    val scoreLines = teamScoreLines(update, scoresHidden)
    builder.setContentTitle(joinMetadata(context.getString(R.string.live_match_notification_final), matchSummary))
      .setContentText(scoreLines)
      .setSubText(null)
      .setStyle(Notification.BigTextStyle().bigText(scoreLines))
  }

  private fun teamScoreLines(update: LiveMatchUpdate, scoresHidden: Boolean): String = update.teams.joinToString("\n") { team ->
    context.getString(
      R.string.widget_live_score,
      team.displayName,
      team.score.takeUnless { scoresHidden }?.toString() ?: context.getString(R.string.widget_score_unavailable),
    )
  }

  private fun scorePair(scores: List<Int?>, hidden: Boolean): String {
    val unavailable = context.getString(R.string.widget_score_unavailable)
    val first = scores.getOrNull(0).takeUnless { hidden }?.toString() ?: unavailable
    val second = scores.getOrNull(1).takeUnless { hidden }?.toString() ?: unavailable
    return "$first–$second"
  }

  private fun shortCriticalText(update: LiveMatchUpdate, scoresHidden: Boolean): String =
    update.currentMap?.let { scorePair(it.scores, scoresHidden) } ?: context.getString(R.string.widget_live)

  private fun joinMetadata(first: String, second: String): String =
    first + context.getString(R.string.widget_match_metadata_separator) + second

  private fun dismissIntent(matchId: String, action: String): PendingIntent = PendingIntent.getBroadcast(
    context,
    31 * matchId.hashCode() + action.hashCode(),
    Intent(context, LiveMatchNotificationReceiver::class.java)
      .setAction(action)
      .setData(android.net.Uri.parse("vlr-live-notification://action/$matchId"))
      .putExtra(LiveMatchNotificationReceiver.ExtraMatchId, matchId),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
  )

  /** Defines the live notification channel, promotion flag, and timeout. */
  private companion object {
    const val ChannelId = "live_matches"
    const val PromotedOngoingExtra = "android.requestPromotedOngoing"
    const val LiveNotificationTimeoutMillis = 5 * 60 * 1_000L
  }
}

/** Describes the active map position within a match series. */
internal data class LiveMatchMapProgress(val currentMapNumber: Int, val totalMaps: Int)

private const val MaxVisibleMapSegments = 9

/**
 * Returns map progress only for an active match with usable map numbers.
 * Omits progress when the map metadata is missing or outside the supported range.
 */
internal fun LiveMatchUpdate.mapProgress(): LiveMatchMapProgress? {
  if (terminal) return null
  val maximumMaps = totalMaps?.takeIf { it in 1..MaxVisibleMapSegments } ?: return null
  val activeMap = currentMap?.number?.takeIf { it in 1..maximumMaps } ?: return null
  return LiveMatchMapProgress(activeMap, maximumMaps)
}

/** Applies the map progress notification style available on Android API 36. */
@RequiresApi(36)
private object Api36Notification {
  fun applyProgressStyle(builder: Notification.Builder, progress: LiveMatchMapProgress) {
    builder.setStyle(
      Notification.ProgressStyle()
        .setProgressSegments(List(progress.totalMaps) { Notification.ProgressStyle.Segment(1) })
        .setProgressPoints((1..progress.totalMaps).map { Notification.ProgressStyle.Point(it) })
        .setProgress(progress.currentMapNumber),
    )
  }
}

/** Applies promoted notifications and map score metrics on Android API 37. */
@RequiresApi(37)
private object Api37Notification {
  fun applyPromotedOngoing(builder: Notification.Builder) {
    builder.setRequestPromotedOngoing(true)
  }

  fun applyMetricStyle(
    builder: Notification.Builder,
    update: LiveMatchUpdate,
    scoresHidden: Boolean,
    unavailable: String,
  ) {
    val mapScores = requireNotNull(update.currentMap).scores
    val style = Notification.MetricStyle()
    update.teams.forEachIndexed { index, team ->
      val score = mapScores[index]
      val value = if (scoresHidden || score == null) {
        Notification.Metric.FixedText(unavailable)
      } else {
        Notification.Metric.FixedInt(score)
      }
      style.addMetric(Notification.Metric(value, team.displayName))
    }
    builder.setStyle(style.setCriticalMetric(Notification.MetricStyle.METRIC_INDEX_NONE))
  }
}
