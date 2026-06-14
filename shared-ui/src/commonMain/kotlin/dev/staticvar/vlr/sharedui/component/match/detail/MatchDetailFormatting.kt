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
  val mapName: String?,
  val playerName: String,
  val agentNames: String,
  val acs: String,
  val kills: String,
  val deaths: String,
  val assists: String,
  val kast: String,
  val rating: String,
)

private data class PlayerStatsAggregate(
  val key: String,
  val playerName: String,
  val agentNames: List<String>,
  val acsTotal: Int,
  val killsTotal: Int,
  val deathsTotal: Int,
  val assistsTotal: Int,
  val kastTotal: Int,
  val ratingTotal: Float,
  val mapsPlayed: Int,
)

internal fun MatchDetails.matchDetailTitle(): String = teams
  .take(2)
  .map(TeamDetails::name)
  .filter(String::isNotBlank)
  .joinToString(separator = " vs ")
  .ifBlank { event.name.ifBlank { "Match details" } }

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
    player.toPlayerStatsRow(keyPrefix = matchDetailMapName(), index = index, mapName = mapName)
  }

internal fun List<MapData>.toAllMapPlayerStatsRows(): List<MatchDetailPlayerStatsRow> = filter(MapData::hasPlayedScore)
  .flatMap(MapData::members)
  .fold(linkedMapOf<String, PlayerStatsAggregate>()) { aggregates, player ->
    val key = player.aggregateKey()
    val existing = aggregates[key]
    aggregates[key] = if (existing == null) {
      player.toStatsAggregate(key = key)
    } else {
      existing + player
    }
    aggregates
  }
  .values
  .map(PlayerStatsAggregate::toPlayerStatsRow)

private fun PlayerStats.toPlayerStatsRow(keyPrefix: String, index: Int, mapName: String?): MatchDetailPlayerStatsRow =
  MatchDetailPlayerStatsRow(
    key = "$keyPrefix-$playerId-$index",
    mapName = mapName,
    playerName = name.ifBlank { "Unknown" },
    agentNames = agents.joinToString(separator = ", ") { agent -> agent.name }.ifBlank { "-" },
    acs = acs.toString(),
    kills = kills.toString(),
    deaths = deaths.toString(),
    assists = assists.toString(),
    kast = kast.toString(),
    rating = rating.toCompactRating(),
  )

private fun MapData.hasPlayedScore(): Boolean = teams.any { team -> team.score != null }

private fun PlayerStats.aggregateKey(): String = playerId.takeIf(String::isNotBlank) ?: "$team-$name"

private fun PlayerStats.toStatsAggregate(key: String): PlayerStatsAggregate = PlayerStatsAggregate(
  key = key,
  playerName = name.ifBlank { "Unknown" },
  agentNames = agents.map(AgentInfo::name).filter(String::isNotBlank).distinct(),
  acsTotal = acs,
  killsTotal = kills,
  deathsTotal = deaths,
  assistsTotal = assists,
  kastTotal = kast,
  ratingTotal = rating,
  mapsPlayed = 1,
)

private operator fun PlayerStatsAggregate.plus(player: PlayerStats): PlayerStatsAggregate = copy(
  agentNames = (agentNames + player.agents.map(AgentInfo::name).filter(String::isNotBlank)).distinct(),
  acsTotal = acsTotal + player.acs,
  killsTotal = killsTotal + player.kills,
  deathsTotal = deathsTotal + player.deaths,
  assistsTotal = assistsTotal + player.assists,
  kastTotal = kastTotal + player.kast,
  ratingTotal = ratingTotal + player.rating,
  mapsPlayed = mapsPlayed + 1,
)

private fun PlayerStatsAggregate.toPlayerStatsRow(): MatchDetailPlayerStatsRow = MatchDetailPlayerStatsRow(
  key = "all-$key",
  mapName = null,
  playerName = playerName,
  agentNames = agentNames.joinToString(separator = ", ").ifBlank { "-" },
  acs = (acsTotal.toFloat() / mapsPlayed).toRoundedIntString(),
  kills = killsTotal.toString(),
  deaths = deathsTotal.toString(),
  assists = assistsTotal.toString(),
  kast = (kastTotal.toFloat() / mapsPlayed).toRoundedIntString(),
  rating = (ratingTotal / mapsPlayed).toCompactRating(),
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
  val rounded = kotlin.math.round(this * 100.0f) / 100.0f
  return rounded.toString().trimEnd('0').trimEnd('.')
}

private fun Float.toRoundedIntString(): String = roundToInt().toString()
