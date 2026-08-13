package dev.staticvar.vlr.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.staticvar.vlr.data.model.MatchFav
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchFavDao {

  @Insert
  suspend fun addFavMatch(matchFav: MatchFav)

  @Insert
  suspend fun addFavMatches(favMatches: List<MatchFav>)

  @Query("DELETE from MatchFav where id = :id")
  suspend fun deleteFavMatch(id: String)

  @Query("DELETE from MatchFav where id in (:ids)")
  suspend fun deleteFavMatches(ids: List<String>)

  @Query("SELECT EXISTS(SELECT 1 FROM MatchFav WHERE id = :id)")
  suspend fun isFavorite(id: String): Boolean

  @Query("SELECT * from MatchFav")
  fun getFavoriteMatches(): Flow<List<MatchFav>>
}