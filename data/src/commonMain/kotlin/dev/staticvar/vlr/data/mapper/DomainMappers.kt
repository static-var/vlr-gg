/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.data.EventMatches
import dev.staticvar.vlr.data.EventPrizes
import dev.staticvar.vlr.data.EventStandings
import dev.staticvar.vlr.data.EventTeams
import dev.staticvar.vlr.data.MatchBans
import dev.staticvar.vlr.data.MatchMapPlayerStats
import dev.staticvar.vlr.data.MatchMapRounds
import dev.staticvar.vlr.data.MatchMaps
import dev.staticvar.vlr.data.MatchVideos
import dev.staticvar.vlr.data.News
import dev.staticvar.vlr.data.NewsMedia
import dev.staticvar.vlr.data.PlayerAgentStats
import dev.staticvar.vlr.data.PlayerTeamHistory
import dev.staticvar.vlr.data.Players
import dev.staticvar.vlr.data.Rankings
import dev.staticvar.vlr.data.Standings
import dev.staticvar.vlr.data.Teams
import dev.staticvar.vlr.domain.model.AgentInfo
import dev.staticvar.vlr.domain.model.ArticleLink
import dev.staticvar.vlr.domain.model.CircuitRegion
import dev.staticvar.vlr.domain.model.CircuitStandings
import dev.staticvar.vlr.domain.model.CircuitTeam
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventPrizeTeam
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.EventTeam
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.domain.model.PlayerAgentStat
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.PlayerTeam
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.model.RoundInfo
import dev.staticvar.vlr.domain.model.RoundSide
import dev.staticvar.vlr.domain.model.RoundWinType
import dev.staticvar.vlr.domain.model.RoundWinner
import dev.staticvar.vlr.domain.model.TeamCompletedMatch
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.model.TeamPlayer
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.model.TeamUpcomingMatch
import dev.staticvar.vlr.domain.model.VideoReference
import dev.staticvar.vlr.localsource.database.Events
import dev.staticvar.vlr.localsource.database.GetEventWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetEventsWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetMatchWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetMatchesWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetPlayerWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetPlayersByTeam
import dev.staticvar.vlr.localsource.database.GetTeamWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetTeamsByRegion
import dev.staticvar.vlr.localsource.database.GetTeamsWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.Team_completed_matches
import dev.staticvar.vlr.localsource.database.Team_roster
import dev.staticvar.vlr.localsource.database.Team_upcoming_matches
import dev.staticvar.vlr.domain.model.MatchVideos as DomainMatchVideos

/**
 * Maps database query result to domain MatchPreview model.
 */
internal fun GetMatchesWithFavoriteStatus.toDomain(): MatchPreview = MatchPreview(
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
    isFavorite = false, // Team favorites handled separately
  ),
  team2 = TeamPreview(
    id = team2_id.takeIf { it.isNotEmpty() },
    name = team2_name,
    region = "", // Not available in matches table
    img = team2_logo_url,
    score = team2_score?.toInt(),
    isWinner = determineWinner(team1_score, team2_score, false),
    isFavorite = false,
  ),
  time = time.takeIf { it.isNotEmpty() },
  eventId = event_id ?: "",
  isFavorite = is_direct_favorite == 1L,
)

/**
 * Determines if a team is the winner based on scores.
 */
private fun determineWinner(team1Score: Long?, team2Score: Long?, isTeam1: Boolean): Boolean? {
  if (team1Score == null || team2Score == null) return null
  return if (isTeam1) team1Score > team2Score else team2Score > team1Score
}

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
  previousEncounters: List<PreviousEncounter>,
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
          isFavorite = false,
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
          isFavorite = false,
        ),
      ),
      rounds = mapRounds.map { it.toRoundInfo() },
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
      status = match.status,
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
        isFavorite = false,
      ),
      TeamDetails(
        id = match.team2_id.takeIf { it.isNotEmpty() },
        name = match.team2_name,
        region = "",
        img = match.team2_logo_url,
        score = match.team2_score?.toInt(),
        isWinner = determineWinner(match.team1_score, match.team2_score, false),
        isFavorite = false,
      ),
    ),
    bans = bans.map { it.ban_value },
    videos = DomainMatchVideos(
      streams = videos.filter { it.video_type == "stream" }.map { it.toVideoReference() },
      vods = videos.filter { it.video_type == "vod" }.map { it.toVideoReference() },
    ),
    matchData = mapData,
    mapCount = match.map_count.toInt(),
    isFavorite = match.is_direct_favorite == 1L,
  )
}

