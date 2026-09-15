/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_status_unknown

internal val String?.matchDetailStatusLabel: String
  @Composable
  get() = orEmpty()
    .trim()
    .takeUnless(String::isBlank)
    ?.uppercase()
    ?: stringResource(Res.string.format_status_unknown)

internal val String?.matchDetailStatusTagStyle: PrismTagStyle
  get() = when (orEmpty().lowercase()) {
    "live",
    "ongoing",
    -> PrismTagStyle.Danger

    "upcoming" -> PrismTagStyle.Info

    "completed" -> PrismTagStyle.Success

    else -> PrismTagStyle.Neutral
  }
