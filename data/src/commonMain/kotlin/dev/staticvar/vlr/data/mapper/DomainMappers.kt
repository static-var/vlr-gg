package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.data.MatchBans
import dev.staticvar.vlr.data.MatchMapPlayerStats
import dev.staticvar.vlr.data.MatchMapRounds
import dev.staticvar.vlr.data.MatchMaps
import dev.staticvar.vlr.data.MatchVideos
import dev.staticvar.vlr.domain.model.AgentInfo
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.MatchVideos as DomainMatchVideos
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.RoundInfo
import dev.staticvar.vlr.domain.model.RoundSide
import dev.staticvar.vlr.domain.model.RoundWinType
import dev.staticvar.vlr.domain.model.RoundWinner
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.model.VideoReference
import dev.staticvar.vlr.localsource.database.GetMatchesWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetMatchWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetAllFavoriteMatches

/**
 * Maps database query result to domain MatchPreview model.
 */
internal fun GetMatchesWithFavoriteStatus.toDomain(): MatchPreview =
  MatchPreview(
    id = id,
    event = event_name,
    series = series,
    status = status.toMatchStatus(),
    team1 = TeamPreview(
      id = team1_id.takeIf { it.isNotEmpty() },
      name = team1_name,
      region = "", // Not available in matches table
      img = team1_logo_url,
      score = team1_score?.toInt(),
      isWinner = determineWinner(team1_score, team2_score, true),
      isFavorite = false // Team favorites handled separately
    ),
    team2 = TeamPreview(
      id = team2_id.takeIf { it.isNotEmpty() },
      name = team2_name,
      region = "", // Not available in matches table
      img = team2_logo_url,
      score = team2_score?.toInt(),
      isWinner = determineWinner(team1_score, team2_score, false),
      isFavorite = false
    ),
    time = time.takeIf { it.isNotEmpty() },
    eventId = event_id ?: "",
    isFavorite = is_direct_favorite == 1L
  )

/**
 * Determines if a team is the winner based on scores.
 */
private fun determineWinner(team1Score: Long?, team2Score: Long?, isTeam1: Boolean): Boolean? {
  if (team1Score == null || team2Score == null) return null
  return if (isTeam1) team1Score > team2Score else team2Score > team1Score
}

/**
 * Maps GetAllFavoriteMatches to domain MatchPreview model.
 */
internal fun GetAllFavoriteMatches.toDomain(): MatchPreview =
  MatchPreview(
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
      isWinner = determineWinner(team1_score, team2_score, true),
      isFavorite = false
    ),
    team2 = TeamPreview(
      id = team2_id.takeIf { it.isNotEmpty() },
      name = team2_name,
      region = "",
      img = team2_logo_url,
      score = team2_score?.toInt(),
      isWinner = determineWinner(team1_score, team2_score, false),
      isFavorite = false
    ),
    time = time.takeIf { it.isNotEmpty() },
    eventId = event_id ?: "",
    isFavorite = is_direct_favorite == 1L
  )

/**
 * Maps status string to MatchStatus enum.
 */
private fun String.toMatchStatus(): MatchStatus = when (this.uppercase()) {
  "UPCOMING" -> MatchStatus.UPCOMING
  "LIVE" -> MatchStatus.LIVE
  "COMPLETED" -> MatchStatus.COMPLETED
  else -> MatchStatus.UNKNOWN
}

/**
 * Aggregates match details from multiple database tables.
 */
