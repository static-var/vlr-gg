/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.featurenews.presentation.list.NewsListScreen
import dev.staticvar.vlr.featurenews.presentation.list.NewsListViewModel
import org.koin.compose.currentKoinScope

@Composable
public fun NewsRootScreen(
  onArticleSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  selectedArticleId: String? = null,
) {
  val viewModel: NewsListViewModel = rememberKoinInstance()
  val uiState by viewModel.uiState.collectAsState()


  NewsListScreen(
    uiState = uiState,
    selectedArticleId = selectedArticleId,
    onArticleSelected = onArticleSelected,
    onRefresh = viewModel::refresh,
    modifier = modifier
      .fillMaxSize()
      .background(Prism.color.background),
  )
}

@Composable
private inline fun <reified T : Any> rememberKoinInstance(): T {
  val scope = currentKoinScope()
  return remember(scope) { scope.get<T>() }
}
