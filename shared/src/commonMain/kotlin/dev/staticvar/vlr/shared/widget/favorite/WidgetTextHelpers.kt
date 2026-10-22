/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

internal fun meaningfulText(value: String?): String? = value?.trim()?.takeUnless {
  it.isEmpty() || it == "-" || it.equals("TBD", ignoreCase = true)
}

internal fun nameKey(value: String): String = value.trim().lowercase()
