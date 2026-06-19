/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import dev.staticvar.vlr.domain.model.AgentInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlin.math.round
import kotlin.math.roundToInt

internal const val AllMapsOptionId: String = "all"

internal data class MatchDetailMapOption(val id: String, val label: String, val mapIndex: Int?)

internal data class MatchDetailHeadToHeadSummary(
  val firstTeamName: String,
  val firstTeamWins: Int,
  val secondTeamName: String,
  val secondTeamWins: Int,
  val totalPlayed: Int,
)

internal data class MatchDetailPlayerStatsRow(
  val key: String,
  val playerId: String?,
  val mapName: String?,
  val playerName: String,
  val agentNames: String,
  val acs: String,
  val kills: String,
  val deaths: String,
  val assists: String,
  val kast: String,
  val rating: String,
  val teamColorRole: MatchDetailPlayerStatsTeamColorRole,
  val teamName: String,
  val teamLogoUrl: String,
)

internal enum class MatchDetailPlayerStatsTeamColorRole {
  Accent,
  Neutral,
}

private data class PlayerStatsAggregate(
  val key: String,
  val playerName: String,
  val agentNames: List<String>,
  val playerId: String?,
  val teamKey: String?,
  val teamName: String,
  val teamLogoUrl: String,
  val acsTotal: Int,
  val killsTotal: Int,
  val deathsTotal: Int,
  val assistsTotal: Int,
  val kastTotal: Int,
  val ratingTotal: Float,
  val mapsPlayed: Int,
)

internal fun MatchDetails.matchDetailMeta(): String = listOfNotNull(
  event.stage.takeIf(String::isNotBlank),
  event.series.takeIf(String::isNotBlank),
).joinToString(separator = " • ")

internal fun MatchDetails.matchDetailDateStat(): String = event.date?.takeIf(String::isNotBlank) ?: "Pending"

internal fun MatchDetails.matchDetailStageStat(): String = event.stage.takeIf(String::isNotBlank) ?: "Stage"

internal fun MatchDetails.matchDetailPatchStat(): String = event.patch?.takeIf(String::isNotBlank) ?: "Patch"

internal fun MatchDetails.matchDetailBanStat(): String = when (bans.size) {
  0 -> "No bans"
  1 -> "1 ban"
  else -> "${bans.size} bans"
}

internal fun List<MapData>.matchDetailMapOptions(): List<MatchDetailMapOption> {
  val mapOptions = mapIndexed { index, map ->
    MatchDetailMapOption(
      id = index.toString(),
      label = map.matchDetailMapOptionLabel(),
      mapIndex = index,
    )
  }

  return if (size >= 2) {
    listOf(MatchDetailMapOption(id = AllMapsOptionId, label = "All maps", mapIndex = null)) + mapOptions
  } else {
    mapOptions
  }
}

internal fun List<MapData>.resolveSelectedMap(selectedMapIndex: Int?): MapData? = when {
  isEmpty() -> null
  selectedMapIndex == null && size == 1 -> first()
  selectedMapIndex == null -> null
  else -> getOrNull(selectedMapIndex)
}

internal fun MapData.matchDetailMapOptionLabel(): String = "${matchDetailMapName()} - ${matchDetailMapScoreLabel()}"

internal fun MapData.matchDetailMapName(): String = map.ifBlank { "Map" }

internal fun MapData.matchDetailMapScoreLabel(): String {
  val scores = teams.take(2).mapNotNull(TeamDetails::score)
  return if (scores.size == 2) {
    "${scores[0]}-${scores[1]}"
  } else {
    "Pending"
  }
}

internal fun MapData.matchDetailMapMeta(index: Int? = null): String = listOfNotNull(
  index?.let { mapIndex -> "Map ${mapIndex + 1}" },
  if (teams.any { team -> team.score != null }) "final" else "pending",
  "player stats",
).joinToString(separator = " • ")

internal fun List<MapData>.matchDetailAllMapsMeta(): String {
  val playedMaps = count(MapData::hasPlayedScore)
  val playedLabel = if (playedMaps == 1) "1 played map" else "$playedMaps played maps"
  return "$playedLabel • sorted by map order"
}

internal fun MapData.toPlayerStatsRows(mapName: String? = null): List<MatchDetailPlayerStatsRow> =
  members.mapIndexed { index, player ->
    player.toPlayerStatsRow(
      keyPrefix = matchDetailMapName(),
      index = index,
      mapName = mapName,
      teamColorRole = resolvePlayerTeamColorRole(player.team),
      team = resolvePlayerTeam(player.team),
    )
  }

