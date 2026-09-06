/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.article

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.ArticleBlock
import dev.staticvar.vlr.domain.model.ArticleTextRun
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.domain.repository.NewsRepository
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsArticleUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NewsArticleViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

  @Test
  fun openArticleCollectsCachedArticleWithoutRefresh() {
    runTest(dispatcher) {
      val article = newsArticle(articleId = "article-1", contentHtml = "Ready.").copy(blocks = listOf(ArticleBlock("paragraph", runs = listOf(ArticleTextRun("Ready.")))))
      val repository = FakeNewsRepository(article = article)
      val viewModel = createViewModel(repository)

      viewModel.openArticle("article-1")
      advanceUntilIdle()

      assertEquals(article, viewModel.uiState.value.article)
      assertEquals(false, viewModel.uiState.value.isLoading)
      assertEquals(emptyList(), repository.refreshArticleRequests)

      viewModel.clear()
    }
  }

  @Test
  fun openArticleRefreshesWhenArticleIsMissing() {
    runTest(dispatcher) {
      val repository = FakeNewsRepository(article = null)
      val viewModel = createViewModel(repository)

      viewModel.openArticle("article-9")
      runCurrent()
      assertEquals(true, viewModel.uiState.value.isLoading)
      advanceUntilIdle()

      assertEquals(listOf("article-9"), repository.refreshArticleRequests)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  @Test
  fun legacyCacheRefreshesOnlyOnceEvenWhenServerStillReturnsNoBlocks() = runTest(dispatcher) {
    val cached = newsArticle("old", "<p>Old cached article</p>")
    val repository = FakeNewsRepository(cached)
    val viewModel = createViewModel(repository)
    viewModel.openArticle("old")
    advanceUntilIdle()
    repository.articleFlow.value = cached.copy(contentHtml = "Updated legacy content")
    advanceUntilIdle()
    viewModel.openArticle("old")
    advanceUntilIdle()
    assertEquals(listOf("old"), repository.refreshArticleRequests)
    assertEquals("Updated legacy content", viewModel.uiState.value.article?.contentHtml)
    viewModel.clear()
  }

  private fun createViewModel(repository: FakeNewsRepository): NewsArticleViewModel = NewsArticleViewModel(
    observeNewsArticleUseCase = ObserveNewsArticleUseCase(repository),
    refreshNewsArticleUseCase = RefreshNewsArticleUseCase(repository),
    dispatchers = dispatchers,
  )

  private fun newsArticle(articleId: String, contentHtml: String): NewsArticle = NewsArticle(
    id = articleId,
    url = "https://vlr.gg/$articleId",
    title = "Masters Toronto",
    author = "vlr.gg",
    date = "2h ago",
    coverUrl = "",
    contentHtml = contentHtml,
    media = NewsArticleMedia(links = emptyList(), images = emptyList(), videos = emptyList()),
  )

  private class FakeNewsRepository(article: NewsArticle?) : NewsRepository {
    val articleFlow = MutableStateFlow(article)
    val refreshArticleRequests: MutableList<String> = mutableListOf()

    override fun getNewsList(): Flow<List<NewsItem>> = flowOf(emptyList())

    override fun getNewsArticle(articleId: String): Flow<NewsArticle?> = articleFlow

    override suspend fun refreshNews(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshNewsArticle(articleId: String): Result<Unit> {
      refreshArticleRequests += articleId
      delay(100)
      return Result.success(Unit)
    }
  }

  private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
