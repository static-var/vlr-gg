/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.FirebaseApp
import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
  private val fixtureIds = setOf("991000001", "991000002", "991000003")

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun liveNotificationsAreAvailableWithPlayServicesAndFirebaseOnAndroid16() {
    assumeTrue(GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS)
    val options = runCatching { FirebaseApp.getInstance().options }.getOrNull()
    assumeTrue(options != null && options.applicationId.isNotBlank() && !options.gcmSenderId.isNullOrBlank())

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
        remove("match.$matchId.generation")
        remove("match.$matchId.observed_at")
        remove("match.$matchId.terminal")
        remove("match.$matchId.dismissed")
        remove("match.$matchId.restoring")
        remove("match.$matchId.recorded_at")
        remove("match.$matchId.recorded_elapsed_realtime")
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
    assertEquals("https://example.test/pr.png", valid?.teams?.get(1)?.imageUrl)
    assertEquals(listOf(8, 6), valid?.currentMap?.scores)
    assertNull(parser.parse(payload(type = "other")))
    assertNull(parser.parse(payload(state = "{not-json")))
    assertNull(parser.parse(payload(state = validState().replace("[8,6]", "[8,-1]"))))
    assertNull(parser.parse(payload(state = validState().replace("\"score\":1", "\"score\":\"invalid\""))))
    assertNull(parser.parse(payload(matchId = "١٢٣")))

    // The CDN manifest maps team IDs to logos; entries on hosts the logo cache refuses are ignored.
    val cdnLogo = "https://files.akhilnarang.dev/cdn/valorant/teams/624.png"
    val manifest = java.io.File(context.filesDir, "team_logos.json")
    manifest.writeText("""{"624":{"logo":{"url":"$cdnLogo"}},"1":{"logo":{"url":"https://evil.example/1.png"}}}""")
    try {
      val directory = TeamLogoDirectory(context, KoinPlatform.getKoin().get<Json>())
      assertTrue(runBlocking { directory.refresh() })
      assertEquals(cdnLogo, directory.logoUrl("624"))
      assertNull(directory.logoUrl("1"))
      assertNull(directory.logoUrl(null))
    } finally {
      manifest.delete()
    }
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
    assertEquals(LiveMatchMapProgress(3, 3), update.copy(terminal = true).mapProgress())
  }

  @Test
  fun winnerIdsResolveToTeamsAndUnknownOrUnfinishedMapsRemainNeutral() {
    val parser = LiveMatchUpdateParser(Json)
    val state = validState()
      .replace("\"name\":\"Team Liquid\"", "\"id\":474,\"name\":\"Team Liquid\"")
      .replace("\"name\":\"Paper Rex\"", "\"id\":\"624\",\"name\":\"Paper Rex\"")
      .replace("\"future_field\":true", "\"total_maps\":5,\"map_winners\":[\"474\",624,999,null,\"624\"]")
      .replace("\"name\":\"Ascent\"", "\"name\":\"Ascent\",\"number\":4")
    val match = requireNotNull(parser.parse(payload(state = state)))
    assertEquals(listOf("474", "624"), match.teams.map { it.id })
    assertEquals(listOf("474", "624", "999", null, "624"), match.mapWinners)
    assertEquals(listOf(0, 1, null, null, 1), match.mapProgress()?.winnerTeamIndices)
    val swapped = match.copy(teams = match.teams.reversed())
    assertEquals(listOf(1, 0, null, null, 0), swapped.mapProgress()?.winnerTeamIndices)
    assertTrue(match.copy(teams = listOf(match.teams[0], match.teams[0])).mapProgress()!!.winnerTeamIndices.all { it == null })
    assertEquals(
      listOf(0, 1, 1, null, null),
      match.copy(mapWinners = listOf("474", "624", "624"), currentMap = match.currentMap?.copy(number = 3))
        .mapProgress()?.winnerTeamIndices,
    )
    assertEquals(
      listOf(0, 1, null, null, 1),
      match.copy(terminal = true, currentMap = null).mapProgress()?.winnerTeamIndices,
    )
    assertEquals(
      List(5) { null },
      match.copy(terminal = true, mapWinners = emptyList()).mapProgress()?.winnerTeamIndices,
    )
    listOf("null", "{}", "true", "[{},false,null]").forEach { metadata ->
      val parsed = requireNotNull(parser.parse(payload(state = validState().replace("\"future_field\":true", "\"map_winners\":$metadata"))))
      assertEquals(listOf(8, 6), parsed.currentMap?.scores)
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun progressWinnerColorsMatchTeamTextAndHideWithSpoilers() {
    val match = update("991000001", 60).copy(
      totalMaps = 3,
      mapWinners = listOf("474", "624", null),
      currentMap = LiveMatchMap("Ascent", listOf(8, 6), number = 3),
    )
    val renderer = LiveMatchNotificationRenderer(context)
    val notification = renderer.build(match, false, 36)
    val style = Notification.Builder.recoverBuilder(context, notification).style as Notification.ProgressStyle

    val firstColor = style.progressSegments[0].color
    val secondColor = style.progressSegments[1].color
    val neutralColor = style.progressSegments[2].color
    assertTrue(firstColor != secondColor)
    assertTrue(neutralColor != firstColor && neutralColor != secondColor)
    assertEquals(listOf(firstColor, secondColor, neutralColor), style.progressSegments.map { it.color })
    assertEquals(listOf(neutralColor, neutralColor), style.progressPoints.map { it.color })
    assertFalse(style.isStyledByProgress)

    val logoCache = LiveMatchLogoCache(context)
    val testBmp1 = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
    val testBmp2 = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
    logoCache.putLogo("https://example.test/t1.png", testBmp1)
    logoCache.putLogo("https://example.test/t2.png", testBmp2)
    val matchWithLogos = match.copy(
      teams = listOf(
        match.teams[0].copy(imageUrl = "https://example.test/t1.png"),
        match.teams[1].copy(imageUrl = "https://example.test/t2.png"),
      ),
    )
    val withLogosRenderer = LiveMatchNotificationRenderer(context, logoCache)
    val notifWithLogos = withLogosRenderer.build(matchWithLogos, false, 36)
    val recoveredStyle = Notification.Builder.recoverBuilder(context, notifWithLogos).style as Notification.ProgressStyle
    assertNull(recoveredStyle.progressStartIcon)
    assertNull(recoveredStyle.progressEndIcon)
    assertNotNull(recoveredStyle.progressTrackerIcon)
    assertNotNull(notifWithLogos.getLargeIcon())

    val final = renderer.build(match.copy(terminal = true, currentMap = null), false, 36)
    val finalStyle = Notification.Builder.recoverBuilder(context, final).style as Notification.ProgressStyle
    assertEquals(listOf(firstColor, secondColor), finalStyle.progressSegments.take(2).map { it.color })
    assertEquals(300, finalStyle.progress)
    val hidden = Notification.Builder.recoverBuilder(context, renderer.build(match, true, 36)).style
    assertTrue(hidden is Notification.BigTextStyle)
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
      assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains(expected))
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
    val expectedRoundsProgress = (8 + 6) * 100 / 24
    assertEquals((2 - 1) * 100 + expectedRoundsProgress, progress.progress)
    assertEquals(300, progress.progressMax)
    assertEquals(listOf(100, 100, 100), progress.progressSegments.map { it.length })
    assertEquals(listOf(100, 200), progress.progressPoints.map { it.position })
    assertEquals("Ascent", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    assertEquals("Team Liquid\u20038 : 6\u2003Paper Rex", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
    assertTrue(title !is Spanned || title.getSpans(0, title.length, StyleSpan::class.java).isEmpty())
    assertEquals("Map 2 of 3 · Series 1 : 1", notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString())
    assertEquals("8–6", notification.extras.getCharSequence("android.shortCriticalText")?.toString())
    assertEquals(Icon.TYPE_RESOURCE, notification.smallIcon.type)

    // The chip shows the leader first, as its logo silhouette when that reads, else its tag if the text fits.
    val logos = LiveMatchLogoCache(context)
    logos.putLogo("https://example.test/mark.png", ringLogo(Color.RED))
    logos.putLogo("https://example.test/plate.png", Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) })
    fun chip(state: LiveMatchUpdate): Pair<String?, Int> =
      LiveMatchNotificationRenderer(context, logos).build(state, scoresHidden = false, sdkInt = 36).let {
        it.extras.getCharSequence("android.shortCriticalText")?.toString() to it.smallIcon.type
      }
    val trailing = live.copy(
      teams = listOf(
        live.teams[0].copy(tag = "TL", imageUrl = "https://example.test/plate.png"),
        live.teams[1].copy(tag = "PRX", imageUrl = "https://example.test/mark.png"),
      ),
      currentMap = live.currentMap?.copy(scores = listOf(6, 8)),
    )
    assertEquals("8–6" to Icon.TYPE_BITMAP, chip(trailing))
    // A logo that is a solid plate reads as a block, so the leader is named instead.
    assertEquals("TL 8–6" to Icon.TYPE_RESOURCE, chip(trailing.copy(currentMap = live.currentMap)))
    assertEquals("8–8" to Icon.TYPE_RESOURCE, chip(trailing.copy(currentMap = live.currentMap?.copy(scores = listOf(8, 8)))))
    val longTag = trailing.copy(teams = listOf(trailing.teams[0].copy(tag = "TEAMLIQ", imageUrl = null), trailing.teams[1]))
    assertEquals("12–10" to Icon.TYPE_RESOURCE, chip(longTag.copy(currentMap = live.currentMap?.copy(scores = listOf(12, 10)))))

    val firstMap = live.copy(currentMap = live.currentMap?.copy(number = 1))
    assertEquals(LiveMatchMapProgress(1, 3), firstMap.mapProgress())
    val lastMap = live.copy(currentMap = live.currentMap?.copy(number = 3))
    assertEquals(LiveMatchMapProgress(3, 3), lastMap.mapProgress())
    listOf(live.copy(totalMaps = null), live.copy(currentMap = null)).forEach { state ->
      assertTrue(Notification.Builder.recoverBuilder(context, renderer.build(state, false, 36)).style is Notification.BigTextStyle)
    }
    val final = Notification.Builder.recoverBuilder(context, renderer.build(live.copy(terminal = true), false, 36)).style
    assertTrue(final is Notification.ProgressStyle)
    assertEquals(300, (final as Notification.ProgressStyle).progress)
    assertTrue(Notification.Builder.recoverBuilder(context, renderer.build(live, true, 36)).style is Notification.BigTextStyle)
    if (Build.VERSION.SDK_INT >= 37) {
      assertTrue(Notification.Builder.recoverBuilder(context, renderer.build(live, false)).style is Notification.ProgressStyle)
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
  fun timeoutDeletionDoesNotSuppressLaterUpdates() {
    // Android sends the delete intent on timeout too; only a swipe within the timeout is a dismissal.
    var now = 2_000_000L
    var elapsed = 1_000_000L
    val store = LiveMatchNotificationStateStore(context, nowMillis = { now }, elapsedRealtimeMillis = { elapsed })
    store.record(update(matchId = "991000003", observedAt = 50), "original")
    elapsed += 60_000
    assertTrue(store.shouldRememberDeletion("991000003", "original"))
    now -= 3_600_000
    assertTrue(store.shouldRememberDeletion("991000003", "original"))
    elapsed += LiveNotificationTimeoutMillis
    assertFalse(LiveMatchNotificationStateStore(context, { now }, { elapsed }).shouldRememberDeletion("991000003", "original"))
    elapsed = 0 // A reboot resets elapsedRealtime, but cannot make this a swipe from this boot.
    assertFalse(store.shouldRememberDeletion("991000003", "original"))
  }

  @Test
  fun delayedTimeoutCannotDismissAReplacementNotification() {
    var elapsed = 1_000_000L
    val store = LiveMatchNotificationStateStore(context, elapsedRealtimeMillis = { elapsed })
    val live = update("991000001", 60)
    store.record(live, "old")
    elapsed += LiveNotificationTimeoutMillis
    store.record(live.copy(observedAt = 61), "new")

    val restored = LiveMatchNotificationStateStore(context, elapsedRealtimeMillis = { elapsed })
    assertFalse(restored.shouldRememberDeletion(live.matchId, "old"))
    assertTrue(restored.shouldAccept(live.copy(observedAt = 62)))
    assertTrue(restored.shouldRememberDeletion(live.matchId, "new"))
    restored.dismiss(live.matchId)
    assertFalse(restored.shouldAccept(live.copy(observedAt = 62)))
  }

  @Test
  fun finalDeletionHasNoLiveTimeoutAndOldLiveIntentCannotDeleteIt() {
    var elapsed = 1_000_000L
    val store = LiveMatchNotificationStateStore(context, elapsedRealtimeMillis = { elapsed })
    val live = update("991000002", 60)
    store.record(live, "live")
    store.record(live.copy(terminal = true), "final")
    elapsed += LiveNotificationTimeoutMillis + 1

    assertFalse(store.shouldRememberDeletion(live.matchId, "live"))
    assertTrue(store.shouldRememberDeletion(live.matchId, "final"))
  }

  @Test
  fun eachPostHasAnIndependentDeleteIntentButUnpinStillTargetsTheMatch() {
    val renderer = LiveMatchNotificationRenderer(context)
    val live = update("991000003", 60)
    val original = renderer.build(live, false, generation = "old")
    val replacement = renderer.build(live.copy(observedAt = 61), false, generation = "new")

    assertNotEquals(original.deleteIntent, replacement.deleteIntent)
    assertEquals(original.actions.last().actionIntent, replacement.actions.last().actionIntent)
    original.deleteIntent.cancel()
    replacement.deleteIntent.cancel()
  }

  @Test
  fun staleLifecycleStateIsPrunedAfterTerminalDeliveryCanNoLongerArrive() {
    var now = 1_000_000L
    val terminal = update(matchId = "991000001", observedAt = 50).copy(terminal = true)
    LiveMatchNotificationStateStore(context, nowMillis = { now }).record(terminal)
    now += LiveMatchNotificationStateStore.RetentionMillis + 1

    assertTrue(LiveMatchNotificationStateStore(context, nowMillis = { now }).shouldAccept(terminal.copy(observedAt = 51, terminal = false)))
  }

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun rendererUsesNativeLiveAndFinalStylesWithoutLeakingLastMapIntoFinal() {
    val renderer = LiveMatchNotificationRenderer(context)
    val live = renderer.build(update("991000001", 60), scoresHidden = false, sdkInt = 36)

    assertTrue(live.flags and Notification.FLAG_ONGOING_EVENT != 0)
    assertEquals("Team Liquid\u20038 : 6\u2003Paper Rex", live.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    assertEquals("Ascent", live.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
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
    assertEquals("Team Liquid vs Paper Rex", hidden.extras.getCharSequence(Notification.EXTRA_TITLE).toString())

    val final = renderer.build(update = update("991000001", 61).copy(terminal = true), scoresHidden = false)
    assertFalse(final.flags and Notification.FLAG_ONGOING_EVENT != 0)
    assertTrue(final.flags and Notification.FLAG_AUTO_CANCEL != 0)
    assertFalse(final.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("8–6"))
    assertEquals(context.getString(R.string.live_match_notification_final), final.extras.getCharSequence(Notification.EXTRA_TITLE))
    assertEquals("Team Liquid\u20031 : 1\u2003Paper Rex", final.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    assertFalse(final.extras.toString().contains("Ascent"))
    val hiddenFinal = renderer.build(update("991000001", 61).copy(terminal = true), scoresHidden = true)
    assertEquals(context.getString(R.string.live_match_notification_final), hiddenFinal.extras.getCharSequence(Notification.EXTRA_TITLE))
    assertEquals("Team Liquid vs Paper Rex", hiddenFinal.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    assertFalse(hiddenFinal.extras.toString().contains("Ascent"))
    assertEquals(
      listOf(context.getString(R.string.live_match_notification_open_match)),
      final.actions.map { it.title.toString() },
    )

    if (Build.VERSION.SDK_INT >= 37) {
      val progress = renderer.build(update("991000001", 62), scoresHidden = false)
      val style = Notification.Builder.recoverBuilder(context, progress).style as Notification.ProgressStyle
      assertEquals(1, style.progressSegments.size)
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun missingScoresAndMapKeepTheMatchIdentifiable() {
    val renderer = LiveMatchNotificationRenderer(context)
    val match = update("991000001", 60).copy(
      teams = update("991000001", 60).teams.map { it.copy(score = null) },
      currentMap = LiveMatchMap("Ascent", listOf(null, 6), number = 2),
      totalMaps = 3,
    )
    val notification = renderer.build(match, false, 36)
    assertEquals("Team Liquid\u2003— : 6\u2003Paper Rex", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    assertEquals("Map 2 of 3 · Series — : —", notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString())
    assertEquals("—–6", notification.extras.getCharSequence("android.shortCriticalText").toString())
    val final = renderer.build(match.copy(terminal = true), false, 36)
    assertEquals("Team Liquid\u2003— : —\u2003Paper Rex", final.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    val betweenMaps = renderer.build(match.copy(currentMap = null), false, 36)
    assertEquals("Team Liquid vs Paper Rex", betweenMaps.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
  }

  @Test
  @SdkSuppress(minSdkVersion = 36)
  fun trackerOnlyReachesMapBoundaryWhenTheMapHasFinished() {
    val renderer = LiveMatchNotificationRenderer(context)
    fun position(scores: List<Int?>, winners: List<String?> = emptyList()): Int {
      val match = update("991000001", 60).copy(
        totalMaps = 3,
        currentMap = LiveMatchMap("Ascent", scores, number = 2),
        mapWinners = winners,
      )
      return (Notification.Builder.recoverBuilder(context, renderer.build(match, false, 36)).style as Notification.ProgressStyle).progress
    }
    assertEquals(200, position(listOf(13, 5)))
    assertEquals(199, position(listOf(12, 12)))
    assertEquals(199, position(listOf(15, 14)))
    assertEquals(200, position(listOf(16, 14)))
    assertEquals(200, position(listOf(null, null), listOf(null, "474")))
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
      assertEquals(before.extras.getCharSequence(Notification.EXTRA_TITLE).toString(), after.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
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

    val fixture = InstrumentationRegistry.getArguments().getString("fixture") ?: "mid_map"
    val update = when (fixture) {
      "between_maps" -> LiveMatchUpdate(
        matchId = "110601034",
        observedAt = 100,
        terminal = false,
        teams = listOf(
          LiveMatchTeam("NS", "https://owcdn.net/img/6399bb707aacb.png", 0, tag = "NS", id = "11060"),
          LiveMatchTeam("NRG", "https://owcdn.net/img/6610f026c1a9e.png", 1, tag = "NRG", id = "1034"),
        ),
        currentMap = LiveMatchMap("Split", listOf(5, 13), number = 1),
        totalMaps = 3,
        mapWinners = listOf("1034", null, null),
      )
      "pre_first_round" -> LiveMatchUpdate(
        matchId = "110601034",
        observedAt = 100,
        terminal = false,
        teams = listOf(
          LiveMatchTeam("NS", "https://owcdn.net/img/6399bb707aacb.png", 0, tag = "NS", id = "11060"),
          LiveMatchTeam("NRG", "https://owcdn.net/img/6610f026c1a9e.png", 0, tag = "NRG", id = "1034"),
        ),
        currentMap = LiveMatchMap("Lotus", listOf(0, 0), number = 1),
        totalMaps = 3,
        mapWinners = listOf(null, null, null),
      )
      "terminal" -> LiveMatchUpdate(
        matchId = "110601034",
        observedAt = 100,
        terminal = true,
        teams = listOf(
          LiveMatchTeam("NS", "https://owcdn.net/img/6399bb707aacb.png", 0, tag = "NS", id = "11060"),
          LiveMatchTeam("NRG", "https://owcdn.net/img/6610f026c1a9e.png", 2, tag = "NRG", id = "1034"),
        ),
        currentMap = LiveMatchMap("Split", listOf(8, 13), number = 2),
        totalMaps = 3,
        mapWinners = listOf("1034", "1034", null),
      )
      else -> LiveMatchUpdate(
        matchId = "110601034",
        observedAt = 100,
        terminal = false,
        teams = listOf(
          LiveMatchTeam("NS", "https://owcdn.net/img/6399bb707aacb.png", 0, tag = "NS", id = "11060"),
          LiveMatchTeam("NRG", "https://owcdn.net/img/6610f026c1a9e.png", 1, tag = "NRG", id = "1034"),
        ),
        currentMap = LiveMatchMap("Split", listOf(5, 10), number = 2),
        totalMaps = 3,
        mapWinners = listOf("1034", null, null),
      )
    }

    val logoCache = LiveMatchLogoCache(context)
    val t1Url = update.teams[0].imageUrl
    val t2Url = update.teams[1].imageUrl
    runBlocking { runCatching { logoCache.loadAndCacheLogos(t1Url, t2Url) } }
    val renderer = LiveMatchNotificationRenderer(context, logoCache)
    manager.notify(
      "live-match-${update.matchId}",
      1,
      renderer.build(update, scoresHidden = false),
    )
  }

  /** A red ring: a mark with inner detail, which survives as a chip silhouette. */
  private fun ringLogo(color: Int): Bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply {
    Canvas(this).drawCircle(24f, 24f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.color = color
      style = Paint.Style.STROKE
      strokeWidth = 6f
    })
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
      LiveMatchTeam("Team Liquid", null, 1, id = "474"),
      LiveMatchTeam("Paper Rex", null, 1, id = "624"),
    ),
    currentMap = LiveMatchMap("Ascent", listOf(8, 6)),
  )
}
