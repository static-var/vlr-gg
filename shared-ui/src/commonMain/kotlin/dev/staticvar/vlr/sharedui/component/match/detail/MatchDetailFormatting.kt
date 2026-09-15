/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.runtime.Composable
import dev.staticvar.vlr.domain.model.AgentInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlin.math.round
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_allMaps
import vlr.shared_ui.generated.resources.format_final
import vlr.shared_ui.generated.resources.format_map
import vlr.shared_ui.generated.resources.format_mapNumber
import vlr.shared_ui.generated.resources.format_maps
import vlr.shared_ui.generated.resources.format_matches
import vlr.shared_ui.generated.resources.format_pending
import vlr.shared_ui.generated.resources.format_pendingLower
import vlr.shared_ui.generated.resources.format_playedMaps
import vlr.shared_ui.generated.resources.format_playerStats
import vlr.shared_ui.generated.resources.format_sortedByMapOrder
import vlr.shared_ui.generated.resources.format_stage
import vlr.shared_ui.generated.resources.format_steps
import vlr.shared_ui.generated.resources.format_teamOne
import vlr.shared_ui.generated.resources.format_teamTwo
import vlr.shared_ui.generated.resources.format_unknown

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
  val recordedStatsCount: Int,
)

internal fun MatchDetails.matchDetailMeta(): String = event.stage.ifBlank { event.series }

internal fun MatchDetails.matchDetailDateStat(labels: MatchFormattingLabels): String =
  event.date?.takeIf(String::isNotBlank) ?: labels.pending

internal fun MatchDetails.matchDetailMapCountStat(labels: MatchFormattingLabels): String = when {
  mapCount > 0 -> labels.maps
  else -> "-"
}

internal fun MatchDetails.matchDetailStageStat(labels: MatchFormattingLabels): String =
  event.stage.takeIf(String::isNotBlank) ?: labels.stage

internal fun MatchDetails.matchDetailVetoStat(labels: MatchFormattingLabels): String =
  when (bans.count(String::isNotBlank)) {
    0 -> "-"
    else -> labels.steps
  }

internal fun List<MapData>.matchDetailMapOptions(labels: MatchFormattingLabels): List<MatchDetailMapOption> {
  val mapOptions = mapIndexed { index, map ->
    MatchDetailMapOption(
      id = index.toString(),
      label = map.matchDetailMapOptionLabel(labels),
      mapIndex = index,
    )
  }

  return if (size >= 2) {
    listOf(MatchDetailMapOption(id = AllMapsOptionId, label = labels.allMaps, mapIndex = null)) + mapOptions
  } else {
    mapOptions
  }
}

public fun List<MapData>.resolveSelectedMapIndex(selectedMapIndex: Int?): Int? = when {
  size == 1 -> 0
  selectedMapIndex != null && selectedMapIndex in indices -> selectedMapIndex
  else -> null
}

internal fun List<MapData>.resolveSelectedMap(selectedMapIndex: Int?): MapData? =
  resolveSelectedMapIndex(selectedMapIndex)?.let(::get)

internal fun List<MapData>.resolveSelectedMapOptionId(selectedMapIndex: Int?): String =
  resolveSelectedMapIndex(selectedMapIndex)?.toString() ?: if (size >= 2) AllMapsOptionId else ""

internal fun MapData.matchDetailMapOptionLabel(labels: MatchFormattingLabels): String =
  "${matchDetailMapName(labels)} - ${matchDetailMapScoreLabel(labels)}"

internal fun MapData.matchDetailMapName(labels: MatchFormattingLabels): String = map.ifBlank { labels.map }

internal fun MapData.matchDetailMapScoreLabel(labels: MatchFormattingLabels): String {
  val scores = teams.take(2).mapNotNull(TeamDetails::score)
  return if (scores.size == 2) {
    "${scores[0]}-${scores[1]}"
  } else {
    labels.pending
  }
}

internal fun MapData.matchDetailMapMeta(labels: MatchFormattingLabels, index: Int? = null): String = listOfNotNull(
  index?.let { labels.mapNumber },
  if (teams.any { team -> team.score != null }) labels.final else labels.pendingLower,
  labels.playerStats,
).joinToString(separator = " • ")

internal fun List<MapData>.matchDetailAllMapsMeta(labels: MatchFormattingLabels): String = labels.sortedByMapOrder

