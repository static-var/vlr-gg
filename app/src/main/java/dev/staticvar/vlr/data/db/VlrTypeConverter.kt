package dev.staticvar.vlr.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import dev.staticvar.vlr.data.api.response.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
@Singleton
class VlrTypeConverter @Inject constructor(private val json: Json) {

  @TypeConverter
  fun eventToString(data: Event): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToEvent(data: String): Event {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun teamToString(data: Team?): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToTeam(data: String): Team? {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun matchDetailDataToString(data: List<MatchInfo.MatchDetailData>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToMatchDetailDataToTeam(data: String): List<MatchInfo.MatchDetailData> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun head2headToString(data: List<MatchInfo.Head2head>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToHead2head(data: String): List<MatchInfo.Head2head> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun listOfTeamsToString(data: List<Team>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToListOfTeams(data: String): List<Team> {
    return json.decodeFromString(data)
  }
  
  @TypeConverter
  fun listOfGamesToString(data: List<TournamentDetails.Games>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToListOfGames(data: String): List<TournamentDetails.Games> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun listOfParticipantToString(data: List<TournamentDetails.Participant>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToListOfParticipant(data: String): List<TournamentDetails.Participant> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun listOfPrizeToString(data: List<TournamentDetails.Prize>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToListOfPrize(data: String): List<TournamentDetails.Prize> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun listOfStringToString(data: List<String>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToListOfString(data: String): List<String> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun videosToString(data: MatchInfo.Videos): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToVideos(data: String): MatchInfo.Videos {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun rosterToString(data: List<TeamDetails.Roster>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToRoster(data: String): List<TeamDetails.Roster> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun gamesToString(data: List<TeamDetails.Games>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToGames(data: String): List<TeamDetails.Games> {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun agentToString(data: List<PlayerData.Agent>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToAgent(data: String): List<PlayerData.Agent> {
    return json.decodeFromString(data)
  }
}