internal fun List<MapData>.toAllMapPlayerStatsRows(): List<MatchDetailPlayerStatsRow> {
  val winnerTeamKey = toPlayerStatsWinnerTeamKey()
  return filter(MapData::hasPlayedScore)
    .fold(linkedMapOf<String, PlayerStatsAggregate>()) { aggregates, map ->
      map.members.forEach { player ->
        val key = player.aggregateKey()
        val team = map.resolvePlayerTeam(player.team)
        val teamKey = team?.teamKey()
        val existing = aggregates[key]
        aggregates[key] = if (existing == null) {
          player.toStatsAggregate(key = key, team = team)
        } else {
          existing.withPlayer(player = player, mapTeam = team)
        }
      }
      aggregates
    }
    .values
    .map { aggregate ->
      aggregate.toPlayerStatsRow(winnerTeamKey = winnerTeamKey)
    }
}

private fun PlayerStats.toPlayerStatsRow(
  keyPrefix: String,
  index: Int,
  mapName: String?,
  teamColorRole: MatchDetailPlayerStatsTeamColorRole,
  team: TeamDetails?,
): MatchDetailPlayerStatsRow =
  MatchDetailPlayerStatsRow(
    key = "$keyPrefix-$playerId-$index",
    playerId = playerId.takeIf(String::isNotBlank),
    mapName = mapName,
    playerName = name.ifBlank { "Unknown" },
    agentNames = agents.joinToString(separator = ", ") { agent -> agent.name }.ifBlank { "-" },
    acs = acs.toString(),
    kills = kills.toString(),
    deaths = deaths.toString(),
    assists = assists.toString(),
    kast = kast.toString(),
    rating = rating.toCompactRating(),
    teamColorRole = teamColorRole,
    teamName = team?.name.orEmpty(),
    teamLogoUrl = team?.img.orEmpty(),
  )

private fun MapData.resolvePlayerTeamColorRole(playerTeam: String): MatchDetailPlayerStatsTeamColorRole {
  val winnerTeam = winningTeam() ?: return MatchDetailPlayerStatsTeamColorRole.Neutral
  val resolvedPlayerTeam = teams.firstOrNull { team -> team.matchesPlayerTeam(playerTeam) }
    ?: return MatchDetailPlayerStatsTeamColorRole.Neutral

  return if (resolvedPlayerTeam.matchesTeam(winnerTeam)) {
    MatchDetailPlayerStatsTeamColorRole.Accent
  } else {
    MatchDetailPlayerStatsTeamColorRole.Neutral
  }
}

private fun MapData.resolvePlayerTeam(playerTeam: String): TeamDetails? = teams
  .firstOrNull { team -> team.matchesPlayerTeam(playerTeam) }

private fun MapData.winningTeam(): TeamDetails? = teams
  .firstOrNull { it.isWinner == true } ?: teams.firstByScoreWinner()

private fun List<MapData>.toPlayerStatsWinnerTeamKey(): String? {
  val winnerKeys = mapNotNull { it.winningTeam()?.teamKey() }
  if (winnerKeys.isEmpty()) return null

  val winnerCounts = winnerKeys.groupingBy { it }.eachCount()
  val maxWins = winnerCounts.values.maxOrNull() ?: return null
  val topWinners = winnerCounts.filterValues { it == maxWins }
  return if (topWinners.size == 1) topWinners.keys.first() else null
}

private fun List<TeamDetails>.firstByScoreWinner(): TeamDetails? {
  val firstTeam = getOrNull(0) ?: return null
  val secondTeam = getOrNull(1) ?: return null
  val firstTeamScore = firstTeam.score
  val secondTeamScore = secondTeam.score

  return when {
    firstTeamScore == null || secondTeamScore == null -> null
    firstTeamScore > secondTeamScore -> firstTeam
    firstTeamScore < secondTeamScore -> secondTeam
    else -> null
  }
}

private fun TeamDetails.matchesPlayerTeam(playerTeam: String): Boolean {
  val normalizedPlayerTeam = playerTeam.trim()
  if (normalizedPlayerTeam.isBlank()) return false

  val teamId = id?.trim().orEmpty()

  return (teamId.isNotBlank() && teamId.equals(normalizedPlayerTeam, ignoreCase = true)) ||
    name.equals(normalizedPlayerTeam, ignoreCase = true)
}

private fun TeamDetails.teamKey(): String? {
  val teamId = id?.trim().orEmpty()
  return when {
    teamId.isNotBlank() -> "id:${teamId.lowercase()}"
    name.trim().isNotBlank() -> "name:${name.trim().lowercase()}"
    else -> null
  }
}

private fun TeamDetails.matchesTeam(other: TeamDetails): Boolean {
  val thisId = id?.trim().orEmpty()
  val otherId = other.id?.trim().orEmpty()
  return when {
    thisId.isNotBlank() && otherId.isNotBlank() -> thisId == otherId
    else -> name.equals(other.name, ignoreCase = true)
  }
}

private fun MapData.hasPlayedScore(): Boolean = teams.any { team -> team.score != null }

private fun PlayerStats.aggregateKey(): String = playerId.takeIf(String::isNotBlank) ?: "$team-$name"