internal fun MapData.toPlayerStatsRows(
  labels: MatchFormattingLabels,
  mapName: String? = null,
): List<MatchDetailPlayerStatsRow> = members.mapIndexed { index, player ->
  player.toPlayerStatsRow(
    labels = labels,
    keyPrefix = matchDetailMapName(labels),
    index = index,
    mapName = mapName,
    teamColorRole = resolvePlayerTeamColorRole(player.team),
    team = resolvePlayerTeam(player.team),
  )
}

internal fun List<MapData>.toAllMapPlayerStatsRows(labels: MatchFormattingLabels): List<MatchDetailPlayerStatsRow> {
  val winnerTeamKey = toPlayerStatsWinnerTeamKey()
  return filter(MapData::hasPlayedScore)
    .fold(linkedMapOf<String, PlayerStatsAggregate>()) { aggregates, map ->
      map.members.forEach { player ->
        val key = player.aggregateKey()
        val team = map.resolvePlayerTeam(player.team)
        val teamKey = team?.teamKey()
        val existing = aggregates[key]
        aggregates[key] = if (existing == null) {
          player.toStatsAggregate(key = key, team = team, labels = labels)
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
  labels: MatchFormattingLabels,
  keyPrefix: String,
  index: Int,
  mapName: String?,
  teamColorRole: MatchDetailPlayerStatsTeamColorRole,
  team: TeamDetails?,
): MatchDetailPlayerStatsRow = MatchDetailPlayerStatsRow(
  key = "$keyPrefix-$playerId-$index",
  playerId = playerId.takeIf(String::isNotBlank),
  mapName = mapName,
  playerName = name.ifBlank { labels.unknown },
  agentNames = agents.joinToString(separator = ", ") { agent -> agent.name }.ifBlank { "-" },
  acs = recordedStatOrDash(acs.toString()),
  kills = recordedStatOrDash(kills.toString()),
  deaths = recordedStatOrDash(deaths.toString()),
  assists = recordedStatOrDash(assists.toString()),
  kast = recordedStatOrDash(kast.toString()),
  rating = recordedStatOrDash(rating.toCompactRating()),
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

private fun PlayerStatsAggregate.toPlayerStatsRow(winnerTeamKey: String?): MatchDetailPlayerStatsRow {
  val divisor = recordedStatsCount.takeIf { it > 0 }
  return MatchDetailPlayerStatsRow(
    key = "all-$key",
    playerId = playerId,
    mapName = null,
    playerName = playerName,
    agentNames = agentNames.joinToString(separator = ", ").ifBlank { "-" },
    acs = divisor?.let { (acsTotal.toFloat() / it).toRoundedIntString() } ?: "-",
    kills = divisor?.let { killsTotal.toString() } ?: "-",
    deaths = divisor?.let { deathsTotal.toString() } ?: "-",
    assists = divisor?.let { assistsTotal.toString() } ?: "-",
    kast = divisor?.let { (kastTotal.toFloat() / it).toRoundedIntString() } ?: "-",
    rating = divisor?.let { (ratingTotal / it).toCompactRating() } ?: "-",
    teamColorRole = winnerTeamKey
      ?.takeIf { teamKey != null && it == teamKey }
      ?.let { MatchDetailPlayerStatsTeamColorRole.Accent }
      ?: MatchDetailPlayerStatsTeamColorRole.Neutral,
    teamName = teamName,
    teamLogoUrl = teamLogoUrl,
  )
}

private fun PlayerStats.toStatsAggregate(
  key: String,
  team: TeamDetails?,
  labels: MatchFormattingLabels,
): PlayerStatsAggregate {
  val hasRecordedStats = hasRecordedStats()
  return PlayerStatsAggregate(
    key = key,
    playerName = name.ifBlank { labels.unknown },
    agentNames = agents.map(AgentInfo::name).filter(String::isNotBlank).distinct(),
    playerId = playerId.takeIf(String::isNotBlank),
    teamKey = team?.teamKey(),
    teamName = team?.name.orEmpty(),
    teamLogoUrl = team?.img.orEmpty(),
    acsTotal = acs.takeIf { hasRecordedStats } ?: 0,
    killsTotal = kills.takeIf { hasRecordedStats } ?: 0,
    deathsTotal = deaths.takeIf { hasRecordedStats } ?: 0,
    assistsTotal = assists.takeIf { hasRecordedStats } ?: 0,
    kastTotal = kast.takeIf { hasRecordedStats } ?: 0,
    ratingTotal = rating.takeIf { hasRecordedStats } ?: 0f,
    recordedStatsCount = if (hasRecordedStats) 1 else 0,
  )
}

private fun PlayerStatsAggregate.withPlayer(player: PlayerStats, mapTeam: TeamDetails?): PlayerStatsAggregate {
  val hasRecordedStats = player.hasRecordedStats()
  return copy(
    agentNames = (agentNames + player.agents.map(AgentInfo::name).filter(String::isNotBlank)).distinct(),
    playerId = playerId ?: player.playerId.takeIf(String::isNotBlank),
    teamKey = teamKey ?: mapTeam?.teamKey(),
    teamName = teamName.ifBlank { mapTeam?.name.orEmpty() },
    teamLogoUrl = teamLogoUrl.ifBlank { mapTeam?.img.orEmpty() },
    acsTotal = acsTotal + player.acs.takeIf { hasRecordedStats }.orZero(),
    killsTotal = killsTotal + player.kills.takeIf { hasRecordedStats }.orZero(),
    deathsTotal = deathsTotal + player.deaths.takeIf { hasRecordedStats }.orZero(),
    assistsTotal = assistsTotal + player.assists.takeIf { hasRecordedStats }.orZero(),
    kastTotal = kastTotal + player.kast.takeIf { hasRecordedStats }.orZero(),
    ratingTotal = ratingTotal + (player.rating.takeIf { hasRecordedStats } ?: 0f),
    recordedStatsCount = recordedStatsCount + if (hasRecordedStats) 1 else 0,
  )
}

private fun PlayerStats.recordedStatOrDash(value: String): String = if (hasRecordedStats()) value else "-"

private fun PlayerStats.hasRecordedStats(): Boolean = agents.isNotEmpty() ||
  acs != 0 ||
  adr != 0 ||
  kills != 0 ||
  deaths != 0 ||
  assists != 0 ||
  kast != 0 ||
  firstKills != 0 ||
  firstDeaths != 0 ||
  firstKillsDiff != 0 ||
  hsPercent != 0 ||
  rating != 0f

private fun Int?.orZero(): Int = this ?: 0

internal fun List<PreviousEncounter>.matchDetailHeadToHeadSummary(
  labels: MatchFormattingLabels,
): MatchDetailHeadToHeadSummary? {
  val firstEncounterTeams = firstOrNull()?.teams?.take(2) ?: return null
  if (firstEncounterTeams.size < 2) return null

  val firstTeamName = firstEncounterTeams[0].name.ifBlank { labels.teamOne }
  val secondTeamName = firstEncounterTeams[1].name.ifBlank { labels.teamTwo }
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

internal fun MatchDetailHeadToHeadSummary.matchDetailTagLabel(labels: MatchFormattingLabels): String {
  val difference = firstTeamWins - secondTeamWins
  return when {
    difference > 0 -> "$firstTeamName +$difference"
    difference < 0 -> "$secondTeamName +${-difference}"
    else -> labels.matches
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

internal data class MatchFormattingLabels(
  val pending: String,
  val map: String,
  val stage: String,
  val maps: String,
  val steps: String,
  val allMaps: String,
  val mapNumber: String,
  val final: String,
  val pendingLower: String,
  val playerStats: String,
  val sortedByMapOrder: String,
  val unknown: String,
  val teamOne: String,
  val teamTwo: String,
  val matches: String,
)

@Composable
internal fun matchFormattingLabels(
  mapCount: Int = 0,
  vetoStepCount: Int = 0,
  playedMapCount: Int = 0,
  matchCount: Int = 0,
  mapNumber: Int = 0,
): MatchFormattingLabels = MatchFormattingLabels(
  pending = stringResource(Res.string.format_pending),
  map = stringResource(Res.string.format_map),
  stage = stringResource(Res.string.format_stage),
  maps = pluralStringResource(Res.plurals.format_maps, mapCount, mapCount),
  steps = pluralStringResource(Res.plurals.format_steps, vetoStepCount, vetoStepCount),
  allMaps = stringResource(Res.string.format_allMaps),
  mapNumber = stringResource(Res.string.format_mapNumber, mapNumber),
  final = stringResource(Res.string.format_final),
  pendingLower = stringResource(Res.string.format_pendingLower),
  playerStats = stringResource(Res.string.format_playerStats),
  sortedByMapOrder = stringResource(
    Res.string.format_sortedByMapOrder,
    pluralStringResource(Res.plurals.format_playedMaps, playedMapCount, playedMapCount),
  ),
  unknown = stringResource(Res.string.format_unknown),
  teamOne = stringResource(Res.string.format_teamOne),
  teamTwo = stringResource(Res.string.format_teamTwo),
  matches = pluralStringResource(Res.plurals.format_matches, matchCount, matchCount),
)
