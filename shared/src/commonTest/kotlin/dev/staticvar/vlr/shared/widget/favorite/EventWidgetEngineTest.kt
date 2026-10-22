/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.WidgetFavoriteEvent
import dev.staticvar.vlr.shared.widget.WidgetFavoriteTeam
import dev.staticvar.vlr.shared.widget.WidgetFavorites
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
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

class EventWidgetEngineTest {
  private val locale = EnglishWidgetLocale()
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun cancellationDuringBaseOrOptionalDetailsDoesNotReachTheStoreCallback() = runTest {
    for (cancelledPath in listOf("/api/v1/events/3122", "/api/v1/matches/777919")) {
      val started = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      val client = widgetTestClient { path, _, _ ->
        if (path == cancelledPath) {
          started.complete(Unit)
          release.await()
        }
        WidgetTestResponse(if (path == "/api/v1/events/3122") readWidgetFixture("event-3122.json") else "{}")
      }
      val storedRecords = mutableListOf<WidgetRefreshResult.Store>()
      var refreshCompleted = false
      val callingJob = async {
        val result = engine(client, eventNow).refresh(eventRequest(), WidgetCacheCodec.encode(eventRecord(eventData())))
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
  fun failedRefreshAfterClockRollbackKeepsContentRevisionAndRetryDeadline() = runTest {
    val original = eventRecord(eventData(), revision = 7)
    val completion = eventNow - 3600.seconds
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("", HttpStatusCode.ServiceUnavailable) }
    try {
      val engine = engine(client, completion)
      val result =
        assertIs<WidgetRefreshResult.Store>(
          engine.refresh(eventRequest(at = eventNow + 900.seconds), WidgetCacheCodec.encode(original)),
        )
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, eventKey()))
      assertEquals(original.value, saved.value)
      assertEquals(eventNow, assertIs<CachedValue.Event>(saved.value).fetchedAt)
      assertEquals(completion, saved.attemptedAt)
      assertEquals(8L, saved.revision)
      assertEquals(RecordOrigin.CachedFallback, saved.origin)
      assertEquals(Instant.parse("2026-10-20T13:00:00Z"), saved.fetchAfter)
      val projection = engine.project(eventRequest(at = completion + 1.seconds), result.record, locale)
      val frame = assertIs<FavoriteWidgetFrame.Event>(projection.frame)
      assertEquals("Project Blender 2026", frame.medium.header.title)
      assertIs<VisibleEventText.Scheduled>(assertIs<EventDisplayBody.Visible>(frame.medium.body).state)
      assertEquals(8L, projection.recordRevision)
      assertEquals(Instant.parse("2026-10-20T13:00:00Z"), projection.schedule.fetchAt)
    } finally {
      client.close()
    }
  }

