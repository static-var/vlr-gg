package dev.staticvar.vlr.featurenews.navigation

public sealed interface NewsRoute {
  public data object List : NewsRoute

  public data class Article(
    val articleId: String,
  ) : NewsRoute
}
