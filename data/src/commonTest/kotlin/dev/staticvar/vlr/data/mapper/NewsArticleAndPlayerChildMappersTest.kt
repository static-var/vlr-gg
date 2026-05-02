/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.remotesource.news.NewsArticleDto
import dev.staticvar.vlr.remotesource.player.PlayerAgentStatsDto
import dev.staticvar.vlr.remotesource.player.PlayerTeamRefDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewsArticleAndPlayerChildMappersTest {

  @Test
  fun `news article entity and media mapping positive`() {
    val dto = NewsArticleDto(
      id = "n1",
      title = "Title",
      content = "<p>Content</p>",
      links = listOf(mapOf("text" to "Site", "href" to "https://x.y")),
      images = listOf("img1.png", "img2.png"),
      videos = listOf("vid1.mp4"),
      date = "2024-01-01",
      author = "Author",
    )

    val entity = dto.toEntity()
    assertEquals("n1", entity.id)
    assertEquals("img1.png", entity.cover_url)
    assertEquals("<p>Content</p>", entity.content_html)
    assertTrue(entity.last_updated > 0)

    val media = dto.toMediaEntities()
    assertEquals(1, media.count { it.media_type == "link" })
    assertEquals(2, media.count { it.media_type == "image" })
    assertEquals(1, media.count { it.media_type == "video" })
  }

  @Test
  fun `news article mapping negative edge`() {
    val dto = NewsArticleDto(
      id = "n2",
      title = "",
      content = "",
      links = listOf(mapOf("text" to "MissingHref")), // invalid link
      images = emptyList(),
      videos = emptyList(),
      date = null,
      author = "",
    )
    val entity = dto.toEntity()
    assertEquals("n2", entity.id)
    assertEquals("", entity.cover_url) // Schema has NOT NULL, so empty string
    assertEquals("", entity.date)
    val media = dto.toMediaEntities()
    assertTrue(media.isEmpty())
  }

  @Test
  fun `player agent stats and team history mapping`() {
    val agentDto = PlayerAgentStatsDto(
      name = "Jett",
      img = "jett.png",
      count = 12,
      percent = 25.0,
      rounds = 100,
      rating = 1.2,
      acs = 250.0,
      kd = 1.1,
      adr = 140.0,
      kast = 70.0,
      kpr = 0.9,
      apr = 0.2,
      fkpr = 0.15,
      fdpr = 0.05,
      k = 180,
      d = 160,
      a = 40,
      fk = 18,
      fd = 6,
    )
    val agentEntity = agentDto.toEntity("p1")
    assertEquals("Jett", agentEntity.agent_name)
    assertEquals(12L, agentEntity.usage_count)
    assertEquals(180L, agentEntity.kills)
    assertEquals(18L, agentEntity.first_kills)

    val currentTeam = PlayerTeamRefDto(id = "t1", name = "Team1", img = "t1.png")
    val historyEntity = currentTeam.toEntity("p1", true)
    assertEquals("t1", historyEntity.team_id)
    assertEquals(1L, historyEntity.is_current)

    val pastTeam = PlayerTeamRefDto(id = "", name = "NoId", img = "")
    val pastHistory = pastTeam.toEntity("p1", false)
    assertNull(pastHistory.team_id)
    assertEquals(0L, pastHistory.is_current)
  }
}
