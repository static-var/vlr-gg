package dev.staticvar.vlr.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity
data class EventFav(
  @PrimaryKey
  val id: String = "",
)