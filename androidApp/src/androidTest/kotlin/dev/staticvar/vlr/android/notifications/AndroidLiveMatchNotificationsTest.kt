/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.android.R
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.mp.KoinPlatform

class AndroidLiveMatchNotificationsTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val fixtureIds = setOf("991000001", "991000002")

  @Before
  fun clearPreviousFixtureState() {
    removeFixtureState()
  }

  @After
  fun removeFixtureState() {
    val storage = context.getSharedPreferences(LiveMatchNotificationStateStore.StorageName, 0)
    val tracked = storage.getStringSet("tracked_matches", emptySet()).orEmpty() - fixtureIds
    storage.edit().apply {
      fixtureIds.forEach { matchId ->
        remove("match.$matchId.observed_at")
        remove("match.$matchId.terminal")
        remove("match.$matchId.dismissed")
        remove("match.$matchId.recorded_at")
      }
      putStringSet("tracked_matches", tracked)
    }.commit()
  }

  @Test
  fun parserAcceptsTheFcmContractAndRejectsMalformedState() {
    val parser = LiveMatchUpdateParser(KoinPlatform.getKoin().get<Json>())
    val valid = parser.parse(payload(matchId = "991000001", observedAt = 40))

    assertNotNull(valid)
    assertEquals("Paper Rex", valid?.teams?.get(1)?.name)
    assertEquals(listOf(8, 6), valid?.currentMap?.scores)
    assertNull(parser.parse(payload(type = "other")))
    assertNull(parser.parse(payload(state = "{not-json")))
    assertNull(parser.parse(payload(state = validState().replace("[8,6]", "[8,-1]"))))
    assertNull(parser.parse(payload(state = validState().replace("\"score\":1", "\"score\":\"invalid\""))))
    assertNull(parser.parse(payload(matchId = "١٢٣")))
  }

  @Test
  fun observedTimeIsMonotonicAndTerminalCannotRevive() {
    val store = LiveMatchNotificationStateStore(context)
    val live = update(matchId = "991000001", observedAt = 40)
    val terminal = live.copy(observedAt = 41, terminal = true)

    assertTrue(store.shouldAccept(live))
    store.record(live)
    assertFalse(store.shouldAccept(live))
    assertFalse(store.shouldAccept(live.copy(observedAt = 39)))
    assertTrue(store.shouldAccept(terminal.copy(observedAt = 40)))
    store.record(terminal.copy(observedAt = 40))
    assertFalse(store.shouldAccept(live.copy(observedAt = 42)))
  }

  @Test
  fun dismissalSurvivesAStoreRecreation() {
    val update = update(matchId = "991000002", observedAt = 50)
    LiveMatchNotificationStateStore(context).dismiss(update.matchId)

    assertFalse(LiveMatchNotificationStateStore(context).shouldAccept(update))
  }

  @Test
  fun staleLifecycleStateIsPrunedAfterTerminalDeliveryCanNoLongerArrive() {
    var now = 1_000_000L
    val terminal = update(matchId = "991000001", observedAt = 50).copy(terminal = true)
    LiveMatchNotificationStateStore(context) { now }.record(terminal)
    now += LiveMatchNotificationStateStore.RetentionMillis + 1

    assertTrue(LiveMatchNotificationStateStore(context) { now }.shouldAccept(terminal.copy(observedAt = 51, terminal = false)))
  }

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun rendererUsesNativeLiveAndFinalStylesWithoutLeakingLastMapIntoFinal() {
    val renderer = LiveMatchNotificationRenderer(context)
    val live = renderer.build(update("991000001", 60), scoresHidden = false, sdkInt = 36)

    assertTrue(live.flags and Notification.FLAG_ONGOING_EVENT != 0)
    assertEquals("Ascent · 8–6", live.extras.getCharSequence(Notification.EXTRA_TITLE))
    assertEquals("Team Liquid : 1\nPaper Rex : 1", live.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
    assertEquals(
      listOf(
        context.getString(R.string.live_match_notification_open_match),
        context.getString(R.string.live_match_notification_unpin),
      ),
      live.actions.map { it.title.toString() },
    )
    assertTrue(live.extras.getBoolean("android.requestPromotedOngoing"))

    val hidden = renderer.build(update("991000001", 60), scoresHidden = true, sdkInt = 36)
    assertEquals(context.getString(R.string.widget_scores_hidden), hidden.extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
    assertEquals("Team Liquid : —\nPaper Rex : —", hidden.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))

    val final = renderer.build(update = update("991000001", 61).copy(terminal = true), scoresHidden = false)
    assertFalse(final.flags and Notification.FLAG_ONGOING_EVENT != 0)
    assertTrue(final.flags and Notification.FLAG_AUTO_CANCEL != 0)
    assertFalse(final.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("8–6"))
    assertEquals("Team Liquid : 1\nPaper Rex : 1", final.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
    assertEquals(
      listOf(context.getString(R.string.live_match_notification_open_match)),
      final.actions.map { it.title.toString() },
    )

    if (Build.VERSION.SDK_INT >= 37) {
      val metric = renderer.build(update("991000001", 62), scoresHidden = false)
      val style = Notification.Builder.recoverBuilder(context, metric).style as Notification.MetricStyle
      assertEquals(listOf("Team Liquid", "Paper Rex"), style.metrics.map { it.label.toString() })
      assertEquals(2, style.metrics.size)
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun postNativeLiveFixtureWhenRequested() {
    if (InstrumentationRegistry.getArguments().getString("leave_notification") != "true") return
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      NotificationChannel(
        "live_matches",
        context.getString(R.string.live_match_notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
      ),
    )
    manager.notify(
      "live-match-991000001",
      1,
      LiveMatchNotificationRenderer(context).build(update("991000001", 70), scoresHidden = false),
    )
  }

  private fun payload(
    type: String = "match-live-v1",
    matchId: String = "991000001",
    observedAt: Long = 40,
    state: String = validState(matchId, observedAt),
  ): Map<String, String> = mapOf("type" to type, "state" to state)

  private fun validState(matchId: String = "991000001", observedAt: Long = 40): String =
    """
    {
      "match_id":"$matchId",
      "observed_at":$observedAt,
      "terminal":false,
      "teams":[
        {"name":"Team Liquid","img":null,"score":1},
        {"name":"Paper Rex","img":"https://example.test/pr.png","score":1}
      ],
      "current_map":{"name":"Ascent","scores":[8,6]},
      "future_field":true
    }
    """.trimIndent()

  private fun update(matchId: String, observedAt: Long): LiveMatchUpdate = LiveMatchUpdate(
    matchId = matchId,
    observedAt = observedAt,
    terminal = false,
    teams = listOf(
      LiveMatchTeam("Team Liquid", null, 1),
      LiveMatchTeam("Paper Rex", null, 1),
    ),
    currentMap = LiveMatchMap("Ascent", listOf(8, 6)),
  )
}
