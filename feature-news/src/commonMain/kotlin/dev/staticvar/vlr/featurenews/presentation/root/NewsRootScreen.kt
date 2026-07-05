/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.featurenews.presentation.list.NewsListScreen
import dev.staticvar.vlr.featurenews.presentation.list.NewsListUiState

@Composable
public fun NewsRootScreen(
  uiState: NewsListUiState,
  selectedArticleId: String?,
  onArticleSelected: (String) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NewsListScreen(
    uiState = uiState,
    selectedArticleId = selectedArticleId,
    onArticleSelected = onArticleSelected,
    onRefresh = onRefresh,
    modifier = modifier
      .fillMaxSize()
      .background(Prism.color.background),
  )
}
