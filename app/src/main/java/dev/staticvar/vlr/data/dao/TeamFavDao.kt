package dev.staticvar.vlr.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.staticvar.vlr.data.model.TeamFav
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamFavDao {

  @Insert(entity = TeamFav::class)
  suspend fun addFavTeam(teamFav: TeamFav)

  @Query("DELETE from TeamFav where id = :id")
  suspend fun deleteFavTeam(id: String)

  @Query("SELECT EXISTS(SELECT 1 FROM TeamFav WHERE id = :id)")
  suspend fun isFavorite(id: String): Boolean

  @Query("SELECT * from TeamFav")
  fun getFavoriteTeams(): Flow<List<TeamFav>>
}