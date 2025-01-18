package dev.staticvar.vlr.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.staticvar.vlr.data.model.EventFav
import kotlinx.coroutines.flow.Flow

@Dao
interface EventFavDao {

  @Insert
  suspend fun addFavEvent(eventFav: EventFav)

  @Query("DELETE from EventFav where id = :id")
  suspend fun deleteFavEvent(id: String)

  @Query("SELECT * from EventFav")
  fun getFavoriteEvents(): Flow<List<EventFav>>
}