/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R

/** Posts the legacy pre-match reminders sent to favorite FCM topics. */
internal class AndroidMatchAlertNotifications(
  private val context: Context,
  private val notificationsAllowed: () -> Boolean = {
    context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
  },
  private val nowMillis: () -> Long = System::currentTimeMillis,
) {
  private val manager = context.getSystemService(NotificationManager::class.java)
  private val delivered = context.getSharedPreferences(StorageName, Context.MODE_PRIVATE)

  /** Posts at most one alert per match, including duplicate deliveries through different favorite topics. */
  fun handle(data: Map<String, String>) {
    val matchId = data["match_id"]?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true } ?: return
    val title = data["title"]?.trim()?.takeIf(String::isNotEmpty) ?: return
    val body = data["body"]?.trim()?.takeIf(String::isNotEmpty) ?: return
    synchronized(DeliveryLock) {
      val now = nowMillis()
      val recent = delivered.all.mapNotNull { (id, value) ->
        (value as? Long)?.takeIf { now - it in 0 until RetentionMillis }?.let { id to it }
      }.toMap()
      if (matchId in recent) return
      if (!notificationsAllowed()) return
      try {
        if (Build.VERSION.SDK_INT >= 26) {
          manager.createNotificationChannel(
            NotificationChannel(ChannelId, context.getString(R.string.match_alert_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
              .apply { description = context.getString(R.string.match_alert_channel_description) },
          )
          if (manager.getNotificationChannel(ChannelId)?.importance == NotificationManager.IMPORTANCE_NONE) return
        }
        val intent = PendingIntent.getActivity(
          context,
          matchId.hashCode(),
          Intent(Intent.ACTION_VIEW, Uri.parse("https://valorantesports.staticvar.dev/match/$matchId"), context, MainActivity::class.java),
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, ChannelId) else Notification.Builder(context)
        val notification = builder
          .setSmallIcon(R.drawable.ic_launcher_monochrome)
          .setColor(context.getColor(R.color.widget_preview_accent))
          .setContentTitle(title)
          .setContentText(body)
          .setStyle(Notification.BigTextStyle().bigText(body))
          .setContentIntent(intent)
          .setCategory(Notification.CATEGORY_EVENT)
          .setAutoCancel(true)
          .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
          .setOnlyAlertOnce(true)
          .build()
        manager.notify("match-alert:$matchId", NotificationId, notification)
        record(matchId, now, recent)
      } catch (error: Exception) {
        LiveNotificationDiagnostics.failed("match_alert_post", error)
      }
    }
  }

  /** Removes visible fallback alerts when the user disables match notifications. */
  fun cancelAll() {
    synchronized(DeliveryLock) {
      manager.activeNotifications.filter { it.tag?.startsWith("match-alert:") == true }
        .forEach { manager.cancel(it.tag, it.id) }
    }
  }

  private fun record(matchId: String, now: Long, recent: Map<String, Long>) {
    delivered.edit().clear().apply {
      recent.entries.sortedByDescending { it.value }.take(MaxEntries - 1).forEach { putLong(it.key, it.value) }
      putLong(matchId, now)
    }.apply()
  }

  internal companion object {
    const val ChannelId = "match_alerts"
    const val StorageName = "match_alert_deliveries"
    const val NotificationId = 2
    private val DeliveryLock = Any()
    private const val MaxEntries = 256
    private const val RetentionMillis = 7 * 24 * 60 * 60 * 1_000L
  }
}
