/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.data.News
import dev.staticvar.vlr.data.NewsMedia
import dev.staticvar.vlr.data.PlayerAgentStats
import dev.staticvar.vlr.data.PlayerTeamHistory
import dev.staticvar.vlr.data.Rankings
import dev.staticvar.vlr.data.Standings
import dev.staticvar.vlr.remotesource.news.NewsArticleDto
import dev.staticvar.vlr.remotesource.news.NewsItemDto
import dev.staticvar.vlr.remotesource.player.PlayerAgentStatsDto
import dev.staticvar.vlr.remotesource.player.PlayerTeamRefDto
import dev.staticvar.vlr.remotesource.rankings.TeamRankingDto
import dev.staticvar.vlr.remotesource.standings.TeamStandingDto
import kotlin.time.Clock

/**
 * Simple manual mappers for News, Rankings, and Standings.
 * These are 1:1 or near-1:1 mappings that don't require Konvert.
 *
 * Note: EventPrizeDto and EventTeamDto mappers are in EventTeamMappers.kt
 * Note: PlayerDetailsDto mapper is complex and should be manual
 */

// ----------------------------- News -----------------------------

internal fun NewsItemDto.toEntity(): News = News(
  id = url.toArticleId(),
  url = url.toAbsoluteVlrUrl(),
  title = title,
  author = author,
  date = date,
  description = description,
  cover_url = "", // Not provided in NewsItemDto
  content_html = null,
  last_updated = Clock.System.now().toEpochMilliseconds(),
)

internal fun NewsArticleDto.toEntity(): News = News(
  id = id.toArticleId(),
  url = id.toAbsoluteVlrUrl(),
  title = title,
  author = author,
  date = date ?: "",
  description = null,
  cover_url = images.firstOrNull() ?: "",
  content_html = content,
  last_updated = Clock.System.now().toEpochMilliseconds(),
)

internal fun NewsArticleDto.toMediaEntities(articleId: String = id): List<NewsMedia> {
  val linkMedia = links.mapNotNull { map ->
    val text = map["text"] ?: return@mapNotNull null
    val href = map["href"] ?: return@mapNotNull null
    NewsMedia(id = 0, news_id = articleId, media_type = "link", media_value = href, media_text = text)
  }
  val imageMedia = images.map { url ->
    NewsMedia(id = 0, news_id = articleId, media_type = "image", media_value = url, media_text = null)
  }
  val videoMedia = videos.map { vid ->
    NewsMedia(id = 0, news_id = articleId, media_type = "video", media_value = vid, media_text = null)
  }
  return linkMedia + imageMedia + videoMedia
}

private fun String.toAbsoluteVlrUrl(): String {
  val value = trim()
  return when {
    value.startsWith("https://") || value.startsWith("http://") -> value
    value.startsWith("/") -> "https://www.vlr.gg$value"
    else -> "https://www.vlr.gg/$value"
  }
}

private fun String.toArticleId(): String {
  val absolute = toAbsoluteVlrUrl()
  return absolute
    .removePrefix("https://www.vlr.gg/")
    .removePrefix("http://www.vlr.gg/")
    .trim('/')
}

// ----------------------------- Rankings -----------------------------

internal fun TeamRankingDto.toEntity(region: String): Rankings = Rankings(
  team_id = id.toString(),
  region = region,
  team_name = name,
  team_logo = logo,
  country = country,
  rank = rank.toLong(),
  points = points.toString(),
  last_updated = Clock.System.now().toEpochMilliseconds(),
)

// ----------------------------- Standings -----------------------------

internal fun TeamStandingDto.toEntity(year: Int, circuit: String, region: String): Standings = Standings(
  team_id = id.toString(),
  year = year.toLong(),
  circuit = circuit,
  region = region,
  team_name = name,
  team_logo = logo,
  country = country,
  rank = rank.toLong(),
  points = points.toString(),
  last_updated = Clock.System.now().toEpochMilliseconds(),
)

// ----------------------------- Player Child Tables -----------------------------

internal fun PlayerAgentStatsDto.toEntity(playerId: String): PlayerAgentStats = PlayerAgentStats(
  id = 0,
  player_id = playerId,
  agent_name = name,
  agent_image_url = img,
  usage_count = count.toLong(),
  usage_percent = percent,
  rounds_played = rounds.toLong(),
  rating = rating,
  acs = acs,
  kd_ratio = kd,
  adr = adr,
  kast = kast,
  kpr = kpr,
  apr = apr,
  fkpr = fkpr,
  fdpr = fdpr,
  kills = k.toLong(),
  deaths = d.toLong(),
  assists = a.toLong(),
  first_kills = fk.toLong(),
  first_deaths = fd.toLong(),
)

internal fun PlayerTeamRefDto.toEntity(playerId: String, isCurrent: Boolean?): PlayerTeamHistory = PlayerTeamHistory(
  id = 0,
  player_id = playerId,
  team_id = id.takeIf { it.isNotBlank() },
  team_name = name,
  team_logo_url = img,
  is_current = if (isCurrent == true) 1 else 0,
)
