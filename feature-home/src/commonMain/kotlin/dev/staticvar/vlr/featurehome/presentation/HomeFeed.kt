/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.presentation

import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.MatchPreview

public data class HomeFeed(
  public val directFavorites: DirectFavoriteSnapshot = DirectFavoriteSnapshot(),
  public val hasDirectFavorites: Boolean = directFavorites.hasAny,
  public val personalizedMatches: List<MatchPreview> = emptyList(),
  public val personalizedEvents: List<EventPreview> = emptyList(),
)
