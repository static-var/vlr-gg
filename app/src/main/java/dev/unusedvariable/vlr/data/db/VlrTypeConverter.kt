package dev.unusedvariable.vlr.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dev.unusedvariable.vlr.data.Status
import dev.unusedvariable.vlr.data.model.MapData
import javax.inject.Inject
import javax.inject.Singleton

@ProvidedTypeConverter
@Singleton
class VlrTypeConverter @Inject constructor(private val moshi: Moshi) {

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
}