/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsArticleMedia

private val HtmlTagRegex: Regex = Regex("<[^>]+>")
private val HtmlEntityRegex: Regex = Regex("&nbsp;|&amp;|&quot;|&#39;")
private val ImageTagRegex: Regex = Regex("(?is)<img\\b([^>]*)/?>")
private val IframeTagRegex: Regex = Regex("(?is)<iframe\\b([^>]*)>.*?</iframe>")
private val VideoTagRegex: Regex = Regex("(?is)<video\\b([^>]*)>.*?</video>")

internal sealed interface NewsDetailArticleBlock {
  val text: String

  data class Paragraph(override val text: String) : NewsDetailArticleBlock

  data class ListItem(override val text: String) : NewsDetailArticleBlock

  data class Image(val url: String, val description: String) : NewsDetailArticleBlock {
    override val text: String = description.ifBlank { url }
  }

  data class Video(val url: String) : NewsDetailArticleBlock {
    override val text: String = url
  }
}

internal fun NewsArticle.newsDetailAuthorLabel(): String = author.ifBlank { "Unknown" }

internal fun NewsArticle.newsDetailDateLabel(): String = date.ifBlank { "Recent" }

internal fun newsDetailArticleParagraphs(contentHtml: String): List<String> =
  newsDetailArticleBlocks(contentHtml = contentHtml).map { block ->
    when (block) {
      is NewsDetailArticleBlock.Image -> block.text
      is NewsDetailArticleBlock.ListItem -> "- ${block.text}"
      is NewsDetailArticleBlock.Paragraph -> block.text
      is NewsDetailArticleBlock.Video -> block.text
    }
  }

internal fun newsDetailArticleBlocks(contentHtml: String): List<NewsDetailArticleBlock> {
  if (contentHtml.isBlank()) return emptyList()

  val withLineBreaks =
    contentHtml
      .replace(ImageTagRegex) { match ->
        val attributes = match.groupValues[1]
        "\n[image src=\"${attributes.htmlAttribute("src")}\" alt=\"${attributes.htmlAttribute("alt")}\"]\n"
      }
      .replace(IframeTagRegex) { match ->
        val attributes = match.groupValues[1]
        "\n[video src=\"${attributes.htmlAttribute("src")}\"]\n"
      }
      .replace(VideoTagRegex) { match ->
        val attributes = match.groupValues[1]
        "\n[video src=\"${attributes.htmlAttribute("src")}\"]\n"
      }
      .replace(Regex("(?i)<br\\s*/?>"), "\n")
      .replace(Regex("(?i)</p\\s*>"), "\n")
      .replace(Regex("(?i)</h[1-6]\\s*>"), "\n")
      .replace(Regex("(?i)</blockquote\\s*>"), "\n")
      .replace(Regex("(?i)</li\\s*>"), "\n")
      .replace(Regex("(?i)<li(\\s+[^>]*)?>"), "\n[list-item]")

  return withLineBreaks
    .split('\n')
    .mapNotNull(String::toArticleBlock)
}

internal fun NewsArticleMedia.newsDetailMediaSummaryLabels(): List<String> = listOfNotNull(
  links.size.toCountLabel(singular = "link", plural = "links"),
  images.size.toCountLabel(singular = "image", plural = "images"),
  videos.size.toCountLabel(singular = "video", plural = "videos"),
)

internal fun NewsArticleMedia.newsDetailAssetCountLabel(): String {
  val assetCount = links.size + images.size + videos.size
  return when (assetCount) {
    0 -> "No assets"
    1 -> "1 asset"
    else -> "$assetCount assets"
  }
}

private fun String.stripHtml(): String = replace(HtmlTagRegex, " ")
  .replace(HtmlEntityRegex) { match ->
    when (match.value) {
      "&nbsp;" -> " "
      "&amp;" -> "&"
      "&quot;" -> "\""
      "&#39;" -> "'"
      else -> match.value
    }
  }
  .replace(Regex("\\s+"), " ")
  .trim()

private fun String.toArticleBlock(): NewsDetailArticleBlock? {
  val normalized = trimStart()
  if (normalized.startsWith("[image")) {
    return normalized.toImageBlock()
  }
  if (normalized.startsWith("[video")) {
    return normalized.toVideoBlock()
  }

  val isListItem = normalized.startsWith("[list-item]")
  val text = normalized.removePrefix("[list-item]").stripHtml()
  if (text.isBlank()) return null

  return if (isListItem) {
    NewsDetailArticleBlock.ListItem(text = text)
  } else {
    NewsDetailArticleBlock.Paragraph(text = text)
  }
}

private fun String.toImageBlock(): NewsDetailArticleBlock.Image? {
  val url = htmlAttribute("src")
  val description = htmlAttribute("alt")
  return url.takeIf(String::isNotBlank)?.let { imageUrl ->
    NewsDetailArticleBlock.Image(url = imageUrl, description = description)
  }
}

private fun String.toVideoBlock(): NewsDetailArticleBlock.Video? = htmlAttribute("src")
  .takeIf(String::isNotBlank)
  ?.let(NewsDetailArticleBlock::Video)

private fun String.htmlAttribute(name: String): String = Regex("""(?i)\b$name\s*=\s*["']([^"']*)["']""")
  .find(this)
  ?.groupValues
  ?.getOrNull(1)
  .orEmpty()
  .stripHtml()

private fun Int.toCountLabel(singular: String, plural: String): String? = when (this) {
  0 -> null
  1 -> "1 $singular"
  else -> "$this $plural"
}
