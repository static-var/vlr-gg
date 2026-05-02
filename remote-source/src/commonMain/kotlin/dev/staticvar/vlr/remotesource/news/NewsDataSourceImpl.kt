/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.news

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class NewsDataSourceImpl(private val client: HttpClient) : NewsDataSource {
  override suspend fun list(): Result<List<NewsItemDto>> = runCatching {
    client.get(ApiPaths.NEWS).body()
  }

  override suspend fun article(id: String): Result<NewsArticleDto> = runCatching {
    val apiDto: NewsArticleDto? =
      runCatching { client.get(ApiPaths.news(id)).body<NewsArticleDto>() }.getOrNull()
    if (apiDto != null && apiDto.content.isNotBlank()) {
      apiDto
    } else {
      val html: String = client.get(vlrArticleUrl(id)).body()
      NewsHtmlParser.parse(
        articleId = normalizeArticleId(id),
        html = html,
        fallback = apiDto,
      )
    }
  }
}

private fun normalizeArticleId(id: String): String {
  val trimmed = id.trim()
  return when {
    trimmed.startsWith("https://www.vlr.gg/") -> trimmed.removePrefix("https://www.vlr.gg/")
    trimmed.startsWith("http://www.vlr.gg/") -> trimmed.removePrefix("http://www.vlr.gg/")
    trimmed.startsWith("/") -> trimmed.removePrefix("/")
    else -> trimmed
  }
}

private fun vlrArticleUrl(id: String): String {
  val normalized = normalizeArticleId(id)
  return "https://www.vlr.gg/$normalized"
}
