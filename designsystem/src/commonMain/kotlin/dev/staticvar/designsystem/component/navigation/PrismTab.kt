/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.runtime.Immutable

/**
 * Declarative model for a standard tab item.
 *
 * @param count Optional number of items behind the tab, shown beside the label. Leave it null while
 * the number is unknown.
 */
@Immutable
public data class PrismTab(val id: String, val label: String, val enabled: Boolean = true, val count: Int? = null)
