/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.localsource.database.GetRankingQueryTeamsWithFavoriteStatus
import dev.staticvar.vlr.remotesource.rankings.RankingsRequest

internal fun RankingsQuery.toRequest(): RankingsRequest = RankingsRequest(
  circuit = circuit.apiValue,
  region = region?.apiValue ?: "all",
  minMatches = minMatches,
  includeInactive = includeInactive,
  sort = metric.apiValue,
  order = order.apiValue,
)

internal fun GetRankingQueryTeamsWithFavoriteStatus.toTeamRanking(): TeamRanking = TeamRanking(
  teamId = team_id,
  teamName = team_name,
  teamLogo = team_logo,
  country = country,
  rank = rank.toInt(),
  elo = elo,
  wins = match_wins.toInt(),
  losses = match_losses.toInt(),
  isFavorite = is_favorite == 1L,
  region = RankingRegion.entries.firstOrNull { it.apiValue == region },
  mapElo = map_elo,
  matchesPlayed = matches_played.toInt(),
  winRate = win_rate,
  overallRank = overall_rank?.toInt(),
)
