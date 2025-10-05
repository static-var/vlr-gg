package dev.staticvar.vlr.remotesource.news

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface NewsDataSource {
  suspend fun list(): Result<List<NewsItemDto>>
  suspend fun article(id: String): Result<NewsArticleDto>
}
