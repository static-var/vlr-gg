/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

internal const val NavigationEntryScopeQualifier: String = "navigation-entry"

internal val AppRoute.navigationScopeId: String
  get() = "${NavigationEntryScopeQualifier}:${this::class.simpleName}:$this"
