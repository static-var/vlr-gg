package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.data.MatchBans
import dev.staticvar.vlr.data.MatchMapPlayerStats
import dev.staticvar.vlr.data.MatchMapRounds
import dev.staticvar.vlr.data.MatchMaps
import dev.staticvar.vlr.data.MatchPreviousEncounters
import dev.staticvar.vlr.data.MatchVideos
import dev.staticvar.vlr.data.Matches
import dev.staticvar.vlr.remotesource.api.MatchPreviewDto as ApiMatchPreviewDto
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MapDataDto
import dev.staticvar.vlr.remotesource.match.PlayerStatsDto
import dev.staticvar.vlr.remotesource.match.RoundInfoDto
import dev.staticvar.vlr.remotesource.match.TeamDto as DetailTeamDto
import dev.staticvar.vlr.remotesource.api.TeamDto as PreviewTeamDto
import dev.staticvar.vlr.remotesource.match.VideoReferenceDto
import kotlinx.datetime.Clock

/** Manual mapping for match preview (complex flattening) */
internal fun ApiMatchPreviewDto.toEntity(): Matches =
  Matches(
    id = id,
    event_id = eventId.takeIf { it.isNotEmpty() },
    event_name = event,
    event_logo_url = "", // Not present in preview DTO
    series = series,
    stage = "", // Not available in preview DTO
    status = status.ifEmpty { "UNKNOWN" },
    time = time ?: "", // DB requires NOT NULL
    eta = null,
    note = "", // Not in preview
    patch = null,
    team1_id = team1.id ?: "",
    team1_name = team1.name,
    team1_logo_url = team1.img,
    team1_score = team1.score?.toLong(),
    team2_id = team2.id ?: "",
    team2_name = team2.name,
    team2_logo_url = team2.img,
    team2_score = team2.score?.toLong(),
    map_count = 0, // Unknown at preview stage
    last_updated = Clock.System.now().toEpochMilliseconds()
  )

// ---------------- Match Details ----------------

/** Core match row from details (updates existing or insert). */
internal fun MatchDetailsDto.toMatchEntity(): Matches {
  val team1 = teams.getOrNull(0)
  val team2 = teams.getOrNull(1)
  return Matches(
    id = id.ifEmpty { event.id + "_" + (team1?.name ?: "") + "_vs_" + (team2?.name ?: "") },
    event_id = event.id,
    event_name = event.name,
    event_logo_url = event.img,
    series = event.series,
    stage = event.stage,
    status = event.status?.name ?: "UNKNOWN",
    time = event.date ?: "",
    eta = null,
    note = note,
    patch = event.patch,
    team1_id = team1?.id ?: team1?.name ?: "", // Some APIs omit id
    team1_name = team1?.name ?: "",
    team1_logo_url = team1?.img ?: "",
    team1_score = parseScoreComponent(score, 0),
    team2_id = team2?.id ?: team2?.name ?: "",
    team2_name = team2?.name ?: "",
    team2_logo_url = team2?.img ?: "",
    team2_score = parseScoreComponent(score, 1),
    map_count = mapCount.toLong(),
    last_updated = Clock.System.now().toEpochMilliseconds()
  )
}

private fun parseScoreComponent(score: String, index: Int): Long? =
  score.split(":").takeIf { it.size == 2 }?.getOrNull(index)?.trim()?.toLongOrNull()

// Maps table rows
internal fun MatchDetailsDto.toMapEntities(matchId: String): List<MatchMaps> =
  matchData.map { mapDto ->
    MatchMaps(
      id = 0, // AUTOINCREMENT placeholder
      match_id = matchId,
      map_name = mapDto.map,
      team1_score = mapDto.teams.getOrNull(0)?.score?.toLong(),
      team2_score = mapDto.teams.getOrNull(1)?.score?.toLong(),
      duration = null, // Not provided
      stats_url = null // Not provided
    )
  }

// Rounds
internal fun MatchDetailsDto.toRoundEntities(matchId: String): List<MatchMapRounds> =
  matchData.flatMap { mapDto ->
    mapDto.rounds.map { round ->
      MatchMapRounds(
        id = 0,
        match_id = matchId,
        map_name = mapDto.map,
        round_number = round.roundNo.toLong(),
        round_score = round.score,
        winner = round.winner.ifEmpty { "NOT_PLAYED" },
        side = round.side.ifEmpty { "NOT_PLAYED" },
        win_type = round.winType.ifEmpty { "NOT_PLAYED" }
      )
    }
  }

// Player stats per map
internal fun MatchDetailsDto.toPlayerStatEntities(matchId: String): List<MatchMapPlayerStats> =
  matchData.flatMap { mapDto ->
    mapDto.members.flatMap { player ->
      if (player.agents.isEmpty()) listOf(player.toStatEntity(matchId, mapDto.map, null))
      else player.agents.map { agent -> player.toStatEntity(matchId, mapDto.map, agent.name to agent.img) }
    }
  }

private fun PlayerStatsDto.toStatEntity(matchId: String, mapName: String, agent: Pair<String, String?>?): MatchMapPlayerStats =
  MatchMapPlayerStats(
    id = 0,
    match_id = matchId,
    map_name = mapName,
    player_id = playerId,
    player_name = name,
    team_id = team,
    agent_name = agent?.first ?: "",
    agent_image_url = agent?.second ?: "",
    rating = rating.toDouble(),
    acs = acs.toLong(),
    kills = kills.toLong(),
    deaths = deaths.toLong(),
    assists = assists.toLong(),
    kast_percent = kast.toDoubleOrNullSafe(),
    adr = adr.toDoubleOrNullSafe(),
    hs_percent = hsPercent.toDoubleOrNullSafe(),
    first_kills = firstKills.toLong(),
    first_deaths = firstDeaths.toLong(),
    first_kills_diff = firstKillsDiff.toLong()
  )

private fun Int.toDoubleOrNullSafe(): Double? = this.takeIf { it != 0 }?.toDouble()

// Bans (simple string list; ban_type hard-coded as "map" until other types appear)
internal fun MatchDetailsDto.toBanEntities(matchId: String): List<MatchBans> =
  bans.map { value ->
    MatchBans(
      id = 0,
      match_id = matchId,
      ban_type = "map",
      ban_value = value
    )
  }

// Videos (streams + vods)
internal fun MatchDetailsDto.toVideoEntities(matchId: String): List<MatchVideos> =
  videos.streams.map { it.toVideoEntity(matchId, "stream") } +
    videos.vods.map { it.toVideoEntity(matchId, "vod") }

private fun VideoReferenceDto.toVideoEntity(matchId: String, type: String): MatchVideos =
  MatchVideos(
    id = 0,
    match_id = matchId,
    video_type = type,
    name = name,
    url = url
  )

// Previous encounters
internal fun MatchDetailsDto.toPreviousEncounterEntities(matchId: String): List<MatchPreviousEncounters> =
  head2head.mapNotNull { prev ->
    val t1 = prev.teams.getOrNull(0)
    val t2 = prev.teams.getOrNull(1)
    if (t1 == null || t2 == null) null else MatchPreviousEncounters(
      id = 0,
      match_id = matchId,
      previous_match_id = prev.id.ifEmpty { "" },
      team1_name = t1.name,
      team1_score = t1.score?.toLong(),
      team2_name = t2.name,
      team2_score = t2.score?.toLong()
    )
  }
