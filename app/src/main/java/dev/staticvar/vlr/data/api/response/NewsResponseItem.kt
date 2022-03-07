package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Entity
data class NewsResponseItem(
  @SerialName("author") val author: String = "", // Eutalyx
  @SerialName("date") val date: String = "", // January 29, 2022
  @SerialName("description")
  val description: String =
    "", // BDS are going international in 2022 after several months of unsuccessful results.
  @SerialName("url") @PrimaryKey val link: String = "", // /67532/report-bds-to-overhaul-roster
  @SerialName("title") val title: String = "" // Report: BDS to overhaul roster
)
