package dev.staticvar.vlr.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
public sealed interface AppRoute : NavKey {
  @Serializable
  public data object News : AppRoute

  @Serializable
  public data object Matches : AppRoute

  @Serializable
  public data object Events : AppRoute

  @Serializable
  public data object Rankings : AppRoute

  @Serializable
  public data object About : AppRoute

  @Serializable
  public data class MatchDetails(
    val matchId: String,
  ) : AppRoute

  @Serializable
  public data class EventDetails(
    val eventId: String,
  ) : AppRoute

  @Serializable
  public data class TeamDetails(
    val teamId: String,
  ) : AppRoute

  @Serializable
  public data class PlayerDetails(
    val playerId: String,
  ) : AppRoute
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
      is AppRoute.MatchDetails -> AppRoute.Matches
      is AppRoute.EventDetails -> AppRoute.Events
      is AppRoute.TeamDetails -> AppRoute.Rankings
      is AppRoute.PlayerDetails -> AppRoute.Rankings
    }
