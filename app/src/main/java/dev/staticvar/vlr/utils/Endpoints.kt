package dev.staticvar.vlr.utils

object Endpoints {
  const val NEWS = "api/v1/news/"
  const val MATCHES_OVERVIEW = "api/v1/matches/"
  const val EVENTS_OVERVIEW = "api/v1/events/"
  const val RANK_OVERVIEW = "api/v1/rankings/"
  private const val TEAM_DETAILS = "api/v1/team/"
  private const val EVENT_DETAILS = "api/v1/events/"
  private const val MATCH_DETAILS = "api/v1/matches/"
  private const val PLAYER_DETAILS = "/api/v1/player/"

  fun teamDetails(id: String) = TEAM_DETAILS + id
  fun eventDetails(id: String) = EVENT_DETAILS + id
  fun matchDetails(id: String) = MATCH_DETAILS + id
  fun playerDetails(id: String) = PLAYER_DETAILS + id
}
