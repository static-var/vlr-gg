/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.localsource.database.Event_overview
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.localsource.database.GetEventOverviewWithFavoriteStatus
import dev.staticvar.vlr.remotesource.events.EventListDto

internal fun EventListDto.toOverviewEntity(): Event_overview = Event_overview(
  id = id,
  name = title,
  status = status?.name ?: "UNKNOWN",
  prizes = prize,
  dates = dates,
  region = location.ifBlank { null },
  logo_url = img,
)

internal fun GetEventOverviewWithFavoriteStatus.toEventPreview(): EventPreview = EventPreview(
  id = id,
  title = name,
  status = EventStatus.entries.firstOrNull { it.name == status?.uppercase() } ?: EventStatus.UNKNOWN,
  prize = prizes,
  dates = dates,
  region = region.orEmpty(),
  logoUrl = logo_url,
  isFavorite = is_favorite == 1L,
)
