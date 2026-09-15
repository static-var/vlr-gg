/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.search

import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SearchFavoritesTest {
  @Test
  fun allFavoriteKindsHaveIndependentStableIdentifiersAndOnlySearchMetadata() {
    val snapshot = DirectFavoriteSnapshot(
      teams = listOf(DirectFavorite.Team("12", "Team Liquid", "team-logo")),
      events = listOf(DirectFavorite.Event("12", "Champions", "event-logo")),
      matches = listOf(DirectFavorite.Match("12", "Liquid vs PRX", "match-logo")),
      players = listOf(DirectFavorite.Player("12", "Player", "player-photo")),
    )

    val json = Json.parseToJsonElement(Json.encodeToString(snapshot.searchFavorites())).jsonArray
    assertEquals(
      setOf("event:12", "match:12", "player:12", "team:12"),
      json.map { it.jsonObject.getValue("id").jsonPrimitive.content }.toSet(),
    )
    for (item in json) {
      val fields = item.jsonObject
      assertEquals(setOf("id", "kind", "sourceId", "title"), fields.keys)
      assertEquals("12", fields.getValue("sourceId").jsonPrimitive.content)
      assertEquals("${fields.getValue("kind").jsonPrimitive.content}:12", fields.getValue("id").jsonPrimitive.content)
    }
    assertEquals("Team Liquid", json.last().jsonObject.getValue("title").jsonPrimitive.content)
  }

  @Test
  fun reorderingAndDuplicatesDoNotChangeIndexButRenamingDoes() {
    val first = DirectFavorite.Team("1", "Alpha", "")
    val second = DirectFavorite.Team("2", "Bravo", "")
    val initial = DirectFavoriteSnapshot(teams = listOf(first, second)).searchFavorites()
    val reordered = DirectFavoriteSnapshot(teams = listOf(second, first, first)).searchFavorites()
    val renamed = DirectFavoriteSnapshot(teams = listOf(first.copy(title = "New name"), second)).searchFavorites()

    assertEquals(initial, reordered)
    assertNotEquals(initial, renamed)
    assertEquals(initial.map { it.id }, renamed.map { it.id })
  }

  @Test
  fun removingAllFavoritesProducesAnEmptyIndex() {
    assertEquals("[]", Json.encodeToString(DirectFavoriteSnapshot().searchFavorites()))
  }
}
