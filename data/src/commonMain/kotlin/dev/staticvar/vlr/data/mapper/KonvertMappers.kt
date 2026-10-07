/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.data.PlayerAgentStats
import dev.staticvar.vlr.data.PlayerTeamHistory
import dev.staticvar.vlr.data.Standings
import dev.staticvar.vlr.remotesource.player.PlayerAgentStatsDto
import dev.staticvar.vlr.remotesource.player.PlayerTeamRefDto
import dev.staticvar.vlr.remotesource.standings.TeamStandingDto
import kotlin.time.Clock

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
