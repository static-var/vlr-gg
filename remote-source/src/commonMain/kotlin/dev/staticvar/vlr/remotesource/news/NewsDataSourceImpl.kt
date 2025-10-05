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
    client.get(ApiPaths.news(id)).body()
  }
}
