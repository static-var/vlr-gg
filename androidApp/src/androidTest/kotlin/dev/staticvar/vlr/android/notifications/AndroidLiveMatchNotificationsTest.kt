/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.FirebaseApp
import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.koin.mp.KoinPlatform

/** Checks live notification parsing, saved state, and Android rendering. */
class AndroidLiveMatchNotificationsTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val fixtureIds = setOf("991000001", "991000002")

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun liveNotificationsAreAvailableWithPlayServicesAndFirebaseOnAndroid16() {
    assumeTrue(GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS)
    val options = FirebaseApp.getInstance().options
    assumeTrue(options.applicationId.isNotBlank() && !options.gcmSenderId.isNullOrBlank())

    assertTrue(AndroidLiveNotificationAvailability.isAvailable(context))
  }

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
        remove("match.$matchId.restoring")
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
  fun parserAcceptsMapMetadataAndKeepsScoresWhenOptionalMetadataIsMalformed() {
    val parser = LiveMatchUpdateParser(KoinPlatform.getKoin().get<Json>())
    val state = validState().replace("\"future_field\":true", "\"total_maps\":3")
      .replace("\"name\":\"Ascent\"", "\"name\":\"Ascent\",\"number\":2")
      .replace("\"name\":\"Paper Rex\"", "\"name\":\"Paper Rex\",\"tag\":\"PRX\"")
    val update = requireNotNull(parser.parse(payload(state = state)))
    assertEquals(3, update.totalMaps)
    assertEquals(2, update.currentMap?.number)
    assertEquals("PRX", update.teams[1].tag)
    assertEquals(LiveMatchMapProgress(2, 3), update.mapProgress())

    listOf("null", "0", "-1", "10", "2147483647", "\"3\"", "{}", "[]").forEach { invalid ->
      val parsed = requireNotNull(parser.parse(payload(state = state.replace("\"total_maps\":3", "\"total_maps\":$invalid"))))
      assertNull(parsed.mapProgress())
      assertEquals(listOf(8, 6), parsed.currentMap?.scores)
    }
    listOf("null", "0", "-1", "4", "\"2\"", "{}", "[]").forEach { invalid ->
      val parsed = requireNotNull(parser.parse(payload(state = state.replace("\"number\":2", "\"number\":$invalid"))))
      assertNull(parsed.mapProgress())
    }
    assertNull(requireNotNull(parser.parse(payload())).mapProgress())
    assertNull(update.copy(terminal = true).mapProgress())
  }

  @Test
  fun teamTagsUseNamesUntilAvailable() {
    val parser = LiveMatchUpdateParser(KoinPlatform.getKoin().get<Json>())
    for (tag in listOf("null", "\"\"", "\"  \"", "\"PRX\"")) {
      val state = validState().replace("\"name\":\"Paper Rex\"", "\"name\":\"Paper Rex\",\"tag\":$tag")
      val match = requireNotNull(parser.parse(payload(state = state)))
      val expected = if (tag == "\"PRX\"") "PRX" else "Paper Rex"
      assertEquals(expected, match.teams[1].displayName)
      val notification = LiveMatchNotificationRenderer(context).build(match.copy(terminal = true), false)
      assertEquals("Team Liquid : 1\n$expected : 1", notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun progressShowsOneStopPerMapAndFallsBackWithoutMetadata() {
    val renderer = LiveMatchNotificationRenderer(context)
    val live = update("991000001", 60).copy(
      totalMaps = 3,
      currentMap = LiveMatchMap("Ascent", listOf(8, 6), number = 2),
    )
    val notification = renderer.build(live, scoresHidden = false, sdkInt = 36)
    val progress = Notification.Builder.recoverBuilder(context, notification).style as Notification.ProgressStyle
    assertEquals(2, progress.progress)
    assertEquals(3, progress.progressMax)
    assertEquals(listOf(1, 1, 1), progress.progressSegments.map { it.length })
    assertEquals(listOf(1, 2, 3), progress.progressPoints.map { it.position })
    assertEquals("Team Liquid : 1\nPaper Rex : 1", notification.extras.getCharSequence(Notification.EXTRA_TEXT))

    val firstMap = live.copy(currentMap = live.currentMap?.copy(number = 1))
    assertEquals(LiveMatchMapProgress(1, 3), firstMap.mapProgress())
    val lastMap = live.copy(currentMap = live.currentMap?.copy(number = 3))
    assertEquals(LiveMatchMapProgress(3, 3), lastMap.mapProgress())
    listOf(live.copy(totalMaps = null), live.copy(currentMap = null), live.copy(terminal = true)).forEach { state ->
      assertTrue(Notification.Builder.recoverBuilder(context, renderer.build(state, false, 36)).style is Notification.BigTextStyle)
    }
    assertTrue(Notification.Builder.recoverBuilder(context, renderer.build(live, true, 36)).style is Notification.BigTextStyle)
    if (Build.VERSION.SDK_INT >= 37) {
      assertTrue(Notification.Builder.recoverBuilder(context, renderer.build(live, false)).style is Notification.MetricStyle)
    }
  }

  @Test
  fun observedTimeIsMonotonicAndTerminalCannotRevive() {
    val store = LiveMatchNotificationStateStore(context)
    val live = update(matchId = "991000001", observedAt = 40)
    val terminal = live.copy(observedAt = 41, terminal = true)

    assertTrue(store.shouldAccept(live))
    store.record(live)
    assertFalse(store.shouldAccept(live))
    assertEquals("duplicate_or_out_of_order", store.rejectionReason(live))
    assertFalse(store.shouldAccept(live.copy(observedAt = 39)))
    assertTrue(store.shouldAccept(terminal.copy(observedAt = 40)))
    store.record(terminal.copy(observedAt = 40))
    assertFalse(store.shouldAccept(live.copy(observedAt = 42)))
    assertEquals("match_terminal", store.rejectionReason(live.copy(observedAt = 42)))
  }

  @Test
  fun dismissalSurvivesAStoreRecreation() {
    val update = update(matchId = "991000002", observedAt = 50)
    LiveMatchNotificationStateStore(context).dismiss(update.matchId)

    assertFalse(LiveMatchNotificationStateStore(context).shouldAccept(update))
    assertEquals("match_dismissed", LiveMatchNotificationStateStore(context).rejectionReason(update))
  }

  @Test
  fun explicitRestoreAllowsSameSnapshotOnceWithoutAcceptingOldOrFinishedState() {
    val store = LiveMatchNotificationStateStore(context)
    val live = update(matchId = "991000001", observedAt = 50)
    store.record(live)
    store.dismiss(live.matchId)
    assertEquals(setOf(live.matchId), store.dismissedMatchIds().intersect(fixtureIds))
    assertTrue(store.restore(live.matchId))
    assertFalse(store.restore(live.matchId))
    assertFalse(store.shouldAccept(live.copy(observedAt = 49)))
    assertTrue(store.shouldAccept(live))
    store.record(live)
    store.rejectRestore(live.matchId)
    assertFalse(live.matchId in store.dismissedMatchIds())
    assertFalse(store.shouldAccept(live))
    val final = live.copy(observedAt = 51, terminal = true)
    store.record(final)
    store.dismiss(live.matchId)
    assertFalse(store.restore(live.matchId))
    assertFalse(store.shouldAccept(live.copy(observedAt = 52)))
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
  fun dismissedNotificationCanBeRestoredFromTheSamePayload() {
    assertTrue(AndroidLiveNotificationAvailability.isAvailable(context))
    InstrumentationRegistry.getInstrumentation().uiAutomation
      .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    val preferences = LiveMatchNotificationPreferencesRepository(MapSettings()).apply { setEnabled(true) }
    val notifications = AndroidLiveMatchNotifications(
      context,
      KoinPlatform.getKoin().get<Json>(),
      preferences,
      SpoilerPreferencesRepository(MapSettings()),
    )
    val manager = context.getSystemService(NotificationManager::class.java)
    val matchId = "991000001"
    val tag = "live-match-$matchId"
    fun awaitVisible(visible: Boolean) {
      val deadline = SystemClock.elapsedRealtime() + 3_000
      while (manager.activeNotifications.any { it.tag == tag } != visible && SystemClock.elapsedRealtime() < deadline) {
        SystemClock.sleep(20)
      }
      assertEquals(visible, manager.activeNotifications.any { it.tag == tag })
    }
    try {
      val data = payload(matchId = matchId, observedAt = 70)
      notifications.handle(data)
      awaitVisible(true)
      notifications.dismiss(matchId)
      awaitVisible(false)
      assertTrue(matchId in notifications.dismissedMatchIds.value)
      assertTrue(notifications.restoreDismissedMatch(matchId))
      notifications.handle(data)
      awaitVisible(true)
      assertFalse(matchId in notifications.dismissedMatchIds.value)
      val before = manager.activeNotifications.single { it.tag == tag }.notification
      notifications.handle(payload(state = validState(matchId, 69).replace("[8,6]", "[0,0]")))
      val after = manager.activeNotifications.single { it.tag == tag }.notification
      assertEquals(before.extras.getCharSequence(Notification.EXTRA_TITLE), after.extras.getCharSequence(Notification.EXTRA_TITLE))
      assertFalse(LiveMatchNotificationStateStore(context).shouldAccept(update(matchId, 70)))
    } finally {
      manager.cancel(tag, 1)
      awaitVisible(false)
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
      LiveMatchNotificationRenderer(context).build(
        update("991000001", 70).copy(totalMaps = 3, currentMap = LiveMatchMap("Ascent", listOf(8, 6), number = 2)),
        scoresHidden = false,
      ),
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
