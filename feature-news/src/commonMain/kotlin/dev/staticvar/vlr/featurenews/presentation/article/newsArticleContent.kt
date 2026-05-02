/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.article

private val HtmlTagRegex: Regex = Regex("<[^>]+>")
private val HtmlEntityRegex: Regex = Regex("&nbsp;|&amp;|&quot;|&#39;")

internal fun articleParagraphs(contentHtml: String): List<String> {
  if (contentHtml.isBlank()) {
    return emptyList()
  }

  val withLineBreaks =
    contentHtml
      .replace(Regex("(?i)<br\\s*/?>"), "\n")
      .replace(Regex("(?i)</p\\s*>"), "\n")
      .replace(Regex("(?i)</h[1-6]\\s*>"), "\n")
      .replace(Regex("(?i)</blockquote\\s*>"), "\n")
      .replace(Regex("(?i)</li\\s*>"), "\n")
      // Keep list semantics (otherwise <li> collapses into a single paragraph).
      .replace(Regex("(?i)<li(\\s+[^>]*)?>"), "\n- ")

  return withLineBreaks
    .split('\n')
    .map { paragraph -> paragraph.stripHtml() }
    .filter { paragraph -> paragraph.isNotBlank() }
}

private fun String.stripHtml(): String = replace(HtmlTagRegex, " ")
  .replace(HtmlEntityRegex) { match ->
    when (match.value) {
      "&nbsp;" -> " "
      "&amp;" -> "&"
      "&quot;" -> "\""
      "&#39;" -> "'"
      else -> match.value
    }
  }
  .replace(Regex("\\s+"), " ")
  .trim()
