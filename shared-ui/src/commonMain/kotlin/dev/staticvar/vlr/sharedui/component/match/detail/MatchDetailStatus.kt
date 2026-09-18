/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.vlr.domain.model.MatchStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_status_completed
import vlr.shared_ui.generated.resources.format_status_live
import vlr.shared_ui.generated.resources.format_status_ongoing
import vlr.shared_ui.generated.resources.format_status_paused
import vlr.shared_ui.generated.resources.format_status_unknown
import vlr.shared_ui.generated.resources.format_status_upcoming

internal val String?.matchDetailStatusLabel: String
  @Composable
  get() = stringResource(matchDetailStatusLabelResource)

internal val String?.matchDetailStatusLabelResource: StringResource
  get() = when (orEmpty().trim().lowercase()) {
    "live" -> Res.string.format_status_live
    "ongoing" -> Res.string.format_status_ongoing
    "paused" -> Res.string.format_status_paused
    "upcoming", "tbd" -> Res.string.format_status_upcoming
    "completed", "final" -> Res.string.format_status_completed
    else -> Res.string.format_status_unknown
  }

internal val MatchStatus.matchDetailStatusLabel: String
  @Composable
  get() = when (this) {
    MatchStatus.LIVE -> stringResource(Res.string.format_status_live)
    MatchStatus.UPCOMING -> stringResource(Res.string.format_status_upcoming)
    MatchStatus.COMPLETED -> stringResource(Res.string.format_status_completed)
    MatchStatus.UNKNOWN -> stringResource(Res.string.format_status_unknown)
  }

internal val String?.matchDetailStatusTagStyle: PrismTagStyle
  get() = when (orEmpty().trim().lowercase()) {
    "live",
    "ongoing",
    -> PrismTagStyle.Danger

    "upcoming", "tbd" -> PrismTagStyle.Info

    "completed", "final" -> PrismTagStyle.Success

    else -> PrismTagStyle.Neutral
  }

internal val MatchStatus.matchDetailStatusTagStyle: PrismTagStyle
  get() = when (this) {
    MatchStatus.LIVE -> PrismTagStyle.Danger
    MatchStatus.UPCOMING -> PrismTagStyle.Info
    MatchStatus.COMPLETED -> PrismTagStyle.Success
    MatchStatus.UNKNOWN -> PrismTagStyle.Neutral
  }
