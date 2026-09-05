/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
public sealed interface AppRoute : NavKey {
  public sealed interface Root : AppRoute

  @Serializable
  public data object News : Root

  @Serializable
  public data object Matches : Root

  @Serializable
  public data object Events : Root

  @Serializable
  public data object Rankings : Root

  @Serializable
  public data object About : Root

  @Serializable
  public data object Settings : AppRoute

  @Serializable
  public data class MatchDetails(val matchId: String) : AppRoute

  @Serializable
  public data class EventDetails(val eventId: String) : AppRoute

  @Serializable
  public data class NewsArticle(val articleId: String) : AppRoute

  @Serializable
  public data class TeamDetails(val teamId: String) : AppRoute

  @Serializable
  public data class PlayerDetails(val playerId: String) : AppRoute
}

public val AppRoute.rootDestination: AppRoute
  get() =
    when (this) {
      AppRoute.News,
      AppRoute.Matches,
      AppRoute.Events,
      AppRoute.Rankings,
      AppRoute.About,
      -> this

      AppRoute.Settings -> AppRoute.About

      is AppRoute.MatchDetails -> AppRoute.Matches

      is AppRoute.EventDetails -> AppRoute.Events

      is AppRoute.NewsArticle -> AppRoute.News

      is AppRoute.TeamDetails -> AppRoute.Rankings

      is AppRoute.PlayerDetails -> AppRoute.Rankings
    }
