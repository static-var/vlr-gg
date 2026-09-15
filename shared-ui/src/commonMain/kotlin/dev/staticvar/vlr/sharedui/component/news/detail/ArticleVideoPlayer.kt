/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.news_close_player
import vlr.shared_ui.generated.resources.news_open_provider
import vlr.shared_ui.generated.resources.news_play_clip
import vlr.shared_ui.generated.resources.news_play_video
import vlr.shared_ui.generated.resources.news_player_load_failed
import vlr.shared_ui.generated.resources.news_retry_player
import vlr.shared_ui.generated.resources.news_watch_video

public class ArticleVideoPlaybackState internal constructor() {
  internal var activeKey: String? by mutableStateOf(null)
  internal fun stop(key: String) {
    if (activeKey == key) activeKey = null
  }
}

@Composable
public fun rememberArticleVideoPlaybackState(articleId: String): ArticleVideoPlaybackState =
  remember(articleId) { ArticleVideoPlaybackState() }

@Composable
internal fun ArticleVideoPlayer(
  video: NewsDetailContentBlock.Video,
  playback: ArticleVideoPlaybackState,
  itemKey: String,
) {
  val uriHandler = LocalUriHandler.current
  val player = video.player
  val provider = when (player?.provider) {
    "youtube" -> "YouTube"
    "twitch" -> "Twitch"
    else -> null
  }
  val externalUrl = player?.externalUrl ?: video.url
  val supported = player != null && provider != null && player.playerUrl.startsWith("https://") &&
    supportsArticleVideoWebView()
  if (!supported) {
    PrismButton(
      onClick = { uriHandler.openUri(externalUrl) },
      style = PrismButtonStyle.Secondary,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        provider?.let { stringResource(Res.string.news_open_provider, it) }
          ?: stringResource(Res.string.news_watch_video),
      )
    }
    return
  }
  requireNotNull(player)
  var failed by remember(itemKey, player.playerUrl) { mutableStateOf(false) }
  DisposableEffect(playback, itemKey) {
    onDispose { playback.stop(itemKey) }
  }
  val active = playback.activeKey == itemKey
  val onError = {
    failed = true
    playback.stop(itemKey)
  }
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val minimumWidth = if (player.provider == "twitch") 400.dp else 200.dp
      val minimumHeight = if (player.provider == "twitch") 300.dp else 200.dp
      val contentScale = minOf(1f, maxWidth / minimumWidth)
      val playerHeight = maxOf(maxWidth * 9f / 16f, minimumHeight * contentScale)
      if (active) {
        ArticleVideoWebView(
          playerUrl = player.playerUrl,
          modifier = Modifier.fillMaxWidth().height(playerHeight),
          contentScale = contentScale,
          onError = onError,
        )
      } else {
        Box(
          Modifier.fillMaxWidth().height(playerHeight).background(Prism.color.surfaceVariant),
          contentAlignment = Alignment.Center,
        ) {
          if (player.provider == "youtube") {
            Image(
              painter = rememberAsyncImagePainter("https://i.ytimg.com/vi/${player.mediaId}/hqdefault.jpg"),
              contentDescription = null,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
          }
          PrismButton(
            onClick = {
              failed = false
              playback.activeKey = itemKey
            },
            style = PrismButtonStyle.Secondary,
          ) {
            Text(
              if (failed) {
                stringResource(Res.string.news_retry_player)
              } else {
                stringResource(
                  if (player.provider == "twitch") Res.string.news_play_clip else Res.string.news_play_video,
                  requireNotNull(provider),
                )
              },
            )
          }
        }
      }
    }
    if (failed) Text(stringResource(Res.string.news_player_load_failed), style = Prism.typography.caption)
    Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      PrismButton(onClick = { uriHandler.openUri(externalUrl) }, style = PrismButtonStyle.Tertiary) {
        Text(stringResource(Res.string.news_open_provider, requireNotNull(provider)))
      }
      if (active) {
        PrismButton(onClick = {
          playback.stop(itemKey)
        }, style = PrismButtonStyle.Tertiary) { Text(stringResource(Res.string.news_close_player)) }
      }
    }
  }
}