  @Test
  fun eventBaseFailureWithoutOldDataStoresNoDataAndAFifteenMinuteRetry() = runTest {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("", HttpStatusCode.ServiceUnavailable) }
    try {
      val engine = engine(client, eventNow)
      val unrelated = eventRecord(eventData(), revision = 7).copy(
        key = assertNotNull(WidgetKey.parse(WidgetKind.Event, "3122", "other-client")),
      )
      val selectedRequest = eventRequest()
      val expectedKey = eventKey()
      for (old in listOf(null, WidgetCacheCodec.encode(unrelated))) {
        val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(selectedRequest, old))
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, expectedKey))
        assertEquals(CachedValue.NoData, saved.value)
        assertEquals(eventNow, saved.attemptedAt)
        assertEquals(eventNow + 900.seconds, saved.fetchAfter)
        assertEquals(RecordOrigin.CachedFallback, saved.origin)
        assertEquals(1L, saved.revision)
        for (offset in listOf(0, 60, 901)) {
          val projection = engine.project(selectedRequest.copy(now = eventNow + offset.seconds), result.record, locale)
          assertIs<FavoriteWidgetFrame.Placeholder>(projection.frame)
          assertEquals(eventNow + 900.seconds, projection.schedule.fetchAt)
          assertNull(projection.schedule.renderAt)
          assertEquals(1L, projection.recordRevision)
        }
      }
    } finally {
      client.close()
    }
  }

  @Test
  fun eventRefreshLoadsTheBaseThenOnlyTheSelectedDetail() = runTest {
    val paths = mutableListOf<String>()
    val headers = mutableListOf<String>()
    val payload = json.parseToJsonElement(readWidgetFixture("event-2766.json")).jsonObject
    val liveFinal = JsonObject(
      payload + (
        "matches" to JsonArray(
          payload.getValue("matches").jsonArray.map { element ->
            val match = element.jsonObject
            if (match["id"] == JsonPrimitive("754737")) {
              JsonObject(match + ("status" to JsonPrimitive("live")))
            } else {
              element
            }
          },
        )
        ),
    ).toString()
    val client = widgetTestClient { path, authorization, appName ->
      paths += path
      headers += "$authorization|$appName"
      when (path) {
        "/api/v1/events/2766" -> WidgetTestResponse(liveFinal)
        "/api/v1/matches/754737" -> WidgetTestResponse(readWidgetFixture("match-754737.json"))
        else -> WidgetTestResponse("", HttpStatusCode.NotFound)
      }
    }
    try {
      val request = eventRequest("2766")
      val engine = engine(client, eventNow)
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request, null))
      assertEquals(eventKey("2766"), result.key)
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, result.key))
      assertEquals(1, saved.version)
      assertEquals(1L, saved.revision)
      assertEquals(eventNow, assertIs<CachedValue.Event>(saved.value).fetchedAt)
      assertEquals(eventNow, saved.attemptedAt)
      assertEquals(RecordOrigin.Fresh, saved.origin)
      assertEquals(Instant.parse("2026-10-20T12:15:00Z"), saved.fetchAfter)
      val data = assertIs<CachedValue.Event>(saved.value).value
      assertEquals("Valorant Champions 2026", data.event.title)
      assertEquals(34, data.matches.size)
      assertEquals("754737", data.detail?.matchId)
      assertEquals(listOf("Summit", "Lotus", "Sunset", "Haven", "Abyss"), data.detail?.maps?.map { it.name })
      assertEquals(WidgetSeriesFormat.Bo5, data.matches.single { it.fixture.id == "754737" }.fixture.format)
      val frame = assertIs<FavoriteWidgetFrame.Event>(engine.project(request, result.record, locale).frame)
      assertEquals(WidgetTap.Event("2766"), frame.tap)
      for (presentation in listOf(frame.medium, frame.large)) {
        val hero = assertIs<VisibleEventText.Live>(assertIs<EventDisplayBody.Visible>(presentation.body).state).hero
        assertEquals("LOUD vs Team Vitality", hero.lead)
        assertEquals("Live", hero.liveLabel)
        assertNull(hero.otherLive)
        assertEquals("3–1", hero.score)
        assertEquals(StageText("Grand Final", "Bo5"), hero.beside)
      }
      val hidden = assertIs<FavoriteWidgetFrame.Event>(
        engine.project(request.copy(source = request.source?.copy(spoilersHidden = true)), result.record, locale).frame,
      )
      for (presentation in listOf(hidden.medium, hidden.large)) {
        val hero = assertIs<HiddenEventText.Live>(assertIs<EventDisplayBody.Hidden>(presentation.body).state).hero
        assertEquals("LOUD vs Team Vitality", hero.lead)
        assertEquals("Live now", hero.label)
        assertEquals("Scores hidden · Grand Final", hero.context)
        assertEquals("Live", hero.liveLabel)
      }
      assertEquals(listOf("/api/v1/events/2766", "/api/v1/matches/754737"), paths)
      assertEquals(listOf("test-token|dev.staticvar.vlr", "test-token|dev.staticvar.vlr"), headers)
    } finally {
      client.close()
    }
  }

  @Test
  fun eventOptionalDetailFailureKeepsFreshBaseData() = runTest {
    for (response in listOf(
      WidgetTestResponse("", HttpStatusCode.ServiceUnavailable),
      WidgetTestResponse("not json"),
    )) {
      val paths = mutableListOf<String>()
      val client = widgetTestClient { path, _, _ ->
        paths += path
        if (path == "/api/v1/events/3122") WidgetTestResponse(readWidgetFixture("event-3122.json")) else response
      }
      try {
        val engine = engine(client, eventNow)
        val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(eventRequest(), null))
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, eventKey()))
        assertEquals(RecordOrigin.Fresh, saved.origin)
        assertEquals(eventNow, assertIs<CachedValue.Event>(saved.value).fetchedAt)
        val data = assertIs<CachedValue.Event>(saved.value).value
        assertEquals(78, data.matches.size)
        assertNull(data.detail)
        val frame = assertIs<FavoriteWidgetFrame.Event>(engine.project(eventRequest(), result.record, locale).frame)
        val hero = assertIs<VisibleEventText.Scheduled>(
          assertIs<EventDisplayBody.Visible>(frame.medium.body).state,
        ).hero
        assertEquals("CGN Esports vs Joblife", hero.lead)
        assertEquals("Tomorrow", hero.day)
        assertEquals("4:00\u202FPM", hero.time)
        assertEquals(listOf("/api/v1/events/3122", "/api/v1/matches/777919"), paths)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun eventBaseFailureRetainsDataAndFetchedAt() = runTest {
    val original = eventRecord(eventData(), revision = 7)
    val completion = Instant.parse("2026-10-20T12:01:00Z")
    for (response in listOf(
      WidgetTestResponse("", HttpStatusCode.NotFound),
      WidgetTestResponse("", HttpStatusCode.ServiceUnavailable),
      WidgetTestResponse("not json"),
      WidgetTestResponse("{}"),
      WidgetTestResponse(readWidgetFixture("event-2766.json")),
    )) {
      val paths = mutableListOf<String>()
      val client = widgetTestClient { path, _, _ ->
        paths += path
        response
      }
      try {
        val engine = engine(client, completion)
        val result = assertIs<WidgetRefreshResult.Store>(
          engine.refresh(eventRequest(at = completion), WidgetCacheCodec.encode(original)),
        )
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, eventKey()))
        assertEquals(original.value, saved.value)
        assertEquals(eventNow, assertIs<CachedValue.Event>(saved.value).fetchedAt)
        assertEquals(completion, saved.attemptedAt)
        assertEquals(8L, saved.revision)
        assertEquals(RecordOrigin.CachedFallback, saved.origin)
        assertEquals(Instant.parse("2026-10-20T14:01:00Z"), saved.fetchAfter)
        val frame = assertIs<FavoriteWidgetFrame.Event>(
          engine.project(eventRequest(at = completion), result.record, locale).frame,
        )
        assertEquals(WidgetTap.Event("3122"), frame.tap)
        assertEquals("Project Blender 2026", frame.medium.header.title)
        assertEquals(listOf("/api/v1/events/3122"), paths)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun eventRefreshEncodesEventAndSelectedMatchPathSegments() = runTest {
    val id = "3122/part?#"
    val paths = mutableListOf<String>()
    val payload = readWidgetFixture("event-3122.json")
      .replace("\"3122\"", "\"3122/part?#\"")
      .replace("\"777919\"", "\"777919/part?#\"")
    val client = widgetTestClient { path, _, _ ->
      paths += path
      when (path) {
        "/api/v1/events/3122%2Fpart%3F%23" -> WidgetTestResponse(payload)
        "/api/v1/matches/777919%2Fpart%3F%23" -> WidgetTestResponse("{}")
        else -> WidgetTestResponse("", HttpStatusCode.NotFound)
      }
    }
    try {
      val engine = engine(client, eventNow)
      val request = eventRequest(id)
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request, null))
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, eventKey(id)))
      assertEquals(RecordOrigin.Fresh, saved.origin)
      assertEquals("777919/part?#", assertIs<CachedValue.Event>(saved.value).value.detail?.matchId)
      assertEquals(
        listOf("/api/v1/events/3122%2Fpart%3F%23", "/api/v1/matches/777919%2Fpart%3F%23"),
        paths,
      )
      assertEquals(WidgetTap.Event(id), engine.project(request, result.record, locale).frame.tap)
    } finally {
      client.close()
    }
  }

  @Test
  fun eventSetupChecksDirectEventFavoritesAndRefusesHttp() = runTest {
    var requests = 0
    val client = widgetTestClient { _, _, _ ->
      requests++
      WidgetTestResponse(readWidgetFixture("event-3122.json"))
    }
    try {
      val engine = engine(client, eventNow)
      val selected = eventRequest()
      val cases = listOf(
        selected.copy(source = null) to SetupReason.NoFavorites,
        selected.copy(source = selected.source?.copy(favoriteEvents = emptyList())) to SetupReason.NoFavorites,
        selected.copy(selection = WidgetSelection.None(WidgetKind.Event), source = null) to SetupReason.NoFavorites,
        selected.copy(selection = WidgetSelection.None(WidgetKind.Event)) to SetupReason.Choose,
        selected.copy(selection = WidgetSelection.Chosen(WidgetKind.Event, "2766")) to SetupReason.Choose,
      )
      for ((request, reason) in cases) {
        val projection = engine.project(request, StoredWidgetRecord("corrupt"), locale)
        val frame = assertIs<FavoriteWidgetFrame.Setup>(projection.frame)
        assertEquals(WidgetKind.Event, frame.kind)
        assertEquals(reason, frame.reason)
        assertEquals(
          if (reason ==
            SetupReason.NoFavorites
          ) {
            "No favorite events"
          } else {
            "Choose an event"
          },
          frame.text.title,
        )
        assertEquals(
          if (reason == SetupReason.NoFavorites) "Open Val Esports" else "Tap to choose",
          frame.text.action,
        )
        assertEquals("To use this widget,", frame.text.lead)
        assertEquals(if (reason == SetupReason.NoFavorites) WidgetTap.App else WidgetTap.Chooser, frame.tap)
        assertEquals(WidgetSchedule(null, null), projection.schedule)
        assertNull(projection.key)
        assertEquals(0L, projection.recordRevision)
        assertEquals(WidgetRefreshResult.Refused, engine.refresh(request, null))
      }
      assertEquals(0, requests)
    } finally {
      client.close()
    }
  }

  @Test
  fun eventCacheRejectsWrongKindAndIdentityAndKeepsConfiguredPlaceholderTap() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("{}") }
    try {
      val engine = engine(client, eventNow)
      val valid = eventRecord(eventData())
      val wrongTeamKey = assertNotNull(WidgetKey.parse(WidgetKind.Team, "3122", "saved-client"))
      val team = TeamContent(
        TeamHeader(WidgetTeam("3122", "Sentinels", "SEN", null), 4, "Americas", 1),
        emptyList(),
        emptyList(),
        TeamState.Idle(null),
      )
      val invalid = listOf(
        null,
        StoredWidgetRecord("not json"),
        WidgetCacheCodec.encode(valid.copy(key = eventKey("2766"))),
        WidgetCacheCodec.encode(
          valid.copy(key = assertNotNull(WidgetKey.parse(WidgetKind.Event, "3122", "other-client"))),
        ),
        WidgetCacheCodec.encode(valid.copy(key = wrongTeamKey)),
        WidgetCacheCodec.encode(valid.copy(value = CachedValue.Event(eventData("2766"), eventNow))),
        WidgetCacheCodec.encode(valid.copy(value = CachedValue.Team(team, eventNow))),
        StoredWidgetRecord(
          JsonObject(
            json.parseToJsonElement(WidgetCacheCodec.encode(valid).json).jsonObject.let { record ->
              record + ("value" to JsonObject(record.getValue("value").jsonObject + ("fetchedAt" to JsonNull)))
            },
          ).toString(),
        ),
      )
      for (stored in invalid) {
        assertNull(WidgetCacheCodec.decode(stored, eventKey()))
        val projection = engine.project(eventRequest(), stored, locale)
        val frame = assertIs<FavoriteWidgetFrame.Placeholder>(projection.frame)
        assertEquals(eventKey(), frame.key)
        assertEquals("TBD", frame.text.value)
        assertEquals(WidgetTap.Event("3122"), frame.tap)
        assertEquals(WidgetSchedule(null, eventNow), projection.schedule)
        assertEquals(0L, projection.recordRevision)
      }
      assertNull(WidgetCacheCodec.decode(WidgetCacheCodec.encode(valid.copy(key = wrongTeamKey)), wrongTeamKey))
      val stored = WidgetCacheCodec.encode(valid)
      assertEquals(valid, WidgetCacheCodec.decode(stored, eventKey()))
      assertIs<FavoriteWidgetFrame.Event>(engine.project(eventRequest(), stored, locale).frame)
    } finally {
      client.close()
    }
  }

  @Test
  fun hiddenFinishedEventReprojectsItsRowAtMidnight() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("{}") }
    try {
      val engine = engine(client, eventNow)
      val start = Instant.parse("2026-10-18T23:55:00Z")
      val midnight = Instant.parse("2026-10-19T00:00:00Z")
      val request = eventRequest("2766", at = start, hidden = true)
      val stored = WidgetCacheCodec.encode(eventRecord(eventData("2766"), at = start))
      val initial = engine.project(request, stored, locale)
      assertEquals(midnight, initial.schedule.renderAt)
      val boundary = walkEventTimeline(engine, request, stored, midnight).last().second
      val frame = assertIs<FavoriteWidgetFrame.Event>(boundary.frame)
      assertEquals(WidgetTap.Event("2766"), frame.tap)
      val ended = assertIs<HiddenEventText.Finished>(assertIs<EventDisplayBody.Hidden>(frame.large.body).state)
      assertEquals("Oct 18", ended.hero.date)
      val rows = assertNotNull(ended.list).rows
      assertEquals("Yesterday", rows.first().day)
      assertEquals(3, rows.size)
      assertIs<HiddenEventText.Finished>(assertIs<EventDisplayBody.Hidden>(frame.medium.body).state)
    } finally {
      client.close()
    }
  }

  @Test
  fun eventTimelineReprojectsBothUpcomingExpiryBoundaries() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("{}") }
    try {
      val engine = engine(client, eventNow)
      val start = Instant.parse("2026-10-21T15:55:00Z")
      val request = eventRequest(at = start)
      val stored = WidgetCacheCodec.encode(eventRecord(eventData(), at = start))
      val entries = walkEventTimeline(engine, request, stored, Instant.parse("2026-10-22T00:30:01Z"))
      val firstExpiry = entries.single { it.first == Instant.parse("2026-10-21T22:00:01Z") }.second
      val secondExpiry = entries.single { it.first == Instant.parse("2026-10-22T00:30:01Z") }.second
      for ((projection, lead, day) in listOf(
        Triple(entries.first().second, "CGN Esports vs Joblife", "Today"),
        Triple(firstExpiry, "TEAM GEORGE vs Natus Vincere", null),
        Triple(secondExpiry, "CGN Esports vs Eintracht Frankfurt", "Today"),
      )) {
        val frame = assertIs<FavoriteWidgetFrame.Event>(projection.frame)
        for (presentation in listOf(frame.medium, frame.large)) {
          val hero = assertIs<VisibleEventText.Scheduled>(
            assertIs<EventDisplayBody.Visible>(presentation.body).state,
          ).hero
          assertEquals(lead, hero.lead)
          assertEquals(day, hero.day)
        }
        assertEquals(WidgetTap.Event("3122"), frame.tap)
        assertEquals(1L, projection.recordRevision)
      }
      assertEquals(entries.size, entries.map { it.first }.toSet().size)
      val saved = assertNotNull(WidgetCacheCodec.decode(stored, eventKey()))
      assertEquals(start, assertIs<CachedValue.Event>(saved.value).fetchedAt)
    } finally {
      client.close()
    }
  }

  @Test
  fun hiddenBetweenEventHeaderUsesTheDisplayedNextStage() {
    val client = widgetTestClient { _, _, _ -> WidgetTestResponse("{}") }
    try {
      val at = Instant.parse("2026-10-04T15:00:00Z")
      val engine = engine(client, at)
      val frame = assertIs<FavoriteWidgetFrame.Event>(
        engine.project(
          eventRequest(at = at, hidden = true),
          WidgetCacheCodec.encode(eventRecord(eventData(), at = at)),
          locale,
        ).frame,
      )
      for (presentation in listOf(frame.medium, frame.large)) {
        assertEquals("Phase 2 · Paris", presentation.header.subtitle)
        val hero = assertIs<HiddenEventText.Scheduled>(assertIs<EventDisplayBody.Hidden>(presentation.body).state).hero
        assertEquals("CGN Esports vs Joblife", hero.lead)
        assertEquals("Wed, Oct 21", hero.day)
      }
    } finally {
      client.close()
    }
  }

  @Test
  fun overdueEventRefreshUsesEveryEligibleStartBeforeTheFavorite() = runTest {
    val at = Instant.parse("2026-10-21T16:01:00Z")
    val paths = mutableListOf<String>()
    val client = widgetTestClient { path, _, _ ->
      paths += path
      WidgetTestResponse(if (path == "/api/v1/events/3122") readWidgetFixture("event-3122.json") else "{}")
    }
    try {
      val engine = engine(client, at)
      val request = eventRequest(at = at, teams = listOf("4915"))
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request, null))
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, eventKey()))
      assertEquals(Instant.parse("2026-10-21T16:16:00Z"), saved.fetchAfter)
      val projection = engine.project(request, result.record, locale)
      assertEquals(Instant.parse("2026-10-21T16:16:00Z"), projection.schedule.fetchAt)
      val frame = assertIs<FavoriteWidgetFrame.Event>(projection.frame)
      val hero = assertIs<VisibleEventText.Scheduled>(assertIs<EventDisplayBody.Visible>(frame.medium.body).state).hero
      assertEquals("TEAM GEORGE vs Natus Vincere", hero.lead)
      assertEquals("Today", hero.day)
      assertEquals("6:30\u202FPM", hero.time)
      assertEquals(listOf("/api/v1/events/3122", "/api/v1/matches/777925"), paths)
    } finally {
      client.close()
    }
  }

  @Test
  fun eventRefreshUsesTheSuppliedZoneForTheBetweenCutoff() = runTest {
    val payload = """
      {"id":"3122","title":"Event","status":"ongoing",
       "teams":[{"id":"a","name":"Alpha"},{"id":"b","name":"Bravo"}],
       "matches":[
         {"id":"last","date":"2026-10-20","time":"00:00:01","status":"completed",
          "teams":[{"name":"Alpha","score":2},{"name":"Bravo","score":1}]},
         {"id":"next","date":"2026-10-20","time":"20:00:00","status":"upcoming",
          "teams":[{"name":"Alpha"},{"name":"Bravo"}]}]}
    """.trimIndent()
    val client = widgetTestClient { path, _, _ ->
      if (path == "/api/v1/events/3122") {
        WidgetTestResponse(payload)
      } else {
        WidgetTestResponse("", HttpStatusCode.NotFound)
      }
    }
    try {
      val engine = engine(client, eventNow)
      for ((zone, expected) in listOf(
        TimeZone.UTC to "2026-10-20T12:30:00Z",
        TimeZone.of("Asia/Kolkata") to "2026-10-20T12:00:01Z",
      )) {
        val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(eventRequest().copy(timeZone = zone), null))
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, eventKey()))
        assertEquals(Instant.parse(expected), saved.fetchAfter)
        val projection = engine.project(eventRequest().copy(timeZone = zone), result.record, locale)
        assertEquals(Instant.parse(expected), projection.schedule.fetchAt)
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
      val engine = engine(client, eventNow)
      val old = WidgetCacheCodec.encode(eventRecord(eventData()))
      for (clientId in listOf("", " ", "\t")) {
        val request = eventRequest().let { it.copy(source = it.source?.copy(clientId = clientId)) }
        val projection = engine.project(request, old, locale)
        val frame = assertIs<FavoriteWidgetFrame.Placeholder>(projection.frame)
        assertEquals("TBD", frame.text.value)
        assertEquals(WidgetTap.Event("3122"), frame.tap)
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
      WidgetTestResponse(if (path == "/api/v1/events/3122") readWidgetFixture("event-3122.json") else "{}")
    }
    try {
      val engine = engine(client, eventNow)
      val request = eventRequest().let { it.copy(source = it.source?.copy(clientId = "new-client")) }
      val expectedKey = assertNotNull(WidgetKey.parse(WidgetKind.Event, "3122", "new-client"))
      val old = WidgetCacheCodec.encode(eventRecord(eventData()))
      val initial = engine.project(request, old, locale)
      assertIs<FavoriteWidgetFrame.Placeholder>(initial.frame)
      assertEquals(expectedKey, initial.key)
      assertEquals(0L, initial.recordRevision)
      assertEquals(WidgetSchedule(null, eventNow), initial.schedule)
      val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(request, old))
      assertEquals(expectedKey, result.key)
      val saved = assertNotNull(WidgetCacheCodec.decode(result.record, expectedKey))
      assertEquals(1L, saved.revision)
      assertEquals(RecordOrigin.Fresh, saved.origin)
      val projection = engine.project(request, result.record, locale)
      assertIs<FavoriteWidgetFrame.Event>(projection.frame)
      assertEquals(expectedKey, projection.key)
      assertEquals(1L, projection.recordRevision)
      assertEquals(setOf("/api/v1/events/3122", "/api/v1/matches/777919"), paths.toSet())
      assertEquals(2, paths.size)
    } finally {
      client.close()
    }
  }

  @Test
  fun refreshReadsTheClockOnceForSuccessAndFallback() = runTest {
    val completion = eventNow + 60.seconds
    for (succeeds in listOf(true, false)) {
      var clockReads = 0
      val client = widgetTestClient { path, _, _ ->
        if (succeeds) {
          WidgetTestResponse(if (path == "/api/v1/events/3122") readWidgetFixture("event-3122.json") else "{}")
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
        val result = assertIs<WidgetRefreshResult.Store>(engine.refresh(eventRequest(), null))
        val saved = assertNotNull(WidgetCacheCodec.decode(result.record, eventKey()))
        assertEquals(1, clockReads)
        assertEquals(completion, saved.attemptedAt)
        if (succeeds) {
          assertEquals(completion, assertIs<CachedValue.Event>(saved.value).fetchedAt)
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

  private val eventNow = Instant.parse("2026-10-20T12:00:00Z")

  private fun eventKey(id: String = "3122"): WidgetKey =
    assertNotNull(WidgetKey.parse(WidgetKind.Event, id, "saved-client"))

  private fun eventRequest(
    id: String = "3122",
    at: Instant = eventNow,
    hidden: Boolean = false,
    teams: List<String> = emptyList(),
  ): WidgetRequest = WidgetRequest(
    WidgetSelection.Chosen(WidgetKind.Event, id),
    source().copy(
      favoriteEvents = listOf(WidgetFavoriteEvent(id, "Event")),
      favorites = WidgetFavorites(teamIds = teams, eventIds = listOf(id)),
      spoilersHidden = hidden,
    ),
    at,
    TimeZone.UTC,
  )

  private fun eventData(id: String = "3122"): EventData = EventWidgetMapper.normalize(
    WidgetWireDecoders.event(json.parseToJsonElement(readWidgetFixture("event-$id.json")), id, eventNow),
  )

  private fun eventRecord(data: EventData, at: Instant = eventNow, revision: Long = 1): WidgetCacheRecord =
    WidgetCacheRecord(
      1,
      eventKey(data.event.id),
      revision,
      at,
      at + 7200.seconds,
      RecordOrigin.Fresh,
      CachedValue.Event(data, at),
    )

  private fun walkEventTimeline(
    engine: FavoriteWidgetEngine,
    request: WidgetRequest,
    stored: StoredWidgetRecord,
    until: Instant,
  ): List<Pair<Instant, WidgetProjection>> {
    val entries = mutableListOf<Pair<Instant, WidgetProjection>>()
    var at = request.now
    repeat(64) {
      val projection = engine.project(request.copy(now = at), stored, locale)
      entries += at to projection
      if (at == until) return entries
      val next = assertNotNull(projection.schedule.renderAt)
      assertTrue(next > at, "Render deadline must advance from $at")
      assertTrue(next <= until, "Missing render boundary $until before $next")
      at = next
    }
    error("Event timeline did not reach $until")
  }

  private fun source(): UpcomingMatchesSnapshot = UpcomingMatchesSnapshot(
    savedAtEpochMillis = 0,
    clientId = "saved-client",
    hasFavorites = true,
    matches = emptyList(),
    theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
    favoriteTeams = listOf(WidgetFavoriteTeam("2", "Sentinels")),
  )

  private fun engine(client: HttpClient, completion: Instant = eventNow): FavoriteWidgetEngine = FavoriteWidgetEngine(
    FavoriteWidgetRemote(client, json),
    object : Clock {
      override fun now(): Instant = completion
    },
    json,
  )
}
