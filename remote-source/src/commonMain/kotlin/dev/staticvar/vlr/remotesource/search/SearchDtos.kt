package dev.staticvar.vlr.remotesource.search

import dev.staticvar.vlr.remotesource.common.SearchCategory
import dev.staticvar.vlr.remotesource.common.SearchCategoryNullableSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResultDto(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("category") @Serializable(with = SearchCategoryNullableSerializer::class) val category: SearchCategory? = null,
  @SerialName("description") val description: String? = null,
)
