package dev.staticvar.vlr.featurenews.presentation.list

import dev.staticvar.vlr.domain.model.NewsItem

public data class NewsListUiState(
  val items: List<NewsItem> = emptyList(),
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)
