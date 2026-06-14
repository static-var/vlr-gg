/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.list

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.domain.repository.NewsRepository
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsListUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NewsListViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

  @Test
  fun initCollectsCachedNewsWithoutRefresh() {
    runTest(dispatcher) {
      val repository = FakeNewsRepository(items = listOf(newsItem("news-1")))

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(listOf(newsItem("news-1")), viewModel.uiState.value.items)
      assertEquals(false, viewModel.uiState.value.isLoading)
      assertEquals(0, repository.refreshNewsCallCount)

      viewModel.clear()
    }
  }

  @Test
  fun initRefreshesWhenNewsListIsEmpty() {
    runTest(dispatcher) {
      val repository = FakeNewsRepository(items = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(1, repository.refreshNewsCallCount)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeNewsRepository): NewsListViewModel = NewsListViewModel(
    observeNewsListUseCase = ObserveNewsListUseCase(repository),
    refreshNewsUseCase = RefreshNewsUseCase(repository),
    dispatchers = dispatchers,
  )

  private fun newsItem(id: String): NewsItem = NewsItem(
    id = id,
    url = "https://vlr.gg/$id",
    title = "Masters Toronto",
    description = "Playoff bracket locks.",
    date = "2h ago",
    author = "vlr.gg",
    coverUrl = "",
  )

  private class FakeNewsRepository(items: List<NewsItem>) : NewsRepository {
    private val itemsFlow = MutableStateFlow(items)
    var refreshNewsCallCount: Int = 0
      private set

    override fun getNewsList(): Flow<List<NewsItem>> = itemsFlow

    override fun getNewsArticle(articleId: String): Flow<NewsArticle?> = flowOf(null)

    override suspend fun refreshNews(): Result<Unit> {
      refreshNewsCallCount += 1
      return Result.success(Unit)
    }

    override suspend fun refreshNewsArticle(articleId: String): Result<Unit> = Result.success(Unit)
  }

  private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
