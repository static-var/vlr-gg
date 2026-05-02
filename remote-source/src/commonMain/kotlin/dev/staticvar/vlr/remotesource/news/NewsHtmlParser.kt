/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.news

import com.fleeksoft.ksoup.Ksoup

internal object NewsHtmlParser {
  fun parse(articleId: String, html: String, fallback: NewsArticleDto? = null): NewsArticleDto {
    val document = Ksoup.parse(html)
    val header = document.select(".article-header").first()
    val body = document.select(".article-body").first()
    body?.select(".wf-hover-card")?.remove()
    body?.select("style")?.remove()

    val resolvedId = fallback?.id?.ifBlank { articleId } ?: articleId
    val title = header?.select(".wf-title")?.firstOrNull()?.text().normalize().ifBlank { fallback?.title.orEmpty() }
    val author = header?.select(".article-meta-author")?.firstOrNull()?.text().normalize().ifBlank {
      fallback?.author.orEmpty()
    }
    val date = header?.select(".js-date-toggle")?.firstOrNull()?.text().normalize().ifBlank { fallback?.date.orEmpty() }
    // Preserve markup so downstream UI can render lists/headings more accurately.
    val content = body?.html()?.trim().orEmpty().ifBlank { fallback?.content.orEmpty() }

    val parsedLinks: List<Map<String, String>> =
      body
        ?.select("a[href]")
        ?.mapNotNull { element ->
          val rawHref = element.attr("href").trim()
          if (rawHref.isBlank() || rawHref.startsWith("#")) return@mapNotNull null
          if (rawHref.startsWith("mailto:") || rawHref.startsWith("tel:") ||
            rawHref.startsWith("javascript:")
          ) {
            return@mapNotNull null
          }

          val href =
            rawHref
              .normalizeUrl()
              .takeIf { it.isNotBlank() && it.isAllowedReferenceLink() }
              ?: return@mapNotNull null
          val text = element.text().normalize().takeIf { it.isNotBlank() } ?: href
          mapOf("text" to text, "href" to href)
        }
        ?.distinctBy { item -> item["href"] }
        .orEmpty()

    val fallbackLinks: List<Map<String, String>> =
      fallback
        ?.links
        ?.mapNotNull { map ->
          val rawHref = map["href"]?.trim().orEmpty()
          if (rawHref.isBlank() || rawHref.startsWith("#")) return@mapNotNull null
          if (rawHref.startsWith("mailto:") || rawHref.startsWith("tel:") ||
            rawHref.startsWith("javascript:")
          ) {
            return@mapNotNull null
          }

          val href =
            rawHref
              .normalizeUrl()
              .takeIf { it.isNotBlank() && it.isAllowedReferenceLink() }
              ?: return@mapNotNull null
          val text = map["text"]?.trim().orEmpty().ifBlank { href }
          mapOf("text" to text, "href" to href)
        }
        ?.distinctBy { item -> item["href"] }
        .orEmpty()

    val links = parsedLinks.ifEmpty { fallbackLinks }

    val parsedImages: List<String> =
      body
        ?.select("img[src]")
        ?.mapNotNull { element -> element.attr("src").normalizeUrl().takeIf { it.isNotBlank() } }
        ?.distinct()
        .orEmpty()

    val images = parsedImages.ifEmpty { fallback?.images.orEmpty() }

    val parsedVideos: List<String> =
      body
        ?.select("iframe[src]")
        ?.mapNotNull { element -> element.attr("src").normalizeUrl().takeIf { it.isNotBlank() } }
        ?.distinct()
        .orEmpty()

    val videos = parsedVideos.ifEmpty { fallback?.videos.orEmpty() }

    return NewsArticleDto(
      id = resolvedId,
      title = title,
      content = content,
      links = links,
      images = images,
      videos = videos,
      date = date.ifBlank { null },
      author = author,
    )
  }
}

private fun String?.normalize(): String = this?.replace(Regex("\\s+"), " ")?.trim().orEmpty()

private fun String.normalizeUrl(): String {
  val clean = trim()
  return when {
    clean.isBlank() -> ""
    clean.startsWith("https://") || clean.startsWith("http://") -> clean
    clean.startsWith("//") -> "https:$clean"
    clean.startsWith("/") -> "https://www.vlr.gg$clean"
    else -> "https://www.vlr.gg/$clean"
  }
}

private fun String.isAllowedReferenceLink(): Boolean {
  val value = trim()
  if (value.isBlank()) return false

  val internalPrefix =
    when {
      value.startsWith("https://www.vlr.gg/") -> "https://www.vlr.gg/"
      value.startsWith("http://www.vlr.gg/") -> "http://www.vlr.gg/"
      value.startsWith("https://vlr.gg/") -> "https://vlr.gg/"
      value.startsWith("http://vlr.gg/") -> "http://vlr.gg/"
      else -> null
    }

  // External references always allowed.
  if (internalPrefix == null) return true

  // Internal links: exclude the high-noise surfaces (teams/players) that make the "References" section unusable.
  val path = value.removePrefix(internalPrefix)
  return !(path.startsWith("team/") || path.startsWith("player/"))
}
