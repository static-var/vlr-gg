/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.staticvar.vlr.localsource.database.DatabaseDispatchers
import dev.staticvar.vlr.localsource.database.News
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class NewsQueriesTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: VlrDatabase

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VlrDatabase.Schema.create(driver)
        database = VlrDatabase(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    }

    @AfterTest
    fun teardown() { driver.close() }

    @Test
    fun insert_and_query_news() {
        val n = testNews("n1")
        database.newsQueries.insertNews(n)
        val res = database.newsQueries.getNewsById("n1").executeAsOne()
        assertEquals("n1", res.id)
    }

    @Test
    fun ordering_by_date_desc() {
        database.newsQueries.insertNews(testNews("n1", date = "2025-01-01"))
        database.newsQueries.insertNews(testNews("n2", date = "2025-02-01"))
        val list = database.newsQueries.getAllNews().executeAsList()
        assertEquals("n2", list.first().id)
    }

    @Test
    fun filter_by_author() {
        database.newsQueries.insertNews(testNews("n1", author = "Alice"))
        database.newsQueries.insertNews(testNews("n2", author = "Bob"))
        database.newsQueries.insertNews(testNews("n3", author = "Alice"))
        val alice = database.newsQueries.getNewsByAuthor("Alice").executeAsList()
        assertEquals(2, alice.size)
        assertTrue(alice.all { it.author == "Alice" })
    }

    @Test
    fun delete_news_cascades_media() {
        database.newsQueries.insertNews(testNews("n1"))
        database.newsQueries.insertNewsMedia("n1", media_type = "image", media_value = "img.png", media_text = null)
        database.newsQueries.deleteNewsById("n1")
        assertTrue(database.newsQueries.getNewsMedia("n1").executeAsList().isEmpty())
    }

    @Test
    fun media_crud() {
        database.newsQueries.insertNews(testNews("n1"))
        database.newsQueries.insertNewsMedia("n1", media_type = "image", media_value = "a.png", media_text = "Caption")
        val media = database.newsQueries.getNewsMedia("n1").executeAsList()
        assertEquals(1, media.size)
        database.newsQueries.deleteNewsMedia("n1")
        assertTrue(database.newsQueries.getNewsMedia("n1").executeAsList().isEmpty())
    }

    @Test
    fun flow_emits_news() = runTest {
        database.newsQueries.insertNews(testNews("n1"))
        val flow = database.newsQueries.getAllNews().asFlow().mapToList(DatabaseDispatchers.database)
        val firstEmission = flow.first()
        assertEquals(1, firstEmission.size)
    }

    @Test
    fun delete_all_news() {
        database.newsQueries.insertNews(testNews("n1"))
        database.newsQueries.insertNews(testNews("n2"))
        database.newsQueries.deleteAllNews()
        assertTrue(database.newsQueries.getAllNews().executeAsList().isEmpty())
    }

    private fun testNews(
        id: String,
        author: String = "Author",
        date: String = "2025-01-01"
    ): News = News(
        id = id,
        url = "https://example.com/$id",
        title = "Title $id",
        author = author,
        date = date,
        cover_url = "https://example.com/$id.png",
        description = null,
        content_html = null,
        list_position = null,
        last_updated = 0
    )
}
