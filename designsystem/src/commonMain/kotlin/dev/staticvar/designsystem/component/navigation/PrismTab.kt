/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.runtime.Immutable

/**
 * Declarative model for a standard tab item.
 */
@Immutable
public data class PrismTab(val id: String, val label: String, val enabled: Boolean = true)
