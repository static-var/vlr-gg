/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.article

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.sharedui.component.news.detail.NewsDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.news.detail.newsDetailStoryItems
import dev.staticvar.vlr.sharedui.component.news.detail.rememberArticleVideoPlaybackState
import kotlinx.coroutines.launch

@Composable
public fun NewsArticleRoute(
  uiState: NewsArticleUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NewsArticleScreen(
    uiState = uiState,
    onBack = onBack,
    onRefresh = onRefresh,
    showBackAction = true,
    modifier = modifier,
  )
}

@Composable
internal fun NewsArticleScreen(
  uiState: NewsArticleUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  showBackAction: Boolean,
  modifier: Modifier = Modifier,
) {
  val isOnline = LocalIsOnline.current
  val article: NewsArticle? = uiState.article
  val videoPlayback = rememberArticleVideoPlaybackState(article?.id.orEmpty())
  val uriHandler = LocalUriHandler.current
  val scrollState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val showScrollToTop by remember { derivedStateOf { scrollState.firstVisibleItemIndex > 0 } }

  Column(
    modifier =
    modifier
      .fillMaxSize()
      .padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = "News",
        onBackPress = onBack.takeIf { showBackAction },
        actions = {
          if (showScrollToTop) {
            PrismButton(
              onClick = { coroutineScope.launch { scrollState.animateScrollToItem(0) } },
              style = PrismButtonStyle.Tertiary,
            ) {
              Text("Top")
            }
          }
          SharedRefreshButton(
            isLoading = uiState.isLoading,
            isRefreshing = uiState.isRefreshing,
            hasContent = article != null,
            onRefresh = onRefresh,
          )
        },
      )

      SharedRefreshStatus(
        hasContent = article != null,
        isRefreshing = false,
        errorMessage = uiState.errorMessage.takeIf { article != null },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }

    when {
      (!LocalIsOnline.current || uiState.isLoading || uiState.isRefreshing) && article == null -> {
        NewsArticleLoading(
          modifier = Modifier.fillMaxWidth().weight(1f),
          label = "Loading article",
        )
      }

      article == null && uiState.errorMessage != null -> {
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }

      article == null -> {
        SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveEvents,
          title = "No article yet",
          message = "This story has not been published.",
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().cardMascotViewport(),
          state = scrollState,
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          item(key = "header") {
            NewsDetailHeaderItem(article = article)
          }
          if (article.contentHtml.isBlank() && article.blocks.isEmpty()) {
            if (isOnline && !uiState.isLoading && !uiState.isRefreshing && uiState.errorMessage == null) {
              item(key = "article-body-state") {
                SharedEmptyState(
                  artwork = EmptyStateArtwork.NoLiveEvents,
                  title = "No article text yet",
                  message = "The story text has not been published.",
                  compact = true,
                )
              }
            }
          } else {
            newsDetailStoryItems(article = article, playback = videoPlayback)
          }
          item(key = "source") {
            PrismButton(
              onClick = { uriHandler.openUri(article.url) },
              style = PrismButtonStyle.Tertiary,
            ) {
              Text("Read on VLR.gg")
            }
          }
          item(key = "navigation-bar-spacer") {
            Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
          }
        }
      }
    }
  }
}

@Composable
private fun NewsArticleLoading(label: String, modifier: Modifier = Modifier) {
  if (LocalIsOnline.current) {
    PrismFullscreenLoader(modifier = modifier, size = PrismLoaderSize.Large, label = label)
  } else {
    SharedScreenLoading(label = label, modifier = modifier)
  }
}
