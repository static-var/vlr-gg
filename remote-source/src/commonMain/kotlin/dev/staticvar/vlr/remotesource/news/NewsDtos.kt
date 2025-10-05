package dev.staticvar.vlr.remotesource.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsItemDto(
  @SerialName("url") val url: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("description") val description: String = "",
  @SerialName("date") val date: String = "",
  @SerialName("author") val author: String = "",
)

@Serializable
data class NewsArticleDto(
  @SerialName("id") val id: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("content") val content: String = "",
  @SerialName("links") val links: List<Map<String, String>> = emptyList(),
  @SerialName("images") val images: List<String> = emptyList(),
  @SerialName("videos") val videos: List<String> = emptyList(),
  @SerialName("date") val date: String? = null,
  @SerialName("author") val author: String = "",
)
