package dev.unusedvariable.vlr.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.unusedvariable.vlr.data.model.MatchDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDetailsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(matchDetail: List<MatchDetails>)

    @Query("SELECT * from MatchDetails where matchId = :id")
    fun getMatch(id: String): Flow<List<MatchDetails>>

}