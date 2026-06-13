/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.dropdown

import androidx.compose.runtime.Immutable

/**
 * Declarative model for a Prism dropdown option.
 */
@Immutable
public data class PrismDropdownOption(val id: String, val label: String, val enabled: Boolean = true)
