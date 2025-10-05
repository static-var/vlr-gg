package dev.staticvar.vlr.domain.model

/**
 * Domain model for detailed match information.
 */
data class MatchDetails(
  val id: String,
  val event: EventInfo,
  val head2head: List<PreviousEncounter>,
  val note: String,
  val score: String,
  val teams: List<TeamDetails>,
  val bans: List<String>,
  val videos: MatchVideos,
  val matchData: List<MapData>,
  val mapCount: Int,
  val isFavorite: Boolean = false,
)

data class EventInfo(
  val id: String,
  val name: String,
  val series: String,
  val stage: String,
  val img: String,
  val date: String?,
  val patch: String?,
  val status: String?,
)

data class TeamDetails(
  val id: String?,
  val name: String,
  val region: String,
  val img: String,
  val score: Int?,
  val isWinner: Boolean?,
  val isFavorite: Boolean = false,
)

data class PreviousEncounter(
  val id: String,
  val teams: List<TeamPreview>,
)

data class MatchVideos(
  val streams: List<VideoReference>,
  val vods: List<VideoReference>,
)

data class VideoReference(
  val name: String,
  val url: String,
)

data class MapData(
  val map: String,
  val members: List<PlayerStats>,
  val teams: List<TeamDetails>,
  val rounds: List<RoundInfo>,
)

data class PlayerStats(
  val playerId: String,
  val name: String,
  val team: String,
  val acs: Int,
  val adr: Int,
  val kills: Int,
  val deaths: Int,
  val assists: Int,
  val kast: Int,
  val firstKills: Int,
  val firstDeaths: Int,
  val firstKillsDiff: Int,
  val hsPercent: Int,
  val rating: Float,
  val agents: List<AgentInfo>,
)

data class AgentInfo(
  val name: String,
  val img: String,
)

data class RoundInfo(
  val roundNo: Int,
  val score: String,
  val winner: RoundWinner,
  val side: RoundSide,
  val winType: RoundWinType,
)

enum class RoundWinner {
  TEAM1,
  TEAM2,
  NOT_PLAYED
}

enum class RoundSide {
  ATTACK,
  DEFENCE,
  NOT_PLAYED
}

enum class RoundWinType {
  ELIMINATION,
  SPIKE_EXPLODED,
  DEFUSED,
  TIME_OUT,
  NOT_PLAYED
}
