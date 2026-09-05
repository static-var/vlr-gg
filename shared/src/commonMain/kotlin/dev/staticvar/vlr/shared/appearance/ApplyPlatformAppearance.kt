/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.appearance

import androidx.compose.runtime.Composable

/** Keeps native system controls consistent with the resolved app theme. */
@Composable
internal expect fun ApplyPlatformAppearance(isDark: Boolean, followSystem: Boolean)
