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
)

/**
 * Media content associated with a news article.
 */
data class NewsArticleMedia(
  val links: List<ArticleLink>,
  val images: List<String>,
  val videos: List<String>,
)

/**
 * A link within an article with display text and URL.
 */
data class ArticleLink(
  val text: String,
  val url: String,
)
