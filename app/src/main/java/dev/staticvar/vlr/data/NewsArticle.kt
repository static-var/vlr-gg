/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class NewsArticle(
  val title: String = "",
  val authorName: String = "",
  val time: String = "",
  val news: List<HtmlDataType>? = null,
)