private fun PlayerStatsAggregate.toPlayerStatsRow(winnerTeamKey: String?): MatchDetailPlayerStatsRow = MatchDetailPlayerStatsRow(
  key = "all-$key",
  playerId = playerId,
  mapName = null,
  playerName = playerName,
  agentNames = agentNames.joinToString(separator = ", ").ifBlank { "-" },
  acs = (acsTotal.toFloat() / mapsPlayed).toRoundedIntString(),
  kills = killsTotal.toString(),
  deaths = deathsTotal.toString(),
  assists = assistsTotal.toString(),
  kast = (kastTotal.toFloat() / mapsPlayed).toRoundedIntString(),
  rating = (ratingTotal / mapsPlayed).toCompactRating(),
  teamColorRole = winnerTeamKey
    ?.takeIf { teamKey != null && it == teamKey }
    ?.let { MatchDetailPlayerStatsTeamColorRole.Accent }
    ?: MatchDetailPlayerStatsTeamColorRole.Neutral,
  teamName = teamName,
  teamLogoUrl = teamLogoUrl,
)

private fun PlayerStats.toStatsAggregate(key: String, team: TeamDetails?): PlayerStatsAggregate = PlayerStatsAggregate(
  key = key,
  playerName = name.ifBlank { "Unknown" },
  agentNames = agents.map(AgentInfo::name).filter(String::isNotBlank).distinct(),
  playerId = playerId.takeIf(String::isNotBlank),
  teamKey = team?.teamKey(),
  teamName = team?.name.orEmpty(),
  teamLogoUrl = team?.img.orEmpty(),
  acsTotal = acs,
  killsTotal = kills,
  deathsTotal = deaths,
  assistsTotal = assists,
  kastTotal = kast,
  ratingTotal = rating,
  mapsPlayed = 1,
)

private fun PlayerStatsAggregate.withPlayer(player: PlayerStats, mapTeam: TeamDetails?): PlayerStatsAggregate = copy(
  agentNames = (agentNames + player.agents.map(AgentInfo::name).filter(String::isNotBlank)).distinct(),
  playerId = playerId ?: player.playerId.takeIf(String::isNotBlank),
  teamKey = teamKey ?: mapTeam?.teamKey(),
  teamName = teamName.ifBlank { mapTeam?.name.orEmpty() },
  teamLogoUrl = teamLogoUrl.ifBlank { mapTeam?.img.orEmpty() },
  acsTotal = acsTotal + player.acs,
  killsTotal = killsTotal + player.kills,
  deathsTotal = deathsTotal + player.deaths,
  assistsTotal = assistsTotal + player.assists,
  kastTotal = kastTotal + player.kast,
  ratingTotal = ratingTotal + player.rating,
  mapsPlayed = mapsPlayed + 1,
)

internal fun List<PreviousEncounter>.matchDetailHeadToHeadSummary(): MatchDetailHeadToHeadSummary? {
  val firstEncounterTeams = firstOrNull()?.teams?.take(2) ?: return null
  if (firstEncounterTeams.size < 2) return null

  val firstTeamName = firstEncounterTeams[0].name.ifBlank { "Team 1" }
  val secondTeamName = firstEncounterTeams[1].name.ifBlank { "Team 2" }
  var firstWins = 0
  var secondWins = 0
  var totalPlayed = 0

  forEach { encounter ->
    val teams = encounter.teams.take(2)
    val winner = teams.firstOrNull { team -> team.isWinner == true } ?: return@forEach
    totalPlayed += 1
    when {
      winner.matchesTeam(firstEncounterTeams[0]) -> firstWins += 1
      winner.matchesTeam(firstEncounterTeams[1]) -> secondWins += 1
    }
  }

  return if (totalPlayed == 0) {
    null
  } else {
    MatchDetailHeadToHeadSummary(
      firstTeamName = firstTeamName,
      firstTeamWins = firstWins,
      secondTeamName = secondTeamName,
      secondTeamWins = secondWins,
      totalPlayed = totalPlayed,
    )
  }
}

internal fun TeamPreview.matchDetailScoreText(): String = score?.toString() ?: "-"

internal fun MatchDetailHeadToHeadSummary.matchDetailTagLabel(): String {
  val difference = firstTeamWins - secondTeamWins
  return when {
    difference > 0 -> "$firstTeamName +$difference"
    difference < 0 -> "$secondTeamName +${-difference}"
    else -> "$totalPlayed matches"
  }
}

private fun TeamPreview.matchesTeam(candidate: TeamPreview): Boolean =
  id?.takeIf(String::isNotBlank)?.let { teamId -> teamId == candidate.id?.takeIf(String::isNotBlank) }
    ?: (name == candidate.name)

private fun Float.toCompactRating(): String {
  val rounded = round(this * 100.0f) / 100.0f
  return rounded.toString().trimEnd('0').trimEnd('.')
}

private fun Float.toRoundedIntString(): String = roundToInt().toString()
