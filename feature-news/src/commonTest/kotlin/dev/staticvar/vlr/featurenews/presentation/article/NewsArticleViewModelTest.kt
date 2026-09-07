/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.article

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.ArticleBlock
import dev.staticvar.vlr.domain.model.ArticleTextRun
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.domain.repository.NewsRepository
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsArticleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runCurrent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NewsArticleViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val viewModelStore = ViewModelStore()

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun initCollectsCachedArticleWithoutRefresh() {
    runTest(dispatcher) {
      val article = newsArticle(articleId = "article-1", contentHtml = "Ready.").copy(
        blocks = listOf(ArticleBlock("paragraph", runs = listOf(ArticleTextRun("Ready.")))),
      )
      val repository = FakeNewsRepository(article = article)
      val viewModel = createViewModel(repository, "article-1")

      advanceUntilIdle()

      assertEquals(article, viewModel.uiState.value.article)
      assertEquals(false, viewModel.uiState.value.isLoading)
      assertEquals(emptyList(), repository.refreshArticleRequests)
    }
  }

  @Test
  fun missingArticleFinishesLoadingWithoutAutomaticNetworkRequest() {
    runTest(dispatcher) {
      val repository = FakeNewsRepository(article = null)
      val viewModel = createViewModel(repository, "article-9")

      runCurrent()
      assertEquals(false, viewModel.uiState.value.isLoading)
      advanceUntilIdle()

      assertEquals(emptyList(), repository.refreshArticleRequests)
      assertEquals(false, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun legacyCacheObservesUpdatesWithoutAutomaticRefresh() = runTest(dispatcher) {
    val cached = newsArticle("old", "<p>Old cached article</p>")
    val repository = FakeNewsRepository(cached)
    val viewModel = createViewModel(repository, "old")
    advanceUntilIdle()
    repository.articleFlow.value = cached.copy(contentHtml = "Updated legacy content")
    advanceUntilIdle()
    assertEquals(emptyList(), repository.refreshArticleRequests)
    assertEquals("Updated legacy content", viewModel.uiState.value.article?.contentHtml)
  }

  @Test
  fun refreshCoalescesAndPreservesSavedArticleOnFailure() = runTest(dispatcher) {
    val cached = newsArticle("saved", "<p>Saved article</p>")
    val repository = FakeNewsRepository(cached)
    repository.refreshResult = Result.failure(IllegalStateException("Offline"))
    val viewModel = createViewModel(repository, "saved")
    advanceUntilIdle()

    viewModel.refresh()
    viewModel.refresh()
    runCurrent()
    assertEquals(listOf("saved"), repository.refreshArticleRequests)
    assertEquals(cached, viewModel.uiState.value.article)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("Offline", viewModel.uiState.value.errorMessage)

    repository.articleFlow.value = cached.copy(title = "Saved update")
    advanceUntilIdle()
    assertEquals("Offline", viewModel.uiState.value.errorMessage)
    repository.refreshResult = Result.success(Unit)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(null, viewModel.uiState.value.errorMessage)
    assertEquals("Saved update", viewModel.uiState.value.article?.title)
  }

  private fun createViewModel(repository: FakeNewsRepository, articleId: String): NewsArticleViewModel = NewsArticleViewModel(
    articleId = articleId,
    observeNewsArticleUseCase = ObserveNewsArticleUseCase(repository),
    refreshNewsArticleUseCase = RefreshNewsArticleUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
  ).also { viewModelStore.put("viewModel", it) }

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
    var refreshResult: Result<Unit> = Result.success(Unit)
    val articleFlow = MutableStateFlow(article)
    val refreshArticleRequests: MutableList<String> = mutableListOf()

    override fun getNewsList(): Flow<List<NewsItem>> = flowOf(emptyList())

    override fun getNewsArticle(articleId: String): Flow<NewsArticle?> = articleFlow

    override suspend fun refreshNews(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshNewsArticle(articleId: String): Result<Unit> {
      refreshArticleRequests += articleId
      delay(100)
      return refreshResult
    }
  }
}
