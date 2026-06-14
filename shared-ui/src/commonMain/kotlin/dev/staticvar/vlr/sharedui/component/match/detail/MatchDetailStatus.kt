/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import dev.staticvar.designsystem.component.tag.PrismTagStyle

internal val String?.matchDetailStatusLabel: String
  get() = orEmpty()
    .trim()
    .takeUnless(String::isBlank)
    ?.uppercase()
    ?: "UNKNOWN"

internal val String?.matchDetailStatusTagStyle: PrismTagStyle
  get() = when (orEmpty().lowercase()) {
    "live",
    "ongoing",
    -> PrismTagStyle.Danger

    "upcoming" -> PrismTagStyle.Info

    "completed" -> PrismTagStyle.Success

    else -> PrismTagStyle.Neutral
  }
