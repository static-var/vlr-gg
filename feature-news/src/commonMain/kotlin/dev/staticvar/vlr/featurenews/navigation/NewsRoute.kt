/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.navigation

public sealed interface NewsRoute {
  public data object List : NewsRoute

  public data class Article(val articleId: String) : NewsRoute
}
