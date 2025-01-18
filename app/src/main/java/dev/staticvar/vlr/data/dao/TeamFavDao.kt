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

  fun getFavoriteTeams(): Flow<List<TeamFav>>
}