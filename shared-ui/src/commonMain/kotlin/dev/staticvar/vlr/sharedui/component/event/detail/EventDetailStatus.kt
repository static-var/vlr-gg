/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.vlr.domain.model.EventStatus

internal val EventStatus.eventDetailLabel: String
  get() = when (this) {
    EventStatus.ONGOING -> "ONGOING"
    EventStatus.UPCOMING -> "UPCOMING"
    EventStatus.COMPLETED -> "COMPLETED"
    EventStatus.UNKNOWN -> "UNKNOWN"
  }

internal val EventStatus.eventDetailTagStyle: PrismTagStyle
  get() = when (this) {
    EventStatus.ONGOING -> PrismTagStyle.Danger
    EventStatus.UPCOMING -> PrismTagStyle.Info
    EventStatus.COMPLETED -> PrismTagStyle.Success
    EventStatus.UNKNOWN -> PrismTagStyle.Neutral
  }

internal val String.eventMatchStatusTagStyle: PrismTagStyle
  get() = when (lowercase()) {
    "live", "ongoing" -> PrismTagStyle.Danger
    "upcoming" -> PrismTagStyle.Info
    "completed" -> PrismTagStyle.Success
    else -> PrismTagStyle.Neutral
  }
