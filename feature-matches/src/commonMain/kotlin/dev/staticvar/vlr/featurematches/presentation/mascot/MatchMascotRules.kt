/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation.mascot

import dev.staticvar.vlr.sharedui.mascot.MascotCue
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.RoundWinner
import dev.staticvar.vlr.domain.model.TeamDetails

/** Returns eligible celebrations, with the strongest cue first. */
public fun matchMascotCues(
  match: MatchDetails,
  favoriteTeamIds: Set<String>,
  favoritePlayerIds: Set<String>,
  selectedMapIndex: Int? = null,
): List<MascotCue> {
  val status = when (match.event.status?.trim()?.lowercase()) {
    "completed" -> MatchStatus.COMPLETED
    "live", "ongoing" -> MatchStatus.LIVE
    else -> return emptyList()
  }
  if (match.id.isBlank()) return emptyList()
  val cues = mutableListOf<MascotCue>()
  val seriesTeamCue = teamCue(match.id, status, match.teams, favoriteTeamIds)
  seriesTeamCue?.let(cues::add)
  val selectedMap = selectedMapIndex?.let(match.matchData::getOrNull)
  val scope = selectedMapIndex?.let { "map:$it:${selectedMap?.map.orEmpty()}" } ?: "series"
  if (status == MatchStatus.LIVE && selectedMap != null &&
    selectedMapIndex == match.matchData.indexOfLast { it.hasBeenPlayed() } &&
    !selectedMap.isFinished()
  ) {
    teamCue(match.id, status, selectedMap.teams, favoriteTeamIds, scope, minimumLead = 2)
      ?.takeUnless { cue -> cues.any { it.message == cue.message } }
      ?.let(cues::add)
  }
  val maps = if (selectedMapIndex == null) {
    match.matchData.filter { it.hasBeenPlayed() }
  } else {
    listOfNotNull(selectedMap?.takeIf { it.hasBeenPlayed() })
  }
  standoutPlayer(maps)?.let { player ->
    if (player.playerId in favoritePlayerIds && player.name.isNotBlank() &&
      player.kills >= if (status == MatchStatus.LIVE) 5 else 1
    ) {
      val completed = status == MatchStatus.COMPLETED
      val playerCue = MascotCue(
        id = "${match.id}:${if (completed) "player-result" else "player-live"}:${player.playerId}:$scope",
        message = "${player.name} ${if (completed) "cooked!" else "is cooking!"}",
        priority = if (completed) 40 else 20,
      )
      if (completed && seriesTeamCue != null) {
        cues.remove(seriesTeamCue)
        cues += MascotCue(
          id = "${seriesTeamCue.id}:${playerCue.id}",
          message = "${seriesTeamCue.message.removeSuffix("!")} &",
          secondaryMessage = "${player.name} cooked!!!",
          priority = 50,
        )
      } else {
        cues += playerCue
      }
    }
  }
  return cues.sortedWith(compareByDescending<MascotCue> { it.priority }.thenBy { it.id })
}

public fun matchMascotCues(match: MatchPreview, favoriteTeamIds: Set<String>): List<MascotCue> =
  listOfNotNull(
    teamCue(
      match.id,
      match.status,
      listOf(match.team1, match.team2).map {
        TeamDetails(it.id, it.name, it.region, it.img, it.score, it.isWinner)
      },
      favoriteTeamIds,
    ),
  )

private fun teamCue(
  matchId: String,
  status: MatchStatus,
  teams: List<TeamDetails>,
  favorites: Set<String>,
  scope: String = "series",
  minimumLead: Int = 1,
): MascotCue? {
  if (matchId.isBlank() || status !in listOf(MatchStatus.COMPLETED, MatchStatus.LIVE)) return null
  if (teams.size != 2 || teams.any { it.id.isNullOrBlank() || (it.score ?: -1) < 0 } ||
    teams[0].id == teams[1].id
  ) return null
  val leader = teams.maxBy { it.score!! }
  val opponent = teams.first { it.id != leader.id }
  if (leader.id !in favorites || leader.name.isBlank() || leader.score!! - opponent.score!! < minimumLead) return null
  val completed = status == MatchStatus.COMPLETED
  return MascotCue(
    id = "$matchId:${if (completed) "team-win" else "team-lead"}:${leader.id}:$scope",
    message = if (completed) "${leader.name} won!" else "Go ${leader.name}!",
    priority = if (completed) 30 else 10,
  )
}

private fun MapData.hasBeenPlayed(): Boolean = teams.any { (it.score ?: 0) > 0 } ||
  members.any { it.kills > 0 || it.deaths > 0 } || rounds.any { it.winner != RoundWinner.NOT_PLAYED }

private fun MapData.isFinished(): Boolean {
  val scores = teams.mapNotNull { it.score }
  return scores.size == 2 && scores.max() >= 13 && scores.max() - scores.min() >= 2
}

private fun standoutPlayer(maps: List<MapData>): PlayerStats? {
  if (maps.isEmpty() || maps.any { !it.hasFullRoster() }) return null
  val roster = maps.first().members.associate { it.playerId to it.team }
  if (maps.any { map -> map.members.associate { it.playerId to it.team } != roster }) return null
  val totals = maps.flatMap { it.members }.groupBy { it.playerId }.values.map { stats ->
    stats.first().copy(kills = stats.sumOf { it.kills })
  }
  val highestKills = totals.maxOf { it.kills }
  return totals.singleOrNull { it.kills == highestKills }
}

private fun MapData.hasFullRoster(): Boolean = members.size == 10 &&
  members.all { it.playerId.isNotBlank() && it.team.isNotBlank() && it.kills >= 0 } &&
  members.map { it.playerId }.distinct().size == 10 &&
  members.groupingBy { it.team }.eachCount().values.sorted() == listOf(5, 5)
