/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal expect fun supportsArticleVideoWebView(): Boolean

@Composable
internal expect fun ArticleVideoWebView(
  playerUrl: String,
  modifier: Modifier = Modifier,
  contentScale: Float = 1f,
  onError: () -> Unit,
)
