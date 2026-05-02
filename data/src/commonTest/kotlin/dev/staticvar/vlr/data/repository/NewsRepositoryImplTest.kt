/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.News
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.news.NewsArticleDto
import dev.staticvar.vlr.remotesource.news.NewsDataSource
import dev.staticvar.vlr.remotesource.news.NewsItemDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NewsRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: JdbcSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeNewsDataSource
  private lateinit var repository: NewsRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VlrDatabase.Schema.create(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakeNewsDataSource()
    repository = NewsRepositoryImpl(
      newsDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider,
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun refreshNews_upsertsAndPrunes() = runTest(dispatcher) {
    insertNews(
      id = "keep",
      title = "Old Keep",
      description = "Old",
      contentHtml = "<p>cached</p>",
    )
    insertNews(
      id = "stale",
      title = "Stale News",
      description = "Stale",
      contentHtml = "<p>stale</p>",
    )

    dataSource.listResult = Result.success(
      listOf(
        NewsItemDto(
          url = "keep",
          title = "Keep Updated",
          description = "New Desc",
          date = "2024-01-06",
          author = "Author",
        ),
        NewsItemDto(
          url = "fresh",
          title = "Fresh Title",
          description = "Fresh Desc",
          date = "2024-01-07",
          author = "Reporter",
        ),
      ),
    )

    val result = repository.refreshNews()
    assertTrue(result.isSuccess)

    val records = database.newsQueries.getAllNews().executeAsList()
    assertEquals(setOf("fresh", "keep"), records.map { it.id }.toSet())

    val keep = records.first { it.id == "keep" }
    assertEquals("<p>cached</p>", keep.content_html)
    assertEquals("New Desc", keep.description)

    val fresh = records.first { it.id == "fresh" }
    assertEquals("Fresh Title", fresh.title)
    assertEquals("Fresh Desc", fresh.description)
  }

  @Test
  fun getNewsList_emitsStoredItems() = runTest(dispatcher) {
    dataSource.listResult = Result.success(
      listOf(
        NewsItemDto(
          url = "alpha",
          title = "Alpha",
          description = "Alpha Desc",
          date = "2024-02-01",
          author = "A",
        ),
        NewsItemDto(
          url = "beta",
          title = "Beta",
          description = "Beta Desc",
          date = "2024-02-02",
          author = "B",
        ),
      ),
    )

    assertTrue(repository.refreshNews().isSuccess)

    repository.getNewsList().test {
      val emission = awaitItem()
      assertEquals(setOf("alpha", "beta"), emission.map { it.id }.toSet())
      assertTrue(emission.any { it.title == "Alpha" && it.description == "Alpha Desc" })
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun getNewsArticle_emitsStoredData_untilExplicitRefresh() = runTest(dispatcher) {
    insertNews(
      id = "story",
      title = "Story",
      description = "Summary",
      contentHtml = null,
    )

    dataSource.articleResults["story"] = Result.success(
      NewsArticleDto(
        id = "story",
        title = "Story",
        content = "<p>Remote Body</p>",
        links = listOf(mapOf("text" to "Source", "href" to "https://example.com")),
        images = listOf("img.png"),
        videos = listOf("clip.mp4"),
        date = "2024-03-01",
        author = "Reporter",
      ),
    )

    repository.getNewsArticle("story").test {
      val first = awaitItem()
      assertTrue(first?.contentHtml.isNullOrEmpty())
      cancelAndIgnoreRemainingEvents()
    }
    assertTrue(dataSource.articleRequests.isEmpty())

    assertTrue(repository.refreshNewsArticle("story").isSuccess)
    advanceUntilIdle()

    val storedArticle = database.newsQueries.getNewsById("story").executeAsOne()
    assertEquals("<p>Remote Body</p>", storedArticle.content_html)
    val storedMedia = database.newsQueries.getNewsMedia("story").executeAsList()
    assertEquals(3, storedMedia.size)

    repository.getNewsArticle("story").test {
      val article = awaitItem()
      requireNotNull(article)
      assertEquals("<p>Remote Body</p>", article.contentHtml)
      assertEquals(listOf("img.png"), article.media.images)
      assertEquals(listOf("clip.mp4"), article.media.videos)
      assertEquals("Source", article.media.links.firstOrNull()?.text)
      cancelAndIgnoreRemainingEvents()
    }

    assertEquals(listOf("story"), dataSource.articleRequests)
  }

  private fun insertNews(
    id: String,
    title: String,
    description: String?,
    contentHtml: String?,
    author: String = "Author",
    date: String = "2024-01-01",
    coverUrl: String = "",
  ) {
    database.newsQueries.insertNews(
      News(
        id = id,
        url = id,
        title = title,
        author = author,
        date = date,
        cover_url = coverUrl,
        description = description,
        content_html = contentHtml,
        last_updated = 0,
      ),
    )
  }

  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }

  private class FakeNewsDataSource : NewsDataSource {
    var listResult: Result<List<NewsItemDto>> = Result.success(emptyList())
    val articleResults: MutableMap<String, Result<NewsArticleDto>> = mutableMapOf()
    val articleRequests = mutableListOf<String>()

    override suspend fun list(): Result<List<NewsItemDto>> = listResult

    override suspend fun article(id: String): Result<NewsArticleDto> {
      articleRequests += id
      return articleResults[id] ?: Result.failure(IllegalStateException("No article for $id"))
    }
  }
}
