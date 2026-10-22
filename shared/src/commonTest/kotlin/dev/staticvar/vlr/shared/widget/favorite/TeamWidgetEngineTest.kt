/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.WidgetFavoriteTeam
import dev.staticvar.vlr.shared.widget.WidgetTheme
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TeamWidgetEngineTest {
  private val now = Instant.fromEpochSeconds(1_792_152_000)
  private val key = assertNotNull(WidgetKey.parse(WidgetKind.Team, "2", "saved-client"))
  private val locale = EnglishWidgetLocale()
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun successfulRefreshLoadsBaseRequestsInParallelThenOnlyTheSelectedDetail() = runTest {
    for (phase in listOf("upcoming", "live")) {
      val rankingStarted = CompletableDeferred<Unit>()
      val previewsStarted = CompletableDeferred<Unit>()
      val paths = mutableListOf<String>()
      val headers = mutableListOf<String>()
      val lock = Mutex()
      val client = widgetTestClient { path, authorization, appName ->
        lock.withLock {
          paths += path
          headers += "$authorization|$appName"
        }
        when (path) {
          rankingPath -> {
            rankingStarted.complete(Unit)
            previewsStarted.await()
            WidgetTestResponse(ranking)
          }

          previewsPath -> {
            previewsStarted.complete(Unit)
            rankingStarted.await()
            WidgetTestResponse(previews(phase) + "," + preview("later", "upcoming") + "]")
          }

          detailPath -> {
            assertEquals(true, rankingStarted.isCompleted)
            assertEquals(true, previewsStarted.isCompleted)
            WidgetTestResponse(details)
          }

          else -> WidgetTestResponse("", HttpStatusCode.NotFound)
        }
      }
      try {
        val engine = engine(client)
        val result =
          withContext(Dispatchers.Default) { withTimeout(5_000) { engine.refresh(request(), null) } }
        val stored = assertIs<WidgetRefreshResult.Store>(result)
        assertEquals(key, stored.key)
        val record = assertNotNull(WidgetCacheCodec.decode(stored.record, key))
        assertEquals(1, record.version)
        assertEquals(1L, record.revision)
        assertEquals(now, assertIs<CachedValue.Team>(record.value).fetchedAt)
        assertEquals(now, record.attemptedAt)
        assertEquals(RecordOrigin.Fresh, record.origin)
        val content = assertIs<CachedValue.Team>(record.value).value
        assertEquals("Sentinels", content.header.team.name)
        assertEquals(4, content.header.rank)
        if (phase == "live") {
          val live = assertIs<TeamState.Live>(content.state)
          assertEquals("1–0", live.score?.text)
          assertEquals("7–5", live.maps.single().score?.text)
          assertEquals(now + 900.seconds, record.fetchAfter)
          val frame = assertIs<FavoriteWidgetFrame.Team>(engine.project(request(), stored.record, locale).frame)
          for (presentation in listOf(frame.medium, frame.large)) {
            val hero = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(presentation.body).hero)
            assertEquals("Playing FNATIC", hero.lead)
            assertEquals("1–0", hero.value)
            assertEquals("Live", hero.liveLabel)
          }
          val mediumHero = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
          assertEquals(
            listOf(PillText("Ascent 7–5", PillTone.Live)),
            assertIs<TeamLiveBelow.Pills>(mediumHero.below).items,
          )
          val largeHero = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.large.body).hero)
          assertEquals(TeamLiveBelow.Context("Masters · Upper final · Bo3"), largeHero.below)
          val hiddenRequest = request().copy(source = source().copy(spoilersHidden = true))
          val hidden = assertIs<FavoriteWidgetFrame.Team>(engine.project(hiddenRequest, stored.record, locale).frame)
          for (presentation in listOf(hidden.medium, hidden.large)) {
            val hero = assertIs<NeutralTeamHeroText.Live>(assertIs<TeamDisplayBody.Hidden>(presentation.body).hero)
            assertEquals("Playing FNATIC", hero.lead)
            assertEquals("Live now", hero.value)
            assertEquals("Scores hidden · Bo3", hero.context)
            assertEquals("Live", hero.liveLabel)
          }
        } else {
          val fixture = assertIs<TeamState.Upcoming>(content.state).fixture
          assertEquals("100", fixture.id)
          assertEquals("Upper final", fixture.stage)
          assertEquals("Bo3", fixture.format?.label)
          assertEquals(WidgetTeam("1", "FNATIC", "FNC", "https://example.test/fnatic.png"), fixture.opponent)
          assertEquals(now + 7200.seconds, record.fetchAfter)
        }
        assertEquals(setOf(rankingPath, previewsPath, detailPath), paths.toSet())
        assertEquals(3, paths.size)
        assertEquals(detailPath, paths.last())
        assertEquals(setOf("test-token|dev.staticvar.vlr"), headers.toSet())
        assertEquals(WidgetTap.Match("100"), engine.project(request(), stored.record, locale).frame.heroTap())
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun baseFailureKeepsTheOriginalValueAndFetchTimeAndIncrementsRevision() = runTest {
    val original = record(content(TeamState.Upcoming(fixture())), revision = 7)
    val completion = now + 60.seconds
    for ((failedPath, response) in listOf(
      rankingPath to WidgetTestResponse("", HttpStatusCode.NotFound),
      rankingPath to WidgetTestResponse("", HttpStatusCode.ServiceUnavailable),
      previewsPath to WidgetTestResponse("", HttpStatusCode.ServiceUnavailable),
      previewsPath to WidgetTestResponse("{}"),
    )) {
      val client = widgetTestClient { path, _, _ ->
        if (path == failedPath) response else WidgetTestResponse(if (path == rankingPath) ranking else "[]")
      }
      try {
        val engine = engine(client, completion)
        val result =
          assertIs<WidgetRefreshResult.Store>(
            engine.refresh(request(completion), WidgetCacheCodec.encode(original)),
          )
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, key))
        assertEquals(original.value, saved.value)
        assertEquals(now, assertIs<CachedValue.Team>(saved.value).fetchedAt)
        assertEquals(completion, saved.attemptedAt)
        assertEquals(8L, saved.revision)
        assertEquals(RecordOrigin.CachedFallback, saved.origin)
        assertEquals(now + 7260.seconds, saved.fetchAfter)
        assertIs<FavoriteWidgetFrame.Team>(engine.project(request(completion), result.record, locale).frame)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun failedRefreshAfterClockRollbackKeepsContentRevisionAndRetryDeadline() = runTest {
    val original = record(content(TeamState.Live(fixture(), WidgetScore(1, 0), emptyList())), revision = 7)
    val completion = now - 3600.seconds
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("", HttpStatusCode.ServiceUnavailable) }
    try {
      val engine = engine(client, completion)
      val result =
        assertIs<WidgetRefreshResult.Store>(
          engine.refresh(request(now + 900.seconds), WidgetCacheCodec.encode(original)),
        )
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, key))
      assertEquals(original.value, saved.value)
      assertEquals(now, assertIs<CachedValue.Team>(saved.value).fetchedAt)
      assertEquals(completion, saved.attemptedAt)
      assertEquals(8L, saved.revision)
      assertEquals(RecordOrigin.CachedFallback, saved.origin)
      assertEquals(now - 2700.seconds, saved.fetchAfter)
      val projection = engine.project(request(completion + 1.seconds), result.record, locale)
      val frame = assertIs<FavoriteWidgetFrame.Team>(projection.frame)
      val hero = assertIs<TeamHeroText.Live>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
      assertEquals("1–0", hero.value)
      assertEquals(8L, projection.recordRevision)
      assertEquals(now - 2700.seconds, projection.schedule.fetchAt)
    } finally {
      client.close()
    }
  }

  @Test
  fun persistedKeysValidateIdsAndKeepTheirReadableIdentity() {
    val encoded = """{"kind":"Team","entityId":"2","clientId":"saved-client"}"""
    assertEquals(encoded, json.encodeToString(key))
    assertEquals(key, json.decodeFromString<WidgetKey>(encoded))
    assertEquals("WidgetKey(kind=Team, entityId=2, clientId=saved-client)", key.toString())
    for (invalid in listOf(encoded.replace("\"2\"", "\" \""), encoded.replace("saved-client", ""))) {
      assertFailsWith<IllegalArgumentException> { json.decodeFromString<WidgetKey>(invalid) }
    }
  }

  @Test
  fun baseFailureWithoutOldDataStoresNoDataAndAFifteenMinuteRetry() = runTest {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("", HttpStatusCode.ServiceUnavailable) }
    try {
      val engine = engine(client)
      val unrelated = record(content(TeamState.Idle(null)), revision = 7).copy(
        key = assertNotNull(WidgetKey.parse(WidgetKind.Team, "2", "other-client")),
      )
      val selectedRequest = request()
      val expectedKey = key
      for (old in listOf(null, WidgetCacheCodec.encode(unrelated))) {
        val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(selectedRequest, old))
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, expectedKey))
        assertEquals(CachedValue.NoData, saved.value)
        assertEquals(now, saved.attemptedAt)
        assertEquals(now + 900.seconds, saved.fetchAfter)
        assertEquals(RecordOrigin.CachedFallback, saved.origin)
        assertEquals(1L, saved.revision)
        for (offset in listOf(0, 60, 901)) {
          val projection = engine.project(selectedRequest.copy(now = now + offset.seconds), result.record, locale)
          assertIs<FavoriteWidgetFrame.Placeholder>(projection.frame)
          assertEquals(now + 900.seconds, projection.schedule.fetchAt)
          assertNull(projection.schedule.renderAt)
          assertEquals(1L, projection.recordRevision)
        }
      }
    } finally {
      client.close()
    }
  }

  @Test
  fun refreshEncodesEveryEntityClientAndMatchPathSegment() = runTest {
    val encodedKey = assertNotNull(WidgetKey.parse(WidgetKind.Team, "2/part?#", "saved/client?#"))
    val paths = mutableListOf<String>()
    val lock = Mutex()
    val client = widgetTestClient { path, _, _ ->
      lock.withLock { paths += path }
      when (path) {
        "/api/v2/rankings/teams/2%2Fpart%3F%23" -> WidgetTestResponse(
          ranking.replace("\"id\":\"2\"", "\"id\":\"2/part?#\""),
        )

        "/api/v1/favorites/saved%2Fclient%3F%23/matches?include_results=false" -> WidgetTestResponse(
          "[" + preview("100/part?#", "upcoming").replace("\"id\":\"2\"", "\"id\":\"2/part?#\"") + "]",
        )

        "/api/v1/matches/100%2Fpart%3F%23" -> WidgetTestResponse("{}")

        else -> WidgetTestResponse("", HttpStatusCode.NotFound)
      }
    }
    try {
      val request = request().copy(
        selection = WidgetSelection.Chosen(WidgetKind.Team, encodedKey.entityId),
        source = source().copy(
          clientId = "saved/client?#",
          favoriteTeams = listOf(WidgetFavoriteTeam("2/part?#", "Sentinels")),
        ),
      )
      val engine = engine(client)
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request, null))
      assertEquals(encodedKey, result.key)
      assertEquals(
        setOf(
          "/api/v2/rankings/teams/2%2Fpart%3F%23",
          "/api/v1/favorites/saved%2Fclient%3F%23/matches?include_results=false",
          "/api/v1/matches/100%2Fpart%3F%23",
        ),
        paths.toSet(),
      )
      assertEquals(3, paths.size)
      assertEquals(WidgetTap.Match("100/part?#"), engine.project(request, result.record, locale).frame.heroTap())
    } finally {
      client.close()
    }
  }

  @Test
  fun optionalDetailFailureStillStoresFreshBaseData() = runTest {
    for (response in listOf(
      WidgetTestResponse("", HttpStatusCode.ServiceUnavailable),
      WidgetTestResponse("not json"),
    )) {
      val client = widgetTestClient { path, _, _ ->
        when (path) {
          rankingPath -> WidgetTestResponse(ranking)
          previewsPath -> WidgetTestResponse(previews("upcoming") + "]")
          else -> response
        }
      }
      try {
        val engine = engine(client)
        val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request(), null))
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, key))
        assertEquals(RecordOrigin.Fresh, saved.origin)
        assertEquals(now, assertIs<CachedValue.Team>(saved.value).fetchedAt)
        val fixture = assertIs<TeamState.Upcoming>(assertIs<CachedValue.Team>(saved.value).value.state).fixture
        assertEquals("100", fixture.id)
        assertEquals("Week 1–Quarterfinals", fixture.stage)
        assertNull(fixture.format)
        assertEquals(WidgetTeam("1", "FNATIC", null, null), fixture.opponent)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun cancellationDuringBaseOrOptionalDetailsDoesNotReachTheStoreCallback() = runTest {
    for (cancelledPath in listOf(rankingPath, previewsPath, detailPath)) {
      val started = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      val client = widgetTestClient { path, _, _ ->
        if (path == cancelledPath) {
          started.complete(Unit)
          release.await()
        }
        WidgetTestResponse(
          when (path) {
            rankingPath -> ranking
            previewsPath -> previews("live") + "]"
            else -> details
          },
        )
      }
      val storedRecords = mutableListOf<WidgetRefreshResult.Store>()
      var refreshCompleted = false
      val callingJob = async {
        val result = engine(client).refresh(
          request(),
          WidgetCacheCodec.encode(record(content(TeamState.Idle(null)))),
        )
        if (result is WidgetRefreshResult.Store) storedRecords += result
        refreshCompleted = true
      }
      try {
        withContext(Dispatchers.Default) { withTimeout(5_000) { started.await() } }
        assertFalse(release.isCompleted)
        callingJob.cancelAndJoin()
        assertTrue(callingJob.isCancelled)
        assertFailsWith<CancellationException> { callingJob.await() }
        assertFalse(refreshCompleted)
        assertEquals(emptyList(), storedRecords)
      } finally {
        release.complete(Unit)
        callingJob.cancelAndJoin()
        client.close()
      }
    }
  }

  @Test
  fun setupGatesRunInOrderAndRefuseHttp() = runTest {
    var requests = 0
    val client = widgetTestClient { _, _, _ ->
      requests++
      WidgetTestResponse(ranking)
    }
    try {
      val engine = engine(client)
      val cases = listOf(
        request().copy(source = null) to SetupReason.NoFavorites,
        request().copy(source = source().copy(favoriteTeams = emptyList())) to SetupReason.NoFavorites,
        request().copy(selection = WidgetSelection.None(WidgetKind.Team), source = null) to SetupReason.NoFavorites,
        request().copy(selection = WidgetSelection.None(WidgetKind.Team)) to SetupReason.Choose,
        request().copy(
          selection = WidgetSelection.Chosen(WidgetKind.Team, "1"),
        ) to
          SetupReason.Choose,
      )
      for ((request, reason) in cases) {
        val projection = engine.project(request, StoredWidgetRecord("corrupt"), locale)
        val frame = assertIs<FavoriteWidgetFrame.Setup>(projection.frame)
        assertEquals(reason, frame.reason)
        assertEquals(if (reason == SetupReason.NoFavorites) WidgetTap.App else WidgetTap.Chooser, frame.tap)
        assertEquals(
          if (reason == SetupReason.NoFavorites) "Open Val Esports" else "Tap to choose",
          frame.text.action,
        )
        assertEquals("To use this widget,", frame.text.lead)
        assertEquals(if (reason == SetupReason.NoFavorites) "No favorite teams" else "Choose a team", frame.text.title)
        assertNull(projection.key)
        assertEquals(0L, projection.recordRevision)
        assertEquals(WidgetSchedule(null, null), projection.schedule)
        assertEquals(WidgetRefreshResult.Refused, engine.refresh(request, null))
      }
      assertEquals(0, requests)
    } finally {
      client.close()
    }
  }

  @Test
  fun missingCorruptOrMismatchedRecordsProjectAPlaceholderDueNow() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse(ranking) }
    try {
      val engine = engine(client)
      val valid = record(content(TeamState.Idle(null)))
      val stored = WidgetCacheCodec.encode(valid)
      val wrongTeam = content(TeamState.Idle(null)).let {
        it.copy(header = it.header.copy(team = WidgetTeam("1", "FNATIC", null, null)))
      }
      val invalid = listOf(
        null,
        StoredWidgetRecord("not json"),
        StoredWidgetRecord("{}"),
        WidgetCacheCodec.encode(valid.copy(key = assertNotNull(WidgetKey.parse(WidgetKind.Team, "1", "saved-client")))),
        WidgetCacheCodec.encode(valid.copy(key = assertNotNull(WidgetKey.parse(WidgetKind.Team, "2", "other-client")))),
        WidgetCacheCodec.encode(valid.copy(version = 99)),
        WidgetCacheCodec.encode(valid.copy(value = CachedValue.Team(wrongTeam, now))),
        StoredWidgetRecord(
          JsonObject(
            json.parseToJsonElement(WidgetCacheCodec.encode(valid).json).jsonObject.let { record ->
              record + ("value" to JsonObject(record.getValue("value").jsonObject + ("fetchedAt" to JsonNull)))
            },
          ).toString(),
        ),
        WidgetCacheCodec.encode(valid.copy(revision = 0)),
        WidgetCacheCodec.encode(valid.copy(fetchAfter = now)),
        StoredWidgetRecord(
          JsonObject(json.parseToJsonElement(stored.json).jsonObject + ("value" to JsonNull)).toString(),
        ),
        StoredWidgetRecord(
          JsonObject(
            json.parseToJsonElement(stored.json).jsonObject + ("origin" to JsonPrimitive("Unknown")),
          ).toString(),
        ),
      )
      for (candidate in invalid) {
        val projection = engine.project(request(), candidate, locale)
        val frame = assertIs<FavoriteWidgetFrame.Placeholder>(projection.frame)
        assertEquals(key, frame.key)
        assertEquals(WidgetTap.Team("2"), frame.tap)
        assertEquals("Loading team…", frame.text.value)
        assertEquals(WidgetSchedule(null, now), projection.schedule)
        assertEquals(0L, projection.recordRevision)
      }
      assertIs<FavoriteWidgetFrame.Team>(engine.project(request(), stored, locale).frame)
    } finally {
      client.close()
    }
  }

  @Test
  fun sourceDecoderReadsCurrentFavoritesAndRejectsCorruptData() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse(ranking) }
    try {
      val engine = engine(client)
      assertEquals(source(), engine.decodeSource(json.encodeToString(source())))
      for (value in listOf("not json", "{}", "null", "[]")) assertNull(engine.decodeSource(value))
      val legacy = JsonObject(json.parseToJsonElement(json.encodeToString(source())).jsonObject - "favoriteTeams")
      assertEquals(emptyList(), assertNotNull(engine.decodeSource(legacy.toString())).favoriteTeams)
    } finally {
      client.close()
    }
  }

  @Test
  fun walkingRenderDeadlinesMatchesTheSwiftUpcomingTimeline() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse(ranking) }
    try {
      val engine = engine(client)
      val cases = listOf(
        3600L to listOf(1_792_152_000L, 1_792_155_600L),
        86400L to listOf(1_792_152_000L, 1_792_195_200L, 1_792_238_400L),
        259200L to listOf(1_792_152_000L, 1_792_281_600L, 1_792_368_000L, 1_792_411_200L),
        43200L to listOf(1_792_152_000L, 1_792_195_200L),
      )
      for ((offset, expected) in cases) {
        val start = now + offset.seconds
        val stored = WidgetCacheCodec.encode(record(content(TeamState.Upcoming(fixture(start)))))
        val dates = mutableListOf(now.epochSeconds)
        var projection = engine.project(request(), stored, locale)
        while (dates.size < expected.size) {
          val next = assertNotNull(projection.schedule.renderAt)
          dates += next.epochSeconds
          projection = engine.project(request(next), stored, locale)
        }
        assertEquals(expected, dates)
        val hero =
          assertIs<TeamHeroText.Upcoming>(
            assertIs<TeamDisplayBody.Visible>(assertIs<FavoriteWidgetFrame.Team>(projection.frame).medium.body).hero,
          )
        assertEquals("Any minute", hero.value)
        assertEquals(WidgetTap.Match("100"), projection.frame.heroTap())
      }
      val start = now + 259200.seconds
      val stored = WidgetCacheCodec.encode(record(content(TeamState.Upcoming(fixture(start)))))
      val expectedLeads = listOf(
        now to "Mon, Oct 19",
        Instant.fromEpochSeconds(1_792_281_600) to "Tomorrow",
        Instant.fromEpochSeconds(1_792_368_000) to "Today",
      )
      for ((date, day) in expectedLeads) {
        val frame = assertIs<FavoriteWidgetFrame.Team>(engine.project(request(date), stored, locale).frame)
        val hero = assertIs<TeamHeroText.Upcoming>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
        assertEquals("Plays FNATIC", hero.lead)
        assertEquals(day, hero.day)
        assertEquals("12:00\u202FPM", hero.value)
      }
    } finally {
      client.close()
    }
  }

  @Test
  fun offlineFreshRecordsExpireOnlyAfterTheLiveAndUpcomingFallbackLimits() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse(ranking) }
    try {
      val engine = engine(client)
      for ((state, limit) in listOf(
        TeamState.Live(fixture(now), WidgetScore(1, 0), emptyList()) to 10800L,
        TeamState.Upcoming(fixture(now)) to 21600L,
      )) {
        val stored = WidgetCacheCodec.encode(record(content(state), fetchAfter = now + 900.seconds))
        val initial = engine.project(request(), stored, locale)
        assertEquals(now + (limit + 1).seconds, initial.schedule.renderAt)
        val atLimit = engine.project(request(now + limit.seconds), stored, locale)
        val visible = assertIs<TeamDisplayBody.Visible>(assertIs<FavoriteWidgetFrame.Team>(atLimit.frame).medium.body)
        if (state is TeamState.Live) {
          assertIs<TeamHeroText.Live>(
            visible.hero,
          )
        } else {
          assertIs<TeamHeroText.Upcoming>(visible.hero)
        }
        assertEquals(now + (limit + 1).seconds, atLimit.schedule.renderAt)
        val expired = engine.project(request(assertNotNull(atLimit.schedule.renderAt)), stored, locale)
        val frame = assertIs<FavoriteWidgetFrame.Team>(expired.frame)
        val idle = assertIs<TeamHeroText.Idle>(assertIs<TeamDisplayBody.Visible>(frame.medium.body).hero)
        assertEquals("No matches", idle.value)
        assertEquals(WidgetTap.Team("2"), frame.tap)
        assertNull(expired.schedule.renderAt)
        assertEquals(now + 900.seconds, expired.schedule.fetchAt)
        assertEquals(1L, expired.recordRevision)
        assertEquals(RecordOrigin.Fresh, assertNotNull(WidgetCacheCodec.decode(stored, key)).origin)
      }
    } finally {
      client.close()
    }
  }

  @Test
  fun renderProjectionNeverMovesThePersistedFetchDeadline() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse(ranking) }
    try {
      val engine = engine(client)
      val start = now + 120.seconds
      val stored = WidgetCacheCodec.encode(record(content(TeamState.Upcoming(fixture(start))), fetchAfter = start))
      for (offset in listOf(0, 60, 120, 121)) {
        val projection = engine.project(request(now + offset.seconds), stored, locale)
        assertEquals(start, projection.schedule.fetchAt)
        assertEquals(1L, projection.recordRevision)
        val saved = assertNotNull(WidgetCacheCodec.decode(stored, key))
        assertEquals(now, assertIs<CachedValue.Team>(saved.value).fetchedAt)
      }
    } finally {
      client.close()
    }
  }

  @Test
  fun freshOverdueMatchesScheduleExpiryWhenTheyBecomeFallback() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse(ranking) }
    try {
      val engine = engine(client)
      for (start in listOf(now - 25200.seconds, now - 21540.seconds)) {
        val due = now + 300.seconds
        val stored = WidgetCacheCodec.encode(record(content(TeamState.Upcoming(fixture(start))), fetchAfter = due))
        for (offset in listOf(0, 61, 299)) {
          val projection = engine.project(request(now + offset.seconds), stored, locale)
          assertEquals(due, projection.schedule.renderAt)
          assertEquals(
            "Any minute",
            assertIs<TeamHeroText.Upcoming>(
              assertIs<TeamDisplayBody.Visible>(assertIs<FavoriteWidgetFrame.Team>(projection.frame).medium.body).hero,
            ).value,
          )
        }
        val expired = engine.project(request(due), stored, locale)
        assertEquals(
          "No matches",
          assertIs<TeamHeroText.Idle>(
            assertIs<TeamDisplayBody.Visible>(assertIs<FavoriteWidgetFrame.Team>(expired.frame).medium.body).hero,
          ).value,
        )
        assertEquals(WidgetTap.Team("2"), expired.frame.tap)
        assertNull(expired.schedule.renderAt)
      }
    } finally {
      client.close()
    }
  }

  @Test
  fun blankClientWithAValidFavoriteProjectsAPlaceholderAndRefusesHttp() = runTest {
    var requests = 0
    val client = widgetTestClient { _, _, _ ->
      requests++
      WidgetTestResponse("{}")
    }
    try {
      val engine = engine(client)
      val old = WidgetCacheCodec.encode(record(content(TeamState.Idle(null))))
      for (clientId in listOf("", " ", "\t")) {
        val request = request().let { it.copy(source = it.source?.copy(clientId = clientId)) }
        val projection = engine.project(request, old, locale)
        val frame = assertIs<FavoriteWidgetFrame.Placeholder>(projection.frame)
        assertEquals("Loading team…", frame.text.value)
        assertEquals(WidgetTap.Team("2"), frame.tap)
        assertNull(projection.key)
        assertEquals(0L, projection.recordRevision)
        assertEquals(WidgetSchedule(null, null), projection.schedule)
        assertEquals(WidgetRefreshResult.Refused, engine.refresh(request, old))
      }
      assertEquals(0, requests)
    } finally {
      client.close()
    }
  }

  @Test
  fun changedClientUsesTheCurrentSourceForProjectionAndRefreshKeys() = runTest {
    val paths = mutableListOf<String>()
    val lock = Mutex()
    val client = widgetTestClient { path, _, _ ->
      lock.withLock { paths += path }
      WidgetTestResponse(if (path == rankingPath) ranking else "[]")
    }
    try {
      val engine = engine(client)
      val request = request().let { it.copy(source = it.source?.copy(clientId = "new-client")) }
      val expectedKey = assertNotNull(WidgetKey.parse(WidgetKind.Team, "2", "new-client"))
      val old = WidgetCacheCodec.encode(record(content(TeamState.Idle(null))))
      val initial = engine.project(request, old, locale)
      assertIs<FavoriteWidgetFrame.Placeholder>(initial.frame)
      assertEquals(expectedKey, initial.key)
      assertEquals(0L, initial.recordRevision)
      assertEquals(WidgetSchedule(null, now), initial.schedule)
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request, old))
      assertEquals(expectedKey, result.key)
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, expectedKey))
      assertEquals(1L, saved.revision)
      assertEquals(RecordOrigin.Fresh, saved.origin)
      val projection = engine.project(request, result.record, locale)
      assertIs<FavoriteWidgetFrame.Team>(projection.frame)
      assertEquals(expectedKey, projection.key)
      assertEquals(1L, projection.recordRevision)
      assertEquals(setOf(rankingPath, "/api/v1/favorites/new-client/matches?include_results=false"), paths.toSet())
      assertEquals(2, paths.size)
    } finally {
      client.close()
    }
  }

  @Test
  fun refreshReadsTheClockOnceForSuccessAndFallback() = runTest {
    val completion = now + 60.seconds
    for (succeeds in listOf(true, false)) {
      var clockReads = 0
      val client = widgetTestClient { path, _, _ ->
        if (succeeds) {
          WidgetTestResponse(if (path == rankingPath) ranking else "[]")
        } else {
          WidgetTestResponse("", HttpStatusCode.ServiceUnavailable)
        }
      }
      try {
        val engine = FavoriteWidgetEngine(
          FavoriteWidgetRemote(client, json),
          object : Clock {
            override fun now(): Instant = completion + (clockReads++).seconds
          },
          json,
        )
        val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request(), null))
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, key))
        assertEquals(1, clockReads)
        assertEquals(completion, saved.attemptedAt)
        if (succeeds) {
          assertEquals(completion, assertIs<CachedValue.Team>(saved.value).fetchedAt)
          assertEquals(RecordOrigin.Fresh, saved.origin)
          assertEquals(completion + 7200.seconds, saved.fetchAfter)
        } else {
          assertEquals(CachedValue.NoData, saved.value)
          assertEquals(RecordOrigin.CachedFallback, saved.origin)
          assertEquals(completion + 900.seconds, saved.fetchAfter)
        }
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun teamProfileIdentityIsReplacedWithTheRequestedKeyIdentity() = runTest {
    val client = widgetTestClient { path, _, _ ->
      WidgetTestResponse(if (path == rankingPath) ranking.replace("\"id\":\"2\"", "\"id\":\"wrong-team\"") else "[]")
    }
    try {
      val engine = engine(client)
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request(), null))
      assertEquals(key, result.key)
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, key))
      assertEquals(RecordOrigin.Fresh, saved.origin)
      val data = assertIs<CachedValue.Team>(saved.value)
      assertEquals(now, data.fetchedAt)
      assertEquals("2", data.value.header.team.id)
      assertEquals("Sentinels", data.value.header.team.name)
      assertIs<FavoriteWidgetFrame.Team>(engine.project(request(), result.record, locale).frame)
    } finally {
      client.close()
    }
  }

  @Test
  fun unknownEarlierPreviewDoesNotHideTheLaterUpcomingMatch() = runTest {
    val paths = mutableListOf<String>()
    val lock = Mutex()
    val client = widgetTestClient { path, _, _ ->
      lock.withLock { paths += path }
      when (path) {
        rankingPath -> WidgetTestResponse(ranking)

        previewsPath -> WidgetTestResponse(
          "[" + preview("tbd-first", "tbd").dropLast(1) + ",\"time\":\"2026-10-16T12:30:00Z\"}," +
            preview("upcoming-later", "upcoming").dropLast(1) + ",\"time\":\"2026-10-16T13:00:00Z\"}]",
        )

        "/api/v1/matches/upcoming-later" -> WidgetTestResponse(details)

        else -> WidgetTestResponse("", HttpStatusCode.NotFound)
      }
    }
    try {
      val engine = engine(client)
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request(), null))
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, key))
      val fixture = assertIs<TeamState.Upcoming>(assertIs<CachedValue.Team>(saved.value).value.state).fixture
      assertEquals("upcoming-later", fixture.id)
      assertEquals(Instant.parse("2026-10-16T13:00:00Z"), fixture.start)
      assertEquals(setOf(rankingPath, previewsPath, "/api/v1/matches/upcoming-later"), paths.toSet())
      assertEquals(3, paths.size)
      assertEquals(WidgetTap.Match("upcoming-later"), engine.project(request(), result.record, locale).frame.heroTap())
    } finally {
      client.close()
    }
  }

  @Test
  fun projectionUsesTheRequestZoneForTheScheduledDayAndClock() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("{}") }
    try {
      val engine = engine(client)
      val start = Instant.parse("2026-10-16T18:30:00Z")
      val stored = WidgetCacheCodec.encode(record(content(TeamState.Upcoming(fixture(start)))))
      for ((zone, day, time) in listOf(
        Triple(TimeZone.UTC, "Today", "6:30\u202FPM"),
        Triple(TimeZone.of("Asia/Kolkata"), "Tomorrow", "12:00\u202FAM"),
      )) {
        val frame = assertIs<FavoriteWidgetFrame.Team>(
          engine.project(request().copy(timeZone = zone), stored, locale).frame,
        )
        for (presentation in listOf(frame.medium, frame.large)) {
          val hero = assertIs<TeamHeroText.Upcoming>(assertIs<TeamDisplayBody.Visible>(presentation.body).hero)
          assertEquals("Plays FNATIC", hero.lead)
          assertEquals(day, hero.day)
          assertEquals(time, hero.value)
        }
      }
    } finally {
      client.close()
    }
  }

  private fun source(): UpcomingMatchesSnapshot = UpcomingMatchesSnapshot(
    savedAtEpochMillis = 0,
    clientId = "saved-client",
    hasFavorites = true,
    matches = emptyList(),
    theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
    favoriteTeams = listOf(WidgetFavoriteTeam("2", "Sentinels")),
  )

  private fun request(at: Instant = now): WidgetRequest = WidgetRequest(
    WidgetSelection.Chosen(WidgetKind.Team, key.entityId),
    source(),
    at,
    TimeZone.UTC,
  )

  private fun fixture(start: Instant? = null): TeamFixture = TeamFixture(
    "100",
    WidgetTeam("1", "FNATIC", "FNC", null),
    start,
    "Masters",
    "Upper final",
    WidgetSeriesFormat.Bo3,
  )

  private fun content(state: TeamState): TeamContent = TeamContent(
    TeamHeader(WidgetTeam("2", "Sentinels", "SEN", null), 4, "Americas", 1),
    List(5) { FormMark.Empty } + if (state is TeamState.Live) listOf(FormMark.Live) else emptyList(),
    emptyList(),
    state,
  )

  private fun record(
    content: TeamContent,
    revision: Long = 1,
    fetchAfter: Instant = now + 7200.seconds,
  ): WidgetCacheRecord =
    WidgetCacheRecord(1, key, revision, now, fetchAfter, RecordOrigin.Fresh, CachedValue.Team(content, now))

  private fun engine(client: HttpClient, completion: Instant = now): FavoriteWidgetEngine = FavoriteWidgetEngine(
    FavoriteWidgetRemote(client, json),
    object : Clock {
      override fun now(): Instant = completion
    },
    json,
  )

  private fun previews(phase: String): String = "[" + preview("100", phase)

  private fun preview(id: String, phase: String): String = """
    {"id":"$id","event":"Masters","series":"Week 1–Quarterfinals","status":"$phase","team1":{"id":"2","name":"Sentinels","score":1},"team2":{"id":"1","name":"FNATIC","score":0}}
  """.trimIndent()

  private val rankingPath = "/api/v2/rankings/teams/2"
  private val previewsPath = "/api/v1/favorites/saved-client/matches?include_results=false"
  private val detailPath = "/api/v1/matches/100"
  private val ranking = """
    {"team":{"id":"2","name":"Sentinels","tag":"SEN"},"rank":4,"region":"Americas","region_rank":1,"recent":[]}
  """.trimIndent()
  private val details = """
    {"total_maps":3,"map_count":5,"teams":[{"id":"1","name":"FNATIC","tag":"FNC","img":"https://example.test/fnatic.png"},{"id":"2","name":"Sentinels"}],"veto":[{"team":"SEN","action":"pick","map":"Ascent"}],"data":[{"map":"Ascent","live":true,"teams":[{"name":"Sentinels","score":7},{"name":"FNATIC","score":5}]}],"current_map":{"name":"Ascent","number":1,"scores":[5,7]},"event":{"stage":"Upper final"}}
  """.trimIndent()
}
