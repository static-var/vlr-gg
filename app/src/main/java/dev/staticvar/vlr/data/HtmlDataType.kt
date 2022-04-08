package dev.staticvar.vlr.data

sealed class HtmlDataType

data class Paragraph(val text: String) : HtmlDataType()

data class ListItem(val text: String) : HtmlDataType()

data class Tweet(val tweetUrl: String) : HtmlDataType()

data class Unknown(val text: String) : HtmlDataType()

data class Heading(val text: String) : HtmlDataType()

data class Video(val link: String) : HtmlDataType()

data class Subtext(val text: String) : HtmlDataType()

data class Quote(val text: String) : HtmlDataType()
