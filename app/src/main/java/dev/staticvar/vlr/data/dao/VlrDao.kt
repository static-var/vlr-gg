package dev.staticvar.vlr.data.dao

import androidx.room.*
import dev.staticvar.vlr.data.api.response.*
import dev.staticvar.vlr.data.model.TopicTracker
import kotlinx.coroutines.flow.Flow

/** Vlr dao One DAO to rule them all */
@Dao
interface VlrDao {

  // -------------- DAO calls for [NewsResponseItem] start here --------------//
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllNews(news: List<NewsResponseItem>)

  @Query("SELECT * from NewsResponseItem") fun getNews(): Flow<List<NewsResponseItem>>

  @Query("DELETE from NewsResponseItem") fun deleteAllNews()

  @Transaction
  suspend fun deleteAndInsertNews(items: List<NewsResponseItem>) {
    deleteAllNews()
    insertAllNews(items)
  }
  // -------------- DAO calls for [NewsResponseItem] ends here --------------//

  // -------------- DAO calls for [MatchPreviewInfo] start here --------------//
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllMatches(matches: List<MatchPreviewInfo>)

  @Query("SELECT * from MatchPreviewInfo") fun getAllMatchesPreview(): Flow<List<MatchPreviewInfo>>

  @Query("SELECT * from MatchPreviewInfo") fun getAllMatchesPreviewNoFlow(): List<MatchPreviewInfo>

  @Query("DELETE from MatchPreviewInfo") fun deleteAllMatchPreview()

  @Transaction
  suspend fun deleteAndInsertMatchPreviewInfo(items: List<MatchPreviewInfo>) {
    deleteAllMatchPreview()
    insertAllMatches(items)
  }
  // -------------- DAO calls for [MatchPreviewInfo] ends here --------------//

  // -------------- DAO calls for [TournamentPreview] start here --------------//
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllTournamentInfo(tournaments: List<TournamentPreview>)

  @Query("SELECT * from TournamentPreview") fun getTournaments(): Flow<List<TournamentPreview>>

  @Query("DELETE from TournamentPreview") fun deleteAllTournamentPreview()

  @Transaction
  suspend fun deleteAndInsertTournamentPreview(items: List<TournamentPreview>) {
    deleteAllTournamentPreview()
    insertAllTournamentInfo(items)
  }
  // -------------- DAO calls for [TournamentPreview] ends here --------------//

  // -------------- DAO calls for [TournamentDetails] starts here --------------//
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTournamentDetails(match: TournamentDetails)

  @Query("SELECT * from TournamentDetails where id = :id")
  fun getTournamentById(id: String): Flow<TournamentDetails?>
  // -------------- DAO calls for [TournamentDetails] ends here --------------//

  // -------------- DAO calls for [MatchInfo] starts here --------------//
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMatchInfo(match: MatchInfo)

  @Query("SELECT * from MatchInfo where id = :id") fun getMatchById(id: String): Flow<MatchInfo?>
  // -------------- DAO calls for [MatchInfo] ends here --------------//

  // -------------- DAO calls for [TopicTracker] ends here --------------//
  @Query("SELECT EXISTS(SELECT * from TopicTracker where topic = :topic)")
  fun isTopicSubscribed(topic: String): Flow<Boolean>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTopicTracker(topicTracker: TopicTracker)

  @Query("DELETE from TopicTracker where topic = :topic") suspend fun deleteTopic(topic: String)
  // -------------- DAO calls for [TopicTracker] ends here --------------//

}
