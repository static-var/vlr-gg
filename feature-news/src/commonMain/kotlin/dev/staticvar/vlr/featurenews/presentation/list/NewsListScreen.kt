/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.sharedui.component.news.overview.NewsPreviewItem

@Composable
internal fun NewsListScreen(
  uiState: NewsListUiState,
  selectedArticleId: String?,
  onArticleSelected: (String) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
    modifier
      .fillMaxSize()
      .padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    NewsListHeader(
      isLoading = uiState.isLoading,
      isRefreshing = uiState.isRefreshing,
      hasContent = uiState.items.isNotEmpty(),
      errorMessage = uiState.errorMessage,
      errorDetails = uiState.errorDetails,
      onRefresh = onRefresh,
    )

    when {
      (!LocalIsOnline.current || uiState.isLoading || uiState.isRefreshing) && uiState.items.isEmpty() -> {
        SharedScreenLoading(
          modifier = Modifier.fillMaxSize(),
          label = "Loading news",
        )
      }

      uiState.items.isEmpty() && uiState.errorMessage != null -> {
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }

      uiState.items.isEmpty() -> {
        EmptyNewsList(
          onRefresh = onRefresh,
          modifier = Modifier.fillMaxSize(),
        )
      }

      else -> NewsItems(
        items = uiState.items,
        selectedArticleId = selectedArticleId,
        onArticleSelected = onArticleSelected,
        modifier = Modifier.fillMaxSize().cardMascotViewport(),
      )
    }
  }
}

@Composable
private fun NewsListHeader(
  isLoading: Boolean,
  isRefreshing: Boolean,
  hasContent: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  onRefresh: () -> Unit,
) {
  Column {
    PrismScreenTitleBar(
      title = "NEWS",
      subtitle = "Stories from competitive VALORANT",
      actions = {
        SharedRefreshButton(
          isLoading = isLoading,
          isRefreshing = isRefreshing,
          hasContent = hasContent,
          onRefresh = onRefresh,
        )
      },
    )

    SharedRefreshStatus(
      hasContent = hasContent,
      isRefreshing = false,
      errorMessage = errorMessage.takeIf { hasContent },
      errorDetails = errorDetails,
      onRefresh = onRefresh,
    )
  }
}

@Composable
private fun NewsItems(
  items: List<NewsItem>,
  selectedArticleId: String?,
  onArticleSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    items(
      items = items,
      key = { item -> item.id },
    ) { item ->
      NewsPreviewItem(
        newsItem = item,
        selected = selectedArticleId == item.id,
        onClick = { onArticleSelected(item.id) },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun EmptyNewsList(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
  SharedEmptyState(
    artwork = EmptyStateArtwork.NoLiveEvents,
    title = "No stories yet",
    message = "Competitive VALORANT news will appear here when published.",
    modifier = modifier,
    actionLabel = "Refresh",
    onAction = onRefresh,
  )
}
