package dev.unusedvariable.vlr.data.dao

import androidx.room.*
import dev.unusedvariable.vlr.data.model.CompletedMatch
import dev.unusedvariable.vlr.data.model.UpcomingMatch
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedMatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(matchData: List<CompletedMatch>)

    @Query("SELECT * from CompletedMatch")
    fun getAll(): Flow<List<CompletedMatch>>

    @Query("DELETE from CompletedMatch")
    fun deleteAll()

    @Transaction
    fun insertTransaction(list: List<CompletedMatch>) {
        deleteAll()
        insertAll(list)
    }
}