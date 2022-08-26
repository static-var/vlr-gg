package dev.staticvar.vlr.data

import com.github.michaelbull.result.Result
import dev.staticvar.vlr.data.api.response.TwitterOEmbed
import dev.staticvar.vlr.utils.runSuspendCatching
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object NewsParser {

  /**
   * Parser This method will receive the id of the webpage to be parsed and it will return a flow
   * which will emit [NewsArticle]
   *
   * @param id
   * @param json
   */
  fun parser(id: String, json: Json) =
    flow<Result<NewsArticle, Throwable>> {
      runSuspendCatching {
          val webpage = Jsoup.connect("https://www.vlr.gg/$id").get() // Read webpage
          val headerHtml =
            webpage.select(
              ".article-header"
            ) // Separate the section with author and top level article info
          val text = webpage.select(".article-body") // Separate body of the article

          NewsArticle(
            headerHtml.first()?.let {
              it
                .select(".wf-title")
                .first()
                ?.wholeText()
                ?.replace(Regex("\\s+"), " ")
                ?.trim() // Fetch title of the article
            }
              ?: "",
            headerHtml.first()?.let {
              it
                .select(".article-meta-author")
                .first()
                ?.wholeText()
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
            } // Fetch author name
             ?: "",
            headerHtml.first()?.let {
              it
                .select(".js-date-toggle")
                .first()
                ?.wholeText()
                ?.replace(Regex("\\s+"), " ")
                ?.trim() // Fetch publishing time of the article
            }
              ?: "",
            text.first()?.let { elements ->
              elements
                .select(".wf-hover-card")
                .remove() // Remove hover card from HTML before parsing
              elements.children().map { recursiveTextFinder(it, json) }.flatten()
            }
          )
        }
        .also { emit(it) }
    }

  /**
   * Recursive text finder the method ignores unknown and unnecessary document tags and reads data
   * from necessary tags recursively
   *
   * @param element
   * @param json
   * @return
   */
  private fun recursiveTextFinder(element: Element, json: Json): List<HtmlDataType> {
    return if (element.tagName() == "iframe")
      listOf(Video(element.attr("src"))) // Identify Videos / clips from the page
    else if (element.tagName() == "em")
      listOf(Subtext(element.wholeText().trim())) // Identify subtexts from the page
    else if (element.tagName() == "h1")
      listOf(Heading(element.wholeText().trim())) // Identify headings from the page
    else if (element.tagName() == "li")
      listOf<HtmlDataType>(
        ListItem(element.text().trim().trimStart('/'))
      ) // Identify list points from the page
    else if (element.tagName() == "blockquote")
      listOf<HtmlDataType>(Quote(element.wholeText().trim())) // Identify quotes from the page
    else if (
      element.tagName() == "p" && element.hasText() && element.wholeText().isNotBlank()
    ) // Identify paragraph text from the page
     listOf(Paragraph(element.wholeText().replace(Regex("\\s+"), " ").trim()))
    else if (
      element.tagName() == "div" && element.select(".tweet").isNotEmpty()
    ) { // Identify tags which contain tweet
      val url = element.attr("data-url")
      listOf(Tweet(getTweetEmbed(json, url).html))
    } else if (element.childrenSize() != 0) {
      element
        .children()
        .map { recursiveTextFinder(it, json) }
        .flatten() // Iterate children elements and flatten the list
    } else {
      if (
        element.tagName() == "p" && element.hasText() && element.wholeText().isNotBlank()
      ) // Identify paragraph text which were not identified before from the page
       listOf(Paragraph(element.text().replace(Regex("\\s+"), " ").trim()))
      else if (element.text().isBlank()) listOf() else listOf(Unknown(element.html()))
    }
  }

  /**
   * Get tweet embed
   *
   * @param json
   * @param url
   * @return
   */
  private fun getTweetEmbed(json: Json, url: String): TwitterOEmbed {
    val content =
      Jsoup.connect("https://publish.twitter.com/oembed?url=$url").ignoreContentType(true).execute()
    val data = content.body()
    return json.decodeFromString<TwitterOEmbed>(data)
  }
}
