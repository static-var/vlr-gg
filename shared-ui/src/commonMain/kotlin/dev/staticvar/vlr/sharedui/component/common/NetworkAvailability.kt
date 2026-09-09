/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.ProvidableCompositionLocal

public val LocalIsOnline: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }
