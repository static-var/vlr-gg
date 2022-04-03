package dev.staticvar.vlr.data

data class NewsArticle(
  val title: String = "",
  val authorName: String = "",
  val time: String = "",
  val news: List<HtmlDataType>? = null
)