internal fun aggregateMatchDetails(
  match: GetMatchWithFavoriteStatus,
  maps: List<MatchMaps>,
  rounds: List<MatchMapRounds>,
  playerStats: List<MatchMapPlayerStats>,
  bans: List<MatchBans>,
  videos: List<MatchVideos>,
  previousEncounters: List<PreviousEncounter>
): MatchDetails {
  val mapData = maps.map { map ->
    val mapRounds = rounds.filter { it.map_name == map.map_name }
    val mapPlayers = playerStats.filter { it.map_name == map.map_name }
    
    MapData(
      map = map.map_name,
      members = mapPlayers.groupBy { it.player_id }.map { (_, stats) ->
        stats.first().toPlayerStats(stats.map { it.toAgentInfo() })
      },
      teams = listOf(
      TeamDetails(
        id = match.team1_id.takeIf { it.isNotEmpty() },
        name = match.team1_name,
        region = "",
        img = match.team1_logo_url,
        score = map.team1_score?.toInt(),
        isWinner = run {
          val t1 = map.team1_score
          val t2 = map.team2_score
          t1 != null && t2 != null && t1 > t2
        },
        isFavorite = false
      ),
      TeamDetails(
        id = match.team2_id.takeIf { it.isNotEmpty() },
        name = match.team2_name,
        region = "",
        img = match.team2_logo_url,
        score = map.team2_score?.toInt(),
        isWinner = run {
          val t1 = map.team1_score
          val t2 = map.team2_score
          t2 != null && t1 != null && t2 > t1
        },
        isFavorite = false
      )
      ),
      rounds = mapRounds.map { it.toRoundInfo() }
    )
  }

  return MatchDetails(
    id = match.id,
    event = EventInfo(
      id = match.event_id ?: "",
      name = match.event_name,
      series = match.series,
      stage = match.stage,
      img = match.event_logo_url,
      date = match.time.takeIf { it.isNotEmpty() },
      patch = match.patch,
      status = match.status
    ),
    head2head = previousEncounters,
    note = match.note,
    score = buildScoreString(match.team1_score, match.team2_score),
    teams = listOf(
      TeamDetails(
        id = match.team1_id.takeIf { it.isNotEmpty() },
        name = match.team1_name,
        region = "",
        img = match.team1_logo_url,
        score = match.team1_score?.toInt(),
        isWinner = determineWinner(match.team1_score, match.team2_score, true),
        isFavorite = false
      ),
      TeamDetails(
        id = match.team2_id.takeIf { it.isNotEmpty() },
        name = match.team2_name,
        region = "",
        img = match.team2_logo_url,
        score = match.team2_score?.toInt(),
        isWinner = determineWinner(match.team1_score, match.team2_score, false),
        isFavorite = false
      )
    ),
    bans = bans.map { it.ban_value },
    videos = DomainMatchVideos(
      streams = videos.filter { it.video_type == "stream" }.map { it.toVideoReference() },
      vods = videos.filter { it.video_type == "vod" }.map { it.toVideoReference() }
    ),
    matchData = mapData,
    mapCount = match.map_count.toInt(),
    isFavorite = match.is_direct_favorite == 1L
  )
}

private fun buildScoreString(team1Score: Long?, team2Score: Long?): String =
  "${team1Score ?: 0} : ${team2Score ?: 0}"

private fun MatchMapPlayerStats.toPlayerStats(agents: List<AgentInfo>): PlayerStats =
  PlayerStats(
    playerId = player_id,
    name = player_name,
    team = team_id,
    acs = acs.toInt(),
    adr = adr?.toInt() ?: 0,
    kills = kills.toInt(),
    deaths = deaths.toInt(),
    assists = assists.toInt(),
    kast = kast_percent?.toInt() ?: 0,
    firstKills = first_kills.toInt(),
    firstDeaths = first_deaths.toInt(),
    firstKillsDiff = first_kills_diff.toInt(),
    hsPercent = hs_percent?.toInt() ?: 0,
    rating = rating.toFloat(),
    agents = agents
  )

private fun MatchMapPlayerStats.toAgentInfo(): AgentInfo =
  AgentInfo(
    name = agent_name,
    img = agent_image_url
  )

private fun MatchMapRounds.toRoundInfo(): RoundInfo =
  RoundInfo(
    roundNo = round_number.toInt(),
    score = round_score,
    winner = winner.toRoundWinner(),
    side = side.toRoundSide(),
    winType = win_type.toRoundWinType()
  )

private fun String.toRoundWinner(): RoundWinner = when (this.uppercase()) {
  "TEAM1" -> RoundWinner.TEAM1
  "TEAM2" -> RoundWinner.TEAM2
  else -> RoundWinner.NOT_PLAYED
}

private fun String.toRoundSide(): RoundSide = when (this.uppercase()) {
  "ATTACK" -> RoundSide.ATTACK
  "DEFENCE", "DEFENSE" -> RoundSide.DEFENCE
  else -> RoundSide.NOT_PLAYED
}

private fun String.toRoundWinType(): RoundWinType = when (this.uppercase()) {
  "ELIMINATION" -> RoundWinType.ELIMINATION
  "SPIKE_EXPLODED" -> RoundWinType.SPIKE_EXPLODED
  "DEFUSED" -> RoundWinType.DEFUSED
  "TIME_OUT", "TIMEOUT" -> RoundWinType.TIME_OUT
  else -> RoundWinType.NOT_PLAYED
}

private fun MatchVideos.toVideoReference(): VideoReference =
  VideoReference(
    name = name,
    url = url
  )
