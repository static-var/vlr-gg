/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.search

import dev.staticvar.vlr.remotesource.common.ApiPaths
import dev.staticvar.vlr.remotesource.common.SearchCategory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class SearchDataSourceImpl(private val client: HttpClient) : SearchDataSource {
  override suspend fun search(category: SearchCategory, term: String): Result<List<SearchResultDto>> = runCatching {
    client.get(ApiPaths.SEARCH) {
      parameter("search_category", category.name.lowercase())
      parameter("search_term", term)
    }.body()
  }
}
