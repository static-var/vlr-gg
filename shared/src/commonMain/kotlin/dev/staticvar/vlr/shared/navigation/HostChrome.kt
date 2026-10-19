/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp

internal data class HostChrome(val barSide: WindowInsetsSides?, val barTopReserved: Dp, val hinge: Rect?)

internal val LocalHostChrome = compositionLocalOf<HostChrome?> { null }
