/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.refresh

data class RefreshState(
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
  val hasCompleted: Boolean = false,
) {
  fun isLoading(hasContent: Boolean): Boolean = !hasContent && (!hasCompleted || isRefreshing)
}
