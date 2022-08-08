package dev.staticvar.vlr.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import dev.staticvar.vlr.data.api.response.Event
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.data.api.response.Team
import dev.staticvar.vlr.data.api.response.TournamentDetails
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@ProvidedTypeConverter
@Singleton
class VlrTypeConverter @Inject constructor(private val json: Json) {

  @TypeConverter
  fun pairStringToString(pairs: List<Pair<String, String>>?): String? {
    return pairs?.joinToString("|||") { "${it.first}---${it.second}" }
  }

  @TypeConverter
  fun stringToPairString(data: String?): List<Pair<String, String>>? {
    return if (data.isNullOrEmpty()) null
    else data.split("|||").map { Pair(it.split("---")[0], it.split("---")[1]) }
  }

  @TypeConverter
  fun pairToString(pair: Pair<String, String>?): String? {
    return pair?.let { "${pair.first}---${pair.second}" }
  }

  @TypeConverter
  fun stringToPair(data: String?): Pair<String, String>? {
    return data?.let { Pair(it.split("---")[0], it.split("---")[1]) }
  }

  @TypeConverter
  fun eventToString(data: Event): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToEvent(data: String): Event {
    return json.decodeFromString(data)
  }

  @TypeConverter
  fun teamToString(data: Team): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToTeam(data: String): Team {
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
  fun listOfBracketToString(data: List<TournamentDetails.Bracket>): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToListOfBracket(data: String): List<TournamentDetails.Bracket> {
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
  fun roundsToString(data: MatchInfo.MatchDetailData.Rounds): String {
    return json.encodeToString(data)
  }

  @TypeConverter
  fun stringToRounds(data: String): MatchInfo.MatchDetailData.Rounds {
    return json.decodeFromString(data)
  }
}
