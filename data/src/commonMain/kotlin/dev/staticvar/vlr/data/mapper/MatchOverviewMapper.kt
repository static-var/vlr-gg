/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.CurrentMatchMap
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.localsource.database.Match_overview
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto

internal fun MatchPreviewDto.toOverviewEntity(): Match_overview {
  val statusName = status?.name ?: "UNKNOWN"
  // The only live gate for list maps. Refreshes replace the whole row, so a map the server drops clears here.
  val liveMap = currentMap?.toDomain()?.takeIf { statusName.toMatchStatus() == MatchStatus.LIVE }
  return Match_overview(
    id = id,
    event_id = eventId.takeIf { it.isNotEmpty() },
    event_name = event,
    series = series,
    status = statusName,
    time = time.orEmpty(),
    team1_id = team1.id.orEmpty(),
    team1_name = team1.name,
    team1_logo_url = team1.img,
    team1_score = team1.score?.toLong(),
    team2_id = team2.id.orEmpty(),
    team2_name = team2.name,
    team2_logo_url = team2.img,
    team2_score = team2.score?.toLong(),
    current_map_name = liveMap?.name,
    current_map_number = liveMap?.number?.toLong(),
    current_map_team1_score = liveMap?.team1Score?.toLong(),
    current_map_team2_score = liveMap?.team2Score?.toLong(),
  )
}

internal fun Match_overview.toDomain(): MatchPreview = MatchPreview(
  id = id,
  event = event_name,
  series = series,
  status = status.toMatchStatus(),
  team1 = TeamPreview(
    id = team1_id.takeIf { it.isNotEmpty() },
    name = team1_name,
    region = "",
    img = team1_logo_url,
    score = team1_score?.toInt(),
    isWinner = overviewWinner(team1_score, team2_score),
  ),
  team2 = TeamPreview(
    id = team2_id.takeIf { it.isNotEmpty() },
    name = team2_name,
    region = "",
    img = team2_logo_url,
    score = team2_score?.toInt(),
    isWinner = overviewWinner(team2_score, team1_score),
  ),
  time = time.takeIf { it.isNotEmpty() },
  eventId = event_id.orEmpty(),
  currentMap = current_map_name?.let { name ->
    CurrentMatchMap(
      name = name,
      number = current_map_number?.toInt(),
      team1Score = current_map_team1_score?.toInt(),
      team2Score = current_map_team2_score?.toInt(),
    )
  },
)

private fun overviewWinner(score: Long?, opponentScore: Long?): Boolean? =
  if (score != null && opponentScore != null) score > opponentScore else null
