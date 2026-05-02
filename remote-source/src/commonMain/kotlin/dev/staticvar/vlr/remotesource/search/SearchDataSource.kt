/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.search

import dev.staticvar.vlr.remotesource.common.ApiPaths
import dev.staticvar.vlr.remotesource.common.SearchCategory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

interface SearchDataSource {
  suspend fun search(category: SearchCategory, term: String): Result<List<SearchResultDto>>
}
