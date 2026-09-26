/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Exercises legacy reminder parsing, delivery suppression, and ordinary system notifications. */
class AndroidMatchAlertNotificationsTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val manager = context.getSystemService(NotificationManager::class.java)
  private val storage = context.getSharedPreferences(AndroidMatchAlertNotifications.StorageName, 0)
  private val matchId = "991000099"
  private val tag = "match-alert:$matchId"

  @Before
  @After
  fun clearFixture() {
    manager.cancel(tag, AndroidMatchAlertNotifications.NotificationId)
    storage.edit().remove(matchId).commit()
  }

  @Test
  fun rejectsMalformedAndLiveUpdatePayloads() {
    val renderer = AndroidMatchAlertNotifications(context)
    renderer.handle(mapOf("type" to "match-live-v1", "state" to "{invalid"))
    renderer.handle(mapOf("match_id" to "bad", "title" to "Old reminder", "body" to "Soon"))
    assertFalse(storage.contains(matchId))
    assertFalse(manager.activeNotifications.any { it.tag == tag })
  }

  @Test
  fun duplicateReminderForOneMatchIsSuppressed() {
    grantNotificationPermission()
    val renderer = AndroidMatchAlertNotifications(context, notificationsAllowed = { true })
    renderer.handle(payload())
    awaitVisible(true)
    assertTrue(storage.contains(matchId))
    renderer.handle(payload(body = "Duplicate"))
    assertEquals("Match is starting soon", manager.activeNotifications.single { it.tag == tag }.notification.extras.getString(Notification.EXTRA_TEXT))
  }

  @Test
  fun blockedAlertsDoNotRecordDelivery() {
    AndroidMatchAlertNotifications(context, notificationsAllowed = { false }).handle(payload())
    assertFalse(storage.contains(matchId))
    assertFalse(manager.activeNotifications.any { it.tag == tag })
  }

  @Test
  fun postsOrdinaryAlertOnceAcrossRendererInstances() {
    grantNotificationPermission()
    val renderer = AndroidMatchAlertNotifications(context)
    renderer.handle(payload())
    awaitVisible(true)
    val notification = manager.activeNotifications.single { it.tag == tag }.notification
    assertEquals("Alpha vs Beta", notification.extras.getString(Notification.EXTRA_TITLE))
    assertEquals("Match is starting soon", notification.extras.getString(Notification.EXTRA_BIG_TEXT))
    assertNotNull(notification.contentIntent)
    assertEquals(0, notification.flags and Notification.FLAG_ONGOING_EVENT)
    assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
    if (Build.VERSION.SDK_INT >= 26) assertEquals(AndroidMatchAlertNotifications.ChannelId, notification.channelId)

    manager.cancel(tag, AndroidMatchAlertNotifications.NotificationId)
    awaitVisible(false)
    AndroidMatchAlertNotifications(context).handle(payload(body = "Starting now"))
    SystemClock.sleep(100)
    assertFalse(manager.activeNotifications.any { it.tag == tag })
  }

  private fun grantNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33 && !manager.areNotificationsEnabled()) {
      InstrumentationRegistry.getInstrumentation().uiAutomation
        .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  private fun awaitVisible(expected: Boolean) {
    val deadline = SystemClock.elapsedRealtime() + 2_000
    while (manager.activeNotifications.any { it.tag == tag } != expected && SystemClock.elapsedRealtime() < deadline) {
      SystemClock.sleep(20)
    }
    assertEquals(expected, manager.activeNotifications.any { it.tag == tag })
  }

  private fun payload(body: String = "Match is starting soon") = mapOf(
    "match_id" to matchId,
    "title" to "Alpha vs Beta",
    "body" to body,
  )
}
