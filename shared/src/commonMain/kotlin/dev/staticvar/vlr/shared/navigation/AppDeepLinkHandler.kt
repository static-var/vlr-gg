/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Receives app URLs from a platform host and keeps the latest navigation request. */
public class AppDeepLinkHandler {
  private val mutableState = MutableStateFlow(AppDeepLinkState())

  internal val state: StateFlow<AppDeepLinkState> = mutableState.asStateFlow()

  /** Returns whether [url] is a supported Valorant Esports app URL. */
  public fun openUrl(url: String): Boolean {
    val destination = parseAppDeepLink(url) ?: return false
    mutableState.update { current ->
      val requestId = current.lastIssuedId + 1L
      AppDeepLinkState(
        lastIssuedId = requestId,
        pendingRequest = AppDeepLinkRequest(id = requestId, destination = destination),
      )
    }
    return true
  }

  internal fun consume(requestId: Long) {
    mutableState.update { current ->
      if (current.pendingRequest?.id == requestId) {
        current.copy(pendingRequest = null)
      } else {
        current
      }
    }
  }
}

internal data class AppDeepLinkState(
  val lastIssuedId: Long = 0L,
  val pendingRequest: AppDeepLinkRequest? = null,
)

internal data class AppDeepLinkRequest(
  val id: Long,
  val destination: AppDeepLinkDestination,
) {
  fun navigate(appState: VlrAppState) {
    when (val target = destination) {
      AppDeepLinkDestination.Home -> appState.selectRoot(AppRoute.Home)
      is AppDeepLinkDestination.Match -> appState.showRootMatchDetails(target.matchId)
      is AppDeepLinkDestination.Event -> appState.showRootEventDetails(target.eventId)
      is AppDeepLinkDestination.Team -> appState.showRootTeamDetails(target.teamId)
      is AppDeepLinkDestination.Player -> appState.showRootPlayerDetails(target.playerId)
    }
  }
}

internal sealed interface AppDeepLinkDestination {
  data object Home : AppDeepLinkDestination

  data class Match(val matchId: String) : AppDeepLinkDestination

  data class Event(val eventId: String) : AppDeepLinkDestination

  data class Team(val teamId: String) : AppDeepLinkDestination

  data class Player(val playerId: String) : AppDeepLinkDestination
}

private val detailsDeepLink = Regex("^https://valorantesports\\.staticvar\\.dev/(match|event|team|player)/([0-9]+)(?:[?#].*)?$")
private val homeDeepLink = Regex("^https://valorantesports\\.staticvar\\.dev/(?:[?#].*)?$")

internal fun parseAppDeepLink(url: String): AppDeepLinkDestination? {
  val normalizedUrl = url.trim()
  if (homeDeepLink.matches(normalizedUrl)) {
    return AppDeepLinkDestination.Home
  }
  val match = detailsDeepLink.matchEntire(normalizedUrl) ?: return null
  val sourceId = match.groupValues[2]
  return when (match.groupValues[1]) {
    "match" -> AppDeepLinkDestination.Match(sourceId)
    "event" -> AppDeepLinkDestination.Event(sourceId)
    "team" -> AppDeepLinkDestination.Team(sourceId)
    "player" -> AppDeepLinkDestination.Player(sourceId)
    else -> null
  }
}
