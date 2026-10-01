/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.mp.KoinPlatform

class WidgetSnapshotTest {
  private val json: Json = KoinPlatform.getKoin().get()

  @Test
  fun favoritePayloadUsesSharedFieldsAndTheme() {
    val snapshot = requireNotNull(parseWidgetSnapshot(
      """
      {
        "savedAtEpochMillis": 1000,
        "clientId": "test-client",
        "hasFavorites": true,
        "spoilersHidden": true,
        "matches": [{
          "id": "101", "event": "VCT Pacific", "team1": "Paper Rex", "team2": "Team Liquid",
          "startTimeEpochMillis": 1900000000000, "status": "live", "score1": 1, "score2": 2,
          "format": "BO3", "stage": "Playoffs", "futureField": "ignored"
        }],
        "theme": {
          "background": 4278190080, "surface": 4279308561, "accent": 4286332928,
          "content": 4294967295, "secondary": 4289374890, "border": 4281545523,
          "monospace": true, "futureColor": 12
        },
        "favorites": {"matchIds": ["101"]},
        "futureField": {"nested": true}
      }
      """.trimIndent(),
      json,
    ))
    assertEquals(1000L, snapshot.savedAtEpochMillis)
    assertTrue(snapshot.hasFavorites)
    assertTrue(snapshot.spoilersHidden)
    assertEquals(
      WidgetMatch("101", "VCT Pacific", "Paper Rex", "Team Liquid", 1900000000000L,
        WidgetMatchStatus.LIVE, 1, 2, "BO3", "Playoffs"),
      snapshot.matches.single(),
    )
    assertEquals(WidgetColors(4278190080L, 4279308561L, 4286332928L, 4294967295L,
      4289374890L, 4281545523L, true), snapshot.theme)
  }

  @Test
  fun legacyPayloadKeepsOptionalDefaultsAndUnknownStatus() {
    val snapshot = requireNotNull(parseWidgetSnapshot(
      """
      {"savedAtEpochMillis":1000,"matches":[
        {"id":"101","event":"VCT","team1":"A","team2":"B"},
        {"id":"102","event":"VCT","team1":"C","team2":"D",
         "status":"POSTPONED","startTimeEpochMillis":null,"score1":null,"score2":null}
      ],"theme":null}
      """.trimIndent(),
      json,
    ))
    assertFalse(snapshot.hasFavorites)
    assertFalse(snapshot.spoilersHidden)
    assertEquals(WidgetColors.Default, snapshot.theme)
    assertEquals(listOf(WidgetMatchStatus.UPCOMING, WidgetMatchStatus.OTHER), snapshot.matches.map { it.status })
    snapshot.matches.forEach { match ->
      assertNull(match.startTimeEpochMillis)
      assertNull(match.score1)
      assertNull(match.score2)
      assertEquals("", match.format)
      assertEquals("", match.stage)
    }
  }

  @Test
  fun malformedRequiredFieldsInvalidateSnapshot() {
    val payloads = listOf(
      "not json",
      "[]",
      """{"matches":[]}""",
      """{"savedAtEpochMillis":1000}""",
      """{"savedAtEpochMillis":1000,"matches":{}}""",
      """{"savedAtEpochMillis":1000,"matches":[null]}""",
      """{"savedAtEpochMillis":1000,"matches":[{"id":"101","event":"VCT","team1":"A"}]}""",
      """{"savedAtEpochMillis":1000,"matches":[{"id":{},"event":"VCT","team1":"A","team2":"B"}]}""",
      """{"savedAtEpochMillis":1000,"matches":[],"theme":{"background":1}}""",
    )
    payloads.forEach { payload -> assertNull(payload, parseWidgetSnapshot(payload, json)) }
    assertNotNull(parseWidgetSnapshot("""{"savedAtEpochMillis":1000,"matches":[]}""", json))
  }

  @Test
  fun legacyWriterFiltersSortsAndReplacesSnapshotWithHiddenScores() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = context.getFileStreamPath("all_matches_widget.json")
    val previous = file.takeIf { it.exists() }?.readBytes()
    try {
      val matches = listOf(
        match("upcoming", MatchStatus.UPCOMING, "2030-01-01T00:00:00Z"),
        match("completed", MatchStatus.COMPLETED, null),
        match("live", MatchStatus.LIVE, "invalid-time"),
      )
      LegacyMatchSnapshotStore.write(context, matches, spoilersHidden = false, json)
      val root = json.parseToJsonElement(file.readText()).jsonObject
      assertEquals(setOf("savedAtEpochMillis", "spoilersHidden", "matches"), root.keys)
      assertEquals("false", root.getValue("spoilersHidden").jsonPrimitive.content)
      assertTrue(root.getValue("savedAtEpochMillis").jsonPrimitive.content.toLong() > 0)
      val rows = root.getValue("matches").jsonArray.map { it.jsonObject }
      assertEquals(listOf("live", "upcoming"), rows.map { it.getValue("id").jsonPrimitive.content })
      assertEquals("2", rows.first().getValue("score1").jsonPrimitive.content)
      assertEquals("1", rows.first().getValue("score2").jsonPrimitive.content)
      assertEquals("Playoffs", rows.first().getValue("stage").jsonPrimitive.content)
      val visible = requireNotNull(LegacyMatchSnapshotStore.read(context, json))
      assertEquals(WidgetMatchStatus.LIVE, visible.matches.first().status)
      assertNull(visible.matches.first().startTimeEpochMillis)
      assertEquals(1893456000000L, visible.matches.last().startTimeEpochMillis)
      assertEquals(WidgetColors.Default, visible.theme)
      assertFalse(visible.hasFavorites)

      LegacyMatchSnapshotStore.write(context, matches.take(1), spoilersHidden = true, json)
      val hidden = requireNotNull(LegacyMatchSnapshotStore.read(context, json))
      assertEquals(listOf("upcoming"), hidden.matches.map { it.id })
      assertTrue(hidden.spoilersHidden)
      assertNull(hidden.matches.single().score1)
      assertNull(hidden.matches.single().score2)
      val hiddenRow = json.parseToJsonElement(file.readText()).jsonObject.getValue("matches").jsonArray.single().jsonObject
      assertTrue(hiddenRow["score1"] == null || hiddenRow["score1"] == JsonNull)
      assertTrue(hiddenRow["score2"] == null || hiddenRow["score2"] == JsonNull)
    } finally {
      if (previous == null) file.delete() else file.writeBytes(previous)
    }
  }

  private fun match(id: String, status: MatchStatus, time: String?): MatchPreview = MatchPreview(
    id = id,
    event = "VCT Pacific",
    series = "Playoffs",
    status = status,
    team1 = TeamPreview(null, "Paper Rex", "APAC", "", 2, null),
    team2 = TeamPreview(null, "Team Liquid", "EMEA", "", 1, null),
    time = time,
    eventId = "event-1",
  )
}
