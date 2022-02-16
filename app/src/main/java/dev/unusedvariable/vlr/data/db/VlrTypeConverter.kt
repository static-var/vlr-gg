package dev.unusedvariable.vlr.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dev.unusedvariable.vlr.data.Status
import dev.unusedvariable.vlr.data.api.response.Event
import dev.unusedvariable.vlr.data.api.response.MatchInfo.Head2head
import dev.unusedvariable.vlr.data.api.response.MatchInfo.MatchDetailData
import dev.unusedvariable.vlr.data.api.response.Team
import dev.unusedvariable.vlr.data.api.response.TournamentDetails
import dev.unusedvariable.vlr.data.model.MapData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@ProvidedTypeConverter
@Singleton
class VlrTypeConverter @Inject constructor(private val moshi: Moshi, private val json: Json) {

    private val statusAdapter by lazy { moshi.adapter(Status::class.java) }
    private val mapDataAdapter by lazy {
        moshi.adapter<List<MapData>>(
            Types.newParameterizedType(
                List::class.java, MapData::class.java
            )
        )
    }

    @TypeConverter
    fun pairStringToString(pairs: List<Pair<String, String>>?): String? {
        return pairs?.joinToString("|||") { "${it.first}---${it.second}" }
    }

    @TypeConverter
    fun stringToPairString(data: String?): List<Pair<String, String>>? {
        return if (data.isNullOrEmpty())
            null
        else
            data.split("|||").map {
                Pair(it.split("---")[0], it.split("---")[1])
            }
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
    fun statusToString(status: Status): String {
        return statusAdapter.toJson(status)
    }

    @TypeConverter
    fun stringToStatus(status: String): Status? {
        return statusAdapter.fromJson(status)
    }

    @TypeConverter
    fun mapDataListToString(mapData: List<MapData>?): String? {
        return mapDataAdapter.toJson(mapData)
    }

    @TypeConverter
    fun stringToMapDataList(data: String?): List<MapData>? {
        return data?.let { mapDataAdapter.fromJson(it) }
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
    fun matchDetailDataToString(data: List<MatchDetailData>): String {
        return json.encodeToString(data)
    }

    @TypeConverter
    fun stringToMatchDetailDataToTeam(data: String): List<MatchDetailData> {
        return json.decodeFromString(data)
    }

    @TypeConverter
    fun head2headToString(data: List<Head2head>): String {
        return json.encodeToString(data)
    }

    @TypeConverter
    fun stringToHead2head(data: String): List<Head2head> {
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
}