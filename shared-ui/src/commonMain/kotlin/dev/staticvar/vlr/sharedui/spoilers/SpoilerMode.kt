/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.spoilers

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** One score-visibility preference and action shared by every app screen. */
@Immutable
public data class SpoilerMode(val enabled: Boolean, val onToggle: () -> Unit)

public val LocalSpoilerMode: androidx.compose.runtime.ProvidableCompositionLocal<SpoilerMode> =
  staticCompositionLocalOf { SpoilerMode(enabled = false, onToggle = {}) }
