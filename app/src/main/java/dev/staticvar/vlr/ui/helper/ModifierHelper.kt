/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.ui.helper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.takeIf(condition: Boolean, block: @Composable Modifier.() -> Modifier): Modifier = if (condition) {
  block(this)
} else {
  this
}

@Composable
fun Modifier.takeIfNot(condition: Boolean, block: @Composable Modifier.() -> Modifier): Modifier = if (!condition) {
  block(this)
} else {
  this
}
