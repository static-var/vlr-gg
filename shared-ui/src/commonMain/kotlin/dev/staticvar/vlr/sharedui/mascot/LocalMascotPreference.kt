/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/** Null disables mascot appearances throughout the app. */
public val LocalMascotCharacter: ProvidableCompositionLocal<MascotCharacter?> =
  compositionLocalOf { MascotCharacter.Lynx }
