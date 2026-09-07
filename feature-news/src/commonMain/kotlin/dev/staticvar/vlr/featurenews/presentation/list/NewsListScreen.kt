/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
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
          PrismButton(
            onClick = onRefresh,
            enabled = !uiState.isRefreshing,
            style = PrismButtonStyle.Secondary,
          ) {
            Text("REFRESH")
          }
        },
      )

      SharedRefreshStatus(
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage.takeIf { uiState.items.isNotEmpty() },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }

    when {
      uiState.isLoading && uiState.items.isEmpty() -> {
        PrismFullscreenLoader(
          modifier = Modifier.fillMaxSize(),
          label = "SYNCING NEWS",
          supportingText = "Loading local cache and remote updates",
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
          modifier = Modifier.fillMaxSize(),
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
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    PrismCard(
      modifier = Modifier.fillMaxWidth(),
      style = PrismCardStyle.Outlined,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismSectionTitle(
          title = "No stories published yet",
          preLabel = "news",
          showDivider = false,
        )
        PrismButton(
          onClick = onRefresh,
          style = PrismButtonStyle.Primary,
        ) {
          Text("REFRESH")
        }
      }
    }
  }
}
