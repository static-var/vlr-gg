/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.runtime.Immutable

/**
 * Declarative model for a segmented filter tab.
 */
@Immutable
public data class PrismSegmentedFilterTab(val id: String, val label: String, val enabled: Boolean = true)
