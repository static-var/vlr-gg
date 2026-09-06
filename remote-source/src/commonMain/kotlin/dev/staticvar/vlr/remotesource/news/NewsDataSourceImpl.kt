/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.news

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException

internal class NewsDataSourceImpl(private val client: HttpClient) : NewsDataSource {
  override suspend fun list(): Result<List<NewsItemDto>> = runCatching {
    client.get(ApiPaths.NEWS).body()
  }

  override suspend fun article(id: String): Result<NewsArticleDto> = try {
    val response = client.get(ApiPaths.news(id.toNewsApiId()))
    check(response.status.isSuccess()) { "News API returned ${response.status}" }
    Result.success(response.body<NewsArticleDto>().let { article ->
      article.copy(blocks = article.blocks.map { it.resolvePlayer(response.call.request.url) })
    })
  } catch (cancellation: CancellationException) {
    throw cancellation
  } catch (failure: Exception) {
    Result.failure(failure)
  }
}

private fun String.toNewsApiId(): String = trim()
  .removePrefix("https://www.vlr.gg/")
  .removePrefix("http://www.vlr.gg/")
  .trimStart('/')
  .substringBefore('/')
  .substringBefore('?')
  .substringBefore('#')

private fun ArticleBlockDto.resolvePlayer(apiUrl: Url): ArticleBlockDto = copy(
  children = children.map { it.resolvePlayer(apiUrl) },
  player = player?.let { video ->
    val validId = when (video.provider) {
      "youtube" -> Regex("[A-Za-z0-9_-]{11}").matches(video.mediaId)
      "twitch" -> Regex("[A-Za-z0-9_-]{1,200}").matches(video.mediaId)
      else -> false
    }
    val expectedPath = "/media/${video.provider}/${video.mediaId}"
    if (!validId || video.playerUrl != expectedPath || apiUrl.protocol != URLProtocol.HTTPS) {
      null
    } else {
      video.copy(
        playerUrl = URLBuilder().apply {
          protocol = apiUrl.protocol
          host = apiUrl.host
          port = apiUrl.port
          encodedPath = expectedPath
        }.buildString(),
        externalUrl = if (video.provider == "youtube") {
          "https://www.youtube.com/watch?v=${video.mediaId}"
        } else {
          "https://clips.twitch.tv/${video.mediaId}"
        },
      )
    }
  },
)
