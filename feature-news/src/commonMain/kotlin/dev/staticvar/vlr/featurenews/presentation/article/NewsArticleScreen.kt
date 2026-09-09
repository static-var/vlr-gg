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
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
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
          PrismButton(
            onClick = onRefresh,
            enabled = !uiState.isRefreshing,
            style = PrismButtonStyle.Tertiary,
          ) {
            Text(if (uiState.isRefreshing) "Refreshing" else "Refresh")
          }
        },
      )

      SharedRefreshStatus(
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage.takeIf { article != null },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }

    when {
      uiState.isLoading && article == null -> {
        PrismFullscreenLoader(
          modifier = Modifier.fillMaxSize(),
          label = "Loading article",
          supportingText = "Loading the full story",
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
        PrismStateMessage(text = "No article content published yet.")
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
          if (article.contentHtml.isBlank()) {
            item(key = "article-body-state") {
              PrismStateMessage(
                text = if (uiState.isLoading || uiState.isRefreshing) "Loading the full story…" else "No article text published yet.",
              )
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
