/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for News operations.
 * Pure domain contract - no implementation details.
 */
interface NewsRepository {
  /**
   * Get all news items (list view).
   */
  fun getNewsList(): Flow<List<NewsItem>>

  /**
   * Get detailed news article by ID.
   * Returns Flow for reactive updates when article data changes.
   */
  fun getNewsArticle(articleId: String): Flow<NewsArticle?>

  /**
   * Refresh news from remote source.
   */
  suspend fun refreshNews(): Result<Unit>

  /**
   * Refresh a single news article from remote source.
   */
  suspend fun refreshNewsArticle(articleId: String): Result<Unit>
}
