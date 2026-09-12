/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.presentation

public data class HomeUiState(
  public val feed: HomeFeed = HomeFeed(),
  public val hasLoadedFeed: Boolean = false,
  public val isLoading: Boolean = true,
  public val isRefreshing: Boolean = false,
  public val errorMessage: String? = null,
  public val errorDetails: String? = null,
)
