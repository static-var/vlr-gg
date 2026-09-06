/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.runtime.Immutable

/** A labelled choice in [PrismSegmentedButtons]. IDs must be unique within a group. */
@Immutable
public data class PrismSegmentedButtonOption(val id: String, val label: String, val enabled: Boolean = true)
