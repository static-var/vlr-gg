/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.di.dataModule
import dev.staticvar.vlr.domain.repository.NewsRepository
import dev.staticvar.vlr.localsource.database.DatabaseDriverFactory
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.news.NewsArticleDto
import dev.staticvar.vlr.remotesource.news.NewsDataSource
import dev.staticvar.vlr.remotesource.news.NewsItemDto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class NewsPayloadFixtureTest {
  @Test
  fun decodedArticlesSurviveRepositoryPersistence() = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val arguments = InstrumentationRegistry.getArguments()
    if (arguments.getString("cleanup_fixtures") == "true") {
      val fixtureDriver = DatabaseDriverFactory(instrumentation.targetContext).createDriver()
      try {
        val fixtureDatabase = VlrDatabase(fixtureDriver)
        fixtures.forEach { fixtureDatabase.newsQueries.deleteNewsById(it.id) }
        fixtures.forEach {
          assertEquals(null, fixtureDatabase.newsQueries.getNewsById(it.id).executeAsOneOrNull())
          assertTrue(fixtureDatabase.newsQueries.getNewsMedia(it.id).executeAsList().isEmpty())
        }
      } finally {
        fixtureDriver.close()
      }
      return@runBlocking
    }
    val leaveFixtures = arguments.getString("leave_fixtures") == "true"
    val context = if (leaveFixtures) instrumentation.targetContext else object : ContextWrapper(instrumentation.targetContext) {
      override fun getDatabasePath(name: String) = super.getDatabasePath("news-payload-test-$name")
    }
    val json = GlobalContext.get().get<Json>()
    val driver = DatabaseDriverFactory(context).createDriver()
    val database = VlrDatabase(driver)
    val source = object : NewsDataSource {
      override suspend fun list(): Result<List<NewsItemDto>> = Result.success(emptyList())
      override suspend fun article(id: String): Result<NewsArticleDto> = runCatching {
        json.decodeFromString<NewsArticleDto>(fixtures.single { it.id == id }.payload)
      }
    }
    val app = koinApplication {
      modules(dataModule(), module {
        single { database }
        single<NewsDataSource> { source }
        single<DispatcherProvider> {
          object : DispatcherProvider {
            override val default = Dispatchers.Default
            override val io = Dispatchers.IO
            override val main = Dispatchers.Main
          }
        }
      })
    }
    try {
      val repository = app.koin.get<NewsRepository>()
      fixtures.forEachIndexed { index, fixture ->
        repository.refreshNewsArticle(fixture.id).getOrThrow()
        val article = checkNotNull(repository.getNewsArticle(fixture.id).first())
        assertEquals(fixture.title, article.title)
        assertEquals(fixture.links.map { it.first }, article.media.links.map { it.text })
        assertEquals(fixture.links.map { it.second }, article.media.links.map { it.url })
        assertEquals(fixture.blockCount, article.blocks.size)
        val row = database.newsQueries.getNewsById(fixture.id).executeAsOne()
        database.newsQueries.updateNews(row.url, row.title, row.author, row.date, row.cover_url,
          row.description, row.content_html, -100L + index, row.last_updated, row.id)
      }
      assertTrue(repository.getNewsList().first().take(fixtures.size).map { it.id } == fixtures.map { it.id })
      if (leaveFixtures) exportFixtures(context.getDatabasePath("vlr"),
        checkNotNull(context.getExternalFilesDir(null)))
    } finally {
      app.close()
      driver.close()
      if (!leaveFixtures) SQLiteDatabase.deleteDatabase(context.getDatabasePath("vlr"))
    }
  }

  private fun exportFixtures(databaseFile: File, directory: File) {
    val ids = fixtures.joinToString(",") { sqlValue(it.id) }
    SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
      val statements = buildList {
        add("BEGIN;")
        add("DELETE FROM news_media WHERE news_id IN ($ids);")
        add("DELETE FROM news WHERE id IN ($ids);")
        for ((table, filter) in listOf("news" to "id", "news_media" to "news_id")) {
          database.rawQuery("SELECT * FROM $table WHERE $filter IN ($ids)", null).use { cursor ->
            val indexes = (0 until cursor.columnCount).filter { table != "news_media" || cursor.getColumnName(it) != "id" }
            val columns = indexes.joinToString(",") { cursor.getColumnName(it) }
            while (cursor.moveToNext()) {
              val values = indexes.joinToString(",") { sqlValue(cursor.getString(it)) }
              add("INSERT INTO $table ($columns) VALUES ($values);")
            }
          }
        }
        add("COMMIT;")
      }
      File(directory, "news-payload-fixtures.sql").writeText(statements.joinToString("\n", postfix = "\n"))
      File(directory, "news-payload-fixtures.jsonl").writeText(fixtures.joinToString("\n") { it.payload })
    }
  }

  private fun sqlValue(value: String?): String = value?.let { "'${it.replace("'", "''")}'" } ?: "NULL"

  private data class Fixture(
    val id: String,
    val title: String,
    val payload: String,
    val links: List<Pair<String, String>>,
    val blockCount: Int = 0,
  )

  private val fixtures = listOf(
    fixture("99990001", "Full source links", """[{"text":"Official announcement","url":"https://www.vlr.gg/"},{"text":"Team roster","url":"https://www.vlr.gg/teams"}]""",
      listOf("Official announcement" to "https://www.vlr.gg/", "Team roster" to "https://www.vlr.gg/teams")),
    fixture("99990002", "Partial source links", """[{"url":"https://www.vlr.gg/"},{"text":"A source without a URL"},{}]""",
      listOf("" to "https://www.vlr.gg/", "A source without a URL" to "", "" to "")),
    fixture("99990003", "Empty source links", "[]", emptyList()),
    fixture("99990004", "Unknown source fields", """[{"text":"Known source fields retained","url":"https://www.vlr.gg/","extra":{"category":"official"}}]""",
      listOf("Known source fields retained" to "https://www.vlr.gg/"), extra = """, "future":{"enabled":true}"""),
    fixture("99990005", "Structured article blocks", """[{"text":"Structured source","url":"https://www.vlr.gg/"}]""",
      listOf("Structured source" to "https://www.vlr.gg/"), blockCount = 3,
      extra = """, "blocks":[{"type":"heading","level":2,"runs":[{"text":"Roster update"}]},{"type":"paragraph","runs":[{"text":"A typed article paragraph with "},{"text":"an inline source","url":"https://www.vlr.gg/","bold":true}]},{"type":"list","ordered":false,"children":[{"type":"list_item","runs":[{"text":"First roster detail"}]},{"type":"list_item","runs":[{"text":"Second roster detail"}]}]}]"""),
  )

  private fun fixture(
    id: String,
    title: String,
    links: String,
    expectedLinks: List<Pair<String, String>>,
    blockCount: Int = 0,
    extra: String = "",
  ): Fixture {
    val references = expectedLinks.indices.joinToString("\\n\\n") { "Source ${it + 1}: {{link_$it}}" }
    val content = "News payload verification for $title.\\n\\n$references"
    return Fixture(id, title,
      """{"id":"$id","title":"$title","content":"$content","author":"Payload validation","date":"2026-10-01","links":$links$extra}""",
      expectedLinks, blockCount)
  }
}
