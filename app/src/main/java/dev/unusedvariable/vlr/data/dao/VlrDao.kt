package dev.unusedvariable.vlr.data.dao

import androidx.room.*
import dev.unusedvariable.vlr.data.api.response.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VlrDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMatches(matches: List<MatchPreviewInfo>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllNews(news: List<NewsResponseItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTournamentInfo(tournaments: List<TournamentPreview>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentDetails(match: TournamentDetails)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchInfo(match: MatchInfo)


    @Query("SELECT * from NewsResponseItem")
    fun getNews(): Flow<List<NewsResponseItem>>

    @Query("SELECT * from MatchPreviewInfo")
    fun getAllMatchesPreview(): Flow<List<MatchPreviewInfo>>

    @Query("SELECT * from MatchPreviewInfo")
    fun getAllMatchesPreviewNoFlow(): List<MatchPreviewInfo>

    @Query("SELECT * from TournamentPreview")
    fun getTournaments(): Flow<List<TournamentPreview>>

    @Query("SELECT * from MatchInfo where id = :id")
    fun getMatchById(id: String): Flow<MatchInfo>

    @Query("SELECT * from TournamentDetails where id = :id")
    fun getTournamentById(id: String): Flow<TournamentDetails>


    @Query("DELETE from MatchPreviewInfo")
    fun deleteAllMatchPreview()

    @Query("DELETE from TournamentPreview")
    fun deleteAllTournamentPreview()

}