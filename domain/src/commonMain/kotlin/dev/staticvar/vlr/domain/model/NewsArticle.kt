/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for a detailed news article.
 */
data class NewsArticle(
  val id: String,
  val url: String,
  val title: String,
  val author: String,
  val date: String,
  val coverUrl: String,
  val contentHtml: String,
  val media: NewsArticleMedia,
  val blocks: List<ArticleBlock> = emptyList(),
)

/**
 * Media content associated with a news article.
 */
data class NewsArticleMedia(val links: List<ArticleLink>, val images: List<String>, val videos: List<String>)

/**
 * A link within an article with display text and URL.
 */
data class ArticleLink(val text: String, val url: String)

/** Structured content in source document order, including nested lists and quotations. */
data class ArticleBlock(
  val type: String,
  val runs: List<ArticleTextRun> = emptyList(),
  val children: List<ArticleBlock> = emptyList(),
  val level: Int = 2,
  val ordered: Boolean = false,
  val start: Int = 1,
  val url: String? = null,
  val alt: String? = null,
  val player: ArticleVideoPlayer? = null,
)

data class ArticleTextRun(
  val text: String,
  val url: String? = null,
  val bold: Boolean = false,
  val italic: Boolean = false,
)

data class ArticleVideoPlayer(
  val provider: String,
  val mediaId: String,
  val playerUrl: String,
  val externalUrl: String,
)
