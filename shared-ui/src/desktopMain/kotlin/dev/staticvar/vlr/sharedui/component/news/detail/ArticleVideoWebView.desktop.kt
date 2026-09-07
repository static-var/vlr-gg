/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal actual fun supportsArticleVideoWebView(): Boolean = false

@Composable
internal actual fun ArticleVideoWebView(
  playerUrl: String,
  modifier: Modifier,
  contentScale: Float,
  onError: () -> Unit,
) {
  androidx.compose.runtime.LaunchedEffect(playerUrl) { onError() }
}
