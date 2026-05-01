package dev.staticvar.vlr.remotesource.news

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewsHtmlParserTest {

  @Test
  fun parse_preserves_list_markup_and_removes_noise_nodes() {
    val html =
      """
        <html>
          <body>
            <div class="article-header">
              <div class="wf-title">Test Title</div>
              <div class="article-meta-author">Alice</div>
              <div class="js-date-toggle">2026-02-16</div>
            </div>
            <div class="article-body">
              <p>Intro</p>
              <ul>
                <li>One</li>
                <li>Two</li>
              </ul>
              <span class="wf-hover-card"><a href="https://example.com/hover">hover</a></span>
              <style>.x { color: red; }</style>
            </div>
          </body>
        </html>
      """.trimIndent()

    val dto = NewsHtmlParser.parse(articleId = "story", html = html)

    assertTrue(dto.content.contains("<ul"), "Expected list markup to be preserved")
    assertTrue(dto.content.contains("<li"), "Expected list item markup to be preserved")
    assertFalse(dto.content.contains("wf-hover-card"), "Expected hover cards to be removed from stored content")
    assertFalse(dto.content.contains("<style"), "Expected style tags to be removed from stored content")
  }

  @Test
  fun parse_filters_internal_vlr_links_from_reference_list() {
    val html =
      """
        <html>
          <body>
            <div class="article-body">
              <p>
                <a href="https://example.com">External</a>
                <a href="/team/123">Internal</a>
                <a href="mailto:test@example.com">Mail</a>
              </p>
            </div>
          </body>
        </html>
      """.trimIndent()

    val dto = NewsHtmlParser.parse(articleId = "story", html = html)
    val hrefs = dto.links.mapNotNull { it["href"] }

    assertTrue("https://example.com" in hrefs, "Expected external link to be included")
    assertFalse(hrefs.any { it.contains("vlr.gg/team/123") }, "Expected internal vlr.gg link to be excluded")
    assertFalse(hrefs.any { it.startsWith("mailto:") }, "Expected mailto links to be excluded")
  }
}

