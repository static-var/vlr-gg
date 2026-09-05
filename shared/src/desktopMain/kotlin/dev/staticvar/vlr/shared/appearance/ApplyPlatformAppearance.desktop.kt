/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.appearance

import androidx.compose.runtime.Composable

/** Desktop has no mobile status or navigation bars to synchronize. */
@Composable
internal actual fun ApplyPlatformAppearance(isDark: Boolean, followSystem: Boolean) = Unit
