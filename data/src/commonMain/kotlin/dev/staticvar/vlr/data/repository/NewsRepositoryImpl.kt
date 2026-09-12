/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.News
import dev.staticvar.vlr.data.NewsMedia
import dev.staticvar.vlr.data.mapper.aggregateNewsArticle
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.data.mapper.toMediaEntities
import dev.staticvar.vlr.data.mapper.toNewsItem
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.domain.repository.NewsRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.news.NewsArticleDto
import dev.staticvar.vlr.remotesource.news.NewsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [NewsRepository].
 * Persists remote news into the local database and exposes reactive streams.
 */
internal class NewsRepositoryImpl(
  private val newsDataSource: NewsDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : NewsRepository {

  private val queries = database.newsQueries

  override fun getNewsList(): Flow<List<NewsItem>> = queries
    .getAllNews()
    .asFlow()
    .mapToList(dispatchers.io)
    .map { entities -> entities.map { it.toNewsItem() } }

  override fun getNewsArticle(articleId: String): Flow<NewsArticle?> {
    val newsFlow = queries
      .getNewsById(articleId)
      .asFlow()
      .mapToOneOrNull(dispatchers.io)

    val mediaFlow = queries
      .getNewsMedia(articleId)
      .asFlow()
      .mapToList(dispatchers.io)

    return combine(newsFlow, mediaFlow) { news, media ->
      news?.takeIf { it.content_html != null }?.let { aggregateNewsArticle(it, media) }
    }
  }

  override suspend fun refreshNews(): Result<Unit> = withContext(dispatchers.io) {
    newsDataSource.list().mapCatching { dtos ->
      database.transaction {
        val existing = queries.getAllNews().executeAsList().associateBy { it.id }
        val remoteIds = mutableSetOf<String>()

        dtos.forEachIndexed { index, dto ->
          val entity = dto.toEntity(listPosition = index.toLong())
          if (entity.id.isBlank()) return@forEachIndexed
          remoteIds += entity.id
          val current = existing[entity.id]
          val merged = mergeListEntity(entity, current)
          if (current == null) {
            queries.insertNews(merged)
          } else {
            queries.updateNews(
              url = merged.url,
              title = merged.title,
              author = merged.author,
              date = merged.date,
              cover_url = merged.cover_url,
              description = merged.description,
              content_html = merged.content_html,
              list_position = merged.list_position,
              last_updated = merged.last_updated,
              id = merged.id,
            )
          }
        }

        val staleIds = existing.keys - remoteIds
        staleIds.forEach { id -> queries.deleteNewsById(id) }
      }
    }
  }

  override suspend fun refreshNewsArticle(articleId: String): Result<Unit> = withContext(dispatchers.io) {
    newsDataSource.article(articleId).mapCatching { dto ->
      persistArticle(dto, articleId)
    }
  }

  private fun persistArticle(dto: NewsArticleDto, requestedId: String) {
    database.transaction {
      val current = queries.getNewsById(requestedId).executeAsOneOrNull()
      val articleEntity = mergeArticleEntity(dto, requestedId, current)
      queries.insertNews(articleEntity)
      queries.deleteNewsMedia(articleEntity.id)
      dto
        .toMediaEntities(articleEntity.id)
        .forEach { media -> insertMedia(media, articleEntity.id) }
    }
  }

  private fun mergeListEntity(entity: News, current: News?): News = entity.copy(
    content_html = current?.content_html ?: entity.content_html,
    cover_url = when {
      entity.cover_url.isNotBlank() -> entity.cover_url
      current != null -> current.cover_url
      else -> entity.cover_url
    },
    description = entity.description?.takeIf { it.isNotBlank() } ?: current?.description,
    date = entity.date.ifBlank { current?.date ?: "" },
    author = entity.author.ifBlank { current?.author ?: "" },
  )

  private fun mergeArticleEntity(dto: NewsArticleDto, requestedId: String, current: News?): News {
    val requestedUrl = requestedId.toAbsoluteVlrUrl()
    val base = dto.toEntity().copy(id = requestedId, url = current?.url ?: requestedUrl)
    return base.copy(
      list_position = current?.list_position,
      description = current?.description,
      cover_url = base.cover_url.ifBlank { current?.cover_url ?: "" },
      date = base.date.ifBlank { current?.date ?: "" },
    )
  }

  private fun insertMedia(media: NewsMedia, articleId: String) {
    queries.insertNewsMedia(
      news_id = articleId,
      media_type = media.media_type,
      media_value = media.media_value,
      media_text = media.media_text,
    )
  }
}

private fun String.toAbsoluteVlrUrl(): String {
  val value = trim()
  return when {
    value.startsWith("https://") || value.startsWith("http://") -> value
    value.startsWith("/") -> "https://www.vlr.gg$value"
    else -> "https://www.vlr.gg/$value"
  }
}
