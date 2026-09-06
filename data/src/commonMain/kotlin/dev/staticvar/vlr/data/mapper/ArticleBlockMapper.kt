package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.ArticleBlock
import dev.staticvar.vlr.domain.model.ArticleTextRun
import dev.staticvar.vlr.remotesource.news.ArticleBlockDto
import kotlinx.serialization.json.Json

internal val articleBlockJson = Json { ignoreUnknownKeys = true }

internal fun ArticleBlockDto.toDomain(): ArticleBlock = ArticleBlock(
  type = type,
  runs = runs.map { ArticleTextRun(it.text, it.url, it.bold, it.italic) },
  children = children.map { it.toDomain() },
  level = level ?: 2,
  ordered = ordered,
  start = start,
  url = url,
  alt = alt,
)

internal fun decodeArticleBlocks(rows: List<String>): List<ArticleBlock> = runCatching {
  rows.map { articleBlockJson.decodeFromString<ArticleBlockDto>(it).toDomain() }
}.getOrDefault(emptyList())
