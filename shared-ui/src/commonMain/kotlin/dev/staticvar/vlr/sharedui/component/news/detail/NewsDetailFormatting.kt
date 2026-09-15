/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.runtime.Composable
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_imageUnavailable
import vlr.shared_ui.generated.resources.format_linkUnavailable
import vlr.shared_ui.generated.resources.format_noAssets
import vlr.shared_ui.generated.resources.format_recent
import vlr.shared_ui.generated.resources.format_unknown
import vlr.shared_ui.generated.resources.format_videoUnavailable
import vlr.shared_ui.generated.resources.news_asset_count
import vlr.shared_ui.generated.resources.news_image_count
import vlr.shared_ui.generated.resources.news_reference_count
import vlr.shared_ui.generated.resources.news_video_count

internal fun NewsArticle.newsDetailAuthorLabel(labels: NewsFormattingLabels): String = author.ifBlank { labels.unknown }

internal fun NewsArticle.newsDetailDateLabel(labels: NewsFormattingLabels): String = date.ifBlank { labels.recent }

internal fun NewsArticleMedia.newsDetailMediaSummaryLabels(labels: NewsMediaLabels): List<String> = listOfNotNull(
  labels.links.takeIf { links.isNotEmpty() },
  labels.images.takeIf { images.isNotEmpty() },
  labels.videos.takeIf { videos.isNotEmpty() },
)

internal fun NewsArticleMedia.newsDetailAssetCountLabel(labels: NewsMediaLabels): String =
  if (links.isEmpty() && images.isEmpty() && videos.isEmpty()) labels.noAssets else labels.assets

internal data class NewsMediaLabels(
  val links: String,
  val images: String,
  val videos: String,
  val assets: String,
  val noAssets: String,
)

@Composable
internal fun newsMediaLabels(media: NewsArticleMedia): NewsMediaLabels {
  val assetCount = media.links.size + media.images.size + media.videos.size
  return NewsMediaLabels(
    links = pluralStringResource(Res.plurals.news_reference_count, media.links.size, media.links.size),
    images = pluralStringResource(Res.plurals.news_image_count, media.images.size, media.images.size),
    videos = pluralStringResource(Res.plurals.news_video_count, media.videos.size, media.videos.size),
    assets = pluralStringResource(Res.plurals.news_asset_count, assetCount, assetCount),
    noAssets = stringResource(Res.string.format_noAssets),
  )
}

public data class NewsFormattingLabels(
  public val unknown: String,
  public val recent: String,
  public val imageUnavailable: String,
  public val videoUnavailable: String,
  public val linkUnavailable: String,
)

@Composable
public fun newsFormattingLabels(): NewsFormattingLabels = NewsFormattingLabels(
  unknown = stringResource(Res.string.format_unknown),
  recent = stringResource(Res.string.format_recent),
  imageUnavailable = stringResource(Res.string.format_imageUnavailable),
  videoUnavailable = stringResource(Res.string.format_videoUnavailable),
  linkUnavailable = stringResource(Res.string.format_linkUnavailable),
)