private fun buildScoreString(team1Score: Long?, team2Score: Long?): String = "${team1Score ?: 0} : ${team2Score ?: 0}"

private fun MatchMapPlayerStats.toPlayerStats(agents: List<AgentInfo>): PlayerStats = PlayerStats(
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
  agents = agents,
)

private fun MatchMapPlayerStats.toAgentInfo(): AgentInfo = AgentInfo(
  name = agent_name,
  img = agent_image_url,
)

private fun MatchMapRounds.toRoundInfo(): RoundInfo = RoundInfo(
  roundNo = round_number.toInt(),
  score = round_score,
  winner = winner.toRoundWinner(),
  side = side.toRoundSide(),
  winType = win_type.toRoundWinType(),
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

private fun MatchVideos.toVideoReference(): VideoReference = VideoReference(
  name = name,
  url = url,
)

// ============================================================================
// NEWS MAPPERS
// ============================================================================

/**
 * Maps database News entity to domain NewsItem model.
 */
internal fun News.toNewsItem(): NewsItem = NewsItem(
  id = id,
  url = url,
  title = title,
  description = description ?: "",
  date = date,
  author = author,
  coverUrl = cover_url,
)

/**
 * Aggregates News entity with its media to create domain NewsArticle model.
 */
internal fun aggregateNewsArticle(news: News, media: List<NewsMedia>): NewsArticle {
  // API placeholders address each media array by index, including empty entries.
  val orderedMedia = media.sortedBy { it.id }
  val links = orderedMedia
    .filter { it.media_type == "link" }
    .map { mediaItem ->
      ArticleLink(text = mediaItem.media_text.orEmpty(), url = mediaItem.media_value)
    }

  val images = orderedMedia
    .filter { it.media_type == "image" }
    .map { it.media_value }

  val videos = orderedMedia
    .filter { it.media_type == "video" }
    .map { it.media_value }

  return NewsArticle(
    id = news.id,
    url = news.url,
    title = news.title,
    author = news.author,
    date = news.date,
    coverUrl = news.cover_url,
    contentHtml = news.content_html ?: "",
    blocks = decodeArticleBlocks(orderedMedia.filter { it.media_type == "block" }.map { it.media_value }),
    media = NewsArticleMedia(
      links = links,
      images = images,
      videos = videos,
    ),
  )
}

// ============================================================================
// TEAM MAPPERS
// ============================================================================

/**
 * Aggregates Team entity with roster and matches to create domain TeamInfo model.
 */
internal fun aggregateTeamInfo(
  team: GetTeamWithFavoriteStatus,
  roster: List<Team_roster>,
  upcomingMatches: List<Team_upcoming_matches>,
  completedMatches: List<Team_completed_matches>,
): TeamInfo = TeamInfo(
  id = team.id,
  name = team.name,
  tag = team.tag,
  logoUrl = team.logo_url,
  region = team.region ?: "",
  country = team.country,
  rank = team.rank.toInt(),
  website = team.website,
  twitter = team.twitter,
  roster = roster.map { it.toTeamPlayer() },
  upcomingMatches = upcomingMatches.map { it.toDomain() },
  completedMatches = completedMatches.map { it.toDomain() },
  isFavorite = team.is_favorite == 1L,
)

internal fun GetTeamsWithFavoriteStatus.toTeamPreview(
  roster: List<Team_roster>,
  upcomingMatches: List<Team_upcoming_matches>,
  completedMatches: List<Team_completed_matches>,
): TeamInfo = aggregateTeamInfo(
  team = GetTeamWithFavoriteStatus(
    id = id,
    name = name,
    tag = tag,
    logo_url = logo_url,
    region = region,
    country = country,
    roster_url = roster_url,
    earnings = earnings,
    rank = rank,
    website = website,
    twitter = twitter,
    last_updated = last_updated,
    is_favorite = is_favorite,
  ),
  roster = roster,
  upcomingMatches = upcomingMatches,
  completedMatches = completedMatches,
)

internal fun GetTeamsByRegion.toTeamPreview(
  roster: List<Team_roster>,
  upcomingMatches: List<Team_upcoming_matches>,
  completedMatches: List<Team_completed_matches>,
): TeamInfo = aggregateTeamInfo(
  team = GetTeamWithFavoriteStatus(
    id = id,
    name = name,
    tag = tag,
    logo_url = logo_url,
    region = region,
    country = country,
    roster_url = roster_url,
    earnings = earnings,
    rank = rank,
    website = website,
    twitter = twitter,
    last_updated = last_updated,
    is_favorite = is_favorite,
  ),
  roster = roster,
  upcomingMatches = upcomingMatches,
  completedMatches = completedMatches,
)

private fun Team_roster.toTeamPlayer(): TeamPlayer = TeamPlayer(
  id = player_id,
  name = player_name,
  alias = player_alias.ifBlank { player_name },
  role = role,
  imageUrl = player_image_url,
  country = player_country,
  isStandIn = is_stand_in == 1L,
  isCoach = is_coach == 1L,
  isCurrent = is_current == 1L,
)

private fun Team_upcoming_matches.toDomain(): TeamUpcomingMatch = TeamUpcomingMatch(
  matchId = match_id,
  eventName = event_name,
  eventLogoUrl = event_logo_url,
  eventId = event_id,
  stage = stage,
  opponent = opponent_team_name,
  opponentLogoUrl = opponent_team_logo_url,
  date = date,
  eta = eta,
)

private fun Team_completed_matches.toDomain(): TeamCompletedMatch = TeamCompletedMatch(
  matchId = match_id,
  eventName = event_name,
  eventLogoUrl = event_logo_url,
  eventId = event_id,
  stage = stage,
  opponent = opponent_team_name,
  opponentLogoUrl = opponent_team_logo_url,
  date = date,
  result = result,
)

// ============================================================================
// PLAYER MAPPERS
// ============================================================================

/**
 * Aggregates Player entity with agent stats and team history to create domain PlayerInfo model.
 */
internal fun aggregatePlayerInfo(
  player: GetPlayerWithFavoriteStatus,
  agentStats: List<PlayerAgentStats>,
  teamHistory: List<PlayerTeamHistory>,
): PlayerInfo {
  val currentTeam = teamHistory.firstOrNull { it.is_current == 1L }
  val pastTeams = teamHistory.filter { it.is_current == 0L }

  return PlayerInfo(
    id = player.id,
    name = player.name,
    alias = player.alias,
    realName = player.real_name,
    country = player.country,
    imageUrl = player.image_url ?: "",
    twitterUrl = player.twitter_url,
    twitchUrl = player.twitch_url,
    totalWinnings = player.total_winnings,
    currentTeam = currentTeam?.toPlayerTeam(),
    pastTeams = pastTeams.map { it.toPlayerTeam() },
    agentStats = agentStats.map { it.toDomain() },
    isFavorite = player.is_favorite == 1L,
  )
}

private fun PlayerTeamHistory.toPlayerTeam(): PlayerTeam = PlayerTeam(
  id = team_id,
  name = team_name,
  logoUrl = team_logo_url,
  isCurrent = is_current == 1L,
)

private fun PlayerAgentStats.toDomain(): PlayerAgentStat = PlayerAgentStat(
  agentName = agent_name,
  agentImageUrl = agent_image_url,
  usageCount = usage_count.toInt(),
  usagePercent = usage_percent,
  roundsPlayed = rounds_played.toInt(),
  rating = rating,
  acs = acs,
  kdRatio = kd_ratio,
  adr = adr,
  kast = kast,
  kpr = kpr,
  apr = apr,
  fkpr = fkpr,
  fdpr = fdpr,
  kills = kills.toInt(),
  deaths = deaths.toInt(),
  assists = assists.toInt(),
  firstKills = first_kills.toInt(),
  firstDeaths = first_deaths.toInt(),
)

internal fun GetPlayersByTeam.toPlayerInfo(
  agentStats: List<PlayerAgentStats>,
  teamHistory: List<PlayerTeamHistory>,
): PlayerInfo = aggregatePlayerInfo(
  player = GetPlayerWithFavoriteStatus(
    id = id,
    name = name,
    alias = alias,
    real_name = real_name,
    country = country,
    current_team_id = current_team_id,
    image_url = image_url,
    twitter_url = twitter_url,
    twitch_url = twitch_url,
    total_winnings = total_winnings,
    last_updated = last_updated,
    is_favorite = is_favorite,
  ),
  agentStats = agentStats,
  teamHistory = teamHistory,
)

// ============================================================================
// RANKINGS MAPPERS
// ============================================================================

/**
 * Groups rankings by region to create domain RegionalRanking model.
 */
internal fun List<Rankings>.toRegionalRankings(): List<RegionalRanking> = groupBy { it.region }
  .map { (region, rankings) ->
    RegionalRanking(
      region = region,
      teams = rankings.map { it.toTeamRanking() },
    )
  }

private fun Rankings.toTeamRanking(): TeamRanking = TeamRanking(
  teamId = team_id,
  teamName = team_name,
  teamLogo = team_logo,
  country = country,
  rank = rank.toInt(),
  points = points,
)

// ============================================================================
// EVENT MAPPERS
// ============================================================================

/**
 * Maps database Events entity to domain EventPreview model.
 */
internal fun GetEventsWithFavoriteStatus.toEventPreview(): EventPreview = EventPreview(
  id = id,
  title = name,
  status = status.toEventStatus(),
  prize = prizes,
  dates = dates,
  region = region ?: "",
  logoUrl = logo_url,
  isFavorite = is_favorite == 1L,
)

/**
 * Aggregates Events entity with child tables to create domain EventDetails model.
 */
internal fun aggregateEventDetails(
  event: GetEventWithFavoriteStatus,
  prizes: List<EventPrizes>,
  teams: List<EventTeams>,
  standings: List<EventStandings>,
  matches: List<EventMatches>,
): EventDetails = EventDetails(
  id = event.id,
  title = event.name,
  subtitle = event.subtitle,
  status = event.status.toEventStatus(),
  prize = event.prizes,
  dates = event.dates,
  region = event.region ?: "",
  logoUrl = event.logo_url,
  prizes = prizes.map { it.toDomain() },
  teams = teams.map { it.toDomain() },
  matches = matches.map { it.toDomain() },
  standings = standings.map { it.toDomain() },
  isFavorite = event.is_favorite == 1L,
)

private fun String?.toEventStatus(): EventStatus = when (this?.uppercase()) {
  "UPCOMING" -> EventStatus.UPCOMING
  "ONGOING" -> EventStatus.ONGOING
  "PAUSED" -> EventStatus.PAUSED
  "COMPLETED" -> EventStatus.COMPLETED
  else -> EventStatus.UNKNOWN
}

private fun EventPrizes.toDomain(): EventPrize = EventPrize(
  position = position,
  prize = prize,
  team = if (team_name.isNotEmpty()) {
    EventPrizeTeam(
      id = team_id,
      name = team_name,
      logoUrl = team_logo_url,
      country = team_country,
    )
  } else {
    null
  },
)

private fun EventTeams.toDomain(): EventTeam = EventTeam(
  id = team_id,
  name = team_name,
  logoUrl = team_logo_url,
  seed = seed,
)

private fun EventStandings.toDomain(): EventStanding = EventStanding(
  teamName = team_name,
  teamLogoUrl = team_logo_url,
  teamCountry = team_country,
  groupName = group_name,
  wins = wins.toInt(),
  losses = losses.toInt(),
  ties = ties.toInt(),
  mapDifference = map_difference.toInt(),
  roundDifference = round_difference.toInt(),
  roundDelta = round_delta.toInt(),
)

private fun EventMatches.toDomain(): EventMatch = EventMatch(
  matchId = match_id,
  time = time,
  date = date,
  eta = eta,
  status = status,
  teams = listOfNotNull(
    team1_name.toEventMatchTeam(team1_region, team1_score),
    team2_name.toEventMatchTeam(team2_region, team2_score),
  ),
  round = round,
  stage = stage,
)

private fun String.toEventMatchTeam(region: String, score: Long?): EventMatchTeam? = takeIf { it.isNotBlank() }?.let {
  EventMatchTeam(
    name = it,
    region = region,
    score = score?.toInt(),
  )
}

// ============================================================================
// CIRCUIT STANDINGS MAPPERS
// ============================================================================

/**
 * Aggregates Standings entities by year to create CircuitStandings domain model.
 */
internal fun aggregateCircuitStandings(year: Int, standings: List<Standings>): CircuitStandings {
  val circuits = standings
    .groupBy { it.circuit }
    .map { (circuitName, teams) ->
      CircuitRegion(
        circuitName = circuitName,
        region = teams.firstOrNull()?.region ?: "",
        teams = teams.map { it.toCircuitTeam() },
      )
    }

  return CircuitStandings(
    year = year,
    circuits = circuits,
  )
}

/**
 * Maps Standings entity to CircuitRegion domain model for a specific circuit.
 */
internal fun aggregateCircuitRegion(circuitName: String, standings: List<Standings>): CircuitRegion? {
  if (standings.isEmpty()) return null

  return CircuitRegion(
    circuitName = circuitName,
    region = standings.firstOrNull()?.region ?: "",
    teams = standings.map { it.toCircuitTeam() },
  )
}

private fun Standings.toCircuitTeam(): CircuitTeam = CircuitTeam(
  id = team_id,
  name = team_name,
  logo = team_logo,
  rank = rank.toInt(),
  points = points.toInt(),
  country = country,
)
