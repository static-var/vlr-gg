/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.refresh

data class RefreshState(
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)
