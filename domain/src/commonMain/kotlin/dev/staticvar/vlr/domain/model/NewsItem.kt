/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for a news item in the news list.
 */
data class NewsItem(
  val id: String,
  val url: String,
  val title: String,
  val description: String,
  val date: String,
  val author: String,
  val coverUrl: String,
)
