/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.News
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.news.ArticleBlockDto
import dev.staticvar.vlr.remotesource.news.ArticleTextRunDto
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
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeNewsDataSource
  private lateinit var repository: NewsRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)

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
  fun refreshNews_preserves_cached_article_media_and_placeholder_content() = runTest(dispatcher) {
    val content = "Cached {{link_0}}\n\n{image_0}\n\n{video_0}"
    dataSource.articleResults["story"] = Result.success(
      NewsArticleDto(
        id = "story",
        title = "Original title",
        content = content,
        blocks = listOf(ArticleBlockDto("paragraph", runs = listOf(ArticleTextRunDto("Cached", bold = true))),
          ArticleBlockDto("image", url = "https://example.com/image.jpg")),
        links = listOf(mapOf("text" to "Source", "url" to "https://example.com")),
        images = listOf("image.jpg"),
        videos = listOf("video.mp4"),
      ),
    )
    assertTrue(repository.refreshNewsArticle("story").isSuccess)
    val cachedMedia = database.newsQueries.getNewsMedia("story").executeAsList()
    assertEquals(5, cachedMedia.size)

    dataSource.listResult = Result.success(
      listOf(NewsItemDto(url = "story", title = "Updated title", description = "Updated summary")),
    )
    assertTrue(repository.refreshNews().isSuccess)

    val stored = database.newsQueries.getNewsById("story").executeAsOne()
    assertEquals("Updated title", stored.title)
    assertEquals("Updated summary", stored.description)
    assertEquals(content, stored.content_html)
    assertEquals(cachedMedia, database.newsQueries.getNewsMedia("story").executeAsList())
  }

  @Test
  fun refreshNews_preservesServerOrder_afterReorderingAndArticleRefresh() = runTest(dispatcher) {
    val stories = listOf("zebra", "alpha", "middle").map { id ->
      NewsItemDto(url = id, title = id, date = "2026-09-06")
    }
    dataSource.listResult = Result.success(stories)
    assertTrue(repository.refreshNews().isSuccess)

    repository.getNewsList().test {
      assertEquals(listOf("zebra", "alpha", "middle"), awaitItem().map { it.id })

      dataSource.listResult = Result.success(stories.reversed())
      assertTrue(repository.refreshNews().isSuccess)
      assertEquals(listOf("middle", "alpha", "zebra"), awaitItem().map { it.id })

      dataSource.articleResults["alpha"] = Result.success(
        NewsArticleDto(id = "alpha", title = "Updated article", content = "Content"),
      )
      assertTrue(repository.refreshNewsArticle("alpha").isSuccess)
      assertEquals(listOf("middle", "alpha", "zebra"), awaitItem().map { it.id })
      cancelAndIgnoreRemainingEvents()
    }
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
        content = "Remote {{link_2}}\n\n{video_0}\n\n{image_1}",
        links = listOf(
          mapOf("text" to "Earlier", "url" to "https://example.com/earlier"),
          emptyMap(),
          mapOf("text" to "Source", "url" to "https://example.com"),
        ),
        images = listOf("", "img.png"),
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
    assertEquals("Remote {{link_2}}\n\n{video_0}\n\n{image_1}", storedArticle.content_html)
    val storedMedia = database.newsQueries.getNewsMedia("story").executeAsList()
    assertEquals(6, storedMedia.size)

    repository.getNewsArticle("story").test {
      val article = awaitItem()
      requireNotNull(article)
      assertEquals("Remote {{link_2}}\n\n{video_0}\n\n{image_1}", article.contentHtml)
      assertEquals(listOf("", "img.png"), article.media.images)
      assertEquals(listOf("clip.mp4"), article.media.videos)
      assertEquals(listOf("Earlier", "", "Source"), article.media.links.map { it.text })
      assertEquals("https://example.com", article.media.links[2].url)
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
        list_position = null,
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
