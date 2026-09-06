/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsArticleMedia

internal fun NewsArticle.newsDetailAuthorLabel(): String = author.ifBlank { "Unknown" }

internal fun NewsArticle.newsDetailDateLabel(): String = date.ifBlank { "Recent" }

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

private fun Int.toCountLabel(singular: String, plural: String): String? = when (this) {
  0 -> null
  1 -> "1 $singular"
  else -> "$this $plural"
}
