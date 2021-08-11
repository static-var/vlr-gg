package dev.unusedvariable.vlr.data.dao

import androidx.room.*
import dev.unusedvariable.vlr.data.model.UpcomingMatch
import kotlinx.coroutines.flow.Flow

@Dao
interface UpcomingMatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(matchData: List<UpcomingMatch>)

    @Query("SELECT * from UpcomingMatch")
    fun getAll(): Flow<List<UpcomingMatch>>

    @Query("DELETE from UpcomingMatch")
    fun deleteAll()

    @Transaction
    fun insertTransaction(list: List<UpcomingMatch>) {
        deleteAll()
        insertAll(list)
    }
}