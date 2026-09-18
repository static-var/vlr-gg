/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.vlr.domain.model.EventStatus
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_status_completed
import vlr.shared_ui.generated.resources.format_status_live
import vlr.shared_ui.generated.resources.format_status_ongoing
import vlr.shared_ui.generated.resources.format_status_paused
import vlr.shared_ui.generated.resources.format_status_unknown
import vlr.shared_ui.generated.resources.format_status_upcoming

internal val EventStatus.eventDetailLabel: String
  @Composable
  get() = stringResource(eventDetailLabelResource)

internal val EventStatus.eventDetailLabelResource: org.jetbrains.compose.resources.StringResource
  get() = when (this) {
    EventStatus.ONGOING -> Res.string.format_status_ongoing
    EventStatus.PAUSED -> Res.string.format_status_paused
    EventStatus.UPCOMING -> Res.string.format_status_upcoming
    EventStatus.COMPLETED -> Res.string.format_status_completed
    EventStatus.UNKNOWN -> Res.string.format_status_unknown
  }

internal val EventStatus.eventDetailTagStyle: PrismTagStyle
  get() = when (this) {
    EventStatus.ONGOING -> PrismTagStyle.Danger
    EventStatus.PAUSED -> PrismTagStyle.Neutral
    EventStatus.UPCOMING -> PrismTagStyle.Info
    EventStatus.COMPLETED -> PrismTagStyle.Success
    EventStatus.UNKNOWN -> PrismTagStyle.Neutral
  }

internal val String.eventMatchStatusTagStyle: PrismTagStyle
  get() = when (trim().lowercase()) {
    "live", "ongoing" -> PrismTagStyle.Danger
    "upcoming", "tbd" -> PrismTagStyle.Info
    "completed", "final" -> PrismTagStyle.Success
    else -> PrismTagStyle.Neutral
  }

internal val String.eventMatchStatusLabel: String
  @Composable
  get() = stringResource(eventMatchStatusLabelResource)

internal val String.eventMatchStatusLabelResource: org.jetbrains.compose.resources.StringResource
  get() = when (trim().lowercase()) {
    "live" -> Res.string.format_status_live
    "ongoing" -> Res.string.format_status_ongoing
    "paused" -> Res.string.format_status_paused
    "upcoming", "tbd" -> Res.string.format_status_upcoming
    "completed", "final" -> Res.string.format_status_completed
    else -> Res.string.format_status_unknown
  }
