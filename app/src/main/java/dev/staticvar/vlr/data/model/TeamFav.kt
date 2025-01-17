package dev.staticvar.vlr.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity
data class TeamFav(
  @PrimaryKey
  val id: String = "",
)
