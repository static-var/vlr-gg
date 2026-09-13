/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategyScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GroupedListDetailSceneStrategyTest {
  @Test
  fun groupedListsKeepTheirSceneIdentityWhenDetailsOpenAndChange() {
    for (group in listOf("matches", "events", "news", "rankings")) {
      val list = entry(group, listPane(group))
      val firstDetail = entry("$group-first", detailPane(group))
      val nextDetail = entry("$group-next", detailPane(group))
      val listScene = assertNotNull(scene(listOf(list)))
      val firstScene = assertNotNull(scene(listOf(list, firstDetail)))
      val nextScene = assertNotNull(scene(listOf(list, firstDetail, nextDetail)))

      assertEquals(listScene::class, firstScene::class)
      assertEquals(listScene.key, firstScene.key)
      assertEquals(listScene.key, nextScene.key)
      assertEquals(listOf(list), listScene.entries)
      assertEquals(listOf(list, nextDetail), nextScene.entries)
      assertEquals(listOf(list, firstDetail), nextScene.previousEntries)
      assertEquals(listOf(list), firstScene.previousEntries)
      assertEquals(emptyList(), listScene.previousEntries)
    }
  }

  @Test
  fun teamDetailsRemainRankingsDetailUntilAPlayerOpens() {
    val rankings = entry("rankings", listPane("rankings"))
    val team = entry("team", listPane("team") + detailPane("rankings"))
    val player = entry("player", detailPane("team"))
    val teamScene = assertNotNull(scene(listOf(rankings, team)))
    val playerScene = assertNotNull(scene(listOf(rankings, team, player)))

    assertEquals(rankings.contentKey, teamScene.key)
    assertEquals(listOf(rankings, team), teamScene.entries)
    assertEquals(team.contentKey, playerScene.key)
    assertEquals(listOf(team, player), playerScene.entries)
    assertEquals(listOf(rankings, team), playerScene.previousEntries)
  }

  @Test
  fun unmatchedDetailsAndUngroupedDestinationsUseTheFallbackScene() {
    val matches = entry("matches", listPane("matches"))
    assertNull(scene(listOf(matches, entry("event", detailPane("events")))))
    assertNull(scene(listOf(matches, entry("settings"))))
    assertNull(scene(listOf(entry("team", listPane("team") + detailPane("rankings")))))
    assertNull(scene(emptyList()))
  }

  @Test
  fun compactLayoutsUseTheFallbackForBothListAndDetail() {
    val list = entry("news", listPane("news"))
    val detail = entry("article", detailPane("news"))
    assertNull(scene(listOf(list), enabled = false))
    assertNull(scene(listOf(list, detail), enabled = false))
  }

  private fun entry(key: String, metadata: Map<String, Any> = emptyMap()): NavEntry<String> =
    NavEntry(key = key, metadata = metadata) {}

  private fun scene(entries: List<NavEntry<String>>, enabled: Boolean = true): Scene<String>? =
    with(GroupedListDetailSceneStrategy<String>(enabled)) {
      SceneStrategyScope<String>().calculateScene(entries)
    }
}
