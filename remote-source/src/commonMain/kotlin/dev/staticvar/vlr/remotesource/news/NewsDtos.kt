/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
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
  @SerialName("blocks") val blocks: List<ArticleBlockDto> = emptyList(),
  @SerialName("links") val links: List<Map<String, String>> = emptyList(),
  @SerialName("images") val images: List<String> = emptyList(),
  @SerialName("videos") val videos: List<String> = emptyList(),
  @SerialName("date") val date: String? = null,
  @SerialName("author") val author: String = "",
)

@Serializable
data class ArticleBlockDto(
  val type: String,
  val runs: List<ArticleTextRunDto> = emptyList(),
  val children: List<ArticleBlockDto> = emptyList(),
  val level: Int? = null,
  val ordered: Boolean = false,
  val start: Int = 1,
  val url: String? = null,
  val alt: String? = null,
)

@Serializable
data class ArticleTextRunDto(
  val text: String,
  val url: String? = null,
  val bold: Boolean = false,
  val italic: Boolean = false,
)
