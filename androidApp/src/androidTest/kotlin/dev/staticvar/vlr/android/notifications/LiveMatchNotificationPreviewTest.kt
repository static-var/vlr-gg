/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.android.R
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Posts an opt-in native notification using the shipping renderer and logo loader for visual review. */
class LiveMatchNotificationPreviewTest {
  /** Pass notification_preview=true and preview_state=live, final, or hidden to leave a screenshot fixture. */
  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun showNotificationWhenRequested() = runBlocking {
    val arguments = InstrumentationRegistry.getArguments()
    if (arguments.getString("notification_preview") != "true") return@runBlocking
    val state = arguments.getString("preview_state") ?: "live"
    require(state in setOf("live", "final", "hidden"))
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    val finished = state == "final"
    val hidden = state == "hidden"
    val update = LiveMatchUpdate(
      matchId = "991000009",
      observedAt = System.currentTimeMillis() / 1_000,
      terminal = finished,
      teams = listOf(
        LiveMatchTeam("Team Liquid", "https://owcdn.net/img/640c381f0603f.png", if (finished) 2 else 1, id = "474"),
        LiveMatchTeam("Paper Rex", "https://owcdn.net/img/62bbeba74d5cb.png", 1, id = "624"),
      ),
      currentMap = LiveMatchMap("Ascent", if (finished) listOf(13, 9) else listOf(8, 6), number = 3),
      totalMaps = 3,
      mapWinners = listOf("474", "624", if (finished) "474" else null),
    )
    val logos = if (Build.VERSION.SDK_INT == 36 && !finished && !hidden) {
      TeamLogoLoader(context).load(update).also {
        assertNotNull("Team Liquid logo must load for the visual fixture", it?.first)
        assertNotNull("Paper Rex logo must load for the visual fixture", it?.second)
      }
    } else null
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      NotificationChannel("live_matches", context.getString(R.string.live_match_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
    )
    manager.notify("live-match-991000009", 1, LiveMatchNotificationRenderer(context).build(update, hidden, logos = logos))
    val deadline = SystemClock.elapsedRealtime() + 3_000
    while (manager.activeNotifications.none { it.tag == "live-match-991000009" } && SystemClock.elapsedRealtime() < deadline) {
      SystemClock.sleep(20)
    }
    assertTrue(manager.activeNotifications.any { it.tag == "live-match-991000009" })
  }
}
