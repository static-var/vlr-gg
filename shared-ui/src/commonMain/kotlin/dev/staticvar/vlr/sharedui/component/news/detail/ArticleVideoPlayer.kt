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
    ) { Text(provider?.let { "Open in $it" } ?: "Watch video") }
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
                "Retry player"
              } else {
                "▶  Play $provider ${if (player.provider == "twitch") "clip" else "video"}"
              },
            )
          }
        }
      }
    }
    if (failed) Text("Could not load the player. You can open it directly instead.", style = Prism.typography.caption)
    Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      PrismButton(onClick = { uriHandler.openUri(externalUrl) }, style = PrismButtonStyle.Tertiary) {
        Text("Open in $provider")
      }
      if (active) {
        PrismButton(onClick = {
          playback.stop(itemKey)
        }, style = PrismButtonStyle.Tertiary) { Text("Close player") }
      }
    }
  }
}
