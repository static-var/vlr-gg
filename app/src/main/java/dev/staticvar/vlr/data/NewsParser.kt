package dev.staticvar.vlr.data

import dev.staticvar.vlr.data.api.response.TwitterOEmbed
import dev.staticvar.vlr.utils.Operation
import dev.staticvar.vlr.utils.Pass
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object NewsParser {
  fun parser(id: String, json: Json) =
    flow<Operation<NewsArticle>> {
      val webpage = Jsoup.connect("https://www.vlr.gg/$id").get()
      val headerHtml = webpage.select(".article-header")
      val text = webpage.select(".article-body")
      NewsArticle(
          headerHtml.first()?.let {
            it.select(".wf-title").first()?.wholeText()?.replace(Regex("\\s+"), " ")?.trim()
          }
            ?: "",
          headerHtml.first()?.let {
            it
              .select(".article-meta-author")
              .first()
              ?.wholeText()
              ?.replace(Regex("\\s+"), " ")
              ?.trim()
          }
            ?: "",
          headerHtml.first()?.let {
            it.select(".js-date-toggle").first()?.wholeText()?.replace(Regex("\\s+"), " ")?.trim()
          }
            ?: "",
          text.first()?.let { elements ->
            elements.select(".wf-hover-card").remove()
            elements.children().map { recursiveTextFinder(it, json) }.flatten()
          }
        )
        .also { emit(Pass(it)) }
    }

  private fun recursiveTextFinder(element: Element, json: Json): List<HtmlDataType> {
    return if (element.tagName() == "iframe") listOf(Video(element.attr("src")))
    else if (element.tagName() == "em") listOf(Subtext(element.wholeText().trim()))
    else if (element.tagName() == "h1") listOf(Heading(element.wholeText().trim()))
    else if (element.tagName() == "li")
      listOf<HtmlDataType>(ListItem(element.text().trim().trimStart('/')))
    else if (element.tagName() == "blockquote")
      listOf<HtmlDataType>(Quote(element.wholeText().trim()))
    else if (element.tagName() == "p" && element.hasText() && element.wholeText().isNotBlank())
      listOf(Paragraph(element.wholeText().replace(Regex("\\s+"), " ").trim()))
    else if (element.tagName() == "div" && element.select(".tweet").isNotEmpty()) {
      val url = element.attr("data-url")
      listOf(Tweet(getTweetEmbed(json, url).html))
    } else if (element.childrenSize() != 0) {
      element.children().map { recursiveTextFinder(it, json) }.flatten()
    } else {
      if (element.tagName() == "p" && element.hasText() && element.wholeText().isNotBlank())
        listOf(Paragraph(element.text().replace(Regex("\\s+"), " ").trim()))
      else if (element.text().isBlank()) listOf() else listOf(Unknown(element.html()))
    }
  }

  private fun getTweetEmbed(json: Json, url: String): TwitterOEmbed {
    val content =
      Jsoup.connect("https://publish.twitter.com/oembed?url=$url").ignoreContentType(true).execute()
    val data = content.body()
    return json.decodeFromString<TwitterOEmbed>(data)
  }
}
