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
    Column {
      PrismScreenTitleBar(
        title = "NEWS",
        subtitle = "Stories from competitive VALORANT",
        actions = {
          SharedRefreshButton(
            isLoading = uiState.isLoading,
            isRefreshing = uiState.isRefreshing,
            hasContent = uiState.items.isNotEmpty(),
            onRefresh = onRefresh,
          )
        },
      )

      SharedRefreshStatus(
        hasContent = uiState.items.isNotEmpty(),
        isRefreshing = false,
        errorMessage = uiState.errorMessage.takeIf { uiState.items.isNotEmpty() },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }

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

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().cardMascotViewport(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          items(
            items = uiState.items,
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
