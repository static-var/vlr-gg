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
import kotlinx.coroutines.CancellationException

internal class NewsDataSourceImpl(private val client: HttpClient) : NewsDataSource {
  override suspend fun list(): Result<List<NewsItemDto>> = runCatching {
    client.get(ApiPaths.NEWS).body()
  }

  override suspend fun article(id: String): Result<NewsArticleDto> = try {
    val response = client.get(ApiPaths.news(id.toNewsApiId()))
    check(response.status.isSuccess()) { "News API returned ${response.status}" }
    Result.success(response.body<NewsArticleDto>())
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
