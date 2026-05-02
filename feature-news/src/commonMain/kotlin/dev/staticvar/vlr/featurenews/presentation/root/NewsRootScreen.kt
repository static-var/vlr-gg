/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.featurenews.navigation.NewsRoute
import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleScreen
import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleUiState
import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleViewModel
import dev.staticvar.vlr.featurenews.presentation.list.NewsListScreen
import dev.staticvar.vlr.featurenews.presentation.list.NewsListUiState
import dev.staticvar.vlr.featurenews.presentation.list.NewsListViewModel
import org.koin.mp.KoinPlatform

private object NewsLayoutConstants {
  val LargeLayoutBreakpoint: Dp = 920.dp
}

@Composable
public fun NewsRootScreen(modifier: Modifier = Modifier) {
  val listViewModel: NewsListViewModel = rememberKoinInstance()
  val articleViewModel: NewsArticleViewModel = rememberKoinInstance()
  val listUiState by listViewModel.uiState.collectAsState()
  val articleUiState by articleViewModel.uiState.collectAsState()
  val backStack = remember { mutableStateListOf<NewsRoute>(NewsRoute.List) }
  val selectedArticleId: String? = (backStack.lastOrNull() as? NewsRoute.Article)?.articleId

  LaunchedEffect(selectedArticleId) {
    if (selectedArticleId != null) {
      articleViewModel.openArticle(selectedArticleId)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      listViewModel.clear()
      articleViewModel.clear()
    }
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().background(Prism.color.background),
  ) {
    val isLargeLayout: Boolean = maxWidth >= NewsLayoutConstants.LargeLayoutBreakpoint

    if (isLargeLayout) {
      NewsLargeLayout(
        listUiState = listUiState,
        articleUiState = articleUiState,
        selectedArticleId = selectedArticleId,
        onArticleSelected = { articleId -> upsertArticleRoute(backStack, articleId) },
        onRefreshList = { listViewModel.refresh() },
        onRefreshArticle = { articleViewModel.refresh() },
      )
    } else {
      NewsCompactLayout(
        backStack = backStack,
        listUiState = listUiState,
        articleUiState = articleUiState,
        onArticleSelected = { articleId -> pushArticleRoute(backStack, articleId) },
        onRefreshList = { listViewModel.refresh() },
        onRefreshArticle = { articleViewModel.refresh() },
      )
    }
  }
}

@Composable
private fun NewsCompactLayout(
  backStack: MutableList<NewsRoute>,
  listUiState: NewsListUiState,
  articleUiState: NewsArticleUiState,
  onArticleSelected: (String) -> Unit,
  onRefreshList: () -> Unit,
  onRefreshArticle: () -> Unit,
) {
  NavDisplay(
    backStack = backStack,
    onBack = {
      if (backStack.size > 1) {
        backStack.removeLastOrNull()
      }
    },
    entryProvider =
    entryProvider {
      entry<NewsRoute.List> {
        NewsListScreen(
          uiState = listUiState,
          selectedArticleId = null,
          onArticleSelected = onArticleSelected,
          onRefresh = onRefreshList,
          modifier = Modifier.fillMaxSize(),
        )
      }
      entry<NewsRoute.Article> { route ->
        NewsArticleScreen(
          uiState = articleUiState,
          onBack = { backStack.removeLastOrNull() },
          onRefresh = onRefreshArticle,
          showBackAction = true,
          modifier = Modifier.fillMaxSize(),
        )
      }
    },
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
private fun NewsLargeLayout(
  listUiState: NewsListUiState,
  articleUiState: NewsArticleUiState,
  selectedArticleId: String?,
  onArticleSelected: (String) -> Unit,
  onRefreshList: () -> Unit,
  onRefreshArticle: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Box(
      modifier =
      Modifier
        .weight(0.42f)
        .fillMaxSize(),
    ) {
      NewsListScreen(
        uiState = listUiState,
        selectedArticleId = selectedArticleId,
        onArticleSelected = onArticleSelected,
        onRefresh = onRefreshList,
        modifier = Modifier.fillMaxSize(),
      )
    }

    Box(
      modifier =
      Modifier
        .weight(0.58f)
        .fillMaxSize(),
    ) {
      if (selectedArticleId != null) {
        NewsArticleScreen(
          uiState = articleUiState,
          onBack = {},
          onRefresh = onRefreshArticle,
          showBackAction = false,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Box(
          modifier = Modifier.fillMaxSize().padding(Prism.dimens.spacingM),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
          ) {
            PrismSectionTitle(
              title = "Select an article",
              preLabel = "news",
            )
            Text(
              text = "Pick a story from the left pane to open detail view.",
              style = Prism.typography.bodySmall,
              color = Prism.color.labelColor,
            )
          }
        }
      }
    }
  }
}

private fun pushArticleRoute(backStack: MutableList<NewsRoute>, articleId: String) {
  if ((backStack.lastOrNull() as? NewsRoute.Article)?.articleId == articleId) {
    return
  }
  backStack.add(NewsRoute.Article(articleId = articleId))
}

private fun upsertArticleRoute(backStack: MutableList<NewsRoute>, articleId: String) {
  val articleRoute: NewsRoute.Article = NewsRoute.Article(articleId = articleId)
  if (backStack.isNotEmpty() && backStack.last() is NewsRoute.Article) {
    backStack[backStack.lastIndex] = articleRoute
  } else {
    backStack.add(articleRoute)
  }
}

@Composable
private inline fun <reified T : Any> rememberKoinInstance(): T = remember {
  KoinPlatform.getKoin().get<T>()
}
