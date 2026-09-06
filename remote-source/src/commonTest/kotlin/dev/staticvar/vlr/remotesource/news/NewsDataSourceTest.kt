/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.news

import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NewsDataSourceTest {
  @Test
  fun resolves_provider_players_only_against_api_origin_and_preserves_nested_metadata() = runTest {
    val client = mockClient {
      respond("""{"blocks":[
        {"type":"video","player":{"provider":"youtube","media_id":"vbBd_Hu6o2M","player_url":"/media/youtube/vbBd_Hu6o2M","external_url":"https://wrong.example"}},
        {"type":"blockquote","children":[{"type":"video","player":{"provider":"twitch","media_id":"Example-Clip_12","player_url":"/media/twitch/Example-Clip_12","external_url":"https://wrong.example"}}]},
        {"type":"video","player":{"provider":"youtube","media_id":"vbBd_Hu6o2M","player_url":"https://wrong.example/player","external_url":"https://wrong.example"}}
      ]}""", headers = jsonHeaders())
    }.config { defaultRequest { url("https://api.example:8443") } }
    try {
      val blocks = NewsDataSourceImpl(client).article("1").getOrThrow().blocks
      assertEquals("https://api.example:8443/media/youtube/vbBd_Hu6o2M", blocks[0].player?.playerUrl)
      assertEquals("https://www.youtube.com/watch?v=vbBd_Hu6o2M", blocks[0].player?.externalUrl)
      assertEquals("https://api.example:8443/media/twitch/Example-Clip_12", blocks[1].children[0].player?.playerUrl)
      assertEquals("https://clips.twitch.tv/Example-Clip_12", blocks[1].children[0].player?.externalUrl)
      assertEquals(null, blocks[2].player)
    } finally {
      client.close()
    }
  }

  @Test
  fun decodes_real_backend_interview_with_video_before_questions_and_quoted_answers() = runTest {
    val client = singleResponseClient(readFixture("news_structured_interview_748106.json"))
    try {
      val article = NewsDataSourceImpl(client).article("748106").getOrThrow()
      assertEquals(14, article.blocks.size)
      assertTrue(article.blocks[0].runs.all { it.italic })
      assertEquals("https://youtu.be/vbBd_Hu6o2M", article.blocks[0].runs[1].url)
      assertEquals("video", article.blocks[1].type)
      assertEquals("paragraph", article.blocks[2].type)
      assertTrue(article.blocks[2].runs.all { it.bold })
      assertEquals("blockquote", article.blocks[3].type)
      assertTrue(article.blocks[3].children.single().runs.first().text.startsWith("It's definitely"))
    } finally {
      client.close()
    }
  }

  @Test
  fun decodes_structured_blocks_in_wire_order_with_nullable_and_omitted_defaults() = runTest {
    val client = mockClient {
      respond("""{"id":"750541","blocks":[
        {"type":"paragraph","level":null,"url":null,"runs":[{"text":"Intro","italic":true}]},
        {"type":"image","url":"https://example.com/photo.jpg","alt":"Team"},
        {"type":"caption","runs":[{"text":"Credit"}]},
        {"type":"heading","level":2,"runs":[{"text":"Interview"}]},
        {"type":"blockquote","children":[{"type":"paragraph","runs":[{"text":"Question","bold":true,"url":"https://example.com"}]}]},
        {"type":"list","ordered":true,"start":3,"children":[{"type":"list_item","children":[{"type":"paragraph","runs":[{"text":"Answer"}]}]}]},
        {"type":"video","url":"https://example.com/video"}
      ]}""", headers = jsonHeaders())
    }
    try {
      val article = NewsDataSourceImpl(client).article("750541").getOrThrow()
      assertEquals(listOf("paragraph", "image", "caption", "heading", "blockquote", "list", "video"), article.blocks.map { it.type })
      assertEquals(ArticleTextRunDto("Intro", italic = true), article.blocks.first().runs.single())
      assertEquals(true, article.blocks[4].children.single().runs.single().bold)
      assertEquals("Answer", article.blocks[5].children.single().children.single().runs.single().text)
      assertEquals(3, article.blocks[5].start)
    } finally {
      client.close()
    }
  }

  @Test
  fun requests_only_the_api_with_numeric_id_and_preserves_the_payload() = runTest {
    var requests = 0
    val client = mockClient { request ->
      requests++
      assertEquals("/api/v1/news/750541", request.url.encodedPath)
      respond(
        """{"id":"750541","title":"Server title","author":"Server author","content":"Opening {{link_0}}\n\n{image_0}\n\n{video_0}","links":[{"text":"Team","url":"https://www.vlr.gg/team/1001"}],"images":["https://example.com/photo.jpg"],"videos":["https://example.com/video"]}""",
        headers = jsonHeaders(),
      )
    }
    try {
      val source = NewsDataSourceImpl(client)
      listOf("750541", "750541/team-heretics", "https://www.vlr.gg/750541/team-heretics").forEach { id ->
        val article = source.article(id).getOrThrow()
        assertEquals("Opening {{link_0}}\n\n{image_0}\n\n{video_0}", article.content)
        assertEquals("https://www.vlr.gg/team/1001", article.links.single()["url"])
        assertEquals("Server author", article.author)
      }
      assertEquals(3, requests)
    } finally {
      client.close()
    }
  }

  @Test
  fun failed_or_empty_api_responses_never_trigger_a_website_request() = runTest {
    var requests = 0
    val client = mockClient {
      requests++
      if (requests == 1) respond("Unavailable", HttpStatusCode.ServiceUnavailable)
      else respond("""{"id":"1","content":""}""", headers = jsonHeaders())
    }
    try {
      val source = NewsDataSourceImpl(client)
      assertTrue(source.article("1").isFailure)
      assertEquals("", source.article("1").getOrThrow().content)
      assertEquals(2, requests)
    } finally {
      client.close()
    }
  }

  @Test
  fun cancellation_is_propagated() = runTest {
    val client = mockClient { throw CancellationException("Cancelled") }
    try {
      assertFailsWith<CancellationException> { NewsDataSourceImpl(client).article("1") }
    } finally {
      client.close()
    }
  }
}
