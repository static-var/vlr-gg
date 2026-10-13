/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.reorder

import androidx.compose.runtime.Immutable

/**
 * One row of a [PrismReorderableOptionList].
 *
 * [key] identifies the row across reorders. [toggleEnabled] only controls the checkbox: a row that
 * cannot be toggled keeps its [checked] value and can still be moved.
 */
@Immutable
public data class PrismReorderableOption<out K : Any>(
  val key: K,
  val title: String,
  val description: String,
  val checked: Boolean,
  val toggleEnabled: Boolean = true,
)
